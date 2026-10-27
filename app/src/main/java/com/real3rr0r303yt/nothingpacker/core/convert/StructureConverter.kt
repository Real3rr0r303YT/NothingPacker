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
import com.real3rr0r303yt.nothingpacker.core.axml.AxmlParser
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.PackageReport
import com.real3rr0r303yt.nothingpacker.core.model.PackageType
import com.real3rr0r303yt.nothingpacker.core.plist.Plist
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Re-packages the *structure* of an APK into an IPA layout and vice versa.
 *
 * Executable code cannot be translated between Android (DEX/ART) and iOS (Mach-O/arm64),
 * so the result is a structural port – metadata, icon, assets and resources arranged the
 * way the other platform expects – meant as a starting point for a native rebuild.
 * It is not installable and never contains code from the source package.
 */
object StructureConverter {

    data class Result(val files: Int, val bytes: Long, val target: PackageType)

    class Cancelled : Exception()

    fun suggestedFileName(report: PackageReport): String {
        val base = safeName(report.displayName)
        val version = report.versionName?.let { "_" + safeName(it) }.orEmpty()
        return "${base}${version}_${report.type.counterpart.extension}-structure.zip"
    }

    fun convert(
        source: File,
        report: PackageReport,
        iconPng: ByteArray?,
        out: OutputStream,
        progress: (Float) -> Unit = {},
        isCancelled: () -> Boolean = { false },
    ): Result = ZipFile(source).use { zip ->
        val writer = Writer(ZipOutputStream(out.buffered()), isCancelled)
        writer.use {
            when (report.type) {
                PackageType.APK -> apkToIpa(zip, report, iconPng, writer, progress)
                PackageType.IPA -> ipaToApk(zip, report, iconPng, writer, progress)
            }
        }
        Result(writer.files, writer.bytes, report.type.counterpart)
    }

    // ------------------------------------------------------------------ APK → IPA

    private fun apkToIpa(zip: ZipFile, report: PackageReport, icon: ByteArray?, w: Writer, progress: (Float) -> Unit) {
        val name = safeName(report.displayName)
        val app = "Payload/$name.app/"
        val androidPermissions = report.permissions.map { it.name }

        val info = linkedMapOf<String, Any?>(
            "CFBundleDevelopmentRegion" to "en",
            "CFBundleDisplayName" to report.displayName,
            "CFBundleName" to name,
            "CFBundleIdentifier" to (report.identifier ?: "com.example.$name"),
            "CFBundleShortVersionString" to (report.versionName ?: "1.0"),
            "CFBundleVersion" to (report.versionCode ?: "1"),
            "CFBundlePackageType" to "APPL",
            "CFBundleInfoDictionaryVersion" to "6.0",
            "LSRequiresIPhoneOS" to true,
            "MinimumOSVersion" to "15.0",
            "UIDeviceFamily" to listOf(1L, 2L),
        )
        if (icon != null) {
            info["CFBundleIcons"] = mapOf("CFBundlePrimaryIcon" to mapOf("CFBundleIconFiles" to listOf("AppIcon60x60")))
        }
        PermissionMap.iosKeysFor(androidPermissions).forEach { key ->
            info[key] = "Converted from Android permissions – please describe why the app needs this."
        }
        info["NothingPacker"] = linkedMapOf(
            "SourcePlatform" to "Android",
            "SourcePackage" to report.identifier,
            "SourceMinOS" to report.minOs,
            "SourceTargetOS" to report.targetOs,
            "AndroidPermissions" to androidPermissions,
            "Note" to "Structure conversion – contains no executable code.",
        )

        w.text(app + "Info.plist", Plist.toXml(info))
        icon?.let { w.bytes(app + "AppIcon60x60@3x.png", it) }
        w.text(app + "Android/AndroidManifest.xml", report.manifestText)

        val copied = report.entries.filter { e ->
            e.category == FileCategory.ASSETS ||
                (e.category == FileCategory.RESOURCES && e.path.startsWith("res/") && !isCompiledXml(e.path))
        }
        copied.forEachIndexed { i, e ->
            w.copy(zip, e.path, app + e.path)
            progress((i + 1f) / copied.size)
        }
        w.text("NothingPacker-README.txt", readme(report, app, copied.size))
    }

    private fun isCompiledXml(path: String) = path.endsWith(".xml") || path.startsWith("res/values")

    // ------------------------------------------------------------------ IPA → APK

