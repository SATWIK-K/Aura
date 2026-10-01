package com.aura.weight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.weight.data.Units
import com.aura.weight.data.UserProfile
import com.aura.weight.ui.components.AccentButton
import com.aura.weight.ui.components.CircleIconButton
import com.aura.weight.ui.components.GhostButton
import com.aura.weight.ui.components.currentLocale
import com.aura.weight.ui.components.NumberField
import com.aura.weight.ui.glass.GlassSurface
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.theme.body
import com.aura.weight.ui.theme.caption
import com.aura.weight.ui.theme.label
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LogSheet(
    open: Boolean,
    profile: UserProfile,
    existingTodayKg: Double?,
    onSave: (kg: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAuraColors.current
    val unit = profile.unit

    AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (c.isDark) 0.52f else 0.30f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }

    AnimatedVisibility(
        visible = open,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        var text by remember(open) {
            mutableStateOf(
                existingTodayKg?.let { formatOne(Units.toDisplay(it, unit)) } ?: "",
            )
        }
        val parsed = text.toDoubleOrNull()
        val kg = parsed?.let { Units.fromDisplay(it, unit) }
        val valid = kg != null && kg in 20.0..400.0
        val keyboard = LocalSoftwareKeyboardController.current

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            GlassSurface(corner = 34.dp, elevation = 28.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(width = 44.dp, height = 5.dp)
                                .background(
                                    c.quaternary.copy(alpha = 0.65f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LOG WEIGHT", style = label(11), color = c.tertiary, modifier = Modifier.weight(1f))
                        CircleIconButton(Icons.Outlined.Close, "Close", onDismiss, 48.dp)
                    }
                    Spacer(Modifier.height(18.dp))

                    NumberField(
                        value = text,
                        onValueChange = { text = it },
                        unitLabel = Units.label(unit),
                        placeholder = formatOne(Units.toDisplay(existingTodayKg ?: 70.0, unit)),
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NudgeButton("−0.1") {
                            text = formatOne(((text.toDoubleOrNull() ?: Units.toDisplay(existingTodayKg ?: 70.0, unit)) - 0.1).coerceAtLeast(20.0))
                        }
                        NudgeButton("+0.1") {
                            text = formatOne(((text.toDoubleOrNull() ?: Units.toDisplay(existingTodayKg ?: 70.0, unit)) + 0.1).coerceAtMost(400.0))
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", currentLocale())),
                        style = caption(13),
                        color = c.secondary,
                    )
                    if (existingTodayKg != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Replaces today's ${formatOne(Units.toDisplay(existingTodayKg, unit))} ${Units.label(unit)}",
                            style = caption(12),
                            color = c.tertiary,
                        )
                    }
                    if (text.isNotEmpty() && !valid) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Enter a weight from 20 to 400 ${Units.label(unit)}.",
                            style = caption(12),
                            color = c.negative,
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    AccentButton(
                        text = "Save weight",
                        enabled = valid,
                        onClick = {
                            keyboard?.hide()
                            kg?.let(onSave)
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    GhostButton(text = "Cancel", onClick = {
                        keyboard?.hide()
                        onDismiss()
                    })
                }
            }
        }
    }
}

@Composable
private fun NudgeButton(text: String, onClick: () -> Unit) {
    val c = LocalAuraColors.current
    Box(
        Modifier
            .size(width = 78.dp, height = 48.dp)
            .background(
                Color.White.copy(alpha = if (c.isDark) 0.07f else 0.55f),
                androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = body(15, FontWeight.SemiBold), color = c.secondary)
    }
}

private fun formatOne(value: Double): String = String.format(Locale.US, "%.1f", value)
