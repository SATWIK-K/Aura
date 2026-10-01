package com.aura.weight

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aura.weight.ui.AuraViewModel
import com.aura.weight.ui.Screen
import com.aura.weight.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AuraViewModelTest {

    private lateinit var vm: AuraViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.deleteDatabase("aura.db")
        vm = AuraViewModel(app)
    }

    @Test
    fun freshInstallStartsOnOnboarding() {
        assertEquals(Screen.Onboarding, vm.screen.value)
        assertEquals(UserProfile(), vm.profile.value)
        assertTrue(vm.entries.value.isEmpty())
    }

    @Test
    fun logWeightInsertsThenReplacesToday() {
        val today = vm.today()
        vm.logWeight(72.4)
        assertEquals(1, vm.entries.value.size)
        assertEquals(72.4, vm.entries.value.last().kg, 1e-9)
        assertEquals(today, vm.entries.value.last().day)

        vm.logWeight(73.1)
        assertEquals(1, vm.entries.value.size)
        assertEquals(73.1, vm.entries.value.last().kg, 1e-9)
    }

    @Test
    fun deleteRemovesEntry() {
        vm.logWeight(70.0)
        vm.delete(vm.today())
        assertTrue(vm.entries.value.isEmpty())
    }

    @Test
    fun saveProfilePersistsAndUpdatesState() {
        val profile = UserProfile(
            onboarded = true,
            heightCm = 181.0,
            startKg = 90.0,
            targetKg = 75.0,
            backdropId = "ocean",
            darkTheme = false,
        )
        vm.saveProfile(profile)
        assertEquals(profile, vm.profile.value)
        assertEquals(profile, WeightRepositoryReload().profile())
    }

    @Test
    fun navigationBetweenScreens() {
        vm.saveProfile(UserProfile(onboarded = true))
        assertEquals(Screen.Onboarding, vm.screen.value)
        vm.go(Screen.Home)
        assertEquals(Screen.Home, vm.screen.value)
        vm.go(Screen.History)
        assertEquals(Screen.History, vm.screen.value)
        vm.go(Screen.Settings)
        assertEquals(Screen.Settings, vm.screen.value)
    }

    @Test
    fun logSheetOpensAndCloses() {
        vm.setLogOpen(true)
        assertTrue(vm.logOpen.value)
        vm.setLogOpen(false)
        assertEquals(false, vm.logOpen.value)
    }

    private fun WeightRepositoryReload(): com.aura.weight.data.WeightRepository =
        com.aura.weight.data.WeightRepository(
            ApplicationProvider.getApplicationContext(),
        )
}
