package com.jhaiian.clint.bookmarks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.content.res.ResourcesCompat
import android.content.res.ColorStateList
import androidx.recyclerview.widget.RecyclerView
import com.jhaiian.clint.R
import java.net.URL
import java.util.concurrent.Executors

class BookmarksAdapter(
    private val items: MutableList<Bookmark>,
    private val onOpen: (Bookmark) -> Unit,
    private val onDelete: (Bookmark, Int) -> Unit
) : RecyclerView.Adapter<BookmarksAdapter.ViewHolder>() {

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val faviconCache = mutableMapOf<String, Bitmap?>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val favicon: ImageView = view.findViewById(R.id.bookmark_favicon)
        val title: TextView = view.findViewById(R.id.bookmark_title)
        val url: TextView = view.findViewById(R.id.bookmark_url)
        val deleteBtn: ImageButton = view.findViewById(R.id.bookmark_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = items[position]
        holder.title.text = bookmark.title.ifBlank { bookmark.url }
        holder.url.text = bookmark.url.removePrefix("https://").removePrefix("http://")

        holder.favicon.setImageResource(R.drawable.ic_globe_24)
        ImageViewCompat.setImageTintList(
            holder.favicon,
            ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.context, R.color.purple_200))
        )

        val faviconUrl = bookmark.faviconUrl.ifEmpty {
            runCatching {
                val host = URL(bookmark.url).host
                "https://www.google.com/s2/favicons?domain=$host&sz=64"
            }.getOrDefault("")
        }

        if (faviconUrl.isNotEmpty()) {
            if (faviconCache.containsKey(faviconUrl)) {
                val cached = faviconCache[faviconUrl]
                if (cached != null) {
                    holder.favicon.setImageBitmap(cached)
                    ImageViewCompat.setImageTintList(holder.favicon, null)
                }
            } else {
                executor.execute {
                    val bitmap = runCatching {
                        BitmapFactory.decodeStream(URL(faviconUrl).openStream())
                    }.getOrNull()
                    faviconCache[faviconUrl] = bitmap
                    mainHandler.post {
                        if (holder.bindingAdapterPosition == position) {
                            if (bitmap != null) {
                                holder.favicon.setImageBitmap(bitmap)
                                ImageViewCompat.setImageTintList(holder.favicon, null)
                            }
                        }
                    }
                }
            }
        }

        holder.itemView.setOnClickListener { onOpen(bookmark) }
        holder.deleteBtn.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_ID.toInt()) {
                onDelete(bookmark, pos)
            }
        }
    }

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}
