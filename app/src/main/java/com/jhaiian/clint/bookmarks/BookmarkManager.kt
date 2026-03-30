package com.jhaiian.clint.bookmarks

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object BookmarkManager {

    private const val PREFS_NAME = "clint_bookmarks"
    private const val KEY_BOOKMARKS = "bookmarks"

    fun getAll(context: Context): MutableList<Bookmark> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<Bookmark>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Bookmark(
                    url = obj.getString("url"),
                    title = obj.optString("title", obj.getString("url")),
                    faviconUrl = obj.optString("faviconUrl", "")
                )
            )
        }
        return list
    }

    fun add(context: Context, bookmark: Bookmark) {
        val list = getAll(context)
        if (list.any { it.url == bookmark.url }) return
        list.add(0, bookmark)
        save(context, list)
    }

    fun remove(context: Context, url: String) {
        val list = getAll(context)
        list.removeAll { it.url == url }
        save(context, list)
    }

    fun isBookmarked(context: Context, url: String): Boolean {
        return getAll(context).any { it.url == url }
    }

    private fun save(context: Context, list: List<Bookmark>) {
        val array = JSONArray()
        list.forEach { bookmark ->
            val obj = JSONObject()
            obj.put("url", bookmark.url)
            obj.put("title", bookmark.title)
            obj.put("faviconUrl", bookmark.faviconUrl)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_BOOKMARKS, array.toString()) }
    }
}
