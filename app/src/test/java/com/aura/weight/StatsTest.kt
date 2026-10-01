package com.aura.weight

import com.aura.weight.data.BmiCategory
import com.aura.weight.data.Stats
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.Units
import com.aura.weight.data.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsTest {

    private val day0: Long = LocalDate.of(2026, 9, 1).toEpochDay()

    private fun entries(vararg kg: Double, startDay: Long = day0): List<WeightEntry> =
        kg.mapIndexed { i, v -> WeightEntry(startDay + i, v) }

    @Test
    fun bmiMatchesFormula() {
        assertEquals(22.22, Stats.bmi(72.0, 180.0), 0.01)
        assertEquals(18.5, Stats.bmi(59.94, 180.0), 0.01)
        assertEquals(0.0, Stats.bmi(70.0, 0.0), 0.0001)
    }

    @Test
    fun bmiCategoryBoundaries() {
        assertEquals(BmiCategory.UNDER, Stats.bmiCategory(18.4))
        assertEquals(BmiCategory.HEALTHY, Stats.bmiCategory(18.5))
        assertEquals(BmiCategory.HEALTHY, Stats.bmiCategory(24.9))
        assertEquals(BmiCategory.OVER, Stats.bmiCategory(25.0))
        assertEquals(BmiCategory.OVER, Stats.bmiCategory(29.9))
        assertEquals(BmiCategory.OBESE, Stats.bmiCategory(30.0))
    }

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val e = entries(70.0, 69.8, 69.5)
        assertEquals(3, Stats.streak(e, day0 + 2))
    }

    @Test
    fun streakSurvivesMissingToday() {
        val e = entries(70.0, 69.8, 69.5)
        assertEquals(3, Stats.streak(e, day0 + 3))
    }

    @Test
    fun streakBreaksOnGap() {
        val e = listOf(
            WeightEntry(day0, 70.0),
            WeightEntry(day0 + 1, 69.8),
            WeightEntry(day0 + 4, 69.4),
            WeightEntry(day0 + 5, 69.2),
        )
        assertEquals(2, Stats.streak(e, day0 + 5))
        assertEquals(0, Stats.streak(e, day0 + 3))
        assertEquals(0, Stats.streak(emptyList(), day0))
    }

    @Test
    fun changeOverDaysUsesNearestEntryAtOrBefore() {
        val e = listOf(
            WeightEntry(day0, 72.0),
            WeightEntry(day0 + 7, 71.0),
            WeightEntry(day0 + 10, 70.5),
        )
        assertEquals(-1.5, Stats.changeOverDays(e, day0 + 10, 10)!!, 0.0001)
        assertEquals(-0.5, Stats.changeOverDays(e, day0 + 12, 5)!!, 0.0001)
        assertNull(Stats.changeOverDays(e, day0 + 2, 30))
        assertNull(Stats.changeOverDays(emptyList(), day0, 7))
    }

    @Test
    fun slopeTrendFollowsDirection() {
        val down = entries(74.0, 73.5, 73.0, 72.5, 72.0)
        val up = entries(70.0, 70.4, 70.8, 71.2)
        assertTrue(Stats.slopePerDay(down)!! < 0)
        assertTrue(Stats.slopePerDay(up)!! > 0)
        assertEquals(-0.5, Stats.slopePerDay(down)!!, 0.0001)
        assertNull(Stats.slopePerDay(entries(70.0)))
        assertNull(Stats.slopePerDay(emptyList()))
        assertNull(
            Stats.slopePerDay(
                listOf(WeightEntry(day0, 70.0), WeightEntry(day0, 69.0)),
            ),
        )
    }

    @Test
    fun movingAverageSmoothsSeries() {
        val e = entries(70.0, 72.0, 74.0, 76.0, 78.0)
        val smoothed = Stats.movingAverage(e, 3)
        assertEquals(5, smoothed.size)
        assertEquals(72.0, smoothed[1].kg, 0.0001)
        assertEquals(74.0, smoothed[2].kg, 0.0001)
        assertEquals(71.0, smoothed[0].kg, 0.0001)
        assertEquals(e, Stats.movingAverage(e, 1))
        assertEquals(0, Stats.movingAverage(emptyList(), 3).size)
    }

    @Test
    fun goalProgressTracksShareOfJourney() {
        assertEquals(0.5f, Stats.goalProgress(74.0, startKg = 78.0, targetKg = 70.0), 0.001f)
        assertEquals(0f, Stats.goalProgress(78.0, 78.0, 70.0), 0.001f)
        assertEquals(1f, Stats.goalProgress(70.0, 78.0, 70.0), 0.001f)
        assertEquals(1f, Stats.goalProgress(68.0, 78.0, 70.0), 0.001f)
        assertEquals(0f, Stats.goalProgress(80.0, 78.0, 70.0), 0.001f)
        assertEquals(0f, Stats.goalProgress(74.0, null, 70.0), 0.001f)
        assertEquals(0f, Stats.goalProgress(74.0, 74.0, 74.0), 0.001f)
    }

    @Test
    fun unitConversionRoundTrips() {
        val kg = 72.4
        val lb = Units.toDisplay(kg, UnitSystem.LB)
        assertEquals(159.61, lb, 0.01)
        assertEquals(kg, Units.fromDisplay(lb, UnitSystem.LB), 1e-9)
        assertEquals(kg, Units.toDisplay(kg, UnitSystem.KG), 1e-9)
    }

    @Test
    fun formattingFollowsUnit() {
        assertEquals("72.4 kg", Units.format(72.4, UnitSystem.KG))
        assertEquals("159.6 lb", Units.format(72.4, UnitSystem.LB))
        assertEquals("+1.5 kg", Units.formatSigned(1.5, UnitSystem.KG))
        assertEquals("−1.5 kg", Units.formatSigned(-1.5, UnitSystem.KG))
        assertEquals("+0.0 kg", Units.formatSigned(0.0, UnitSystem.KG))
    }

    @Test
    fun deltaSignToleratesNoise() {
        assertEquals(1, Stats.deltaSign(0.4))
        assertEquals(-1, Stats.deltaSign(-0.4))
        assertEquals(0, Stats.deltaSign(0.02))
    }
}
