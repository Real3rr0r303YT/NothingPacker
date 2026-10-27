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

package com.real3rr0r303yt.nothingpacker.core.model

enum class PackageType(val extension: String) {
    APK("apk"),
    IPA("ipa"),
    ;

    val counterpart: PackageType get() = if (this == APK) IPA else APK
}

enum class FileCategory { CODE, NATIVE, FRAMEWORKS, RESOURCES, ASSETS, METADATA, SIGNATURE, OTHER }

enum class ComponentKind { ACTIVITY, SERVICE, RECEIVER, PROVIDER, URL_SCHEME, APP_EXTENSION, FRAMEWORK }

enum class Warning {
    /** iOS executable is FairPlay-encrypted (App Store download). */
    ENCRYPTED,
    DEBUGGABLE,
    CLEARTEXT_TRAFFIC,
    UNSIGNED,
    /** Only a split/config APK – the base APK is missing. */
    SPLIT_APK,
}

data class Component(val kind: ComponentKind, val name: String, val flags: Set<String> = emptySet())

data class Permission(val name: String, val detail: String? = null)

data class PackageEntry(val path: String, val size: Long, val compressedSize: Long, val category: FileCategory)

data class CategoryStats(val count: Int, val bytes: Long)

/** Everything NothingPacker learned about one APK or IPA. */
data class PackageReport(
    val type: PackageType,
    val fileName: String,
    val fileSize: Long,
    val appName: String?,
    val identifier: String?,
    val versionName: String?,
    val versionCode: String?,
    val minOs: String?,
    val targetOs: String?,
    val permissions: List<Permission>,
    val components: List<Component>,
    val entries: List<PackageEntry>,
    val architectures: List<String>,
    val signature: String?,
    /** Path of AndroidManifest.xml / Info.plist inside the archive. */
    val manifestPath: String,
    /** Human-readable manifest: decoded XML or XML plist. */
    val manifestText: String,
    val iconPath: String?,
    /** "" for APKs, "Payload/Name.app/" for IPAs. */
    val appRoot: String,
    val executablePath: String?,
    /** Additional technical key/value pairs (not translated). */
    val extra: List<Pair<String, String>>,
    val warnings: Set<Warning>,
) {
    val displayName: String get() = appName?.takeIf { it.isNotBlank() } ?: identifier ?: fileName

    val stats: Map<FileCategory, CategoryStats> by lazy {
        entries.groupBy { it.category }.mapValues { (_, list) -> CategoryStats(list.size, list.sumOf { it.size }) }
    }

    val uncompressedSize: Long get() = entries.sumOf { it.size }
}

class UnsupportedPackageException(message: String) : Exception(message)
