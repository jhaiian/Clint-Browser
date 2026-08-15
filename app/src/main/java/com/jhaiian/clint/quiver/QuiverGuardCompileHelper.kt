package com.jhaiian.clint.quiver

import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.quiver.engine.CompileEvent
import com.jhaiian.clint.quiver.engine.CompileResult
import com.jhaiian.clint.quiver.engine.CompileStage
import com.jhaiian.clint.quiver.engine.CompiledManifest
import com.jhaiian.clint.quiver.engine.CompiledManifestData
import com.jhaiian.clint.quiver.engine.CompiledManifestEntry
import com.jhaiian.clint.quiver.engine.FilterListCompileInput
import com.jhaiian.clint.quiver.engine.QuiverGuardCompiler
import com.jhaiian.clint.quiver.engine.QuiverGuardPaths
import com.jhaiian.clint.quiver.engine.QuiverGuardWebIntegration
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat

// Shown the first time a user enables Quiver Guard to explain that no lists are active
// yet and that they need to download and compile at least one before filtering takes effect.
internal fun QuiverGuardActivity.showSetupGuideDialog() {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.quiver_guard_no_active_lists_title),
        message = getString(R.string.quiver_guard_no_active_lists_message),
        positiveLabel = getString(R.string.action_ok),
        onPositive = {
            // Show the experimental feature notice on first entry so the user is aware
            // that the feature may have rough edges.
            if (!prefs.getBoolean(QuiverGuardActivity.PREF_EXPERIMENTAL_SHOWN, false)) {
                showExperimentalDialog()
            }
        }
    )
}

// Displays a one-time notice that Quiver Guard is an experimental feature. The dialog
// itself (ExperimentalDialog in QuiverGuardStatusDialogs.kt) owns the 3-second countdown
// that keeps its OK button disabled; this function only marks the notice as shown and
// opens it.
internal fun QuiverGuardActivity.showExperimentalDialog() {
    PreferenceManager.getDefaultSharedPreferences(this)
        .edit()
        .putBoolean(QuiverGuardActivity.PREF_EXPERIMENTAL_SHOWN, true)
        .apply()
    uiState.experimentalDialogOpen = true
}

// Handles the hardware or gesture back action. If a compile is running, back is
// suppressed so the user cannot leave mid-compile. If there are unsaved changes, a
// dialog asks whether to compile or discard them before exiting.
internal fun QuiverGuardActivity.handleBackNavigation() {
    if (uiState.isCompileRunning) return
    if (!isConfigurationDirty()) {
        finish()
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.quiver_guard_back_dialog_title),
        message = getString(R.string.quiver_guard_back_dialog_message),
        neutralLabel = getString(R.string.action_cancel),
        negativeLabel = getString(R.string.quiver_guard_back_dialog_discard),
        onNegative = { discardPendingChanges(); finish() },
        positiveLabel = getString(R.string.quiver_guard_back_dialog_compile),
        onPositive = { startCompilation() }
    )
}

// Called on activity start to check whether the compiled database is consistent with
// the current filter list configuration. A mismatch is possible when:
//   - the app was updated and default lists changed;
//   - the user modified lists in a previous session without compiling;
//   - the database file was deleted externally.
// When a mismatch is detected, isStartupDirty is set and a banner prompts the user
// to recompile.
internal fun QuiverGuardActivity.performStartupValidation() {
    val dbFile = QuiverGuardPaths.databaseFile(this)
    val manifest = CompiledManifest.read(QuiverGuardPaths.manifestFile(this))

    if (!dbFile.exists() || manifest == null) {
        uiState.bannerText = getString(R.string.quiver_guard_banner_no_database)
        return
    }

    val currentLists = database().getAllFilterLists()
    // The manual filter's own entry, if any, is compared separately below via
    // isManualFilterDirty since it has no corresponding row in FilterListDatabase.
    val manifestMap = manifest.entries.filterNot { it.id == ManualFilterState.COMPILE_ID }.associateBy { it.id }

    // Compare the count and per-list enabled flags. Count mismatch means lists were
    // added or removed since the last compile; flag mismatch means lists were toggled
    // without recompiling.
    var diffFound = currentLists.size != manifestMap.size
    if (!diffFound) {
        for (fl in currentLists) {
            val entry = manifestMap[fl.id]
            if (entry == null || fl.isEnabled != entry.isEnabled) {
                diffFound = true
                break
            }
        }
    }
    if (!diffFound) diffFound = isManualFilterDirty(manifest)

    if (diffFound) {
        uiState.isStartupDirty = true
    }
}

