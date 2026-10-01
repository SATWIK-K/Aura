package com.aura.weight.data

import kotlin.math.abs

object Stats {

    fun bmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0.0) return 0.0
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun bmiCategory(bmiValue: Double): BmiCategory = when {
        bmiValue < 18.5 -> BmiCategory.UNDER
        bmiValue < 25.0 -> BmiCategory.HEALTHY
        bmiValue < 30.0 -> BmiCategory.OVER
        else -> BmiCategory.OBESE
    }

    fun streak(entries: List<WeightEntry>, todayDay: Long): Int {
        val days = entries.mapTo(HashSet()) { it.day }
        var cursor = if (days.contains(todayDay)) todayDay else todayDay - 1
        if (!days.contains(cursor)) return 0
        var length = 0
        while (days.contains(cursor)) {
            length++
            cursor--
        }
        return length
    }

    fun nearestAtOrBefore(entries: List<WeightEntry>, day: Long): WeightEntry? {
        var best: WeightEntry? = null
        for (e in entries) {
            if (e.day <= day && (best == null || e.day > best.day)) best = e
        }
        return best
    }

    fun latest(entries: List<WeightEntry>): WeightEntry? = entries.maxByOrNull { it.day }

    fun changeOverDays(entries: List<WeightEntry>, todayDay: Long, days: Long): Double? {
        val now = nearestAtOrBefore(entries, todayDay)?.kg ?: return null
        val then = nearestAtOrBefore(entries, todayDay - days)?.kg ?: return null
        return now - then
    }

    /** Least-squares slope in kg/day; null when fewer than two distinct days. */
    fun slopePerDay(entries: List<WeightEntry>): Double? {
        if (entries.size < 2) return null
        val n = entries.size
        val meanX = entries.map { it.day.toDouble() }.average()
        val meanY = entries.map { it.kg }.average()
        var num = 0.0
        var den = 0.0
        for (e in entries) {
            val dx = e.day - meanX
            num += dx * (e.kg - meanY)
            den += dx * dx
        }
        if (abs(den) < 1e-9) return null
        return num / den
    }

    fun movingAverage(entries: List<WeightEntry>, window: Int): List<WeightEntry> {
        if (window <= 1 || entries.isEmpty()) return entries
        val sorted = entries.sortedBy { it.day }
        return sorted.mapIndexed { i, e ->
            val from = (i - window / 2).coerceAtLeast(0)
            val to = (i + window / 2 + 1).coerceAtMost(sorted.size)
            val slice = sorted.subList(from, to)
            e.copy(kg = slice.map { it.kg }.average())
        }
    }

    fun goalProgress(currentKg: Double, startKg: Double?, targetKg: Double?): Float {
        if (startKg == null || targetKg == null) return 0f
        val total = startKg - targetKg
        if (abs(total) < 1e-6) return 0f
        val done = startKg - currentKg
        return (done / total).toFloat().coerceIn(0f, 1f)
    }

    fun toGoal(currentKg: Double, targetKg: Double?): Double? =
        targetKg?.let { it - currentKg }

    fun deltaSign(value: Double, epsilon: Double = 0.05): Int =
        when {
            value > epsilon -> 1
            value < -epsilon -> -1
            else -> 0
        }
}
