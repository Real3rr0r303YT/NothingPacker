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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.real3rr0r303yt.nothingpacker.R
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingBlack
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingOutline
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingRed
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingSurface
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingWhite

/** Top bar with a dot-matrix title and a red "status" dot. */
@Composable
fun NothingTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            DotMatrixText(title, Modifier.weight(1f, fill = false), dotSize = 3.dp, gap = 1.2.dp)
            Spacer(Modifier.width(6.dp))
            RedDot(size = 5.dp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun RedDot(modifier: Modifier = Modifier, size: Dp = 6.dp, color: Color = NothingRed) {
    Box(modifier.size(size).background(color, CircleShape))
}

/** Small monospace section label, e.g. "PRIVACY". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
        style = MaterialTheme.typography.labelMedium,
        color = NothingGrey,
    )
}

/** Hairline card border; red when highlighted (e.g. pinned). */
fun nothingCardStroke(highlight: Boolean = false) =
    BorderStroke(1.dp, if (highlight) NothingRed.copy(alpha = 0.6f) else NothingOutline)

@Composable
fun NothingIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NothingWhite,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription, tint = tint)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingSurface,
        titleContentColor = NothingWhite,
        textContentColor = NothingGrey,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel.uppercase(), color = if (destructive) NothingRed else NothingWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel).uppercase(), color = NothingGrey)
            }
        },
    )
}

/** Centered message used for empty states. */
@Composable
fun EmptyState(title: String, hint: String?, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RedDot(size = 10.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, color = NothingWhite)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = NothingGrey)
        }
    }
}

/** Translucent black so the dot grid shows through cards. */
val CardContainer = NothingBlack.copy(alpha = 0.85f)
