package com.jhaiian.clint.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipFile

data class StagedBackupFile(val rawFile: File, val header: BackupContainerHeader)

sealed class StageResult {
    data class Staged(val file: StagedBackupFile) : StageResult()
    object InvalidFile : StageResult()
    data class Error(val message: String?) : StageResult()
}

sealed class UnlockResult {
    data class Ready(val plainZipFile: File, val manifest: BackupManifest, val availableCategories: List<BackupCategory>) : UnlockResult()
    object WrongPassword : UnlockResult()
    object InvalidFile : UnlockResult()
    object UnsupportedVersion : UnlockResult()
    data class Error(val message: String?) : UnlockResult()
}

data class RestoreOutcome(
    val restoredCategories: Set<BackupCategory>,
    val unavailableCategories: Set<BackupCategory>
)

object RestoreManager {

    suspend fun stageFromUri(context: Context, uri: Uri): StageResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val tempRaw = File.createTempFile("clint_restore_", ".raw", appContext.cacheDir)
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempRaw).use { output -> input.copyTo(output) }
            } ?: return@withContext StageResult.Error(null)

            val header = FileInputStream(tempRaw).use { BackupCrypto.readHeader(it) }
                ?: run { tempRaw.delete(); return@withContext StageResult.InvalidFile }

            StageResult.Staged(StagedBackupFile(tempRaw, header))
        } catch (e: Exception) {
            tempRaw.delete()
            StageResult.Error(e.message)
        }
    }

    suspend fun unlock(context: Context, staged: StagedBackupFile, password: CharArray?): UnlockResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val plainZip = File.createTempFile("clint_restore_plain_", ".zip", appContext.cacheDir)
        try {
            if (staged.header.encrypted) {
                if (password == null || password.isEmpty()) {
                    plainZip.delete()
                    return@withContext UnlockResult.Error("Password required")
                }
                val key = BackupCrypto.deriveKey(
                    password,
                    staged.header.salt,
                    staged.header.memoryKB,
                    staged.header.iterations,
                    staged.header.parallelism
                )
                try {
                    FileInputStream(staged.rawFile).use { rawInput ->
                        skipHeader(rawInput, staged.header)
                        FileOutputStream(plainZip).use { plainOutput ->
                            BackupCrypto.decryptStream(rawInput, plainOutput, key, staged.header.iv)
                        }
                    }
                } catch (e: Exception) {
                    plainZip.delete()
                    key.fill(0)
                    return@withContext UnlockResult.WrongPassword
                }
                key.fill(0)
            } else {
                FileInputStream(staged.rawFile).use { rawInput ->
                    skipHeader(rawInput, staged.header)
                    FileOutputStream(plainZip).use { plainOutput -> rawInput.copyTo(plainOutput) }
                }
            }

            val manifest = readManifest(plainZip) ?: run { plainZip.delete(); staged.rawFile.delete(); return@withContext UnlockResult.InvalidFile }
            if (manifest.format != BACKUP_FORMAT_MAGIC) {
                plainZip.delete()
                staged.rawFile.delete()
                return@withContext UnlockResult.InvalidFile
            }
            if (manifest.formatVersion > BACKUP_FORMAT_VERSION) {
                plainZip.delete()
                staged.rawFile.delete()
                return@withContext UnlockResult.UnsupportedVersion
            }
            val available = manifest.entries.mapNotNull { BackupCategory.fromId(it.category) }.toSet()
            val orderedAvailable = BackupCategory.entries.filter { it in available }
            staged.rawFile.delete()
            UnlockResult.Ready(plainZip, manifest, orderedAvailable)
        } catch (e: Exception) {
            plainZip.delete()
            staged.rawFile.delete()
            UnlockResult.Error(e.message)
        }
    }

    suspend fun performRestore(
        context: Context,
        plainZipFile: File,
        manifest: BackupManifest,
        selectedCategories: Set<BackupCategory>
    ): RestoreOutcome = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val restored = mutableSetOf<BackupCategory>()
        val unavailable = mutableSetOf<BackupCategory>()

        ZipFile(plainZipFile).use { zip ->
            for (category in selectedCategories) {
                var restoredAny = false
                for (target in BackupTargets.forCategory(category)) {
                    when (target.type) {
                        BackupEntryType.DATABASE, BackupEntryType.PREFS -> {
                            val entry = manifest.entries.firstOrNull { it.id == target.id && it.category == category.id }
                            val zipEntry = entry?.let { zip.getEntry(it.zipPath) }
                            if (zipEntry != null) {
                                val destFile = target.file(appContext)
                                restoreSingleFile(appContext, zip, zipEntry, destFile, target.type)
                                restoredAny = true
                            }
                        }
                        BackupEntryType.DIRECTORY -> {
                            val dirEntries = manifest.entries.filter { it.category == category.id && it.id.startsWith("${target.id}:") }
                            if (dirEntries.isNotEmpty()) {
                                val destDir = target.file(appContext)
                                destDir.mkdirs()
                                for (entry in dirEntries) {
                                    val zipEntry = zip.getEntry(entry.zipPath) ?: continue
                                    val safeName = File(entry.zipPath).name
                                    if (safeName.isBlank() || safeName == "." || safeName == "..") continue
                                    val destFile = File(destDir, safeName)
                                    restoreSingleFile(appContext, zip, zipEntry, destFile, BackupEntryType.DIRECTORY)
                                    restoredAny = true
                                }
                            }
                        }
                    }
                }
                if (restoredAny) restored.add(category) else unavailable.add(category)
            }
        }

        if (BackupCategory.QUIVER_GUARD in restored) {
            val compiledArtifactsRestored = manifest.entries.any {
                it.category == BackupCategory.QUIVER_GUARD.id &&
                    (it.id == "quiver_guard_compiled_db" || it.id == "quiver_guard_compiled_manifest")
            }
            if (!compiledArtifactsRestored) {
                BackupTargets.quiverGuardCompiledArtifacts(appContext).forEach { it.delete() }
            }
        }

        plainZipFile.delete()
        RestoreOutcome(restored, unavailable)
    }

    fun discard(staged: StagedBackupFile) {
        staged.rawFile.delete()
    }

    fun discardPlain(file: File) {
        file.delete()
    }

    private fun restoreSingleFile(context: Context, zip: ZipFile, zipEntry: java.util.zip.ZipEntry, destFile: File, type: BackupEntryType) {
        destFile.parentFile?.mkdirs()
        val tempFile = File(destFile.parentFile, "${destFile.name}.restoring")
        zip.getInputStream(zipEntry).use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        if (type == BackupEntryType.DATABASE) {
            if (destFile.parentFile == context.getDatabasePath(destFile.name).parentFile) {
                context.deleteDatabase(destFile.name)
            } else {
                deleteWithSidecars(destFile)
            }
        } else {
            destFile.delete()
        }
        if (!tempFile.renameTo(destFile)) {
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
        }
    }

    private fun deleteWithSidecars(file: File) {
        file.delete()
        val parent = file.parentFile ?: return
        File(parent, "${file.name}-journal").delete()
        File(parent, "${file.name}-wal").delete()
        File(parent, "${file.name}-shm").delete()
    }

    private fun readManifest(zipFile: File): BackupManifest? {
        return runCatching {
            ZipFile(zipFile).use { zip ->
                val entry = zip.getEntry("manifest.json") ?: return null
                val json = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                BackupManifest.fromJson(json)
            }
        }.getOrNull()
    }

    private fun skipHeader(input: FileInputStream, header: BackupContainerHeader) {
        val skipBytes = if (header.encrypted) {
            8 + 1 + 4 + 4 + 4 + 2 + header.salt.size + 2 + header.iv.size
        } else {
            8 + 1
        }
        input.channel.position(skipBytes.toLong())
    }
}
