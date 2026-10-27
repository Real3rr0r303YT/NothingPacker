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

import com.real3rr0r303yt.nothingpacker.core.macho.MachO
import com.real3rr0r303yt.nothingpacker.core.model.Component
import com.real3rr0r303yt.nothingpacker.core.model.ComponentKind
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.PackageEntry
import com.real3rr0r303yt.nothingpacker.core.model.PackageReport
import com.real3rr0r303yt.nothingpacker.core.model.PackageType
import com.real3rr0r303yt.nothingpacker.core.model.Permission
import com.real3rr0r303yt.nothingpacker.core.model.Warning
import com.real3rr0r303yt.nothingpacker.core.plist.Plist
import java.io.File
import java.util.zip.ZipFile

internal object IpaAnalyzer {

    private val INFO_PLIST = Regex("""^Payload/[^/]+\.app/Info\.plist$""")

    fun findInfoPlist(zip: ZipFile): String? =
        zip.entries().asSequence().map { it.name }.firstOrNull { INFO_PLIST.matches(it) }

    @Suppress("UNCHECKED_CAST")
    fun analyze(zip: ZipFile, file: File, fileName: String): PackageReport {
        val plistPath = findInfoPlist(zip) ?: throw IllegalStateException("No Info.plist")
        val appRoot = plistPath.removeSuffix("Info.plist")
        val info = Plist.parseDict(zip.readBytes(plistPath, limit = 8L * 1024 * 1024)!!)

        fun str(key: String) = info[key]?.toString()?.takeIf { it.isNotBlank() }

        val executable = str("CFBundleExecutable")?.let { appRoot + it }
        val all = zip.fileEntries()
        val entries = all.map { PackageEntry(it.name, it.size, it.compressedSize, categorize(it.name, appRoot, executable)) }

        val machO = executable?.let { zip.getEntry(it) }?.let { MachO.inspect(zip.readHead(it, 64 * 1024)) }

        val permissions = info.keys
            .filter { it.startsWith("NS") && it.endsWith("UsageDescription") }
            .sorted()
            .map { Permission(it, str(it)) }

        val urlSchemes = (info["CFBundleURLTypes"] as? List<*>).orEmpty()
            .flatMap { ((it as? Map<String, Any?>)?.get("CFBundleURLSchemes") as? List<*>).orEmpty() }
            .map { Component(ComponentKind.URL_SCHEME, "$it://") }
        val extensionPattern = Regex("""^${Regex.escape(appRoot)}PlugIns/([^/]+\.appex)/""")
        val frameworkPattern = Regex("""^${Regex.escape(appRoot)}Frameworks/([^/]+\.(?:framework|dylib))""")
        val extensions = all.map { it.name }
            .mapNotNull { extensionPattern.find(it)?.groupValues?.get(1) }
            .distinct()
            .map { Component(ComponentKind.APP_EXTENSION, it) }
        val frameworks = all.map { it.name }
            .mapNotNull { frameworkPattern.find(it)?.groupValues?.get(1) }
            .distinct()
            .sorted()
            .map { Component(ComponentKind.FRAMEWORK, it) }

        val signed = entries.any { it.path.startsWith(appRoot + "_CodeSignature/") }
        val provisioned = zip.getEntry(appRoot + "embedded.mobileprovision") != null

        val warnings = buildSet {
            if (machO?.encrypted == true) add(Warning.ENCRYPTED)
            if (!signed) add(Warning.UNSIGNED)
            val ats = info["NSAppTransportSecurity"] as? Map<String, Any?>
            if (ats?.get("NSAllowsArbitraryLoads") == true) add(Warning.CLEARTEXT_TRAFFIC)
        }

        val families = (info["UIDeviceFamily"] as? List<*>).orEmpty().mapNotNull {
            when (it.toString()) { "1" -> "iPhone"; "2" -> "iPad"; "3" -> "Apple TV"; "4" -> "Apple Watch"; "6" -> "CarPlay"; "7" -> "Vision"; else -> null }
        }

        val extra = buildList {
            str("CFBundleExecutable")?.let { add("CFBundleExecutable" to it) }
            if (families.isNotEmpty()) add("UIDeviceFamily" to families.joinToString(", "))
            str("DTPlatformName")?.let { add("DTPlatformName" to it) }
            str("DTXcode")?.let { add("DTXcode" to it) }
            str("DTSDKName")?.let { add("DTSDKName" to it) }
            (info["UIRequiredDeviceCapabilities"] as? List<*>)?.takeIf { it.isNotEmpty() }
                ?.let { add("UIRequiredDeviceCapabilities" to it.joinToString(", ")) }
            (info["UIBackgroundModes"] as? List<*>)?.takeIf { it.isNotEmpty() }
                ?.let { add("UIBackgroundModes" to it.joinToString(", ")) }
            (info["LSApplicationQueriesSchemes"] as? List<*>)?.takeIf { it.isNotEmpty() }
                ?.let { add("LSApplicationQueriesSchemes" to it.size.toString()) }
            add("embedded.mobileprovision" to if (provisioned) "✓" else "–")
            machO?.encrypted?.let { add("FairPlay" to if (it) "encrypted" else "decrypted") }
        }

        return PackageReport(
            type = PackageType.IPA,
            fileName = fileName,
            fileSize = file.length(),
            appName = str("CFBundleDisplayName") ?: str("CFBundleName"),
            identifier = str("CFBundleIdentifier"),
            versionName = str("CFBundleShortVersionString"),
            versionCode = str("CFBundleVersion"),
            minOs = str("MinimumOSVersion")?.let { "iOS $it" },
            targetOs = str("DTPlatformVersion")?.let { "iOS $it" },
            permissions = permissions,
            components = urlSchemes + extensions + frameworks,
            entries = entries,
            architectures = machO?.architectures.orEmpty(),
            signature = if (signed) "Apple code signature" + if (provisioned) " · provisioning profile" else "" else null,
            manifestPath = plistPath,
            manifestText = Plist.toXml(info),
            iconPath = findIcon(info, appRoot, all.map { it.name }.toSet(), entries),
            appRoot = appRoot,
            executablePath = executable,
            extra = extra,
            warnings = warnings,
        )
    }

