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

package com.real3rr0r303yt.nothingpacker.core.macho

import com.real3rr0r303yt.nothingpacker.core.binary.ByteReader

/** What we can learn from the first bytes of an iOS executable. */
data class MachOInfo(val architectures: List<String>, val encrypted: Boolean?)

/** Reads Mach-O / fat headers to report CPU architectures and FairPlay encryption. */
object MachO {

    private const val MH_MAGIC_64 = 0xFEEDFACFL
    private const val MH_MAGIC = 0xFEEDFACEL
    private const val FAT_MAGIC = 0xCAFEBABEL
    private const val LC_ENCRYPTION_INFO = 0x21L
    private const val LC_ENCRYPTION_INFO_64 = 0x2CL

    /** [header] should hold at least the first 64 KiB of the executable. */
    fun inspect(header: ByteArray): MachOInfo? {
        if (header.size < 32) return null
        val be = ByteReader(header, littleEndian = false)
        if (be.u32(0) == FAT_MAGIC) {
            val count = be.u32(4).toInt().coerceIn(0, 16)
            val archs = (0 until count).mapNotNull { i ->
                runCatching { cpuName(be.s32(8 + i * 20), be.s32(12 + i * 20)) }.getOrNull()
            }
            return MachOInfo(archs, encrypted = null)
        }
        val le = ByteReader(header, littleEndian = true)
        val magic = le.u32(0)
        if (magic != MH_MAGIC_64 && magic != MH_MAGIC) return null
        val is64 = magic == MH_MAGIC_64
        val arch = cpuName(le.s32(4), le.s32(8))
        val ncmds = le.u32(16).toInt().coerceIn(0, 4096)
        var offset = if (is64) 32 else 28
        var encrypted: Boolean? = null
        repeat(ncmds) {
            if (offset + 8 > header.size) return MachOInfo(listOf(arch), encrypted)
            val cmd = le.u32(offset)
            val size = le.u32(offset + 4).toInt()
            if (cmd == LC_ENCRYPTION_INFO || cmd == LC_ENCRYPTION_INFO_64) {
                if (offset + 20 <= header.size) encrypted = le.u32(offset + 16) != 0L
            }
            if (size < 8) return MachOInfo(listOf(arch), encrypted)
            offset += size
        }
        return MachOInfo(listOf(arch), encrypted ?: false)
    }

    private fun cpuName(type: Int, subtype: Int): String = when (type) {
        0x0100000C -> if (subtype and 0xFF == 2) "arm64e" else "arm64"
        12 -> "armv7"
        0x01000007 -> "x86_64"
        7 -> "i386"
        else -> "cpu 0x%x".format(type)
    }
}
