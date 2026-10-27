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

package com.real3rr0r303yt.nothingpacker.core.axml

import com.real3rr0r303yt.nothingpacker.core.binary.ByteReader
import com.real3rr0r303yt.nothingpacker.core.binary.MalformedFileException

/** ResStringPool chunk shared by binary XML and resources.arsc. Strings are decoded lazily. */
class StringPool private constructor(
    private val reader: ByteReader?,
    private val offsets: IntArray,
    private val stringsStart: Int,
    private val utf8: Boolean,
) {
    private val cache = HashMap<Int, String>()

    val size: Int get() = offsets.size

    fun getOrNull(index: Int): String? {
        if (index < 0 || index >= offsets.size || reader == null) return null
        return cache.getOrPut(index) { runCatching { decode(stringsStart + offsets[index]) }.getOrDefault("") }
    }

    private fun decode(at: Int): String {
        val r = reader!!
        return if (utf8) {
            var p = at
            // UTF-16 length (skipped), then UTF-8 byte length; each is 1 or 2 bytes.
            p += if (r.u8(p) and 0x80 != 0) 2 else 1
            var len = r.u8(p)
            if (len and 0x80 != 0) {
                len = ((len and 0x7F) shl 8) or r.u8(p + 1)
                p += 2
            } else {
                p += 1
            }
            String(r.bytes(p, len), Charsets.UTF_8)
        } else {
            var p = at
            var len = r.u16(p)
            if (len and 0x8000 != 0) {
                len = ((len and 0x7FFF) shl 16) or r.u16(p + 2)
                p += 4
            } else {
                p += 2
            }
            String(r.bytes(p, len * 2), Charsets.UTF_16LE)
        }
    }

    companion object {
        val EMPTY = StringPool(null, IntArray(0), 0, false)
        private const val UTF8_FLAG = 1 shl 8

        fun read(r: ByteReader, chunkStart: Int): StringPool {
            val headerSize = r.u16(chunkStart + 2)
            val count = r.s32(chunkStart + 8)
            val flags = r.s32(chunkStart + 16)
            val stringsStart = r.s32(chunkStart + 20)
            if (count < 0 || count > 2_000_000) throw MalformedFileException("Unreasonable string count $count")
            val offsets = IntArray(count) { r.s32(chunkStart + headerSize + it * 4) }
            return StringPool(r, offsets, chunkStart + stringsStart, flags and UTF8_FLAG != 0)
        }
    }
}
