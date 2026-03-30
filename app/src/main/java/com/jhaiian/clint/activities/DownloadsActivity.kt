package com.jhaiian.clint.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.ClintDownloadManager.DownloadItem
import com.jhaiian.clint.downloads.DownloadsAdapter

class DownloadsActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: DownloadsAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView

    private var pendingApkItem: DownloadItem? = null

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val item = pendingApkItem ?: return@registerForActivityResult
        pendingApkItem = null
        if (packageManager.canRequestPackageInstalls()) {
            launchApkInstall(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        val toolbar = findViewById<android.view.View>(R.id.downloads_toolbar)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        findViewById<android.widget.ImageView>(R.id.btn_back).setOnClickListener { finish() }

        val clearBtn = findViewById<TextView>(R.id.btn_clear_downloads)
        recycler = findViewById(R.id.downloads_recycler)
        emptyView = findViewById(R.id.downloads_empty)

        adapter = DownloadsAdapter(
            onCancel = { id -> ClintDownloadManager.cancel(this, id) },
            onOpen = { item -> handleOpen(item) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        clearBtn.setOnClickListener {
            ClintDownloadManager.clearCompleted()
            refresh()
        }

        ClintDownloadManager.onDownloadsChanged = { handler.post { refresh() } }
        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        ClintDownloadManager.onDownloadsChanged = null
    }

    private fun handleOpen(item: DownloadItem) {
        val file = item.file ?: return
        if (file.extension.lowercase() == "apk") {
            handleApkOpen(item)
        } else {
            openFile(item)
        }
    }

    private fun handleApkOpen(item: DownloadItem) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_ClintBrowser_Dialog)
            .setTitle("Install APK")
            .setMessage("Do you want to install \"${item.filename}\"?\n\nOnly install APKs from sources you trust.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Install") { _, _ ->
                if (packageManager.canRequestPackageInstalls()) {
                    launchApkInstall(item)
                } else {
                    showInstallPermissionDialog(item)
                }
            }
            .show()
    }

    private fun showInstallPermissionDialog(item: DownloadItem) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_ClintBrowser_Dialog)
            .setTitle("Permission required")
            .setMessage("To install APK files, Clint needs permission to install unknown apps. You'll be taken to Settings to enable it.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open Settings") { _, _ ->
                pendingApkItem = item
                installPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .show()
    }

    private fun launchApkInstall(item: DownloadItem) {
        val file = item.file ?: return
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    private fun openFile(item: DownloadItem) {
        val file = item.file ?: return
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) {}
    }

    private fun refresh() {
        val list = synchronized(ClintDownloadManager.downloads) {
            ClintDownloadManager.downloads.map { it.copy() }
        }
        adapter.setItems(list)
        emptyView.isVisible = list.isEmpty()
        recycler.isVisible = list.isNotEmpty()
    }
}
