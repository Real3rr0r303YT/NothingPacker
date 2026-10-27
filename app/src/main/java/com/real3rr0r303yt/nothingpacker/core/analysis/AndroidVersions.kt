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

/** Maps API levels to marketing versions for display. */
object AndroidVersions {
    private val names = mapOf(
        21 to "5.0", 22 to "5.1", 23 to "6.0", 24 to "7.0", 25 to "7.1", 26 to "8.0", 27 to "8.1",
        28 to "9", 29 to "10", 30 to "11", 31 to "12", 32 to "12L", 33 to "13", 34 to "14", 35 to "15", 36 to "16",
    )

    fun describe(api: String?): String? {
        val level = api?.toIntOrNull() ?: return api
        return names[level]?.let { "API $level · Android $it" } ?: "API $level"
    }
}
