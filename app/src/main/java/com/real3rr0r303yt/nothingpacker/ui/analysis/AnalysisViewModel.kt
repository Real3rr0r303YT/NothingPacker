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

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.real3rr0r303yt.nothingpacker.R
import com.real3rr0r303yt.nothingpacker.core.analysis.PackageAnalyzer
import com.real3rr0r303yt.nothingpacker.core.binary.MalformedFileException
import com.real3rr0r303yt.nothingpacker.core.convert.Extractor
import com.real3rr0r303yt.nothingpacker.core.convert.StructureConverter
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.PackageReport
import com.real3rr0r303yt.nothingpacker.core.model.PackageType
import com.real3rr0r303yt.nothingpacker.core.model.UnsupportedPackageException
import com.real3rr0r303yt.nothingpacker.data.DocumentTreeSink
import com.real3rr0r303yt.nothingpacker.data.ImportedFile
import com.real3rr0r303yt.nothingpacker.data.Icons
import com.real3rr0r303yt.nothingpacker.data.Workspace
import com.real3rr0r303yt.nothingpacker.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

sealed interface AnalysisState {
    data object Loading : AnalysisState
    data class Failed(@StringRes val message: Int) : AnalysisState
    data class Ready(val report: PackageReport, val icon: Bitmap?) : AnalysisState
}

enum class JobKind { CONVERT, EXTRACT }

data class RunningJob(val kind: JobKind, val progress: Float)

/** One-off message for the snackbar: a string resource plus format argument. */
data class UiMessage(@StringRes val text: Int, val arg: Any? = null)

class AnalysisViewModel(application: Application, savedState: SavedStateHandle) : AndroidViewModel(application) {

    private val workspace = Workspace(application)
    private val sourceUri: Uri = Uri.parse(checkNotNull(savedState.get<String>(Routes.ARG_URI)))
    private var imported: ImportedFile? = null

    var state by mutableStateOf<AnalysisState>(AnalysisState.Loading)
        private set
    var job by mutableStateOf<RunningJob?>(null)
        private set

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var running: Job? = null
    private val cancelFlag = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            state = withContext(Dispatchers.IO) {
                try {
                    val file = workspace.import(sourceUri).also { imported = it }
                    val report = PackageAnalyzer.analyze(file.file, file.displayName)
                    val icon = Icons.decode(runCatching { PackageAnalyzer.readIcon(file.file, report) }.getOrNull())
                    AnalysisState.Ready(report, icon)
                } catch (e: UnsupportedPackageException) {
                    AnalysisState.Failed(if (e.message?.contains(".aab") == true) R.string.error_aab else R.string.error_unsupported)
                } catch (e: MalformedFileException) {
                    AnalysisState.Failed(R.string.error_damaged)
                } catch (e: java.io.IOException) {
                    AnalysisState.Failed(R.string.error_io)
                } catch (e: SecurityException) {
                    AnalysisState.Failed(R.string.error_io)
                } catch (e: RuntimeException) {
                    AnalysisState.Failed(R.string.error_damaged)
                }
            }
        }
    }

    fun suggestedArchiveName(): String =
        (state as? AnalysisState.Ready)?.report?.let(StructureConverter::suggestedFileName) ?: "structure.zip"

    /** Writes the structure archive to [target] (a document created via the system picker). */
    fun convert(target: Uri) = launchJob(JobKind.CONVERT, onFailure = { deleteDocument(target) }) { ready, file ->
        val size = if (ready.report.type == PackageType.APK) 180 else 192
        val icon = ready.icon?.let { Icons.toPng(it, size) }
        val resolver = getApplication<Application>().contentResolver
        val result = resolver.openOutputStream(target, "wt")?.use { out ->
            StructureConverter.convert(file.file, ready.report, icon, out, ::onProgress, cancelFlag::get)
        } ?: throw java.io.IOException("Cannot write target")
        UiMessage(R.string.msg_converted, result.files)
    }

    /** Extracts [categories] into a new sub-folder of the picked [tree]. */
    fun extract(tree: Uri, categories: Set<FileCategory>) = launchJob(JobKind.EXTRACT) { ready, file ->
        val folder = ready.report.displayName.replace(Regex("""[\\/:*?"<>|]"""), "_") + "_" + ready.report.type.extension
        val sink = DocumentTreeSink(getApplication(), tree, folder)
        val icon = ready.icon?.let { Icons.toPng(it, it.width.coerceAtMost(1024)) }
        val result = Extractor.extract(file.file, ready.report, categories, icon, sink, ::onProgress, cancelFlag::get)
        UiMessage(R.string.msg_extracted, result.files)
    }

    fun cancelJob() {
        cancelFlag.set(true)
    }

    private fun onProgress(value: Float) {
        // Called from the IO thread; snapshot state writes are thread-safe. Throttled to 1 % steps.
        val current = job ?: return
        if (value >= 1f || value - current.progress >= 0.01f) job = current.copy(progress = value)
    }

    private fun launchJob(
        kind: JobKind,
        onFailure: () -> Unit = {},
        block: suspend (AnalysisState.Ready, ImportedFile) -> UiMessage,
    ) {
        val ready = state as? AnalysisState.Ready ?: return
        val file = imported ?: return
        if (running?.isActive == true) return
        cancelFlag.set(false)
        job = RunningJob(kind, 0f)
        running = viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                try {
                    block(ready, file)
                } catch (e: StructureConverter.Cancelled) {
                    onFailure()
                    UiMessage(R.string.msg_cancelled)
                } catch (e: Exception) {
                    onFailure()
                    UiMessage(R.string.msg_failed, e.message ?: e.javaClass.simpleName)
                }
            }
            job = null
            _messages.send(message)
        }
    }

    private fun deleteDocument(uri: Uri) {
        runCatching { DocumentsContract.deleteDocument(getApplication<Application>().contentResolver, uri) }
    }

    override fun onCleared() {
        cancelFlag.set(true)
        imported?.file?.delete()
    }
}
