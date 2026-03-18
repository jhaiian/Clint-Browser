package com.jhaiian.clint

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.webkit.MimeTypeMap

class DownloadsSheet : BottomSheetDialogFragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var adapter: DownloadsAdapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.ThemeOverlay_ClintBrowser_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_downloads, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        val recycler = view.findViewById<RecyclerView>(R.id.downloads_recycler)
        val emptyView = view.findViewById<View>(R.id.downloads_empty)
        val clearBtn = view.findViewById<TextView>(R.id.btn_clear_downloads)

        adapter = DownloadsAdapter(
            onCancel = { id -> ClintDownloadManager.cancel(requireContext(), id) },
            onOpen = { item ->
                val file = item.file ?: return@DownloadsAdapter
                val ext = file.extension.lowercase()
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                try {
                    val uri = FileProvider.getUriForFile(
                        requireContext(), "${requireContext().packageName}.fileprovider", file
                    )
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (_: Exception) {}
            }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        fun refresh() {
            val list = synchronized(ClintDownloadManager.downloads) {
                ClintDownloadManager.downloads.toList()
            }
            adapter?.submitList(list)
            emptyView.isVisible = list.isEmpty()
            recycler.isVisible = list.isNotEmpty()
        }

        clearBtn.setOnClickListener {
            ClintDownloadManager.clearCompleted()
            refresh()
        }

        ClintDownloadManager.onDownloadsChanged = {
            handler.post { refresh() }
        }

        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ClintDownloadManager.onDownloadsChanged = null
    }
}
