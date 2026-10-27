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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingRed
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingWhite

/**
 * Renders [text] with the built-in [DotMatrixFont] – the Nothing OS "Ndot" look.
 * Scales down automatically when the text would be wider than the available space.
 *
 * @param accentLast paints the final glyph in Nothing red (e.g. a trailing dot).
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.dp,
    gap: Dp = 1.dp,
    color: Color = NothingWhite,
    accentLast: Boolean = false,
    accentColor: Color = NothingRed,
) {
    val normalized = remember(text) { DotMatrixFont.normalize(text) }
    val columns = remember(normalized) { DotMatrixFont.measure(normalized) }
    if (columns == 0) return

    BoxWithConstraints(modifier.semantics { contentDescription = text }) {
        val requestedPitch = dotSize + gap
        val fitPitch = if (maxWidth == Dp.Infinity) requestedPitch else maxWidth / columns
        val pitch = min(requestedPitch, fitPitch)
        val scale = pitch / requestedPitch
        val radius = dotSize * scale / 2

        Canvas(Modifier.size(pitch * columns, pitch * DotMatrixFont.ROWS)) {
            val pitchPx = pitch.toPx()
            val radiusPx = radius.toPx()
            var x = 0
            normalized.forEachIndexed { index, c ->
                val glyph = DotMatrixFont.glyph(c)
                val dotColor = if (accentLast && index == normalized.lastIndex) accentColor else color
                glyph.dots.forEach { (col, row) ->
                    drawCircle(
                        color = dotColor,
                        radius = radiusPx,
                        center = Offset((x + col) * pitchPx + pitchPx / 2, row * pitchPx + pitchPx / 2),
                    )
                }
                x += glyph.width + 1
            }
        }
    }
}
