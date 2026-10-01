package com.aura.weight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightEntry
import com.aura.weight.ui.screens.HistoryScreen
import com.aura.weight.ui.screens.HomeScreen
import com.aura.weight.ui.screens.LogSheet
import com.aura.weight.ui.screens.OnboardingScreen
import com.aura.weight.ui.screens.SettingsScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenRenderTest : BaseVisualTest() {

    private fun profile() = UserProfile(
        onboarded = true,
        unit = UnitSystem.KG,
        heightCm = 179.0,
        startKg = 78.5,
        targetKg = 68.0,
        backdropId = "aurora",
        darkTheme = true,
    )

    private fun entries(): List<WeightEntry> {
        val today = LocalDate.now().toEpochDay()
        return (40L downTo 0L).map { offset ->
            val i = offset.toInt()
            val noise = sin(i * 1.7f) * 0.25
            WeightEntry(today - offset, 74.4 - i * 0.05 + noise, recordedAt = 0L)
        }
    }

    @Test
    fun homeDark() = capture("screen-home-dark.png", dark = true) {
        HomeScreen(
            profile = profile(),
            entries = entries(),
            onLog = {},
            onHistory = {},
            onSettings = {},
        )
    }

    @Test
    fun homeLight() = capture("screen-home-light.png", dark = false) {
        HomeScreen(
            profile = profile(),
            entries = entries(),
            onLog = {},
            onHistory = {},
            onSettings = {},
        )
    }

    @Test
    fun homeEmpty() = capture("screen-home-empty.png", dark = true) {
        HomeScreen(
            profile = profile(),
            entries = emptyList(),
            onLog = {},
            onHistory = {},
            onSettings = {},
        )
    }

    @Test
    fun onboardingWelcome() = capture("screen-onboarding-welcome.png", dark = true) {
        OnboardingScreen(profile = UserProfile()) {}
    }

    @Test
    fun onboardingBody() = capture(
        "screen-onboarding-body.png",
        dark = true,
        beforeShot = {
            rule.onNodeWithText("Get started").performClick()
            rule.onNodeWithText("Continue").performClick()
            val fields = rule.onAllNodes(hasSetTextAction())
            fields[0].performTextInput("179")
            fields[1].performTextInput("74.4")
            rule.waitForIdle()
        },
    ) {
        OnboardingScreen(profile = UserProfile()) {}
    }

    @Test
    fun historyScreen() = capture("screen-history.png", dark = true) {
        HistoryScreen(
            profile = profile(),
            entries = entries(),
            onBack = {},
            onDelete = {},
        )
    }

    @Test
    fun settingsScreen() = capture("screen-settings.png", dark = true) {
        SettingsScreen(profile = profile(), widgetProviders = 6, onBack = {}, onSave = {})
    }

    @Test
    fun logSheetOpen() = capture("screen-logsheet.png", dark = true, beforeShot = { rule.waitForIdle() }) {
        Box(Modifier.fillMaxSize()) {
            HomeScreen(
                profile = profile(),
                entries = entries(),
                onLog = {},
                onHistory = {},
                onSettings = {},
            )
            LogSheet(
                open = true,
                profile = profile(),
                existingTodayKg = entries().last().kg,
                onSave = {},
                onDismiss = {},
            )
        }
    }
}
