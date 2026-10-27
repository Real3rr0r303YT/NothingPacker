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

package com.real3rr0r303yt.nothingpacker.ui.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.real3rr0r303yt.nothingpacker.R
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.PackageReport
import com.real3rr0r303yt.nothingpacker.ui.formatBytes
import com.real3rr0r303yt.nothingpacker.ui.label
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingGreyDim
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingRed
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingSurface
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingWhite

/** Lets the user pick which file categories to extract. Metadata is always offered. */
@Composable
fun ExtractDialog(report: PackageReport, onDismiss: () -> Unit, onConfirm: (Set<FileCategory>) -> Unit) {
    val available = remember(report) {
        (FileCategory.entries.filter { report.stats.containsKey(it) } + FileCategory.METADATA).distinct()
    }
    var selected by remember {
        mutableStateOf(setOf(FileCategory.METADATA, FileCategory.ASSETS, FileCategory.RESOURCES).intersect(available.toSet()))
    }
    val locale = LocalConfiguration.current.locales[0]

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingSurface,
        titleContentColor = NothingWhite,
        title = { Text(stringResource(R.string.extract_title)) },
        text = {
            Column {
                Text(stringResource(R.string.extract_text), style = MaterialTheme.typography.bodySmall, color = NothingGrey)
                Spacer(Modifier.height(8.dp))
                available.forEach { category ->
                    val checked = category in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(checked, role = Role.Checkbox) {
                                selected = if (it) selected + category else selected - category
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = NothingRed, uncheckedColor = NothingGreyDim, checkmarkColor = NothingWhite),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(category.label()), color = NothingWhite, modifier = Modifier.weight(1f))
                        report.stats[category]?.let {
                            Text(formatBytes(it.bytes, locale), style = MaterialTheme.typography.labelSmall, color = NothingGreyDim)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }, enabled = selected.isNotEmpty()) {
                Text(stringResource(R.string.action_extract).uppercase(), color = if (selected.isEmpty()) NothingGreyDim else NothingRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel).uppercase(), color = NothingGrey) }
        },
    )
}
