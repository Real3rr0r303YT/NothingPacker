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

package com.real3rr0r303yt.nothingpacker.ui

import androidx.annotation.StringRes
import com.real3rr0r303yt.nothingpacker.R
import com.real3rr0r303yt.nothingpacker.core.model.ComponentKind
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.Warning
import java.util.Locale

fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return String.format(locale, if (value < 10) "%.1f %s" else "%.0f %s", value, units[unit])
}

@StringRes
fun FileCategory.label(): Int = when (this) {
    FileCategory.CODE -> R.string.category_code
    FileCategory.NATIVE -> R.string.category_native
    FileCategory.FRAMEWORKS -> R.string.category_frameworks
    FileCategory.RESOURCES -> R.string.category_resources
    FileCategory.ASSETS -> R.string.category_assets
    FileCategory.METADATA -> R.string.category_metadata
    FileCategory.SIGNATURE -> R.string.category_signature
    FileCategory.OTHER -> R.string.category_other
}

@StringRes
fun ComponentKind.label(): Int = when (this) {
    ComponentKind.ACTIVITY -> R.string.component_activity
    ComponentKind.SERVICE -> R.string.component_service
    ComponentKind.RECEIVER -> R.string.component_receiver
    ComponentKind.PROVIDER -> R.string.component_provider
    ComponentKind.URL_SCHEME -> R.string.component_url_scheme
    ComponentKind.APP_EXTENSION -> R.string.component_app_extension
    ComponentKind.FRAMEWORK -> R.string.component_framework
}

@StringRes
fun Warning.label(): Int = when (this) {
    Warning.ENCRYPTED -> R.string.warning_encrypted
    Warning.DEBUGGABLE -> R.string.warning_debuggable
    Warning.CLEARTEXT_TRAFFIC -> R.string.warning_cleartext
    Warning.UNSIGNED -> R.string.warning_unsigned
    Warning.SPLIT_APK -> R.string.warning_split
}
