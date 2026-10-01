package com.aura.weight.widget

import android.content.Context
import com.aura.weight.data.Stats
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightEntry
import com.aura.weight.data.WeightRepository
import java.time.LocalDate

enum class WidgetStyle(
    val label: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
) {
    TODAY("Today", 250, 110),
    TREND("Trend", 250, 150),
    RING("Goal ring", 140, 140),
    STREAK("Streak", 250, 70),
    BMI("BMI", 140, 140),
    COMPACT("Compact", 140, 50),
    PROGRESS("Progress", 250, 110),
}

data class WidgetData(
    val entries: List<WeightEntry>,
    val profile: UserProfile,
    val today: Long,
) {
    val latest: WeightEntry? = entries.maxByOrNull { it.day }
    val weekDelta: Double? = Stats.changeOverDays(entries, today, 7)
    val streakDays: Int = Stats.streak(entries, today)
    val progress: Float = Stats.goalProgress(
        latest?.kg ?: (profile.startKg ?: 0.0),
        profile.startKg,
        profile.targetKg,
    )
    val bmiValue: Double =
        if (profile.heightCm > 0 && latest != null) Stats.bmi(latest.kg, profile.heightCm) else 0.0
    val window30: List<WeightEntry> = entries.filter { it.day in (today - 29)..today }

    fun logDaysLast7(): Set<Long> {
        val days = entries.mapTo(HashSet()) { it.day }
        return (0L..6L).map { today - it }.filter { it in days }.toSet()
    }
}

fun loadWidgetData(context: Context): WidgetData {
    val repo = WeightRepository(context)
    return try {
        WidgetData(
            entries = repo.all(),
            profile = repo.profile(),
            today = LocalDate.now().toEpochDay(),
        )
    } finally {
        repo.close()
    }
}
