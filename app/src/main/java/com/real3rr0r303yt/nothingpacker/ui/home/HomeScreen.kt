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

package com.real3rr0r303yt.nothingpacker.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.real3rr0r303yt.nothingpacker.R
import com.real3rr0r303yt.nothingpacker.ui.components.CardContainer
import com.real3rr0r303yt.nothingpacker.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingpacker.ui.components.NothingIconButton
import com.real3rr0r303yt.nothingpacker.ui.components.NothingTopBar
import com.real3rr0r303yt.nothingpacker.ui.components.RedDot
import com.real3rr0r303yt.nothingpacker.ui.components.SectionLabel
import com.real3rr0r303yt.nothingpacker.ui.components.nothingCardStroke
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingRed
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingWhite

/** MIME types offered by the picker; content is verified by the analyser, not by the extension. */
val PACKAGE_MIME_TYPES = arrayOf(
    "application/vnd.android.package-archive",
    "application/octet-stream",
    "application/zip",
    "application/x-zip-compressed",
    "application/x-itunes-ipa",
    "application/x-ios-app",
)

@Composable
fun rememberPackagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPicked(uri)
    }
    return { launcher.launch(PACKAGE_MIME_TYPES) }
}

@Composable
fun HomeScreen(onOpen: (Uri) -> Unit, onOpenSettings: () -> Unit) {
    val pick = rememberPackagePicker(onOpen)

    Column(Modifier.fillMaxSize()) {
        NothingTopBar(title = stringResource(R.string.home_title)) {
            NothingIconButton(Icons.Outlined.Settings, stringResource(R.string.action_settings), onOpenSettings)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RedDot(size = 6.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_offline), style = MaterialTheme.typography.labelSmall, color = NothingGrey)
            }
            Spacer(Modifier.height(20.dp))
            DotMatrixText("APK ⇄ IPA", dotSize = 6.dp, gap = 2.dp, accentLast = true)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyLarge, color = NothingWhite)
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = pick,
                colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = NothingWhite),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Outlined.FolderOpen, null)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.home_pick).uppercase(), style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DirectionCard("APK → IPA", stringResource(R.string.home_apk_to_ipa_text), pick, Modifier.weight(1f))
                DirectionCard("IPA → APK", stringResource(R.string.home_ipa_to_apk_text), pick, Modifier.weight(1f))
            }

            SectionLabel(stringResource(R.string.home_how_title))
            Card(
                shape = MaterialTheme.shapes.medium,
                border = nothingCardStroke(),
                colors = CardDefaults.cardColors(containerColor = CardContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Step("01", stringResource(R.string.home_step_analyse))
                    Step("02", stringResource(R.string.home_step_extract))
                    Step("03", stringResource(R.string.home_step_convert))
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                shape = MaterialTheme.shapes.medium,
                border = nothingCardStroke(highlight = true),
                colors = CardDefaults.cardColors(containerColor = CardContainer),
            ) {
                Text(
                    stringResource(R.string.home_limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = NothingGrey,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DirectionCard(title: String, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        border = nothingCardStroke(),
        colors = CardDefaults.cardColors(containerColor = CardContainer),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DotMatrixText(title, dotSize = 2.2.dp, gap = 0.8.dp)
            Text(text, style = MaterialTheme.typography.bodySmall, color = NothingGrey)
        }
    }
}

@Composable
private fun Step(number: String, text: String) {
    Row {
        Text(number, style = MaterialTheme.typography.labelMedium, color = NothingRed, modifier = Modifier.width(32.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = NothingWhite)
    }
}

