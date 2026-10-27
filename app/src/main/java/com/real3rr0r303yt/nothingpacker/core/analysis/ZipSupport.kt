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

package com.real3rr0r303yt.nothingpacker.core.analysis

import com.real3rr0r303yt.nothingpacker.core.binary.MalformedFileException
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Reads a whole entry, refusing to inflate more than [limit] bytes (zip-bomb guard). */
internal fun ZipFile.readBytes(entry: ZipEntry, limit: Long = 64L * 1024 * 1024): ByteArray {
    if (entry.size > limit) throw MalformedFileException("${entry.name} is too large")
    getInputStream(entry).use { input ->
        val out = ByteArrayOutputStream(if (entry.size in 1..limit) entry.size.toInt() else 8192)
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > limit) throw MalformedFileException("${entry.name} is too large")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}

/** Reads at most [max] bytes from the start of an entry. */
internal fun ZipFile.readHead(entry: ZipEntry, max: Int): ByteArray = getInputStream(entry).use { input ->
    val buf = ByteArray(max)
    var read = 0
    while (read < max) {
        val n = input.read(buf, read, max - read)
        if (n < 0) break
        read += n
    }
    buf.copyOf(read)
}

internal fun ZipFile.readBytes(path: String, limit: Long = 64L * 1024 * 1024): ByteArray? =
    getEntry(path)?.let { readBytes(it, limit) }

internal fun ZipFile.fileEntries(): List<ZipEntry> = entries().asSequence().filterNot { it.isDirectory }.toList()

/** Makes an archive path safe to use as a relative output path (no "..", no absolute paths). */
fun sanitizePath(path: String): String = path.replace('\\', '/')
    .split('/')
    .filter { it.isNotEmpty() && it != "." && it != ".." }
    .joinToString("/") { segment -> segment.map { c -> if (c.code < 0x20 || c in "<>:\"|?*") '_' else c }.joinToString("") }
