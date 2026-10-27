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

package com.real3rr0r303yt.nothingpacker.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

/** A package copied into the private workspace so it can be read with random access. */
data class ImportedFile(val file: File, val displayName: String)

/**
 * Private scratch space in the app cache. ZIP parsing needs random access, which
 * content:// streams do not offer, so opened packages are copied here – and only here.
 */
class Workspace(context: Context) {

    private val resolver = context.applicationContext.contentResolver
    private val dir = File(context.applicationContext.cacheDir, "workspace")

    fun import(uri: Uri): ImportedFile {
        val name = displayName(uri) ?: uri.lastPathSegment ?: "package"
        dir.mkdirs()
        val target = File(dir, UUID.randomUUID().toString())
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { input.copyTo(it, bufferSize = 256 * 1024) }
        } ?: error("Cannot open $uri")
        return ImportedFile(target, name)
    }

    fun sizeBytes(): Long = dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun clear() {
        dir.deleteRecursively()
    }

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
