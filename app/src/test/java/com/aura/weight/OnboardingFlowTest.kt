package com.aura.weight

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightRepository
import com.aura.weight.ui.screens.OnboardingScreen
import com.aura.weight.ui.theme.AuraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class OnboardingFlowTest : BaseVisualTest() {

    private lateinit var repo: WeightRepository

    @Before
    fun cleanDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("aura.db")
        repo = WeightRepository(context)
    }

    @Test
    fun fullWalkthroughProducesProfile() {
        var saved: UserProfile? = null
        rule.setContent {
            AuraTheme(true) {
                OnboardingScreen(profile = UserProfile()) { saved = it }
            }
        }

        rule.onNodeWithText("Get started").performClick()
        rule.onNodeWithText("Pounds").performClick()
        rule.onNodeWithText("Continue").performClick()

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("180")
        fields[1].performTextInput("160")
        rule.onNodeWithText("Continue").performClick()

        rule.onAllNodes(hasSetTextAction())[0].performTextInput("143")
        rule.onNodeWithText("Start tracking").performClick()
        rule.waitForIdle()

        val profile = requireNotNull(saved)
        assertTrue(profile.onboarded)
        assertEquals(com.aura.weight.data.UnitSystem.LB, profile.unit)
        assertEquals(180.0, profile.heightCm, 0.001)
        assertEquals(72.579, requireNotNull(profile.startKg), 0.01)
        assertEquals(64.864, requireNotNull(profile.targetKg), 0.01)
    }

    @Test
    fun continueIsBlockedWithoutValidBody() {
        rule.setContent {
            AuraTheme(true) {
                OnboardingScreen(profile = UserProfile()) {}
            }
        }
        rule.onNodeWithText("Get started").performClick()
        rule.onNodeWithText("Continue").performClick()
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("120")
        rule.onNodeWithText("Continue").performClick()
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 2)
    }
}
