package com.jhaiian.clint.quiver

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class FilterListDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID           INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME         TEXT    NOT NULL,
                $COL_DOWNLOAD_URL TEXT    NOT NULL,
                $COL_ENABLED      INTEGER NOT NULL DEFAULT 0,
                $COL_FILE_PATH    TEXT,
                $COL_FILE_SIZE    INTEGER NOT NULL DEFAULT -1,
                $COL_DOWNLOADED_AT INTEGER NOT NULL DEFAULT 0,
                $COL_RULE_COUNT   INTEGER NOT NULL DEFAULT -1,
                $COL_IS_CUSTOM    INTEGER NOT NULL DEFAULT 0,
                $COL_COMPILED_AT  INTEGER NOT NULL DEFAULT 0,
                $COL_ETAG         TEXT,
                $COL_LAST_MODIFIED TEXT
            )"""
        )

        for ((name, url) in DEFAULT_FILTER_LISTS) {
            val cv = ContentValues()
            cv.put(COL_NAME, name)
            cv.put(COL_DOWNLOAD_URL, url)
            cv.put(COL_ENABLED, 0)
            cv.put(COL_IS_CUSTOM, 0)
            cv.put(COL_COMPILED_AT, 0)
            db.insert(TABLE, null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_IS_CUSTOM INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_COMPILED_AT INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {

            for ((name, url) in NEW_IN_V4_FILTER_LISTS) {
                val exists = db.query(TABLE, arrayOf(COL_ID), "$COL_DOWNLOAD_URL = ?", arrayOf(url), null, null, null).use { it.count > 0 }
                if (!exists) {
                    val cv = ContentValues()
                    cv.put(COL_NAME, name)
                    cv.put(COL_DOWNLOAD_URL, url)
                    cv.put(COL_ENABLED, 0)
                    cv.put(COL_IS_CUSTOM, 0)
                    cv.put(COL_COMPILED_AT, 0)
                    db.insert(TABLE, null, cv)
                }
            }
        }
        if (oldVersion < 5) {

            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_ETAG TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_LAST_MODIFIED TEXT DEFAULT NULL")
        }
    }

    fun getAllFilterLists(): List<FilterList> {
        val db = readableDatabase
        val cursor = db.query(TABLE, null, null, null, null, null, "$COL_ID ASC")
        val result = mutableListOf<FilterList>()
        while (cursor.moveToNext()) {

            val etagIdx = cursor.getColumnIndex(COL_ETAG)
            val lastModifiedIdx = cursor.getColumnIndex(COL_LAST_MODIFIED)
            result.add(
                FilterList(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    downloadUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_DOWNLOAD_URL)),
                    isEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLED)) == 1,
                    localPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_PATH)),
                    fileSizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(COL_FILE_SIZE)),
                    downloadedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DOWNLOADED_AT)),
                    ruleCount = cursor.getLong(cursor.getColumnIndexOrThrow(COL_RULE_COUNT)),
                    isCustom = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_CUSTOM)) == 1,
                    compiledAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_COMPILED_AT)),
                    etag = if (etagIdx >= 0) cursor.getString(etagIdx) else null,
                    lastModified = if (lastModifiedIdx >= 0) cursor.getString(lastModifiedIdx) else null
                )
            )
        }
        cursor.close()
        return result
    }

    fun updateDownloadResult(
        id: Long,
        filePath: String,
        fileSizeBytes: Long,
        downloadedAt: Long,
        ruleCount: Long,
        etag: String? = null,
        lastModified: String? = null
    ) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(COL_FILE_PATH, filePath)
        cv.put(COL_FILE_SIZE, fileSizeBytes)
        cv.put(COL_DOWNLOADED_AT, downloadedAt)
        cv.put(COL_RULE_COUNT, ruleCount)
        cv.put(COL_ETAG, etag)
        cv.put(COL_LAST_MODIFIED, lastModified)
        db.update(TABLE, cv, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun updateEnabled(id: Long, enabled: Boolean) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(COL_ENABLED, if (enabled) 1 else 0)
        db.update(TABLE, cv, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun addCustomFilterList(name: String, downloadUrl: String): Long {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(COL_NAME, name)
        cv.put(COL_DOWNLOAD_URL, downloadUrl)
        cv.put(COL_ENABLED, 0)
        cv.put(COL_IS_CUSTOM, 1)
        cv.put(COL_COMPILED_AT, 0)
        return db.insert(TABLE, null, cv)
    }

    fun addLocalFilterList(name: String): Long {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(COL_NAME, name)
        cv.put(COL_DOWNLOAD_URL, "")
        cv.put(COL_ENABLED, 0)
        cv.put(COL_IS_CUSTOM, 1)
        cv.put(COL_COMPILED_AT, 0)
        return db.insert(TABLE, null, cv)
    }

    fun deleteFilterList(id: Long) {
        val db = writableDatabase
        db.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deleteFilterLists(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (id in ids) {
                db.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getRawEnabled(id: Long): Boolean? {
        val db = readableDatabase
        val cursor = db.query(TABLE, arrayOf(COL_ENABLED), "$COL_ID = ?", arrayOf(id.toString()), null, null, null)
        cursor.use {
            if (!it.moveToFirst()) return null
            return it.getInt(it.getColumnIndexOrThrow(COL_ENABLED)) == 1
        }
    }

    fun hasActiveFilterLists(): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE,
            arrayOf(COL_ID),
            "$COL_ENABLED = 1 AND $COL_COMPILED_AT > 0",
            null, null, null, null, "1"
        )
        return cursor.use { it.count > 0 }
    }

    fun getNeverCompiledIds(): List<Long> {
        val db = readableDatabase
        val cursor = db.query(TABLE, arrayOf(COL_ID), "$COL_COMPILED_AT = 0", null, null, null, null)
        val result = mutableListOf<Long>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(it.getLong(it.getColumnIndexOrThrow(COL_ID)))
            }
        }
        return result
    }

    fun getNeverCompiledCustomIds(): List<Long> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE, arrayOf(COL_ID),
            "$COL_IS_CUSTOM = 1 AND $COL_COMPILED_AT = 0",
            null, null, null, null
        )
        val result = mutableListOf<Long>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(it.getLong(it.getColumnIndexOrThrow(COL_ID)))
            }
        }
        return result
    }

    fun commitCompiledState(enabledStates: Map<Long, Boolean>, compiledAtMillis: Long) {
        if (enabledStates.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((id, enabled) in enabledStates) {
                val cv = ContentValues()
                cv.put(COL_ENABLED, if (enabled) 1 else 0)
                cv.put(COL_COMPILED_AT, compiledAtMillis)
                db.update(TABLE, cv, "$COL_ID = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        const val DB_NAME           = "quiver_guard.db"
        const val DB_VERSION        = 5
        const val TABLE             = "filter_lists"
        const val COL_ID            = "id"
        const val COL_NAME          = "name"
        const val COL_DOWNLOAD_URL  = "download_url"
        const val COL_ENABLED       = "enabled"
        const val COL_FILE_PATH     = "file_path"
        const val COL_FILE_SIZE     = "file_size"
        const val COL_DOWNLOADED_AT = "downloaded_at"
        const val COL_RULE_COUNT    = "rule_count"
        const val COL_IS_CUSTOM     = "is_custom"
        const val COL_COMPILED_AT   = "compiled_at"
        const val COL_ETAG          = "etag"
        const val COL_LAST_MODIFIED = "last_modified"

        private const val FANBOY_ANNOYANCE_URL = "https://secure.fanboy.co.nz/fanboy-annoyance.txt"
        private const val ADGUARD_MOBILE_ADS_URL = "https://filters.adtidy.org/extension/ublock/filters/11.txt"
        private const val ADGUARD_BASE_FILTER_URL = "https://filters.adtidy.org/extension/ublock/filters/2_without_easylist.txt"
        private const val ADGUARD_ANNOYANCES_URL = "https://filters.adtidy.org/extension/ublock/filters/14.txt"

        private val DEFAULT_FILTER_LISTS = listOf(
            Pair("EasyList", "https://easylist.to/easylist/easylist.txt"),
            Pair("EasyPrivacy", "https://easylist.to/easylist/easyprivacy.txt"),
            Pair("Fanboy Annoyances", FANBOY_ANNOYANCE_URL),
            Pair("AdGuard Mobile Ads", ADGUARD_MOBILE_ADS_URL),
            Pair("AdGuard Base Filter", ADGUARD_BASE_FILTER_URL),
            Pair("AdGuard Annoyances", ADGUARD_ANNOYANCES_URL)
        )

        private val NEW_IN_V4_FILTER_LISTS = listOf(
            Pair("AdGuard Base Filter", ADGUARD_BASE_FILTER_URL),
            Pair("AdGuard Annoyances", ADGUARD_ANNOYANCES_URL)
        )
    }
}
