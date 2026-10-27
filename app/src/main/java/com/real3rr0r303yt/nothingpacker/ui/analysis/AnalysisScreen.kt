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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.real3rr0r303yt.nothingpacker.R
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.PackageReport
import com.real3rr0r303yt.nothingpacker.core.model.PackageType
import com.real3rr0r303yt.nothingpacker.ui.components.CardContainer
import com.real3rr0r303yt.nothingpacker.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingpacker.ui.components.EmptyState
import com.real3rr0r303yt.nothingpacker.ui.components.NothingTopBar
import com.real3rr0r303yt.nothingpacker.ui.components.RedDot
import com.real3rr0r303yt.nothingpacker.ui.components.SectionLabel
import com.real3rr0r303yt.nothingpacker.ui.components.nothingCardStroke
import com.real3rr0r303yt.nothingpacker.ui.formatBytes
import com.real3rr0r303yt.nothingpacker.ui.label
import com.real3rr0r303yt.nothingpacker.ui.theme.MonoFamily
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingBlack
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingGreyDim
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingOutline
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingRed
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingSurface
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingSurfaceHigh
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingWhite

private enum class AnalysisTab(val label: Int) {
    OVERVIEW(R.string.tab_overview),
    PERMISSIONS(R.string.tab_permissions),
    COMPONENTS(R.string.tab_components),
    FILES(R.string.tab_files),
    MANIFEST(R.string.tab_manifest),
}

private const val MANIFEST_LINE_LIMIT = 4000

@Composable
fun AnalysisScreen(onBack: () -> Unit, viewModel: AnalysisViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showExtract by remember { mutableStateOf(false) }
    var pendingCategories by remember { mutableStateOf<Set<FileCategory>>(emptySet()) }

    val createArchive = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.convert(uri)
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.extract(uri, pendingCategories)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            val text = if (msg.arg != null) context.getString(msg.text, msg.arg) else context.getString(msg.text)
            snackbar.showSnackbar(text)
        }
    }

    val state = viewModel.state
    val title = (state as? AnalysisState.Ready)?.report?.type?.name ?: "· · ·"

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NothingTopBar(title = title, onBack = onBack)
            when (state) {
                AnalysisState.Loading -> LoadingState()
                is AnalysisState.Failed -> FailedState(state.message, onBack)
                is AnalysisState.Ready -> ReadyContent(
                    state = state,
                    onConvert = { createArchive.launch(viewModel.suggestedArchiveName()) },
                    onExtract = { showExtract = true },
                )
            }
        }

        viewModel.job?.let { job ->
            JobBar(job, onCancel = viewModel::cancelJob, modifier = Modifier.align(Alignment.BottomCenter))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp))
    }

    val ready = state as? AnalysisState.Ready
    if (showExtract && ready != null) {
        ExtractDialog(
            report = ready.report,
            onDismiss = { showExtract = false },
            onConfirm = { categories ->
                showExtract = false
                pendingCategories = categories
                pickFolder.launch(null)
            },
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(color = NothingRed, trackColor = NothingOutline, modifier = Modifier.width(160.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.home_loading), style = MaterialTheme.typography.labelMedium, color = NothingGrey)
    }
}

@Composable
private fun FailedState(message: Int, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyState(stringResource(R.string.error_title), stringResource(message))
        OutlinedButton(onClick = onBack, shape = CircleShape) {
            Text(stringResource(R.string.action_retry).uppercase(), color = NothingWhite, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ReadyContent(state: AnalysisState.Ready, onConvert: () -> Unit, onExtract: () -> Unit) {
    val report = state.report
    var tab by rememberSaveable { mutableStateOf(AnalysisTab.OVERVIEW) }
    var filter by rememberSaveable { mutableStateOf<FileCategory?>(null) }
    val manifestLines = remember(report) { report.manifestText.lines() }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") { Header(state) }
        item(key = "stats") { Stats(report) }
        if (report.warnings.isNotEmpty()) {
            item(key = "warnings") { Warnings(report) }
        }
        item(key = "actions") { ActionsCard(report, onConvert, onExtract) }
        item(key = "tabs") {
            ScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = NothingBlack,
                contentColor = NothingWhite,
                edgePadding = 0.dp,
                divider = { Box(Modifier.fillMaxWidth().height(1.dp).background(NothingOutline)) },
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tab.ordinal]), color = NothingRed)
                },
                modifier = Modifier.padding(top = 20.dp),
            ) {
                AnalysisTab.entries.forEach { t ->
                    Tab(
                        selected = t == tab,
                        onClick = { tab = t },
                        text = { Text(stringResource(t.label), style = MaterialTheme.typography.labelLarge) },
                        selectedContentColor = NothingWhite,
                        unselectedContentColor = NothingGrey,
                    )
                }
            }
        }
        item(key = "tab_spacer") { Spacer(Modifier.height(12.dp)) }
        when (tab) {
            AnalysisTab.OVERVIEW -> overview(report)
            AnalysisTab.PERMISSIONS -> permissions(report)
            AnalysisTab.COMPONENTS -> components(report)
            AnalysisTab.FILES -> files(report, filter) { filter = it }
            AnalysisTab.MANIFEST -> manifest(report, manifestLines)
        }
    }
}

