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

package com.real3rr0r303yt.nothingpacker.core.axml

/**
 * Names of common framework attributes by resource id. Used when an (obfuscated)
 * manifest strips attribute names from the string pool and only keeps the ids.
 */
object AndroidAttributes {
    private val names = mapOf(
        0x01010000 to "theme",
        0x01010001 to "label",
        0x01010002 to "icon",
        0x01010003 to "name",
        0x01010006 to "permission",
        0x0101000c to "persistent",
        0x0101000d to "enabled",
        0x0101000e to "debuggable",
        0x0101000f to "exported",
        0x01010010 to "process",
        0x01010011 to "taskAffinity",
        0x01010018 to "authorities",
        0x0101001d to "screenOrientation",
        0x0101001f to "configChanges",
        0x01010020 to "description",
        0x01010024 to "value",
        0x01010025 to "resource",
        0x01010026 to "mimeType",
        0x01010027 to "scheme",
        0x01010028 to "host",
        0x01010029 to "port",
        0x0101002a to "path",
        0x0101002b to "pathPrefix",
        0x0101002c to "pathPattern",
        0x0101020c to "minSdkVersion",
        0x0101021b to "versionCode",
        0x0101021c to "versionName",
        0x01010270 to "targetSdkVersion",
        0x01010271 to "maxSdkVersion",
        0x01010280 to "allowBackup",
        0x0101028e to "required",
        0x010102b7 to "hardwareAccelerated",
        0x010102d3 to "largeHeap",
        0x01010473 to "roundIcon",
        0x0101048c to "usesCleartextTraffic",
        0x01010527 to "networkSecurityConfig",
        0x01010572 to "compileSdkVersion",
        0x01010573 to "compileSdkVersionCodename",
        0x01010594 to "dataExtractionRules",
        0x01010608 to "localeConfig",
        0x010104ea to "fullBackupContent",
        0x010103af to "windowSoftInputMode",
        0x01010362 to "launchMode",
        0x0101001e to "excludeFromRecents",
        0x01010202 to "supportsRtl",
        0x010104d6 to "extractNativeLibs",
        0x0101051e to "appComponentFactory",
    )

    fun name(id: Int): String? = names[id]
}
