package com.jhaiian.clint.quiver.engine

import android.content.Context
import java.io.File

object QuiverGuardPaths {

    const val DATABASE_FILE_NAME = "quiver_guard_engine.dat"
    const val TEMP_DATABASE_FILE_NAME = "quiver_guard_engine.tmp"
    const val MANIFEST_FILE_NAME = "quiver_guard_manifest.json"

    fun databaseFile(context: Context): File = File(context.filesDir, DATABASE_FILE_NAME)

    fun tempDatabaseFile(context: Context): File = File(context.filesDir, TEMP_DATABASE_FILE_NAME)

    fun manifestFile(context: Context): File = File(context.filesDir, MANIFEST_FILE_NAME)
}
