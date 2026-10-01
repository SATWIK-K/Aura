package com.aura.weight.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for weight entries and user settings.
 * Writes bump [changes] so UI and widgets can refresh.
 */
class WeightRepository(context: Context) {

    private val helper = AuraDb(context)
    private val db: SQLiteDatabase get() = helper.writableDatabase

    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes.asStateFlow()

    fun upsert(entry: WeightEntry) {
        val values = ContentValues().apply {
            put(AuraDb.COL_DAY, entry.day)
            put(AuraDb.COL_KG, entry.kg)
            put(AuraDb.COL_NOTE, entry.note)
            put(AuraDb.COL_RECORDED_AT, entry.recordedAt)
        }
        db.insertWithOnConflict(AuraDb.TABLE_ENTRIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        bump()
    }

    fun delete(day: Long): Boolean {
        val removed = db.delete(AuraDb.TABLE_ENTRIES, "${AuraDb.COL_DAY}=?", arrayOf(day.toString())) > 0
        if (removed) bump()
        return removed
    }

    fun entry(day: Long): WeightEntry? =
        db.query(
            AuraDb.TABLE_ENTRIES,
            null,
            "${AuraDb.COL_DAY}=?",
            arrayOf(day.toString()),
            null,
            null,
            null,
        ).use { c -> if (c.moveToFirst()) readEntry(c) else null }

    fun latest(): WeightEntry? =
        db.query(
            AuraDb.TABLE_ENTRIES,
            null,
            null,
            null,
            null,
            null,
            "${AuraDb.COL_DAY} DESC",
            "1",
        ).use { c -> if (c.moveToFirst()) readEntry(c) else null }

    fun range(fromDay: Long, toDay: Long): List<WeightEntry> =
        db.query(
            AuraDb.TABLE_ENTRIES,
            null,
            "${AuraDb.COL_DAY} BETWEEN ? AND ?",
            arrayOf(fromDay.toString(), toDay.toString()),
            null,
            null,
            AuraDb.COL_DAY,
        ).use { c -> readAll(c) }

    fun all(): List<WeightEntry> =
        db.query(AuraDb.TABLE_ENTRIES, null, null, null, null, null, AuraDb.COL_DAY)
            .use { c -> readAll(c) }

    fun count(): Int =
        db.rawQuery("SELECT COUNT(*) FROM ${AuraDb.TABLE_ENTRIES}", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    fun putSetting(key: String, value: String?) {
        if (value == null) {
            val removed = db.delete(AuraDb.TABLE_SETTINGS, "${AuraDb.COL_KEY}=?", arrayOf(key)) > 0
            if (removed) bump()
            return
        }
        val values = ContentValues().apply {
            put(AuraDb.COL_KEY, key)
            put(AuraDb.COL_VALUE, value)
        }
        db.insertWithOnConflict(AuraDb.TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        bump()
    }

    fun getSetting(key: String): String? =
        db.query(
            AuraDb.TABLE_SETTINGS,
            arrayOf(AuraDb.COL_VALUE),
            "${AuraDb.COL_KEY}=?",
            arrayOf(key),
            null,
            null,
            null,
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }

    fun profile(): UserProfile = UserProfile(
        onboarded = getSetting(KEY_ONBOARDED) == "1",
        unit = when (getSetting(KEY_UNIT)) {
            UnitSystem.LB.name -> UnitSystem.LB
            else -> UnitSystem.KG
        },
        heightCm = getSetting(KEY_HEIGHT_CM)?.toDoubleOrNull() ?: 0.0,
        startKg = getSetting(KEY_START_KG)?.toDoubleOrNull(),
        targetKg = getSetting(KEY_TARGET_KG)?.toDoubleOrNull(),
        backdropId = getSetting(KEY_BACKDROP) ?: "aurora",
        darkTheme = getSetting(KEY_DARK) != "0",
        widgetAlpha = getSetting(KEY_WIDGET_ALPHA)?.toIntOrNull() ?: 72,
    )

    fun saveProfile(p: UserProfile) {
        putSetting(KEY_ONBOARDED, if (p.onboarded) "1" else "0")
        putSetting(KEY_UNIT, p.unit.name)
        putSetting(KEY_HEIGHT_CM, p.heightCm.toString())
        putSetting(KEY_START_KG, p.startKg?.toString())
        putSetting(KEY_TARGET_KG, p.targetKg?.toString())
        putSetting(KEY_BACKDROP, p.backdropId)
        putSetting(KEY_DARK, if (p.darkTheme) "1" else "0")
        putSetting(KEY_WIDGET_ALPHA, p.widgetAlpha.toString())
    }

    fun close() = helper.close()

    private fun bump() {
        _changes.value = _changes.value + 1
    }

    private fun readEntry(c: android.database.Cursor): WeightEntry = WeightEntry(
        day = c.getLong(c.getColumnIndexOrThrow(AuraDb.COL_DAY)),
        kg = c.getDouble(c.getColumnIndexOrThrow(AuraDb.COL_KG)),
        note = c.getStringOrNull(c.getColumnIndexOrThrow(AuraDb.COL_NOTE)),
        recordedAt = c.getLong(c.getColumnIndexOrThrow(AuraDb.COL_RECORDED_AT)),
    )

    private fun readAll(c: android.database.Cursor): List<WeightEntry> {
        val out = ArrayList<WeightEntry>(c.count)
        while (c.moveToNext()) out.add(readEntry(c))
        return out
    }

    private fun android.database.Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    companion object {
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_UNIT = "unit"
        const val KEY_HEIGHT_CM = "height_cm"
        const val KEY_START_KG = "start_kg"
        const val KEY_TARGET_KG = "target_kg"
        const val KEY_BACKDROP = "backdrop"
        const val KEY_DARK = "dark"
        const val KEY_WIDGET_ALPHA = "widget_alpha"
    }
}
