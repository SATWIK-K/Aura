package com.aura.weight.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.weight.core.SquircleShape
import com.aura.weight.core.TrendPoint
import com.aura.weight.core.drawGlowBar
import com.aura.weight.core.drawTrend
import com.aura.weight.data.Stats
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.Units
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightEntry
import com.aura.weight.ui.components.AccentButton
import com.aura.weight.ui.components.CircleIconButton
import com.aura.weight.ui.components.currentLocale
import com.aura.weight.ui.components.QuoteCard
import com.aura.weight.ui.components.StatTile
import com.aura.weight.ui.glass.GlassSurface
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.Quotes
import com.aura.weight.ui.theme.body
import com.aura.weight.ui.theme.caption
import com.aura.weight.ui.theme.hero
import com.aura.weight.ui.theme.label
import com.aura.weight.ui.theme.number
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    profile: UserProfile,
    entries: List<WeightEntry>,
    onLog: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalAuraColors.current
    val locale = currentLocale()
    val now = LocalDate.now()
    val today = now.toEpochDay()
    val unit = profile.unit
    val latest = Stats.latest(entries)
    val weekDelta = Stats.changeOverDays(entries, today, 7)
    val streak = Stats.streak(entries, today)
    val bmiValue = if (profile.heightCm > 0 && latest != null) {
        Stats.bmi(latest.kg, profile.heightCm)
    } else {
        0.0
    }
    val bmiStatus = if (bmiValue > 0) Stats.bmiCategory(bmiValue).label else null
    val loggedToday = entries.any { it.day == today }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        Header(
            greeting = greeting(java.time.LocalTime.now().hour),
            date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
            onHistory = onHistory,
            onSettings = onSettings,
        )

        HeroCard(
            profile = profile,
            latest = latest,
            unit = unit,
            loggedToday = loggedToday,
            weekDelta = weekDelta,
        )

        QuoteCard(
            text = Quotes.forDay(LocalContext.current, today).text,
            author = Quotes.forDay(LocalContext.current, today).author,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                "BMI",
                if (bmiValue > 0) String.format(Locale.US, "%.1f", bmiValue) else "—",
                status = bmiStatus,
                modifier = Modifier.weight(1f),
                accentIndex = 2,
            )
            StatTile(
                "THIS WEEK",
                if (weekDelta != null) signed(weekDelta, unit) else "—",
                modifier = Modifier.weight(1f),
                accentIndex = 0,
            )
            StatTile(
                "STREAK",
                streak.toString(),
                if (streak == 1) "day" else "days",
                Modifier.weight(1f),
                accentIndex = 1,
            )
        }

        ChartCard(entries = entries, today = today, unit = unit, locale = locale, goalKg = profile.targetKg)

        Spacer(Modifier.height(4.dp))

        AccentButton(
            text = if (loggedToday) "Update today's weight" else "Log today's weight",
            height = 64.dp,
            onClick = onLog,
        )

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.navigationBarsPadding().height(1.dp))
    }
}

private fun greeting(hour: Int): String = when {
    hour < 12 -> "GOOD MORNING"
    hour < 18 -> "GOOD AFTERNOON"
    else -> "GOOD EVENING"
}

private fun signed(value: Double, unit: UnitSystem): String {
    val shown = Units.toDisplay(value, unit)
    val sign = if (shown < 0) "−" else "+"
    return sign + String.format(Locale.US, "%.1f", kotlin.math.abs(shown))
}

@Composable
private fun Header(greeting: String, date: String, onHistory: () -> Unit, onSettings: () -> Unit) {
    val c = LocalAuraColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = label(11), color = c.tertiary)
            Spacer(Modifier.height(4.dp))
            Text(date, style = body(15), color = c.primary)
        }
        CircleIconButton(Icons.Outlined.History, "History", onHistory, 48.dp)
        Spacer(Modifier.width(10.dp))
        CircleIconButton(Icons.Outlined.Tune, "Settings", onSettings, 48.dp)
    }
}

private data class GoalState(
    val goalKg: Double,
    val progress: Float,
    val reached: Boolean,
    val remaining: Double,
)

