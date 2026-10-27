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

package com.real3rr0r303yt.nothingpacker.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.real3rr0r303yt.nothingpacker.BuildConfig
import com.real3rr0r303yt.nothingpacker.R
import com.real3rr0r303yt.nothingpacker.data.Workspace
import com.real3rr0r303yt.nothingpacker.ui.components.CardContainer
import com.real3rr0r303yt.nothingpacker.ui.components.ConfirmDialog
import com.real3rr0r303yt.nothingpacker.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingpacker.ui.components.NothingTopBar
import com.real3rr0r303yt.nothingpacker.ui.components.RedDot
import com.real3rr0r303yt.nothingpacker.ui.components.SectionLabel
import com.real3rr0r303yt.nothingpacker.ui.components.nothingCardStroke
import com.real3rr0r303yt.nothingpacker.ui.formatBytes
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingGreyDim
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingOutline
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingRed
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val DONATION_URL = "https://www.buymeacoffee.com/3rr0r303yt"

private enum class AppLanguage(val tag: String, val label: Int) {
    SYSTEM("", R.string.settings_language_system),
    GERMAN("de", R.string.settings_language_de),
    ENGLISH("en", R.string.settings_language_en),
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workspace = remember { Workspace(context) }
    var workspaceBytes by remember { mutableLongStateOf(0L) }
    var confirmDonate by remember { mutableStateOf(false) }
    val language = remember {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language
        AppLanguage.entries.firstOrNull { it.tag.isNotEmpty() && it.tag == tag } ?: AppLanguage.SYSTEM
    }
    val locale = LocalConfiguration.current.locales[0]
    val clearedMessage = stringResource(R.string.settings_workspace_cleared)
    val noBrowser = stringResource(R.string.donate_no_browser)

    LaunchedEffect(Unit) { workspaceBytes = withContext(Dispatchers.IO) { workspace.sizeBytes() } }

    Column(Modifier.fillMaxSize()) {
        NothingTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_section_language))
            SettingsCard(Modifier.selectableGroup()) {
                AppLanguage.entries.forEachIndexed { i, option ->
                    if (i > 0) HorizontalDivider(color = NothingOutline)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == language, role = Role.RadioButton) {
                                // AppCompat persists the choice and recreates the activity.
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(option.tag))
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(option.label), Modifier.weight(1f), color = NothingWhite)
                        RadioButton(
                            selected = option == language,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = NothingRed, unselectedColor = NothingGreyDim),
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.settings_section_privacy))
            SettingsCard(highlight = true) {
                InfoRow(Icons.Outlined.WifiOff, stringResource(R.string.settings_offline_title), stringResource(R.string.settings_offline_text), NothingRed)
                HorizontalDivider(color = NothingOutline)
                InfoRow(Icons.Outlined.FolderOff, stringResource(R.string.settings_storage_title), stringResource(R.string.settings_storage_text))
            }

            SectionLabel(stringResource(R.string.settings_section_workspace))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.CleaningServices, null, tint = NothingWhite)
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.settings_workspace_title), color = NothingWhite, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.settings_workspace_text, formatBytes(workspaceBytes, locale)),
                                color = NothingGrey,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                workspaceBytes = withContext(Dispatchers.IO) {
                                    workspace.clear()
                                    workspace.sizeBytes()
                                }
                                Toast.makeText(context, clearedMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        border = BorderStroke(1.dp, NothingOutline),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(stringResource(R.string.settings_workspace_clear), color = NothingWhite)
                    }
                }
            }

            SectionLabel(stringResource(R.string.settings_section_support))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_donate_text), style = MaterialTheme.typography.bodySmall, color = NothingGrey)
                    OutlinedButton(
                        onClick = { confirmDonate = true },
                        border = BorderStroke(1.dp, NothingOutline),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(Icons.Outlined.Coffee, null, tint = NothingWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_donate_title), color = NothingWhite)
                    }
                }
            }

            SectionLabel(stringResource(R.string.settings_section_about))
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixText("NOTHINGPACKER", dotSize = 2.4.dp, gap = 1.dp)
                    Spacer(Modifier.width(5.dp))
                    RedDot(size = 4.dp)
                }
                Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelMedium, color = NothingGrey)
                Text(stringResource(R.string.settings_license), style = MaterialTheme.typography.labelSmall, color = NothingGreyDim)
                Text(stringResource(R.string.settings_legal), style = MaterialTheme.typography.labelSmall, color = NothingGreyDim)
                Text(stringResource(R.string.settings_copyright), style = MaterialTheme.typography.labelSmall, color = NothingGreyDim)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDonate) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_donate_title),
            text = stringResource(R.string.dialog_donate_text),
            confirmLabel = stringResource(R.string.action_open),
            destructive = false,
            onConfirm = {
                confirmDonate = false
                // The browser does the networking – this app has no INTERNET permission.
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL)).addCategory(Intent.CATEGORY_BROWSABLE)
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, noBrowser, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { confirmDonate = false },
        )
    }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, highlight: Boolean = false, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = nothingCardStroke(highlight),
        colors = CardDefaults.cardColors(containerColor = CardContainer),
    ) { content() }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, text: String, iconTint: Color = NothingWhite) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = iconTint)
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = NothingWhite, style = MaterialTheme.typography.titleMedium)
            Text(text, color = NothingGrey, style = MaterialTheme.typography.bodySmall)
        }
    }
}
