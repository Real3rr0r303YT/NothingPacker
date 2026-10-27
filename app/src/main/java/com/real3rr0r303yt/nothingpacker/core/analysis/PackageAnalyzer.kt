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

import com.real3rr0r303yt.nothingpacker.core.image.CgbiPng
import com.real3rr0r303yt.nothingpacker.core.model.PackageReport
import com.real3rr0r303yt.nothingpacker.core.model.PackageType
import com.real3rr0r303yt.nothingpacker.core.model.UnsupportedPackageException
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile

/** Entry point: detects whether [file] is an APK or IPA and analyses it. Runs fully offline. */
object PackageAnalyzer {

    fun detect(zip: ZipFile): PackageType? = when {
        zip.getEntry(ApkAnalyzer.MANIFEST) != null -> PackageType.APK
        IpaAnalyzer.findInfoPlist(zip) != null -> PackageType.IPA
        else -> null
    }

    fun analyze(file: File, displayName: String = file.name): PackageReport {
        val zip = try {
            ZipFile(file)
        } catch (e: ZipException) {
            throw UnsupportedPackageException("Not a ZIP archive")
        }
        return zip.use {
            when (detect(it)) {
                PackageType.APK -> ApkAnalyzer.analyze(it, file, displayName)
                PackageType.IPA -> IpaAnalyzer.analyze(it, file, displayName)
                null -> throw UnsupportedPackageException(
                    if (it.getEntry("base/manifest/AndroidManifest.xml") != null) "Android App Bundles (.aab) are not supported"
                    else "Neither an APK nor an IPA",
                )
            }
        }
    }

    /** Raw icon bytes as stored in the package; IPA icons are converted from CgBI to standard PNG. */
    fun readIcon(file: File, report: PackageReport): ByteArray? {
        val path = report.iconPath ?: return null
        return ZipFile(file).use { zip ->
            val bytes = zip.getEntry(path)?.let { zip.readBytes(it, limit = 8L * 1024 * 1024) } ?: return null
            if (report.type == PackageType.IPA && CgbiPng.isPng(bytes)) CgbiPng.normalize(bytes) else bytes
        }
    }
}