@Composable
private fun Header(state: AnalysisState.Ready) {
    val report = state.report
    Row(Modifier.padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.large)
                .background(NothingSurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (state.icon != null) {
                Image(state.icon.asImageBitmap(), null, Modifier.fillMaxSize())
            } else {
                Icon(
                    if (report.type == PackageType.APK) Icons.Outlined.Android else Icons.Outlined.PhoneIphone,
                    null,
                    tint = NothingGrey,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(report.displayName, style = MaterialTheme.typography.titleLarge, color = NothingWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
            report.identifier?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = NothingGrey, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                listOfNotNull(report.versionName, report.versionCode?.let { "($it)" }).joinToString(" "),
                style = MaterialTheme.typography.labelSmall,
                color = NothingGreyDim,
            )
        }
    }
}

@Composable
private fun Stats(report: PackageReport) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(report.entries.size.toString(), stringResource(R.string.analysis_files), Modifier.weight(1f))
        StatTile(report.permissions.size.toString(), stringResource(R.string.analysis_permissions), Modifier.weight(1f))
        StatTile(report.components.size.toString(), stringResource(R.string.analysis_components), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        border = nothingCardStroke(),
        colors = CardDefaults.cardColors(containerColor = CardContainer),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DotMatrixText(value, dotSize = 3.dp, gap = 1.dp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = NothingGrey, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Warnings(report: PackageReport) {
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        report.warnings.forEach { w ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(NothingRed.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.WarningAmber, null, tint = NothingRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(w.label()), style = MaterialTheme.typography.bodySmall, color = NothingWhite)
            }
        }
    }
}

@Composable
private fun ActionsCard(report: PackageReport, onConvert: () -> Unit, onExtract: () -> Unit) {
    val target = report.type.counterpart.name
    Card(
        shape = MaterialTheme.shapes.medium,
        border = nothingCardStroke(highlight = true),
        colors = CardDefaults.cardColors(containerColor = CardContainer),
        modifier = Modifier.padding(top = 10.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotMatrixText("${report.type.name} → $target", dotSize = 2.6.dp, gap = 1.dp, accentLast = true)
            }
            Text(stringResource(R.string.analysis_convert_text, target), style = MaterialTheme.typography.bodySmall, color = NothingGrey)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onConvert,
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = NothingWhite),
                    shape = CircleShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.SwapHoriz, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_convert, target), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(onClick = onExtract, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Unarchive, null, Modifier.size(18.dp), tint = NothingWhite)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_extract), color = NothingWhite, maxLines = 1)
                }
            }
        }
    }
}

