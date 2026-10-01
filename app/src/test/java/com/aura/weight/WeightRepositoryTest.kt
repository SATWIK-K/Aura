package com.aura.weight

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WeightRepositoryTest {

    private lateinit var repo: WeightRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("aura.db")
        repo = WeightRepository(context)
    }

    @After
    fun tearDown() {
        repo.close()
    }

    @Test
    fun upsertReplacesEntryForSameDay() {
        repo.upsert(com.aura.weight.data.WeightEntry(100, 72.4))
        repo.upsert(com.aura.weight.data.WeightEntry(100, 71.9, note = "evening"))
        assertEquals(1, repo.count())
        val entry = repo.entry(100)!!
        assertEquals(71.9, entry.kg, 0.0001)
        assertEquals("evening", entry.note)
    }

    @Test
    fun latestReturnsNewestDay() {
        repo.upsert(com.aura.weight.data.WeightEntry(10, 74.0))
        repo.upsert(com.aura.weight.data.WeightEntry(30, 72.0))
        repo.upsert(com.aura.weight.data.WeightEntry(20, 73.0))
        assertEquals(30L, repo.latest()!!.day)
        assertEquals(72.0, repo.latest()!!.kg, 0.0001)
    }

    @Test
    fun rangeFiltersAndSortsAscending() {
        (1L..10L).forEach { repo.upsert(com.aura.weight.data.WeightEntry(it, 70.0 + it)) }
        val window = repo.range(3, 7)
        assertEquals(listOf(3L, 4L, 5L, 6L, 7L), window.map { it.day })
        assertTrue(repo.range(1, 2).size == 2)
        assertTrue(repo.range(20, 30).isEmpty())
    }

    @Test
    fun deleteRemovesOnlyRequestedDay() {
        repo.upsert(com.aura.weight.data.WeightEntry(1, 70.0))
        repo.upsert(com.aura.weight.data.WeightEntry(2, 71.0))
        assertTrue(repo.delete(1))
        assertFalse(repo.delete(1))
        assertEquals(1, repo.count())
        assertNull(repo.entry(1))
        assertEquals(2L, repo.latest()!!.day)
    }

    @Test
    fun allReturnsChronologicalHistory() {
        listOf(5L, 1L, 3L).forEach { repo.upsert(com.aura.weight.data.WeightEntry(it, 70.0)) }
        assertEquals(listOf(1L, 3L, 5L), repo.all().map { it.day })
    }

    @Test
    fun profileRoundTripsThroughSettings() {
        assertNull(repo.getSetting("unit"))
        val profile = UserProfile(
            onboarded = true,
            unit = UnitSystem.LB,
            heightCm = 178.5,
            startKg = 82.3,
            targetKg = 74.0,
            backdropId = "ocean",
            darkTheme = false,
            widgetAlpha = 132,
        )
        repo.saveProfile(profile)
        assertEquals(profile, repo.profile())
    }

    @Test
    fun profileDefaultsWhenUnset() {
        assertEquals(UserProfile(), repo.profile())
    }

    @Test
    fun changesFlowBumpsOnWrites() {
        val before = repo.changes.value
        repo.upsert(com.aura.weight.data.WeightEntry(1, 70.0))
        val afterInsert = repo.changes.value
        assertTrue(afterInsert > before)
        repo.putSetting("unit", "LB")
        assertTrue(repo.changes.value > afterInsert)
        repo.delete(1)
        assertTrue(repo.changes.value > afterInsert + 1)
    }

    @Test
    fun nullSettingIsCleared() {
        repo.putSetting("target_kg", "74.0")
        assertEquals("74.0", repo.getSetting("target_kg"))
        repo.putSetting("target_kg", null)
        assertNull(repo.getSetting("target_kg"))
    }
}
