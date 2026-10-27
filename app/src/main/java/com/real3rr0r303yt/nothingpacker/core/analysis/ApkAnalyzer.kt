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

import com.real3rr0r303yt.nothingpacker.core.arsc.ArscTable
import com.real3rr0r303yt.nothingpacker.core.axml.AxmlParser
import com.real3rr0r303yt.nothingpacker.core.axml.XmlAttribute
import com.real3rr0r303yt.nothingpacker.core.axml.XmlElement
import com.real3rr0r303yt.nothingpacker.core.model.Component
import com.real3rr0r303yt.nothingpacker.core.model.ComponentKind
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.PackageEntry
import com.real3rr0r303yt.nothingpacker.core.model.PackageReport
import com.real3rr0r303yt.nothingpacker.core.model.PackageType
import com.real3rr0r303yt.nothingpacker.core.model.Permission
import com.real3rr0r303yt.nothingpacker.core.model.Warning
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipFile

internal object ApkAnalyzer {

    const val MANIFEST = "AndroidManifest.xml"

    fun analyze(zip: ZipFile, file: File, fileName: String): PackageReport {
        val manifestBytes = zip.readBytes(MANIFEST, limit = 16L * 1024 * 1024)
            ?: throw IllegalStateException("No manifest")
        val manifest = AxmlParser.parse(manifestBytes)
        val table = zip.readBytes("resources.arsc")?.let { runCatching { ArscTable.parse(it) }.getOrNull() }

        val packageName = manifest.value("package", null)
        val application = manifest.child("application")
        val usesSdk = manifest.child("uses-sdk")

        fun resolveString(attr: XmlAttribute?): String? = when {
            attr == null -> null
            attr.isReference -> table?.resolveString(attr.data)
            else -> attr.value
        }

        val label = resolveString(application?.attr("label"))
        val iconPath = (application?.attr("icon") ?: application?.attr("roundIcon"))
            ?.takeIf { it.isReference }
            ?.let { ref -> table?.resolveFiles(ref.data) }
            ?.firstOrNull { it.endsWith(".png") || it.endsWith(".webp") }
            ?: guessIcon(zip)

        val permissions = (manifest.children("uses-permission") + manifest.children("uses-permission-sdk-23"))
            .mapNotNull { it.value("name") }
            .distinct()
            .map { Permission(it) }

        val components = application?.let { components(it, packageName) }.orEmpty()

        val entries = zip.fileEntries().map { PackageEntry(it.name, it.size, it.compressedSize, categorize(it.name)) }
        val abis = entries.mapNotNull { e ->
            e.path.takeIf { it.startsWith("lib/") }?.split('/')?.getOrNull(1)
        }.distinct().sorted()

        val v1 = entries.any { it.path.startsWith("META-INF/") && it.path.substringAfterLast('.').uppercase() in setOf("RSA", "DSA", "EC") }
        val v2 = hasSigningBlock(file)
        val signature = when {
            v2 && v1 -> "APK Signature Scheme v2+ · JAR (v1)"
            v2 -> "APK Signature Scheme v2+"
            v1 -> "JAR (v1)"
            else -> null
        }

        val warnings = buildSet {
            if (application?.value("debuggable") == "true") add(Warning.DEBUGGABLE)
            if (application?.value("usesCleartextTraffic") == "true") add(Warning.CLEARTEXT_TRAFFIC)
            if (signature == null) add(Warning.UNSIGNED)
            if (manifest.value("split", null) != null) add(Warning.SPLIT_APK)
        }

        val extra = buildList {
            add("dex files" to entries.count { it.category == FileCategory.CODE }.toString())
            manifest.value("compileSdkVersion")?.let { add("compileSdkVersion" to it) }
            manifest.value("platformBuildVersionName", null)?.let { add("platformBuildVersionName" to it) }
            manifest.value("installLocation")?.let { add("installLocation" to it) }
            manifest.children("uses-feature").mapNotNull { it.value("name") }.takeIf { it.isNotEmpty() }
                ?.let { add("uses-feature" to it.joinToString("\n")) }
            application?.value("name")?.let { add("application" to qualify(it, packageName)) }
            application?.value("allowBackup")?.let { add("allowBackup" to it) }
            application?.value("extractNativeLibs")?.let { add("extractNativeLibs" to it) }
            manifest.value("split", null)?.let { add("split" to it) }
        }

        return PackageReport(
            type = PackageType.APK,
            fileName = fileName,
            fileSize = file.length(),
            appName = label,
            identifier = packageName,
            versionName = manifest.value("versionName"),
            versionCode = manifest.value("versionCode"),
            minOs = AndroidVersions.describe(usesSdk?.value("minSdkVersion") ?: "1"),
            targetOs = AndroidVersions.describe(usesSdk?.value("targetSdkVersion")),
            permissions = permissions,
            components = components,
            entries = entries,
            architectures = abis,
            signature = signature,
            manifestPath = MANIFEST,
            manifestText = AxmlParser.toXmlString(manifest),
            iconPath = iconPath,
            appRoot = "",
            executablePath = null,
            extra = extra,
            warnings = warnings,
        )
    }