@Composable
private fun HeroCard(
    profile: UserProfile,
    latest: WeightEntry?,
    unit: UnitSystem,
    loggedToday: Boolean,
    weekDelta: Double?,
) {
    val c = LocalAuraColors.current
    val target = profile.targetKg
    val start = profile.startKg
    val current = latest
    val goal = target
    val goalState = if (current != null && goal != null && start != null) {
        val progress = Stats.goalProgress(current.kg, start, goal)
        GoalState(
            goalKg = goal,
            progress = progress,
            reached = progress >= 1f,
            remaining = (current.kg - goal).coerceAtLeast(0.0),
        )
    } else {
        null
    }
    val progress = goalState?.progress ?: 0f

    GlassSurface(corner = 32.dp, elevation = 20.dp) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TODAY",
                    style = label(11),
                    color = c.tertiary,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(
                    text = if (loggedToday) "LOGGED" else "NOT LOGGED",
                    accent = loggedToday,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (latest != null) display(latest.kg, unit) else "—",
                    style = hero(68),
                    color = c.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
                if (latest != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Units.label(unit),
                        style = number(22, androidx.compose.ui.text.font.FontWeight.Medium),
                        color = c.tertiary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                if (weekDelta != null) {
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.padding(bottom = 14.dp)) {
                        Text(
                            signed(weekDelta, unit),
                            style = number(16, androidx.compose.ui.text.font.FontWeight.SemiBold),
                            color = if (weekDelta <= 0) c.positive else c.negative,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            when {
                latest == null -> Text(
                    "Log your first weight to start the story.",
                    style = body(14),
                    color = c.secondary,
                )

                goalState != null -> {
                    Text(
                        when {
                            goalState.reached -> "GOAL REACHED"
                            else -> "${Math.round(progress * 100)}% TO " +
                                "${display(goalState.goalKg, unit)} ${Units.label(unit)}"
                        },
                        style = label(11),
                        color = if (goalState.reached) c.positive else c.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                    Spacer(Modifier.height(10.dp))
                    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                        drawGlowBar(fraction = progress)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (goalState.reached) {
                            "You are at ${display(current.kg, unit)} ${Units.label(unit)}."
                        } else {
                            val shown = Units.toDisplay(goalState.remaining, unit)
                            "${String.format(Locale.US, "%.1f", shown)} " +
                                "${Units.label(unit)} to go"
                        },
                        style = caption(12),
                        color = c.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }

                else -> Text(
                    if (loggedToday) "Logged for today. Nice and consistent."
                    else "No goal yet — set one in settings.",
                    style = body(14),
                    color = c.secondary,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, accent: Boolean) {
    val c = LocalAuraColors.current
    val tint = if (accent) c.positive else c.tertiary
    Box(
        Modifier
            .clip(SquircleShape(9.dp))
            .border(1.dp, tint.copy(alpha = 0.55f), SquircleShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = label(9), color = tint, maxLines = 1)
    }
}

@Composable
private fun ChartCard(
    entries: List<WeightEntry>,
    today: Long,
    unit: UnitSystem,
    locale: Locale,
    goalKg: Double?,
) {
    val c = LocalAuraColors.current
    val window = entries.filter { it.day in (today - 29)..today }
    val delta = if (window.size >= 2) window.last().kg - window.first().kg else null

    GlassSurface(corner = 32.dp, elevation = 20.dp) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "LAST 30 DAYS",
                    style = label(11),
                    color = c.tertiary,
                    modifier = Modifier.weight(1f),
                )
                if (delta != null) {
                    Text(
                        signed(delta, unit) + " " + Units.label(unit),
                        style = number(15),
                        color = if (delta <= 0) c.positive else c.negative,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (window.size >= 2) {
                val vals = window.map { it.kg }
                Canvas(Modifier.fillMaxWidth().height(128.dp)) {
                    drawTrend(
                        points = window.mapIndexed { i, e -> TrendPoint(i, e.kg.toFloat()) },
                        range = Pair((vals.max() + 0.4).toFloat(), (vals.min() - 0.4).toFloat()),
                        lineColor = c.accent2,
                        fillColor = c.accent2.copy(alpha = 0.35f),
                        goal = goalKg?.toFloat(),
                        goalColor = c.positive.copy(alpha = 0.7f),
                        strokeWidth = 4f,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        dayLabel(window.first().day, locale),
                        style = label(10),
                        color = c.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        dayLabel(window.last().day, locale),
                        style = label(10),
                        color = c.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("The trend appears", style = body(14), color = c.secondary)
                    Text("once two days are logged.", style = body(14), color = c.tertiary)
                }
            }
        }
    }
}

private fun display(kg: Double, unit: UnitSystem): String =
    String.format(Locale.US, "%.1f", Units.toDisplay(kg, unit))

private fun dayLabel(epochDay: Long, locale: Locale): String =
    LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("d MMM", locale))
        .uppercase(locale)
