package com.jhaiian.clint

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DownloadsActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: DownloadsAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        findViewById<android.widget.ImageView>(R.id.btn_back).setOnClickListener { finish() }

        val clearBtn = findViewById<TextView>(R.id.btn_clear_downloads)
        recycler = findViewById(R.id.downloads_recycler)
        emptyView = findViewById(R.id.downloads_empty)

        adapter = DownloadsAdapter(
            onCancel = { id -> ClintDownloadManager.cancel(this, id) },
            onOpen = { item ->
                val file = item.file ?: return@DownloadsAdapter
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

    private fun refresh() {
        val list = synchronized(ClintDownloadManager.downloads) {
            ClintDownloadManager.downloads.toList()
        }
        adapter.submitList(list)
        emptyView.isVisible = list.isEmpty()
        recycler.isVisible = list.isNotEmpty()
    }
}
