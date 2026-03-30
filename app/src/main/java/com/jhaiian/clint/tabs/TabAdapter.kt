package com.jhaiian.clint.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jhaiian.clint.R

class TabAdapter(
    private val tabs: MutableList<TabPreview>,
    private val activeIndex: Int,
    private val onTabClick: (Int) -> Unit,
    private val onTabClose: (Int) -> Unit
) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

    inner class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.tabIcon)
        val title: TextView = view.findViewById(R.id.tabTitle)
        val url: TextView = view.findViewById(R.id.tabUrl)
        val closeBtn: ImageButton = view.findViewById(R.id.tabClose)
        val activeIndicator: View = view.findViewById(R.id.tabActiveIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
        return TabViewHolder(v)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifBlank { "New Tab" }
        holder.url.text = tab.url.removePrefix("https://").removePrefix("http://").ifBlank { "" }
        holder.icon.setImageResource(
            if (tab.isIncognito) R.drawable.ic_incognito_24 else R.drawable.ic_globe_24
        )
        holder.activeIndicator.visibility = if (position == activeIndex) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onTabClick(position) }
        holder.closeBtn.setOnClickListener { onTabClose(position) }
    }

    override fun getItemCount() = tabs.size

    fun removeAt(index: Int) {
        if (index in tabs.indices) {
            tabs.removeAt(index)
            notifyItemRemoved(index)
            notifyItemRangeChanged(index, tabs.size)
        }
    }
}
