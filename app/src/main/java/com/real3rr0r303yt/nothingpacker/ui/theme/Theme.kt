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

package com.real3rr0r303yt.nothingpacker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NothingColors = darkColorScheme(
    primary = NothingRed,
    onPrimary = NothingWhite,
    primaryContainer = NothingRedDim,
    onPrimaryContainer = NothingWhite,
    secondary = NothingWhite,
    onSecondary = NothingBlack,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingBlack,
    onSurface = NothingWhite,
    surfaceVariant = NothingSurfaceHigh,
    onSurfaceVariant = NothingGrey,
    surfaceContainerLowest = NothingBlack,
    surfaceContainerLow = NothingSurface,
    surfaceContainer = NothingSurface,
    surfaceContainerHigh = NothingSurfaceHigh,
    surfaceContainerHighest = NothingSurfaceHigh,
    outline = NothingOutline,
    outlineVariant = NothingOutline,
    error = NothingRed,
    onError = NothingWhite,
)

private val NothingShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** NothingPacker is always dark: an OLED black canvas with red accents. */
@Composable
fun NothingPackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NothingColors,
        typography = NothingTypography,
        shapes = NothingShapes,
        content = content,
    )
}
