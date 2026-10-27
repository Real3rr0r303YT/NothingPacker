/*
 * NothingPacker – offline APK ⇄ IPA toolkit for Android
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.real3rr0r303yt.nothingpacker.core.plist

import com.real3rr0r303yt.nothingpacker.core.binary.ByteReader
import com.real3rr0r303yt.nothingpacker.core.binary.MalformedFileException

/** Seconds since 2001-01-01T00:00:00Z (Apple reference date). */
data class PlistDate(val secondsSince2001: Double)

/** Opaque CFKeyedArchiver UID. */
data class PlistUid(val value: Long)

/**
 * Property list reader for Info.plist files: binary ("bplist00") and XML.
 * Values map to Map<String, Any?>, List<Any?>, String, Long, Double, Boolean,
 * ByteArray, [PlistDate] and [PlistUid].
 */
object Plist {

    private const val MAX_DEPTH = 128

    fun parse(bytes: ByteArray): Any? = when {
        bytes.size >= 8 && String(bytes, 0, 8, Charsets.US_ASCII) == "bplist00" -> parseBinary(bytes)
        else -> PlistXml.parse(String(bytes, Charsets.UTF_8))
    }

    @Suppress("UNCHECKED_CAST")
    fun parseDict(bytes: ByteArray): Map<String, Any?> =
        parse(bytes) as? Map<String, Any?> ?: throw MalformedFileException("Property list root is not a dictionary")

    // ---------------------------------------------------------------- binary

    private fun parseBinary(bytes: ByteArray): Any? {
        val r = ByteReader(bytes, littleEndian = false)
        if (bytes.size < 40) throw MalformedFileException("Binary plist too short")
        val t = bytes.size - 32
        val offsetSize = r.u8(t + 6)
        val refSize = r.u8(t + 7)
        val numObjects = r.uint(t + 8, 8)
        val topObject = r.uint(t + 16, 8)
        val offsetTable = r.uint(t + 24, 8)
        if (offsetSize !in 1..8 || refSize !in 1..8 || numObjects <= 0 || numObjects > bytes.size ||
            topObject >= numObjects || offsetTable + numObjects * offsetSize > t
        ) {
            throw MalformedFileException("Corrupt binary plist trailer")
        }
        val ctx = BinaryContext(r, offsetSize, refSize, numObjects.toInt(), offsetTable.toInt())
        return ctx.read(topObject.toInt(), 0)
    }

    private class BinaryContext(
        val r: ByteReader,
        val offsetSize: Int,
        val refSize: Int,
        val numObjects: Int,
        val offsetTable: Int,
    ) {
        fun objectOffset(ref: Int): Int {
            if (ref !in 0 until numObjects) throw MalformedFileException("Bad object reference $ref")
            return r.uint(offsetTable + ref * offsetSize, offsetSize).toInt()
        }

        fun read(ref: Int, depth: Int): Any? {
            if (depth > MAX_DEPTH) throw MalformedFileException("Property list nested too deeply")
            val off = objectOffset(ref)
            val marker = r.u8(off)
            val kind = marker shr 4
            val info = marker and 0xF
            return when (kind) {
                0x0 -> when (info) {
                    0x8 -> false
                    0x9 -> true
                    else -> null
                }
                0x1 -> readInt(off + 1, 1 shl info)
                0x2 -> when (info) {
                    2 -> java.lang.Float.intBitsToFloat(r.uint(off + 1, 4).toInt()).toDouble()
                    3 -> java.lang.Double.longBitsToDouble(r.uint(off + 1, 8))
                    else -> throw MalformedFileException("Bad real size")
                }
                0x3 -> PlistDate(java.lang.Double.longBitsToDouble(r.uint(off + 1, 8)))
                0x4 -> { val (n, start) = count(off, info); r.bytes(start, n) }
                0x5 -> { val (n, start) = count(off, info); String(r.bytes(start, n), Charsets.ISO_8859_1) }
                0x6 -> { val (n, start) = count(off, info); String(r.bytes(start, n * 2), Charsets.UTF_16BE) }
                0x8 -> PlistUid(r.uint(off + 1, info + 1))
                0xA, 0xC -> {
                    val (n, start) = count(off, info)
                    List(n) { read(r.uint(start + it * refSize, refSize).toInt(), depth + 1) }
                }
                0xD -> {
                    val (n, start) = count(off, info)
                    val map = LinkedHashMap<String, Any?>(n)
                    for (i in 0 until n) {
                        val key = read(r.uint(start + i * refSize, refSize).toInt(), depth + 1)?.toString() ?: continue
                        map[key] = read(r.uint(start + (n + i) * refSize, refSize).toInt(), depth + 1)
                    }
                    map
                }
                else -> throw MalformedFileException("Unknown plist marker 0x%02x".format(marker))
            }
        }

        private fun readInt(at: Int, bytes: Int): Long = when (bytes) {
            1, 2, 4 -> r.uint(at, bytes)
            8 -> r.uint(at, 8)
            16 -> r.uint(at + 8, 8)
            else -> throw MalformedFileException("Bad int size")
        }

        /** Returns (count, start of payload) for variable length objects. */
        private fun count(off: Int, info: Int): Pair<Int, Int> {
            if (info != 0xF) return info to off + 1
            val intMarker = r.u8(off + 1)
            if (intMarker shr 4 != 0x1) throw MalformedFileException("Bad length marker")
            val size = 1 shl (intMarker and 0xF)
            val n = readInt(off + 2, size)
            if (n < 0 || n > r.size) throw MalformedFileException("Bad length $n")
            return n.toInt() to off + 2 + size
        }
    }

    // ---------------------------------------------------------------- writing

    /** Serialises [value] as an XML property list (the format Xcode writes). */
    fun toXml(value: Any?): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n")
        append("<plist version=\"1.0\">\n")
        writeValue(value, 0)
        append("</plist>\n")
    }

    private fun StringBuilder.writeValue(value: Any?, depth: Int) {
        val indent = "\t".repeat(depth)
        when (value) {
            is Map<*, *> -> {
                append(indent).append("<dict>\n")
                value.forEach { (k, v) ->
                    append(indent).append("\t<key>").append(escape(k.toString())).append("</key>\n")
                    writeValue(v, depth + 1)
                }
                append(indent).append("</dict>\n")
            }
            is List<*> -> {
                append(indent).append("<array>\n")
                value.forEach { writeValue(it, depth + 1) }
                append(indent).append("</array>\n")
            }
            is Boolean -> append(indent).append(if (value) "<true/>" else "<false/>").append('\n')
            is Int, is Long -> append(indent).append("<integer>").append(value).append("</integer>\n")
            is Float, is Double -> append(indent).append("<real>").append(value).append("</real>\n")
            is ByteArray -> append(indent).append("<data>").append(java.util.Base64.getEncoder().encodeToString(value)).append("</data>\n")
            is PlistDate -> {
                val instant = java.time.Instant.ofEpochSecond(978_307_200L + value.secondsSince2001.toLong())
                append(indent).append("<date>").append(instant).append("</date>\n")
            }
            is PlistUid -> append(indent).append("<dict><key>CF\$UID</key><integer>").append(value.value).append("</integer></dict>\n")
            null -> append(indent).append("<string></string>\n")
            else -> append(indent).append("<string>").append(escape(value.toString())).append("</string>\n")
        }
    }

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
