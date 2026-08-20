package com.jhaiian.clint.backup

import android.content.Context
import android.webkit.CookieManager
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class BackupResult(
    val includedCategories: Set<BackupCategory>,
    val skippedCategories: Set<BackupCategory>,
    val encrypted: Boolean,
    val totalBytes: Long
)

object BackupManager {

    suspend fun createBackup(
        context: Context,
        categories: Set<BackupCategory>,
        password: CharArray?,
        output: OutputStream
    ): BackupResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (BackupCategory.COOKIES in categories) {
            withContext(Dispatchers.Main) { CookieManager.getInstance().flush() }
        }
        val tempZip = File.createTempFile("clint_backup_", ".zip", appContext.cacheDir)
        try {
            val manifestEntries = mutableListOf<BackupManifestEntry>()
            val includedCategories = mutableSetOf<BackupCategory>()
            val skippedCategories = mutableSetOf<BackupCategory>()

            ZipOutputStream(FileOutputStream(tempZip)).use { zip ->
                for (category in categories) {
                    var addedAny = false
                    for (target in BackupTargets.forCategory(category)) {
                        when (target.type) {
                            BackupEntryType.DATABASE, BackupEntryType.PREFS -> {
                                val file = target.file(appContext)
                                if (file.exists() && file.length() > 0) {
                                    addFileToZip(zip, file, target.zipPath)
                                    manifestEntries.add(BackupManifestEntry(target.id, category.id, target.zipPath, file.length()))
                                    addedAny = true
                                }
                            }
                            BackupEntryType.DIRECTORY -> {
                                val dir = target.file(appContext)
                                if (dir.exists() && dir.isDirectory) {
                                    dir.listFiles()?.filter { it.isFile }?.forEach { child ->
                                        val childZipPath = "${target.zipPath}/${child.name}"
                                        addFileToZip(zip, child, childZipPath)
                                        manifestEntries.add(BackupManifestEntry("${target.id}:${child.name}", category.id, childZipPath, child.length()))
                                        addedAny = true
                                    }
                                }
                            }
                        }
                    }
                    if (addedAny) includedCategories.add(category) else skippedCategories.add(category)
                }

                val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                val manifest = BackupManifest(
                    format = BACKUP_FORMAT_MAGIC,
                    formatVersion = BACKUP_FORMAT_VERSION,
                    appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                    appVersionName = packageInfo.versionName ?: "",
                    createdAt = System.currentTimeMillis(),
                    categories = includedCategories.map { it.id },
                    entries = manifestEntries
                )
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toJson().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            var encrypted = false
            if (password != null && password.isNotEmpty()) {
                val salt = BackupCrypto.randomSalt()
                val iv = BackupCrypto.randomIv()
                val key = BackupCrypto.deriveKey(password, salt, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM)
                BackupCrypto.writeEncryptedHeader(output, salt, iv, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM)
                FileInputStream(tempZip).use { input ->
                    BackupCrypto.encryptStream(input, output, key, iv)
                }
                key.fill(0)
                encrypted = true
            } else {
                BackupCrypto.writeUnencryptedHeader(output)
                FileInputStream(tempZip).use { input ->
                    input.copyTo(output)
                }
            }
            output.flush()

            BackupResult(includedCategories, skippedCategories, encrypted, tempZip.length())
        } finally {
            tempZip.delete()
            password?.fill('\u0000')
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, zipPath: String) {
        zip.putNextEntry(ZipEntry(zipPath))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }
}