private fun LazyListScope.overview(report: PackageReport) {
    item(key = "overview") {
        val none = stringResource(R.string.field_none)
        val locale = LocalConfiguration.current.locales[0]
        val rows = listOf(
            R.string.field_identifier to (report.identifier ?: none),
            R.string.field_version to (report.versionName ?: none),
            R.string.field_build to (report.versionCode ?: none),
            R.string.field_min_os to (report.minOs ?: none),
            R.string.field_target_os to (report.targetOs ?: none),
            R.string.field_file_size to formatBytes(report.fileSize, locale),
            R.string.field_unpacked_size to formatBytes(report.uncompressedSize, locale),
            R.string.field_architectures to report.architectures.ifEmpty { listOf(none) }.joinToString(", "),
            R.string.field_signature to (report.signature ?: stringResource(R.string.field_unsigned)),
        )
        Column {
            rows.forEach { (label, value) -> KeyValue(stringResource(label), value) }
            report.extra.forEach { (k, v) -> KeyValue(k, v, mono = true) }
            SectionLabel(stringResource(R.string.tab_files).uppercase())
            FileCategory.entries.forEach { c ->
                report.stats[c]?.let { s ->
                    KeyValue(stringResource(c.label()), "${pluralStringResource(R.plurals.file_count, s.count, s.count)} · ${formatBytes(s.bytes, locale)}")
                }
            }
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(key, style = MaterialTheme.typography.labelMedium, color = NothingGrey, modifier = Modifier.weight(0.42f))
        SelectionContainer(Modifier.weight(0.58f)) {
            Text(
                value,
                style = if (mono) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                color = NothingWhite,
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(NothingOutline))
}

private fun LazyListScope.permissions(report: PackageReport) {
    if (report.permissions.isEmpty()) {
        item(key = "perm_empty") { EmptyHint(stringResource(R.string.analysis_empty_permissions)) }
        return
    }
    items(report.permissions, key = { "p_" + it.name }) { p ->
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RedDot(size = 5.dp)
                Spacer(Modifier.width(10.dp))
                SelectionContainer {
                    Text(p.name, style = MaterialTheme.typography.labelMedium, color = NothingWhite)
                }
            }
            p.detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = NothingGrey, modifier = Modifier.padding(start = 15.dp, top = 4.dp))
            }
        }
    }
}

private fun LazyListScope.components(report: PackageReport) {
    if (report.components.isEmpty()) {
        item(key = "comp_empty") { EmptyHint(stringResource(R.string.analysis_empty_components)) }
        return
    }
    items(report.components.withIndex().toList(), key = { "c_${it.index}" }) { (_, c) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(c.kind.label()).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = NothingGrey,
                modifier = Modifier.width(96.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                c.name,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFamily),
                color = NothingWhite,
                modifier = Modifier.weight(1f),
            )
            if ("launcher" in c.flags) RedDot(size = 6.dp)
        }
    }
}

private fun LazyListScope.files(report: PackageReport, filter: FileCategory?, onFilter: (FileCategory?) -> Unit) {
    item(key = "file_filters") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip(stringResource(R.string.category_all), filter == null) { onFilter(null) }
            FileCategory.entries.filter { report.stats.containsKey(it) }.forEach { c ->
                CategoryChip("${stringResource(c.label())} ${report.stats[c]!!.count}", filter == c) { onFilter(c) }
            }
        }
    }
    val shown = if (filter == null) report.entries else report.entries.filter { it.category == filter }
    items(shown, key = { "f_" + it.path }) { e ->
        val locale = LocalConfiguration.current.locales[0]
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                e.path,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFamily, fontSize = 12.sp),
                color = NothingWhite,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(formatBytes(e.size, locale), style = MaterialTheme.typography.labelSmall, color = NothingGreyDim)
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = NothingBlack,
            labelColor = NothingGrey,
            selectedContainerColor = NothingWhite,
            selectedLabelColor = NothingBlack,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = NothingOutline,
            selectedBorderColor = NothingWhite,
        ),
    )
}

private fun LazyListScope.manifest(report: PackageReport, lines: List<String>) {
    item(key = "manifest_path") {
        Text(report.manifestPath, style = MaterialTheme.typography.labelSmall, color = NothingRed, modifier = Modifier.padding(bottom = 8.dp))
    }
    val shown = lines.take(MANIFEST_LINE_LIMIT)
    items(shown.size, key = { "m_$it" }) { i ->
        Text(
            shown[i],
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
            color = if (shown[i].trimStart().startsWith("<!--")) NothingGreyDim else NothingWhite,
            softWrap = true,
        )
    }
    if (lines.size > MANIFEST_LINE_LIMIT) {
        item(key = "manifest_more") { EmptyHint(stringResource(R.string.analysis_manifest_truncated, MANIFEST_LINE_LIMIT)) }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = NothingGrey, modifier = Modifier.padding(vertical = 24.dp))
}

@Composable
private fun JobBar(job: RunningJob, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(NothingSurface)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Inventory2, null, tint = NothingRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(if (job.kind == JobKind.CONVERT) R.string.job_converting else R.string.job_extracting),
                style = MaterialTheme.typography.labelLarge,
                color = NothingWhite,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel).uppercase(), color = NothingRed) }
        }
        LinearProgressIndicator(
            progress = { job.progress },
            color = NothingRed,
            trackColor = NothingOutline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
