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

package com.real3rr0r303yt.nothingpacker.core.convert

import com.real3rr0r303yt.nothingpacker.core.analysis.sanitizePath
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.PackageReport
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipFile

/** Destination for extracted files, e.g. a SAF document tree or a plain directory. */
fun interface FileSink {
    fun write(relativePath: String, body: (OutputStream) -> Unit)
}

/** Extracts selected categories of a package; metadata is written in readable form. */
object Extractor {

    data class Result(val files: Int)

    fun extract(
        source: File,
        report: PackageReport,
        categories: Set<FileCategory>,
        iconPng: ByteArray?,
        sink: FileSink,
        progress: (Float) -> Unit = {},
        isCancelled: () -> Boolean = { false },
    ): Result = ZipFile(source).use { zip ->
        var count = 0
        fun emit(path: String, body: (OutputStream) -> Unit) {
            if (isCancelled()) throw StructureConverter.Cancelled()
            val safe = sanitizePath(path)
            if (safe.isEmpty()) return
            sink.write(safe, body)
            count++
        }

        if (FileCategory.METADATA in categories) {
            val readableName = if (report.manifestPath.endsWith(".plist")) "Info.plist" else "AndroidManifest.xml"
            emit("metadata/$readableName") { it.write(report.manifestText.toByteArray(Charsets.UTF_8)) }
            emit("metadata/report.txt") { it.write(textReport(report).toByteArray(Charsets.UTF_8)) }
        }
        if (iconPng != null) emit("icon.png") { it.write(iconPng) }

        val selected = report.entries.filter { it.category in categories && it.category != FileCategory.METADATA }
        selected.forEachIndexed { i, e ->
            val entry = zip.getEntry(e.path) ?: return@forEachIndexed
            emit("files/" + e.path) { out -> zip.getInputStream(entry).use { it.copyTo(out) } }
            progress((i + 1f) / selected.size)
        }
        Result(count)
    }

    fun textReport(r: PackageReport): String = buildString {
        appendLine("NothingPacker report")
        appendLine("====================")
        appendLine("File:        ${r.fileName} (${r.type}, ${r.fileSize} bytes)")
        appendLine("Name:        ${r.displayName}")
        appendLine("Identifier:  ${r.identifier.orEmpty()}")
        appendLine("Version:     ${r.versionName.orEmpty()} (${r.versionCode.orEmpty()})")
        appendLine("Min OS:      ${r.minOs.orEmpty()}")
        appendLine("Target OS:   ${r.targetOs.orEmpty()}")
        appendLine("Signature:   ${r.signature ?: "none"}")
        appendLine("Arch:        ${r.architectures.joinToString(", ")}")
        appendLine("Warnings:    ${r.warnings.joinToString(", ")}")
        appendLine()
        appendLine("Permissions (${r.permissions.size})")
        r.permissions.forEach { appendLine("  ${it.name}${it.detail?.let { d -> " – $d" }.orEmpty()}") }
        appendLine()
        appendLine("Components (${r.components.size})")
        r.components.forEach { appendLine("  [${it.kind}] ${it.name} ${it.flags.joinToString(" ")}".trimEnd()) }
        appendLine()
        r.extra.forEach { (k, v) -> appendLine("$k: $v") }
        appendLine()
        appendLine("Files (${r.entries.size})")
        r.entries.forEach { appendLine("  %-10s %10d  %s".format(it.category, it.size, it.path)) }
    }

    /** Sink writing into a local directory (used by tests and as a reference implementation). */
    fun directorySink(root: File) = FileSink { path, body ->
        val target = File(root, sanitizePath(path))
        require(target.canonicalPath.startsWith(root.canonicalPath + File.separator)) { "Path escapes target: $path" }
        target.parentFile?.mkdirs()
        target.outputStream().use(body)
    }
}
