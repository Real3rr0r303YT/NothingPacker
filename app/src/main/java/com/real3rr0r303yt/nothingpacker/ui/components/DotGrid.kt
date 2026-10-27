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

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingBlack
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingDotGrid

/** Black background with the subtle Nothing dot raster. */
fun Modifier.dotGridBackground(
    spacing: Dp = 22.dp,
    dotRadius: Dp = 1.dp,
    dotColor: Color = NothingDotGrid,
    background: Color = NothingBlack,
): Modifier = drawBehind {
    drawRect(background)
    val step = spacing.toPx()
    val r = dotRadius.toPx()
    val offsetX = (size.width % step) / 2
    var y = step / 2
    while (y < size.height) {
        var x = offsetX + step / 2
        while (x < size.width) {
            drawCircle(dotColor, r, Offset(x, y))
            x += step
        }
        y += step
    }
}