    private fun ipaToApk(zip: ZipFile, report: PackageReport, icon: ByteArray?, w: Writer, progress: (Float) -> Unit) {
        val packageName = report.identifier?.let(::toPackageName) ?: "com.example.${safeName(report.displayName).lowercase()}"
        val permissions = PermissionMap.androidPermissionsFor(report.permissions.map { it.name })

        val manifest = buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            append("<!-- Generated by NothingPacker from ${AxmlParser.escape(report.fileName)} (structure conversion). -->\n")
            append("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
            append("    package=\"").append(packageName).append("\"\n")
            append("    android:versionCode=\"").append(versionCode(report.versionCode)).append("\"\n")
            append("    android:versionName=\"").append(AxmlParser.escape(report.versionName ?: "1.0")).append("\">\n\n")
            append("    <uses-sdk android:minSdkVersion=\"26\" android:targetSdkVersion=\"35\" />\n\n")
            permissions.forEach { append("    <uses-permission android:name=\"").append(it).append("\" />\n") }
            if (permissions.isNotEmpty()) append('\n')
            append("    <application\n")
            append("        android:label=\"@string/app_name\"\n")
            if (icon != null) append("        android:icon=\"@mipmap/ic_launcher\"\n")
            append("        android:allowBackup=\"false\" />\n")
            append("</manifest>\n")
        }
        val strings = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
            "    <string name=\"app_name\">${AxmlParser.escape(report.displayName)}</string>\n</resources>\n"

        w.text("AndroidManifest.xml", manifest)
        w.text("res/values/strings.xml", strings)
        icon?.let { w.bytes("res/mipmap-xxxhdpi/ic_launcher.png", it) }
        w.text("ios/Info.plist", report.manifestText)

        val copied = report.entries.filter {
            it.path.startsWith(report.appRoot) && (it.category == FileCategory.ASSETS || it.category == FileCategory.RESOURCES)
        }
        copied.forEachIndexed { i, e ->
            w.copy(zip, e.path, "assets/" + e.path.removePrefix(report.appRoot))
            progress((i + 1f) / copied.size)
        }
        w.text("NothingPacker-README.txt", readme(report, "", copied.size))
    }

    fun toPackageName(bundleId: String): String {
        val parts = bundleId.split('.')
            .map { part -> part.replace('-', '_').filter { it.isLetterOrDigit() || it == '_' }.lowercase() }
            .filter { it.isNotEmpty() }
            .map { if (it.first().isDigit()) "_$it" else it }
        return when {
            parts.size >= 2 -> parts.joinToString(".")
            parts.size == 1 -> "app.${parts[0]}"
            else -> "app.converted"
        }
    }

    fun versionCode(bundleVersion: String?): Long {
        bundleVersion?.trim()?.toLongOrNull()?.let { return it.coerceIn(1, 2_100_000_000) }
        val nums = bundleVersion.orEmpty().split('.').mapNotNull { it.filter(Char::isDigit).toLongOrNull() }
        if (nums.isEmpty()) return 1
        val code = nums.take(3).let { (it + listOf(0L, 0L, 0L)).take(3) }.let { (a, b, c) -> a * 10_000 + b * 100 + c }
        return code.coerceIn(1, 2_100_000_000)
    }

    private fun safeName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }.joinToString("")
            .trim('_').take(48).ifEmpty { "App" }

    private fun readme(report: PackageReport, appDir: String, copied: Int) = """
        NothingPacker – structure conversion
        ====================================

        Source:  ${report.fileName} (${report.type})
        Target:  ${report.type.counterpart} structure
        App:     ${report.displayName}  ${report.identifier.orEmpty()}  ${report.versionName.orEmpty()}
        Copied:  $copied asset/resource files

        What this archive is
        --------------------
        A re-packaged *layout* for the other platform: generated metadata
        (${if (report.type == PackageType.APK) "${appDir}Info.plist" else "AndroidManifest.xml, res/values/strings.xml"}),
        the app icon and all portable assets and resources.

        What it is NOT
        --------------
        It is not an installable app. Android code (DEX) cannot run on iOS and
        iOS code (Mach-O) cannot run on Android, so no executable code was copied.
        Rebuild the app natively and use this archive as the resource base.
        ${if (report.type == PackageType.APK) "iOS also requires an Xcode build and Apple code signing." else "The manifest is plain text; build with Gradle to get a binary manifest, DEX code and a signature."}

        Only convert apps you own or are allowed to modify.
        Generated locally on your device by NothingPacker (GPLv3) – nothing was uploaded.
    """.trimIndent() + "\n"

    /** ZIP writer that de-duplicates paths and supports cancellation. */
    private class Writer(private val zip: ZipOutputStream, private val isCancelled: () -> Boolean) : AutoCloseable {
        var files = 0
        var bytes = 0L
        private val written = HashSet<String>()

        fun text(path: String, content: String) = bytes(path, content.toByteArray(Charsets.UTF_8))

        fun bytes(path: String, data: ByteArray) = put(path) { it.write(data); data.size.toLong() }

        fun copy(source: ZipFile, from: String, to: String) {
            val entry = source.getEntry(from) ?: return
            put(to) { out -> source.getInputStream(entry).use { it.copyTo(out) } }
        }

        private fun put(rawPath: String, body: (OutputStream) -> Long) {
            if (isCancelled()) throw Cancelled()
            val path = sanitizePath(rawPath)
            if (path.isEmpty() || !written.add(path)) return
            zip.putNextEntry(ZipEntry(path))
            bytes += body(zip)
            zip.closeEntry()
            files++
        }

        override fun close() = zip.close()
    }
}
