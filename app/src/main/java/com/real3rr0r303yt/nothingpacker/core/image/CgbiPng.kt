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

package com.real3rr0r303yt.nothingpacker.core.image

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Converts Apple's "CgBI" optimised PNGs (used for icons inside IPAs) back to standard PNG:
 * raw deflate → zlib, BGRA → RGBA and premultiplied → straight alpha.
 */
object CgbiPng {

    private val SIGNATURE = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)
    private const val MAX_PIXELS = 4096 * 4096

    private class Chunk(val type: String, val data: ByteArray)

    fun isPng(bytes: ByteArray) = bytes.size > 8 && bytes.copyOfRange(0, 8).contentEquals(SIGNATURE)

    fun isCgbi(bytes: ByteArray): Boolean = isPng(bytes) && readChunks(bytes).firstOrNull()?.type == "CgBI"

    /** Returns a standard PNG; non-CgBI PNGs are returned unchanged. Null if unsupported. */
    fun normalize(bytes: ByteArray): ByteArray? {
        if (!isPng(bytes)) return null
        val chunks = runCatching { readChunks(bytes) }.getOrNull() ?: return null
        if (chunks.firstOrNull()?.type != "CgBI") return bytes

        val ihdr = chunks.firstOrNull { it.type == "IHDR" }?.data ?: return null
        val width = int(ihdr, 0)
        val height = int(ihdr, 4)
        val bitDepth = ihdr[8].toInt()
        val colorType = ihdr[9].toInt()
        val interlace = ihdr[12].toInt()
        if (bitDepth != 8 || colorType != 6 || interlace != 0) return null
        if (width <= 0 || height <= 0 || width.toLong() * height > MAX_PIXELS) return null

        val compressed = ByteArrayOutputStream().apply {
            chunks.filter { it.type == "IDAT" }.forEach { write(it.data) }
        }.toByteArray()
        val stride = width * 4
        val raw = ByteArray((stride + 1) * height)
        val inflater = Inflater(true)
        try {
            inflater.setInput(compressed)
            var read = 0
            while (read < raw.size && !inflater.finished()) {
                val n = inflater.inflate(raw, read, raw.size - read)
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                read += n
            }
            if (read != raw.size) return null
        } finally {
            inflater.end()
        }

        val pixels = unfilter(raw, stride, height) ?: return null
        for (p in pixels.indices step 4) {
            val b = pixels[p].toInt() and 0xFF
            val g = pixels[p + 1].toInt() and 0xFF
            val r = pixels[p + 2].toInt() and 0xFF
            val a = pixels[p + 3].toInt() and 0xFF
            pixels[p] = unpremultiply(r, a)
            pixels[p + 1] = unpremultiply(g, a)
            pixels[p + 2] = unpremultiply(b, a)
        }

        // Re-encode with filter type 0 for every row.
        val filtered = ByteArray((stride + 1) * height)
        for (y in 0 until height) System.arraycopy(pixels, y * stride, filtered, y * (stride + 1) + 1, stride)
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        val idat = try {
            deflater.setInput(filtered)
            deflater.finish()
            val out = ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf))
            out.toByteArray()
        } finally {
            deflater.end()
        }

        return ByteArrayOutputStream().apply {
            write(SIGNATURE)
            writeChunk("IHDR", ihdr)
            writeChunk("IDAT", idat)
            writeChunk("IEND", ByteArray(0))
        }.toByteArray()
    }

    private fun unpremultiply(c: Int, a: Int): Byte = when (a) {
        0 -> 0
        255 -> c.toByte()
        else -> ((c * 255 + a / 2) / a).coerceAtMost(255).toByte()
    }

    private fun unfilter(raw: ByteArray, stride: Int, height: Int): ByteArray? {
        val out = ByteArray(stride * height)
        val bpp = 4
        for (y in 0 until height) {
            val filter = raw[y * (stride + 1)].toInt()
            val src = y * (stride + 1) + 1
            val dst = y * stride
            for (x in 0 until stride) {
                val v = raw[src + x].toInt() and 0xFF
                val a = if (x >= bpp) out[dst + x - bpp].toInt() and 0xFF else 0
                val b = if (y > 0) out[dst - stride + x].toInt() and 0xFF else 0
                val c = if (x >= bpp && y > 0) out[dst - stride + x - bpp].toInt() and 0xFF else 0
                val predicted = when (filter) {
                    0 -> 0
                    1 -> a
                    2 -> b
                    3 -> (a + b) / 2
                    4 -> paeth(a, b, c)
                    else -> return null
                }
                out[dst + x] = (v + predicted).toByte()
            }
        }
        return out
    }

    private fun paeth(a: Int, b: Int, c: Int): Int {
        val p = a + b - c
        val pa = Math.abs(p - a)
        val pb = Math.abs(p - b)
        val pc = Math.abs(p - c)
        return if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
    }

    private fun readChunks(bytes: ByteArray): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var p = 8
        while (p + 12 <= bytes.size) {
            val length = int(bytes, p)
            if (length < 0 || p + 12 + length > bytes.size) break
            val type = String(bytes, p + 4, 4, Charsets.US_ASCII)
            chunks += Chunk(type, bytes.copyOfRange(p + 8, p + 8 + length))
            p += 12 + length
            if (type == "IEND") break
        }
        return chunks
    }

    private fun int(b: ByteArray, at: Int) =
        ((b[at].toInt() and 0xFF) shl 24) or ((b[at + 1].toInt() and 0xFF) shl 16) or
            ((b[at + 2].toInt() and 0xFF) shl 8) or (b[at + 3].toInt() and 0xFF)

    private fun ByteArrayOutputStream.writeChunk(type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        writeInt(data.size)
        write(typeBytes)
        write(data)
        val crc = CRC32().apply { update(typeBytes); update(data) }
        writeInt(crc.value.toInt())
    }

    private fun ByteArrayOutputStream.writeInt(v: Int) {
        write(v ushr 24); write(v ushr 16); write(v ushr 8); write(v)
    }
}