    private fun components(application: XmlElement, packageName: String?): List<Component> {
        val kinds = mapOf(
            "activity" to ComponentKind.ACTIVITY,
            "activity-alias" to ComponentKind.ACTIVITY,
            "service" to ComponentKind.SERVICE,
            "receiver" to ComponentKind.RECEIVER,
            "provider" to ComponentKind.PROVIDER,
        )
        return application.children.mapNotNull { node ->
            val kind = kinds[node.name] ?: return@mapNotNull null
            val name = node.value("name") ?: return@mapNotNull null
            val filters = node.children("intent-filter")
            val launcher = filters.any { f ->
                f.children("action").any { it.value("name") == "android.intent.action.MAIN" } &&
                    f.children("category").any { it.value("name") == "android.intent.category.LAUNCHER" }
            }
            val flags = buildSet {
                if (launcher) add("launcher")
                if (node.value("exported") == "true") add("exported")
            }
            Component(kind, qualify(name, packageName), flags)
        }.sortedWith(compareBy({ it.kind }, { "launcher" !in it.flags }, { it.name }))
    }

    private fun qualify(name: String, packageName: String?) =
        if (name.startsWith(".") && packageName != null) packageName + name else name

    fun categorize(path: String): FileCategory = when {
        path.endsWith(".dex") -> FileCategory.CODE
        path.startsWith("lib/") -> FileCategory.NATIVE
        path == MANIFEST -> FileCategory.METADATA
        path.startsWith("META-INF/") -> FileCategory.SIGNATURE
        path.startsWith("res/") || path == "resources.arsc" -> FileCategory.RESOURCES
        path.startsWith("assets/") -> FileCategory.ASSETS
        else -> FileCategory.OTHER
    }

    /** Fallback when the icon cannot be resolved via resources.arsc: biggest launcher bitmap. */
    private fun guessIcon(zip: ZipFile): String? = zip.fileEntries()
        .filter { e ->
            val n = e.name.lowercase()
            n.startsWith("res/") && "mipmap" in n && (n.endsWith(".png") || n.endsWith(".webp"))
        }
        .maxByOrNull { it.size }
        ?.name

    /** Detects the APK Signing Block (v2/v3/v4) right before the central directory. */
    private fun hasSigningBlock(file: File): Boolean = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            val length = raf.length()
            val tailSize = minOf(length, 65_557L).toInt()
            val tail = ByteArray(tailSize)
            raf.seek(length - tailSize)
            raf.readFully(tail)
            var eocd = -1
            for (i in tail.size - 22 downTo 0) {
                if (tail[i] == 0x50.toByte() && tail[i + 1] == 0x4b.toByte() && tail[i + 2] == 0x05.toByte() && tail[i + 3] == 0x06.toByte()) {
                    eocd = i
                    break
                }
            }
            if (eocd < 0) return false
            val cdOffset = (tail[eocd + 16].toLong() and 0xFF) or ((tail[eocd + 17].toLong() and 0xFF) shl 8) or
                ((tail[eocd + 18].toLong() and 0xFF) shl 16) or ((tail[eocd + 19].toLong() and 0xFF) shl 24)
            if (cdOffset < 16) return false
            val magic = ByteArray(16)
            raf.seek(cdOffset - 16)
            raf.readFully(magic)
            String(magic, Charsets.US_ASCII) == "APK Sig Block 42"
        }
    }.getOrDefault(false)
}
