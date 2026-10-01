package com.aura.weight.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.weight.core.Backdrops
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.Units
import com.aura.weight.data.UserProfile
import com.aura.weight.ui.components.AccentButton
import com.aura.weight.ui.components.AuraTopBar
import com.aura.weight.ui.components.ChoiceRow
import com.aura.weight.ui.components.LabeledField
import com.aura.weight.ui.components.NumberField
import com.aura.weight.ui.components.SectionCard
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.theme.body
import com.aura.weight.ui.theme.caption
import com.aura.weight.ui.theme.number

@Composable
fun SettingsScreen(
    profile: UserProfile,
    widgetProviders: Int,
    onBack: () -> Unit,
    onSave: (UserProfile) -> Unit,
) {
    val c = LocalAuraColors.current
    var unit by remember { mutableStateOf(profile.unit) }
    var heightText by remember { mutableStateOf(if (profile.heightCm > 0) profile.heightCm.toInt().toString() else "") }
    var startText by remember {
        mutableStateOf(profile.startKg?.let { Units.format(it, profile.unit).substringBefore(' ') } ?: "")
    }
    var targetText by remember {
        mutableStateOf(profile.targetKg?.let { Units.format(it, profile.unit).substringBefore(' ') } ?: "")
    }
    var backdrop by remember { mutableStateOf(profile.backdropId) }
    var dark by remember { mutableStateOf(profile.darkTheme) }
    var widgetAlpha by remember { mutableStateOf(profile.widgetAlpha) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        AuraTopBar(title = "Settings", onBack = onBack, backIcon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(18.dp))

        SectionCard("UNITS") {
            ChoiceRow(
                options = listOf(UnitSystem.KG.name to "Kilograms", UnitSystem.LB.name to "Pounds"),
                selected = unit.name,
                onSelect = { unit = UnitSystem.valueOf(it) },
            )
        }

        Spacer(Modifier.height(14.dp))

        SectionCard("BODY") {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                LabeledField(heading = "HEIGHT (CM)") {
                    NumberField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        unitLabel = "cm",
                        textStyle = number(34),
                        placeholder = "175",
                    )
                }
                LabeledField(heading = "START WEIGHT") {
                    NumberField(
                        value = startText,
                        onValueChange = { startText = it },
                        unitLabel = Units.label(unit),
                        textStyle = number(34),
                        placeholder = "80.0",
                    )
                }
                LabeledField(heading = "TARGET WEIGHT") {
                    NumberField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        unitLabel = Units.label(unit),
                        textStyle = number(34),
                        placeholder = "68.0",
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        SectionCard("BACKDROP") {
            ChoiceRow(
                options = Backdrops.all.map { it.id to it.name },
                selected = backdrop,
                onSelect = { backdrop = it },
            )
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                options = listOf("dark" to "Dark", "light" to "Light"),
                selected = if (dark) "dark" else "light",
                onSelect = { dark = it == "dark" },
            )
        }

        Spacer(Modifier.height(14.dp))

        SectionCard("WIDGETS") {
            ChoiceRow(
                options = listOf("clear" to "Clear", "balanced" to "Balanced", "frosted" to "Frosted"),
                selected = when {
                    widgetAlpha <= 54 -> "clear"
                    widgetAlpha <= 102 -> "balanced"
                    else -> "frosted"
                },
                onSelect = {
                    widgetAlpha = when (it) {
                        "clear" -> 36
                        "frosted" -> 132
                        else -> 72
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Widget background frost. Clear shows more wallpaper blur; frosted is easier to read.",
                style = caption(13),
                color = c.tertiary,
            )
        }

        Spacer(Modifier.height(14.dp))

        SectionCard("DIAGNOSTICS") {
            val status = when {
                widgetProviders < 0 -> "Widget status unavailable"
                widgetProviders == 0 -> "0 widgets registered - system does not see Aura widgets"
                else -> "$widgetProviders widgets registered with the system"
            }
            Text("Widget status", style = body(15), color = c.primary)
            Spacer(Modifier.height(4.dp))
            Text(status, style = caption(13), color = if (widgetProviders > 0) c.accent else c.tertiary)
        }

        Spacer(Modifier.height(14.dp))

        SectionCard("ABOUT") {
            Text("Aura 1.0.7", style = body(15), color = c.primary)
            Spacer(Modifier.height(4.dp))
            Text("A quiet weight journal.", style = caption(13), color = c.tertiary)
        }

        Spacer(Modifier.height(22.dp))

        AccentButton(
            text = "Save changes",
            onClick = {
                val heightCm = heightText.toDoubleOrNull() ?: 0.0
                val startKg = startText.toDoubleOrNull()?.let { Units.fromDisplay(it, unit) }
                val targetKg = targetText.toDoubleOrNull()?.let { Units.fromDisplay(it, unit) }
                onSave(
                    profile.copy(
                        unit = unit,
                        heightCm = heightCm,
                        startKg = startKg,
                        targetKg = targetKg,
                        backdropId = backdrop,
                        darkTheme = dark,
                        widgetAlpha = widgetAlpha,
                    ),
                )
            },
        )

        Spacer(Modifier.height(40.dp))
        Spacer(Modifier.navigationBarsPadding().height(1.dp))
    }
}
