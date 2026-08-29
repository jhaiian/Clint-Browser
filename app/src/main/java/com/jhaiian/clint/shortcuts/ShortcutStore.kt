package com.jhaiian.clint.shortcuts

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

data class WebAppShortcut(
    val id: String,
    val url: String,
    val name: String,
    val iconPath: String?,
    val tabId: String?
)

object ShortcutStore {

    @Volatile private var db: ShortcutDatabase? = null

    private fun db(context: Context): ShortcutDatabase {
        return db ?: synchronized(this) {
            db ?: ShortcutDatabase(context.applicationContext).also { db = it }
        }
    }

    fun save(context: Context, shortcut: WebAppShortcut) {
        val values = ContentValues().apply {
            put(ShortcutDatabase.COL_ID, shortcut.id)
            put(ShortcutDatabase.COL_URL, shortcut.url)
            put(ShortcutDatabase.COL_NAME, shortcut.name)
            put(ShortcutDatabase.COL_ICON_PATH, shortcut.iconPath)
            put(ShortcutDatabase.COL_TAB_ID, shortcut.tabId)
            put(ShortcutDatabase.COL_CREATED_AT, System.currentTimeMillis())
        }
        db(context).writableDatabase.insertWithOnConflict(
            ShortcutDatabase.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun get(context: Context, id: String): WebAppShortcut? {
        val cursor = db(context).readableDatabase.query(
            ShortcutDatabase.TABLE,
            arrayOf(
                ShortcutDatabase.COL_ID,
                ShortcutDatabase.COL_URL,
                ShortcutDatabase.COL_NAME,
                ShortcutDatabase.COL_ICON_PATH,
                ShortcutDatabase.COL_TAB_ID
            ),
            "${ShortcutDatabase.COL_ID} = ?", arrayOf(id), null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                WebAppShortcut(
                    id = it.getString(0),
                    url = it.getString(1),
                    name = it.getString(2),
                    iconPath = it.getString(3),
                    tabId = it.getString(4)
                )
            } else null
        }
    }

    fun updateTabId(context: Context, id: String, tabId: String) {
        val values = ContentValues().apply { put(ShortcutDatabase.COL_TAB_ID, tabId) }
        db(context).writableDatabase.update(
            ShortcutDatabase.TABLE, values, "${ShortcutDatabase.COL_ID} = ?", arrayOf(id)
        )
    }
}