// Drives the full compile workflow: builds inputs from the effective list state, shows
// a progress state with stage labels and a live rule counter, and delegates to
// QuiverGuardCompiler.compile. On success, persists the new compiled state to the
// database and manifest and activates the newly compiled engine. On failure, shows an
// error dialog with a retry option.
internal fun QuiverGuardActivity.startCompilation() {
    if (uiState.isCompileRunning) return

    val effectiveLists = effectiveFilterLists()
    val enabledAndDownloaded = effectiveLists.filter { it.isEnabled && it.isDownloaded }
    val manualFilterRules = manualFilterDb().getAllRules()
    val manualFilterContributes = ManualFilterState.isEnabled(this) && manualFilterRules.isNotEmpty()

    if (enabledAndDownloaded.isEmpty() && !manualFilterContributes) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.quiver_guard_compile_progress_title),
            message = getString(R.string.quiver_guard_banner_no_database),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }

    uiState.isCompileRunning = true

    val inputs = enabledAndDownloaded.map { fl ->
        FilterListCompileInput(
            id = fl.id, name = fl.name,
            rulesFile = FilterListDownloader.localFileFor(applicationContext, fl.id)
        )
    } + if (manualFilterContributes) {
        // Writing the rule set to disk right before compiling, rather than keeping it
        // continuously in sync, avoids a redundant file write on every add/edit/delete in
        // ManualFilterActivity for a file only the compiler ever reads.
        ManualFilterDatabase.writeRulesFile(applicationContext, manualFilterRules)
        listOf(
            FilterListCompileInput(
                id = ManualFilterState.COMPILE_ID,
                name = getString(R.string.quiver_guard_manual_filter_title),
                rulesFile = ManualFilterDatabase.rulesFile(applicationContext)
            )
        )
    } else {
        emptyList()
    }

    val outputFile = QuiverGuardPaths.databaseFile(this)
    val tempFile = QuiverGuardPaths.tempDatabaseFile(this)

    uiState.compileProgress = CompileProgressUi(
        stageText = "", listCounterText = "", rulesText = "",
        elapsedText = getString(R.string.quiver_guard_compile_progress_elapsed, "0s")
    )

    val compileStartMs = System.currentTimeMillis()
    // A separate timer coroutine updates the elapsed time every 500 ms independently of
    // the progress events emitted by the compiler, so the clock ticks smoothly even
    // between long-running parsing bursts.
    var timerJob: Job? = null
    timerJob = activityScope.launch {
        while (true) {
            delay(500L)
            val elapsedSec = (System.currentTimeMillis() - compileStartMs) / 1000L
            uiState.compileProgress = uiState.compileProgress?.copy(
                elapsedText = getString(R.string.quiver_guard_compile_progress_elapsed, formatElapsedSeconds(elapsedSec))
            )
        }
    }

    activityScope.launch {
        try {
            QuiverGuardCompiler.compile(inputs, outputFile, tempFile).collect { event ->
                when (event) {
                    is CompileEvent.Progress -> {
                        val p = event.progress
                        uiState.compileProgress = uiState.compileProgress?.copy(
                            listCounterText = getString(R.string.quiver_guard_compile_progress_list, p.completedLists, p.totalLists),
                            stageText = compileStageLabel(p.stage, p.currentFilterListName),
                            rulesText = getString(R.string.quiver_guard_compile_progress_rules, NumberFormat.getNumberInstance().format(p.rulesProcessed))
                        )
                    }
                    is CompileEvent.Completed -> {
                        timerJob.cancel()
                        uiState.compileProgress = null
                        when (val r = event.result) {
                            is CompileResult.Success -> onCompileSuccess(r, inputs.size, effectiveLists, manualFilterContributes)
                            is CompileResult.Failure -> onCompileFailure(r)
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            timerJob.cancel()
            uiState.compileProgress = null
            throw e
        } catch (e: Exception) {
            timerJob.cancel()
            uiState.compileProgress = null
            onCompileFailure(CompileResult.Failure(e.message ?: e.javaClass.simpleName, null, e))
        } finally {
            uiState.isCompileRunning = false
        }
    }
}

// Called after a successful compile. Flushes pending removals (deletes files and
// database rows), persists the new enabled states and compilation timestamps, writes
// the manifest, clears all pending changes, and activates the newly compiled engine so
// filtering with the new rules starts immediately.
private fun QuiverGuardActivity.onCompileSuccess(
    result: CompileResult.Success,
    compiledListCount: Int,
    effectiveLists: List<FilterList>,
    manualFilterIncluded: Boolean
) {
    val compiledAtMillis = System.currentTimeMillis()
    val survivingLists = database().getAllFilterLists().filterNot { it.id in uiState.pendingRemovedIds }

    for (id in uiState.pendingRemovedIds) {
        val localFile = FilterListDownloader.localFileFor(applicationContext, id)
        if (localFile.exists()) localFile.delete()
        database().deleteFilterList(id)
    }

    val enabledStates = survivingLists.associate { fl ->
        fl.id to (uiState.pendingEnabledOverrides[fl.id] ?: fl.isEnabled)
    }
    database().commitCompiledState(enabledStates, compiledAtMillis)

    val filterListEntries = effectiveLists.filterNot { it.id in uiState.pendingRemovedIds }.map { fl ->
        CompiledManifestEntry(
            id = fl.id,
            name = fl.name,
            downloadUrl = fl.downloadUrl,
            isCustom = fl.isCustom,
            isEnabled = enabledStates[fl.id] ?: fl.isEnabled,
            // Encodes enough information to detect content changes without re-reading the file.
            contentFingerprint = "${fl.id}:${fl.downloadedAt}:${fl.ruleCount}"
        )
    }
    // Recorded as its own manifest entry, using the same reserved id startCompilation() gave
    // it, so isManualFilterDirty can find it again next time without scanning file contents.
    val manualFilterEntries = if (manualFilterIncluded) {
        val rules = manualFilterDb().getAllRules()
        listOf(
            CompiledManifestEntry(
                id = ManualFilterState.COMPILE_ID,
                name = getString(R.string.quiver_guard_manual_filter_title),
                downloadUrl = "",
                isCustom = true,
                isEnabled = true,
                contentFingerprint = ManualFilterState.contentFingerprint(rules)
            )
        )
    } else {
        emptyList()
    }

    CompiledManifest.write(
        QuiverGuardPaths.manifestFile(this),
        CompiledManifestData(
            compiledAtMillis = compiledAtMillis,
            entries = filterListEntries + manualFilterEntries,
            totalRuleLines = result.statistics.ruleLines,
            outputFileSizeBytes = result.outputFileSizeBytes,
            durationMs = result.durationMs
        )
    )

    uiState.pendingEnabledOverrides = emptyMap()
    uiState.pendingRemovedIds = emptySet()
    uiState.isStartupDirty = false
    uiState.bannerText = null
    refreshFilterListDisplay()
    QuiverGuardWebIntegration.onCompileComplete(this)

    showCompileSuccessDialog(result, compiledListCount)
}

private fun QuiverGuardActivity.onCompileFailure(result: CompileResult.Failure) {
    showCompileFailureDialog(result)
}

// Builds the grid-style success result rows. adblock-rust doesn't expose a duplicate-rule
// count or a per-rule rejection reason the way the old compiler did, so those rows (and
// the "unsupported rules" detail dialog that used to hang off one of them) are gone
// entirely rather than shown with fabricated data.
private fun QuiverGuardActivity.showCompileSuccessDialog(result: CompileResult.Success, listCount: Int) {
    val fmt = NumberFormat.getNumberInstance()
    val s = result.statistics
    uiState.compileResult = CompileResultUi(
        isSuccess = true,
        title = getString(R.string.quiver_guard_compile_success_title),
        rows = listOf(
            CompileResultRow(getString(R.string.quiver_guard_compile_result_label_lists), fmt.format(listCount)),
            CompileResultRow(getString(R.string.quiver_guard_compile_result_label_rules), fmt.format(s.ruleLines)),
            CompileResultRow(getString(R.string.quiver_guard_compile_result_label_comments), fmt.format(s.commentLines)),
            CompileResultRow(getString(R.string.quiver_guard_compile_result_label_empty), fmt.format(s.emptyLines)),
            CompileResultRow(getString(R.string.quiver_guard_compile_result_label_size), formatFileSize(result.outputFileSizeBytes)),
            CompileResultRow(getString(R.string.quiver_guard_compile_result_label_duration), formatElapsedSeconds(result.durationMs / 1000L))
        )
    )
}

// Shows an error result with the failed list name, error message, and a note that the
// previous compiled engine is still active. No stat rows since no statistics are
// available on failure; onRetry re-runs startCompilation.
private fun QuiverGuardActivity.showCompileFailureDialog(result: CompileResult.Failure) {
    val detail = buildString {
        result.failedFilterListName?.let {
            append(getString(R.string.quiver_guard_compile_failure_failed_list, it))
            append("\n")
        }
        append(getString(R.string.quiver_guard_compile_failure_details, result.message))
        append("\n")
        append(getString(R.string.quiver_guard_compile_failure_previous_active))
    }
    uiState.compileResult = CompileResultUi(
        isSuccess = false,
        title = getString(R.string.quiver_guard_compile_failure_title),
        rows = emptyList(),
        failureDetail = detail,
        onRetry = { startCompilation() }
    )
}

// Maps a CompileStage enum to the human-readable label shown in the progress dialog.
private fun QuiverGuardActivity.compileStageLabel(stage: CompileStage, currentList: String?): String =
    when (stage) {
        CompileStage.PREPARING  -> getString(R.string.quiver_guard_compile_progress_stage_preparing)
        CompileStage.READING    -> getString(R.string.quiver_guard_compile_progress_stage_reading, currentList ?: "")
        CompileStage.PARSING    -> getString(R.string.quiver_guard_compile_progress_stage_parsing, currentList ?: "")
        CompileStage.FINALIZING -> getString(R.string.quiver_guard_compile_progress_stage_finalizing)
    }

// Formats a duration as "Nm Ns" when >= 60 seconds, or just "Ns" otherwise.
internal fun formatElapsedSeconds(totalSeconds: Long): String {
    val m = totalSeconds / 60L
    val s = totalSeconds % 60L
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
