package com.aura.weight.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.weight.core.SquircleShape
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.Units
import com.aura.weight.data.UserProfile
import com.aura.weight.ui.components.AccentButton
import com.aura.weight.ui.components.ChoiceRow
import com.aura.weight.ui.components.LabeledField
import com.aura.weight.ui.components.NumberField
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.theme.body
import com.aura.weight.ui.theme.caption
import com.aura.weight.ui.theme.hero
import com.aura.weight.ui.theme.label
import com.aura.weight.ui.theme.number
import com.aura.weight.ui.theme.title

@Composable
fun OnboardingScreen(
    profile: UserProfile,
    onComplete: (UserProfile) -> Unit,
) {
    val c = LocalAuraColors.current
    var step by remember { mutableStateOf(0) }
    var unit by remember { mutableStateOf(profile.unit) }
    var heightText by remember { mutableStateOf(if (profile.heightCm > 0) profile.heightCm.toInt().toString() else "") }
    var weightText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }

    val weightKg = weightText.toDoubleOrNull()?.let { Units.fromDisplay(it, unit) }
    val targetKg = targetText.toDoubleOrNull()?.let { Units.fromDisplay(it, unit) }
    val heightCm = heightText.toDoubleOrNull() ?: 0.0

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        Dots(count = 4, active = step)
        Spacer(Modifier.height(40.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding",
        ) { current ->
            when (current) {
                0 -> WelcomeStep()
                1 -> UnitStep(unit) { unit = it }
                2 -> BodyStep(
                    unit = unit,
                    heightText = heightText,
                    onHeight = { heightText = it },
                    weightText = weightText,
                    onWeight = { weightText = it },
                )
                else -> GoalStep(
                    unit = unit,
                    currentText = weightText,
                    targetText = targetText,
                    onTarget = { targetText = it },
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        when (step) {
            0 -> AccentButton("Get started") { step = 1 }
            1 -> AccentButton("Continue") { step = 2 }
            2 -> AccentButton(
                "Continue",
                enabled = heightCm in 100.0..250.0 && weightKg != null && weightKg in 20.0..400.0,
            ) { step = 3 }
            else -> AccentButton(
                "Start tracking",
                enabled = targetKg != null && targetKg in 20.0..400.0,
            ) {
                onComplete(
                    profile.copy(
                        onboarded = true,
                        unit = unit,
                        heightCm = heightCm,
                        startKg = weightKg,
                        targetKg = targetKg,
                    ),
                )
            }
        }

        if (step > 0) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(SquircleShape(16.dp))
                    .clickable { step -= 1 },
                contentAlignment = Alignment.Center,
            ) {
                Text("Back", style = body(14, FontWeight.Medium), color = c.tertiary)
            }
        }

        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.navigationBarsPadding().height(1.dp))
    }
}


@Composable
private fun Dots(count: Int, active: Int) {
    val c = LocalAuraColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(width = if (i == active) 22.dp else 8.dp, height = 8.dp)
                    .clip(SquircleShape(4.dp))
                    .background(if (i == active) c.accent2 else c.quaternary.copy(alpha = 0.6f)),
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    val c = LocalAuraColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(104.dp)
                .clip(SquircleShape(34.dp))
                .background(Brush.horizontalGradient(listOf(c.accent, c.accent2))),
            contentAlignment = Alignment.Center,
        ) {
            Text("A", style = hero(52), color = Color.White)
        }
        Spacer(Modifier.height(28.dp))
        Text("Weight, gently tracked.", style = title(26), color = c.primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "A quiet daily check-in, honest trends,\nand a face that changes with your day.",
            style = body(15),
            color = c.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UnitStep(unit: UnitSystem, onSelect: (UnitSystem) -> Unit) {
    val c = LocalAuraColors.current
    Column(Modifier.fillMaxWidth()) {
        Text("Your unit", style = title(26), color = c.primary)
        Spacer(Modifier.height(8.dp))
        Text("Switch any time — history is converted for you.", style = body(15), color = c.secondary)
        Spacer(Modifier.height(24.dp))
        ChoiceRow(
            options = listOf(UnitSystem.KG.name to "Kilograms", UnitSystem.LB.name to "Pounds"),
            selected = unit.name,
            onSelect = { onSelect(UnitSystem.valueOf(it)) },
        )
        Spacer(Modifier.height(20.dp))
        val sample = 72.4
        Text(
            "Example: ${Units.format(sample, unit)}",
            style = caption(14),
            color = c.tertiary,
        )
    }
}

@Composable
private fun BodyStep(
    unit: UnitSystem,
    heightText: String,
    onHeight: (String) -> Unit,
    weightText: String,
    onWeight: (String) -> Unit,
) {
    val c = LocalAuraColors.current
    Column(Modifier.fillMaxWidth()) {
        Text("About you", style = title(26), color = c.primary)
        Spacer(Modifier.height(8.dp))
        Text("Two numbers set your BMI and starting point.", style = body(15), color = c.secondary)
        Spacer(Modifier.height(28.dp))
        LabeledField(heading = "HEIGHT") {
            NumberField(
                value = heightText,
                onValueChange = onHeight,
                unitLabel = "cm",
                textStyle = number(44),
                placeholder = "175",
            )
        }
        Spacer(Modifier.height(20.dp))
        LabeledField(heading = "CURRENT WEIGHT") {
            NumberField(
                value = weightText,
                onValueChange = onWeight,
                unitLabel = Units.label(unit),
                textStyle = number(44),
                placeholder = "72.4",
            )
        }
    }
}

@Composable
private fun GoalStep(
    unit: UnitSystem,
    currentText: String,
    targetText: String,
    onTarget: (String) -> Unit,
) {
    val c = LocalAuraColors.current
    Column(Modifier.fillMaxWidth()) {
        Text("Your goal", style = title(26), color = c.primary)
        Spacer(Modifier.height(8.dp))
        Text("Where would you like the line to land?", style = body(15), color = c.secondary)
        Spacer(Modifier.height(28.dp))
        if (currentText.isNotEmpty()) {
            Text("STARTING AT ${currentText} ${Units.label(unit).uppercase()}", style = label(10), color = c.tertiary)
            Spacer(Modifier.height(10.dp))
        }
        LabeledField(heading = "TARGET WEIGHT") {
            NumberField(
                value = targetText,
                onValueChange = onTarget,
                unitLabel = Units.label(unit),
                textStyle = number(44),
                placeholder = "68.0",
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "You'll see your progress on the home card.",
            style = caption(13),
            color = c.tertiary,
        )
    }
}

