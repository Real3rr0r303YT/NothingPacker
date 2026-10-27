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

package com.real3rr0r303yt.nothingpacker.core.arsc

import com.real3rr0r303yt.nothingpacker.core.axml.AxmlParser
import com.real3rr0r303yt.nothingpacker.core.axml.StringPool
import com.real3rr0r303yt.nothingpacker.core.binary.ByteReader
import com.real3rr0r303yt.nothingpacker.core.binary.MalformedFileException

/** A resolved resource value in one configuration. */
data class ResValue(val type: Int, val data: Int, val string: String?, val language: String, val density: Int)

/**
 * Minimal resources.arsc reader – just enough to resolve the app label and icon of an APK.
 * Indexes value locations once; values are decoded on demand.
 */
class ArscTable private constructor(
    private val reader: ByteReader,
    private val globalStrings: StringPool,
    /** resource id → list of (entry offset, config offset) */
    private val index: Map<Int, List<Pair<Int, Int>>>,
) {

    fun resolve(id: Int): List<ResValue> = index[id].orEmpty().mapNotNull { (entry, config) -> readValue(entry, config) }

    /** Best string for [id]: default locale first, otherwise any; follows one level of references. */
    fun resolveString(id: Int, depth: Int = 0): String? {
        val values = resolve(id)
        val best = values.firstOrNull { it.language.isEmpty() } ?: values.firstOrNull() ?: return null
        return when {
            best.type == AxmlParser.TYPE_REFERENCE && depth < 4 -> resolveString(best.data, depth + 1)
            best.string != null -> best.string
            else -> AxmlParser.formatValue(best.type, best.data)
        }
    }

    /** File paths (e.g. res/mipmap-xxxhdpi/ic_launcher.png) for [id], highest density first. */
    fun resolveFiles(id: Int, depth: Int = 0): List<String> = resolve(id)
        .sortedByDescending { if (it.density in 1..0xFFFD) it.density else 0 }
        .flatMap { v ->
            when {
                v.type == AxmlParser.TYPE_REFERENCE && depth < 4 -> resolveFiles(v.data, depth + 1)
                v.string != null -> listOf(v.string)
                else -> emptyList()
            }
        }
        .distinct()

    private fun readValue(entry: Int, config: Int): ResValue? = runCatching {
        val flags = reader.u16(entry + 2)
        val (type, data) = if (flags and FLAG_COMPACT != 0) {
            (flags shr 8) to reader.s32(entry + 4)
        } else {
            if (flags and FLAG_COMPLEX != 0) return@runCatching null
            val size = reader.u16(entry)
            reader.u8(entry + size + 3) to reader.s32(entry + size + 4)
        }
        val language = String(reader.bytes(config + 8, 2), Charsets.US_ASCII).trim('\u0000')
        val density = reader.u16(config + 14)
        ResValue(type, data, if (type == AxmlParser.TYPE_STRING) globalStrings.getOrNull(data) else null, language, density)
    }.getOrNull()

    companion object {
        private const val RES_TABLE_TYPE = 0x0002
        private const val RES_STRING_POOL_TYPE = 0x0001
        private const val RES_TABLE_PACKAGE_TYPE = 0x0200
        private const val RES_TABLE_TYPE_TYPE = 0x0201
        private const val FLAG_COMPLEX = 0x0001
        private const val FLAG_COMPACT = 0x0008
        private const val TYPE_FLAG_SPARSE = 0x01
        private const val TYPE_FLAG_OFFSET16 = 0x02
        private const val NO_ENTRY = -1

        fun parse(bytes: ByteArray): ArscTable {
            val r = ByteReader(bytes)
            if (r.u16(0) != RES_TABLE_TYPE) throw MalformedFileException("Not a resource table")
            val end = minOf(r.u32(4), bytes.size.toLong()).toInt()
            var offset = r.u16(2)
            var globalStrings = StringPool.EMPTY
            val index = HashMap<Int, MutableList<Pair<Int, Int>>>()

            while (offset + 8 <= end) {
                val type = r.u16(offset)
                val size = r.u32(offset + 4).toInt()
                if (size < 8 || offset + size > end) throw MalformedFileException("Bad table chunk at $offset")
                when (type) {
                    RES_STRING_POOL_TYPE -> globalStrings = StringPool.read(r, offset)
                    RES_TABLE_PACKAGE_TYPE -> indexPackage(r, offset, size, index)
                }
                offset += size
            }
            return ArscTable(r, globalStrings, index)
        }

        private fun indexPackage(r: ByteReader, start: Int, size: Int, index: HashMap<Int, MutableList<Pair<Int, Int>>>) {
            val packageId = r.s32(start + 8)
            var offset = start + r.u16(start + 2)
            val end = start + size
            while (offset + 8 <= end) {
                val type = r.u16(offset)
                val chunkSize = r.u32(offset + 4).toInt()
                if (chunkSize < 8 || offset + chunkSize > end) break
                if (type == RES_TABLE_TYPE_TYPE) {
                    val headerSize = r.u16(offset + 2)
                    val typeId = r.u8(offset + 8)
                    val flags = r.u8(offset + 9)
                    val entryCount = r.s32(offset + 12)
                    val entriesStart = r.s32(offset + 16)
                    val config = offset + 20
                    val offsetsAt = offset + headerSize
                    val sparse = flags and TYPE_FLAG_SPARSE != 0
                    val offset16 = flags and TYPE_FLAG_OFFSET16 != 0
                    for (i in 0 until entryCount) {
                        val (entryIndex, entryOffset) = when {
                            sparse -> r.u16(offsetsAt + i * 4) to r.u16(offsetsAt + i * 4 + 2) * 4
                            offset16 -> i to r.u16(offsetsAt + i * 2).let { if (it == 0xFFFF) NO_ENTRY else it * 4 }
                            else -> i to r.s32(offsetsAt + i * 4)
                        }
                        if (entryOffset == NO_ENTRY) continue
                        val id = (packageId shl 24) or (typeId shl 16) or entryIndex
                        index.getOrPut(id) { mutableListOf() } += (offset + entriesStart + entryOffset) to config
                    }
                }
                offset += chunkSize
            }
        }
    }
}
