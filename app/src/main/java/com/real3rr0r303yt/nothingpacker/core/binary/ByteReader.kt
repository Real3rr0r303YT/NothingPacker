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

package com.real3rr0r303yt.nothingpacker.core.binary

/** Thrown for any structurally invalid input. Callers show it as "file damaged/unsupported". */
class MalformedFileException(message: String) : Exception(message)

/** Bounds-checked little/big-endian reader over a byte array. Never reads outside [data]. */
class ByteReader(val data: ByteArray, private val littleEndian: Boolean = true) {

    val size: Int get() = data.size

    fun check(offset: Long, length: Long) {
        if (offset < 0 || length < 0 || offset + length > data.size) {
            throw MalformedFileException("Read out of bounds at $offset (+$length) of ${data.size}")
        }
    }

    fun u8(offset: Int): Int {
        check(offset.toLong(), 1)
        return data[offset].toInt() and 0xFF
    }

    fun u16(offset: Int): Int {
        check(offset.toLong(), 2)
        val a = data[offset].toInt() and 0xFF
        val b = data[offset + 1].toInt() and 0xFF
        return if (littleEndian) a or (b shl 8) else (a shl 8) or b
    }

    fun s32(offset: Int): Int {
        check(offset.toLong(), 4)
        var v = 0
        for (i in 0 until 4) {
            val byte = data[offset + i].toInt() and 0xFF
            v = if (littleEndian) v or (byte shl (8 * i)) else (v shl 8) or byte
        }
        return v
    }

    fun u32(offset: Int): Long = s32(offset).toLong() and 0xFFFFFFFFL

    /** Unsigned big/little-endian integer of 1..8 bytes. */
    fun uint(offset: Int, bytes: Int): Long {
        check(offset.toLong(), bytes.toLong())
        var v = 0L
        for (i in 0 until bytes) {
            val byte = (data[offset + i].toLong() and 0xFF)
            v = if (littleEndian) v or (byte shl (8 * i)) else (v shl 8) or byte
        }
        return v
    }

    fun bytes(offset: Int, length: Int): ByteArray {
        check(offset.toLong(), length.toLong())
        return data.copyOfRange(offset, offset + length)
    }
}
