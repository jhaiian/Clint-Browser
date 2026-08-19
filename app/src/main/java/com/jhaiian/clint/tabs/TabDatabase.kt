package com.jhaiian.clint.tabs

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class TabDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_POSITION INTEGER NOT NULL,
                $COL_URL      TEXT    NOT NULL,
                $COL_TITLE    TEXT    NOT NULL DEFAULT '',
                $COL_ACTIVE   INTEGER NOT NULL DEFAULT 0,
                $COL_TAB_ID   TEXT
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // The old per-tab group_id column and tab_groups table are left in place on upgrading
        // installs but are no longer read or written.
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_TAB_ID TEXT")
        }
    }

    companion object {
        const val DB_NAME    = "clint_tabs.db"
        const val DB_VERSION = 3
        const val TABLE      = "tabs"
        const val COL_ID       = "id"
        const val COL_POSITION = "position"
        const val COL_URL      = "url"
        const val COL_TITLE    = "title"
        const val COL_ACTIVE   = "active"
        const val COL_TAB_ID   = "tab_id"
    }
}
