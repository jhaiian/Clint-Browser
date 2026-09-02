package com.jhaiian.clint.userscripts

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserScriptDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "code TEXT NOT NULL," +
                "requiresCache TEXT NOT NULL DEFAULT ''," +
                "enabled INTEGER NOT NULL DEFAULT 1," +
                "createdAt INTEGER NOT NULL," +
                "sourceUrl TEXT," +
                "etag TEXT," +
                "lastModified TEXT," +
                "updatedAt INTEGER NOT NULL DEFAULT 0)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN etag TEXT")
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN lastModified TEXT")
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {
                db.execSQL("DROP TABLE IF EXISTS $TABLE")
                onCreate(db)
            }
        }
    }

    private fun fromCursor(cursor: android.database.Cursor): UserScript {
        val sourceUrlIndex = cursor.getColumnIndexOrThrow("sourceUrl")
        val etagIndex = cursor.getColumnIndex("etag")
        val lastModifiedIndex = cursor.getColumnIndex("lastModified")
        val updatedAtIndex = cursor.getColumnIndex("updatedAt")
        return UserScript(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            code = cursor.getString(cursor.getColumnIndexOrThrow("code")),
            requiresCache = cursor.getString(cursor.getColumnIndexOrThrow("requiresCache")),
            enabled = cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) != 0,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
            sourceUrl = if (cursor.isNull(sourceUrlIndex)) null else cursor.getString(sourceUrlIndex),
            etag = if (etagIndex < 0 || cursor.isNull(etagIndex)) null else cursor.getString(etagIndex),
            lastModified = if (lastModifiedIndex < 0 || cursor.isNull(lastModifiedIndex)) null else cursor.getString(lastModifiedIndex),
            updatedAt = if (updatedAtIndex < 0 || cursor.isNull(updatedAtIndex)) 0L else cursor.getLong(updatedAtIndex)
        )
    }

    fun getAll(): List<UserScript> {
        val result = mutableListOf<UserScript>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE ORDER BY createdAt DESC", null).use { cursor ->
            while (cursor.moveToNext()) result.add(fromCursor(cursor))
        }
        return result
    }

    fun getById(id: Long): UserScript? {
        readableDatabase.rawQuery("SELECT * FROM $TABLE WHERE id = ?", arrayOf(id.toString())).use { cursor ->
            if (cursor.moveToFirst()) return fromCursor(cursor)
        }
        return null
    }

    fun insert(code: String, requiresCache: String, enabled: Boolean, sourceUrl: String? = null): Long {
        val values = ContentValues().apply {
            put("code", code)
            put("requiresCache", requiresCache)
            put("enabled", if (enabled) 1 else 0)
            put("createdAt", System.currentTimeMillis())
            put("sourceUrl", sourceUrl)
            put("updatedAt", 0L)
        }
        return writableDatabase.insert(TABLE, null, values)
    }

    fun update(id: Long, code: String, requiresCache: String) {
        val values = ContentValues().apply {
            put("code", code)
            put("requiresCache", requiresCache)
        }
        writableDatabase.update(TABLE, values, "id = ?", arrayOf(id.toString()))
    }

    fun applyUpdateResult(id: Long, code: String, requiresCache: String, etag: String?, lastModified: String?, updatedAt: Long) {
        val values = ContentValues().apply {
            put("code", code)
            put("requiresCache", requiresCache)
            put("etag", etag)
            put("lastModified", lastModified)
            put("updatedAt", updatedAt)
        }
        writableDatabase.update(TABLE, values, "id = ?", arrayOf(id.toString()))
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        val values = ContentValues().apply { put("enabled", if (enabled) 1 else 0) }
        writableDatabase.update(TABLE, values, "id = ?", arrayOf(id.toString()))
    }

    fun remove(id: Long) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }

    fun removeAll(ids: Set<Long>) {
        if (ids.isEmpty()) return
        val placeholders = ids.joinToString(",") { "?" }
        writableDatabase.delete(TABLE, "id IN ($placeholders)", ids.map { it.toString() }.toTypedArray())
    }

    companion object {
        const val DB_NAME = "user_scripts.db"
        private const val DB_VERSION = 3
        private const val TABLE = "user_scripts"
    }
}
