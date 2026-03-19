package com.jhaiian.clint

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class DownloadsAdapter(
    private val onCancel: (Int) -> Unit,
    private val onOpen: (ClintDownloadManager.DownloadItem) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

    private val items = mutableListOf<ClintDownloadManager.DownloadItem>()

    fun setItems(newItems: List<ClintDownloadManager.DownloadItem>) {
        if (items.size != newItems.size) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
            return
        }
        newItems.forEachIndexed { i, newItem ->
            val old = items[i]
            if (old.id != newItem.id) {
                items.clear()
                items.addAll(newItems)
                notifyDataSetChanged()
                return
            }
            if (old.bytesDownloaded != newItem.bytesDownloaded ||
                old.totalBytes != newItem.totalBytes ||
                old.status != newItem.status
            ) {
                items[i] = newItem
                notifyItemChanged(i, PAYLOAD_PROGRESS)
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val filename: TextView = view.findViewById(R.id.download_filename)
        val status: TextView = view.findViewById(R.id.download_status)
        val progress: ProgressBar = view.findViewById(R.id.download_progress)
        val btnCancel: ImageView = view.findViewById(R.id.download_cancel)
        val btnOpen: TextView = view.findViewById(R.id.download_open)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.contains(PAYLOAD_PROGRESS)) {
            bindProgress(holder, items[position])
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.filename.text = item.filename
        bindProgress(holder, item)
    }

    private fun bindProgress(holder: ViewHolder, item: ClintDownloadManager.DownloadItem) {
        when (item.status) {
            ClintDownloadManager.DownloadStatus.DOWNLOADING -> {
                val pct = item.progressPercent
                holder.status.text = if (pct >= 0) "$pct%  •  ${formatBytes(item.bytesDownloaded)}" else "Downloading…"
                holder.progress.isVisible = true
                holder.progress.isIndeterminate = pct < 0
                if (pct >= 0) holder.progress.progress = pct
                holder.btnCancel.isVisible = true
                holder.btnOpen.isVisible = false
                holder.btnCancel.setOnClickListener { onCancel(item.id) }
            }
            ClintDownloadManager.DownloadStatus.COMPLETE -> {
                holder.status.text = "Complete  •  ${formatBytes(item.bytesDownloaded)}"
                holder.progress.isVisible = false
                holder.btnCancel.isVisible = false
                holder.btnOpen.isVisible = true
                holder.btnOpen.setOnClickListener { onOpen(item) }
            }
            ClintDownloadManager.DownloadStatus.FAILED -> {
                holder.status.text = "Failed: ${item.errorMessage ?: "Unknown error"}"
                holder.progress.isVisible = false
                holder.btnCancel.isVisible = false
                holder.btnOpen.isVisible = false
            }
            ClintDownloadManager.DownloadStatus.CANCELLED -> {
                holder.status.text = "Cancelled"
                holder.progress.isVisible = false
                holder.btnCancel.isVisible = false
                holder.btnOpen.isVisible = false
            }
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    companion object {
        private const val PAYLOAD_PROGRESS = "progress"
    }
}
