package com.aura.weight.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AuraDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_ENTRIES (
                $COL_DAY INTEGER PRIMARY KEY,
                $COL_KG REAL NOT NULL,
                $COL_NOTE TEXT,
                $COL_RECORDED_AT INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_SETTINGS (
                $COL_KEY TEXT PRIMARY KEY,
                $COL_VALUE TEXT NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var from = oldVersion
        while (from < newVersion) {
            when (from) {
                else -> throw IllegalStateException("Missing migration path from version $from")
            }
        }
    }

    companion object {
        const val DB_NAME = "aura.db"
        const val VERSION = 1

        const val TABLE_ENTRIES = "entries"
        const val TABLE_SETTINGS = "settings"

        const val COL_DAY = "day"
        const val COL_KG = "kg"
        const val COL_NOTE = "note"
        const val COL_RECORDED_AT = "recorded_at"
        const val COL_KEY = "key"
        const val COL_VALUE = "value"
    }
}