    fun categorize(path: String, appRoot: String, executable: String?): FileCategory {
        if (!path.startsWith(appRoot)) {
            return if (path == "iTunesMetadata.plist") FileCategory.METADATA else FileCategory.OTHER
        }
        val rel = path.removePrefix(appRoot)
        return when {
            path == executable -> FileCategory.CODE
            rel == "Info.plist" -> FileCategory.METADATA
            rel.startsWith("_CodeSignature/") || rel == "embedded.mobileprovision" || rel.startsWith("SC_Info/") -> FileCategory.SIGNATURE
            rel.startsWith("Frameworks/") -> FileCategory.FRAMEWORKS
            rel.startsWith("PlugIns/") || rel.startsWith("Watch/") || rel.startsWith("Extensions/") -> FileCategory.CODE
            rel.endsWith(".car") || rel.contains(".nib") || rel.contains(".storyboardc") ||
                rel.contains(".lproj/") || rel.endsWith(".strings") -> FileCategory.RESOURCES
            else -> FileCategory.ASSETS
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun findIcon(info: Map<String, Any?>, appRoot: String, names: Set<String>, entries: List<PackageEntry>): String? {
        val declared = buildList {
            for (key in listOf("CFBundleIcons", "CFBundleIcons~ipad")) {
                val primary = (info[key] as? Map<String, Any?>)?.get("CFBundlePrimaryIcon") as? Map<String, Any?>
                (primary?.get("CFBundleIconFiles") as? List<*>)?.forEach { add(it.toString()) }
                (primary?.get("CFBundleIconName") as? String)?.let { add(it) }
            }
            (info["CFBundleIconFiles"] as? List<*>)?.forEach { add(it.toString()) }
            (info["CFBundleIconFile"] as? String)?.let { add(it) }
        }.map { it.removeSuffix(".png") }

        val pngs = entries.filter { it.path.startsWith(appRoot) && it.path.endsWith(".png") && it.path.count { c -> c == '/' } == appRoot.count { c -> c == '/' } }
        val byDeclaration = pngs.filter { e -> declared.any { d -> e.path.removePrefix(appRoot).startsWith(d) } }
        val candidates = byDeclaration.ifEmpty { pngs.filter { it.path.removePrefix(appRoot).startsWith("AppIcon") } }
        return candidates.maxByOrNull { it.size }?.path
            ?: listOf("iTunesArtwork@2x", "iTunesArtwork").firstOrNull { it in names }
    }
}
