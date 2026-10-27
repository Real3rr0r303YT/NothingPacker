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
import androidx.documentfile.provider.DocumentFile
import com.real3rr0r303yt.nothingpacker.core.convert.FileSink
import java.io.IOException

/** Writes extracted files into a folder the user picked via the system file picker. */
class DocumentTreeSink(context: Context, treeUri: Uri, folderName: String) : FileSink {

    private val resolver = context.contentResolver
    private val root: DocumentFile
    private val dirs = HashMap<String, DocumentFile>()

    init {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: throw IOException("Folder not accessible")
        var name = folderName
        var n = 2
        while (tree.findFile(name) != null) name = "$folderName ($n)".also { n++ }
        root = tree.createDirectory(name) ?: throw IOException("Cannot create $name")
        dirs[""] = root
    }

    override fun write(relativePath: String, body: (java.io.OutputStream) -> Unit) {
        val parentPath = relativePath.substringBeforeLast('/', "")
        val fileName = relativePath.substringAfterLast('/')
        val parent = directory(parentPath)
        val file = parent.createFile("application/octet-stream", fileName) ?: throw IOException("Cannot create $relativePath")
        resolver.openOutputStream(file.uri)?.use(body) ?: throw IOException("Cannot write $relativePath")
    }

    private fun directory(path: String): DocumentFile = dirs.getOrPut(path) {
        val parent = directory(path.substringBeforeLast('/', ""))
        val name = path.substringAfterLast('/')
        parent.findFile(name)?.takeIf { it.isDirectory } ?: parent.createDirectory(name)
            ?: throw IOException("Cannot create folder $path")
    }
}
