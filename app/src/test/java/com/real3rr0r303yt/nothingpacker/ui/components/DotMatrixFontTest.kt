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

package com.real3rr0r303yt.nothingpacker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DotMatrixFontTest {

    @Test
    fun `all glyphs are 7 rows and rectangular`() {
        assertTrue(DotMatrixFont.isWellFormed())
    }

    @Test
    fun `covers every dot-matrix label of both languages`() {
        val labels = listOf(
            "NOTES", "NOTIZEN", "SETTINGS", "EINSTELLUNGEN", "NEW NOTE", "NEUE NOTIZ",
            "NothingPacker", "APK → IPA", "APK ⇄ IPA", "PACKER", "26.10.26, 23:27", "10/26/26, 11:27 PM",
            "PRIVATSPHÄRE", "ÜBER", "UNTERSTÜTZEN", "größe",
        )
        labels.flatMap { DotMatrixFont.normalize(it).toList() }.forEach { c ->
            assertTrue("missing glyph '$c'", DotMatrixFont.supports(c))
        }
    }

    @Test
    fun `measure adds one column between glyphs`() {
        // I = 3 columns, 1 = 3 columns, plus one gap.
        assertEquals(7, DotMatrixFont.measure("I1"))
        assertEquals(0, DotMatrixFont.measure(""))
    }
}
