package com.aura.weight.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.weight.data.Stats
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.Units
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightEntry
import com.aura.weight.ui.components.AuraTopBar
import com.aura.weight.ui.components.currentLocale
import com.aura.weight.ui.components.SectionCard
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.theme.body
import com.aura.weight.ui.theme.caption
import com.aura.weight.ui.theme.label
import com.aura.weight.ui.theme.number
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface RowItem {
    data class Month(val title: String) : RowItem
    data class Entry(val entry: WeightEntry) : RowItem
}

@Composable
fun HistoryScreen(
    profile: UserProfile,
    entries: List<WeightEntry>,
    onBack: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    val c = LocalAuraColors.current
    val unit = profile.unit
    val today = LocalDate.now().toEpochDay()
    val descending = entries.sortedByDescending { it.day }
    val first = entries.minByOrNull { it.day }
    val totalDelta = if (entries.size >= 2) descending.first().kg - descending.last().kg else null

    val locale = currentLocale()
    val rows = buildList {
        var lastHeader = ""
        descending.forEach { entry ->
            val header = LocalDate.ofEpochDay(entry.day)
                .format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                .uppercase(locale)
            if (header != lastHeader) {
                lastHeader = header
                add(RowItem.Month(header))
            }
            add(RowItem.Entry(entry))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        AuraTopBar(title = "History", onBack = onBack, backIcon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(18.dp))

        SectionCard("SUMMARY") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${entries.size} ${if (entries.size == 1) "entry" else "entries"}",
                        style = number(26),
                        color = c.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        first?.let {
                            "Since " + LocalDate.ofEpochDay(it.day)
                                .format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
                        } ?: "No entries yet",
                        style = caption(12),
                        color = c.tertiary,
                    )
                }
                if (totalDelta != null) {
                    Text(
                        Units.formatSigned(totalDelta, unit),
                        style = number(18),
                        color = if (totalDelta <= 0) c.positive else c.negative,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (rows.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing here yet.", style = body(15), color = c.secondary)
                Spacer(Modifier.height(6.dp))
                Text("Logged days will collect here.", style = body(14), color = c.tertiary)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rows, key = { row ->
                    when (row) {
                        is RowItem.Month -> "m-${row.title}"
                        is RowItem.Entry -> "e-${row.entry.day}"
                    }
                }) { row ->
                    when (row) {
                        is RowItem.Month -> MonthHeader(row.title)
                        is RowItem.Entry -> EntryRow(
                            entry = row.entry,
                            previousKg = Stats.nearestAtOrBefore(entries, row.entry.day - 1)?.kg,
                            unit = unit,
                            isToday = row.entry.day == today,
                            onDelete = { onDelete(row.entry.day) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.navigationBarsPadding().height(1.dp))
    }
}

@Composable
private fun MonthHeader(text: String) {
    val c = LocalAuraColors.current
    Text(text, style = label(10), color = c.tertiary, modifier = Modifier.padding(start = 4.dp, top = 10.dp))
}

@Composable
private fun EntryRow(
    entry: WeightEntry,
    previousKg: Double?,
    unit: UnitSystem,
    isToday: Boolean,
    onDelete: () -> Unit,
) {
    val c = LocalAuraColors.current
    val locale = currentLocale()
    val date = LocalDate.ofEpochDay(entry.day)
    val delta = previousKg?.let { entry.kg - it }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)),
                style = body(15, FontWeight.Medium),
                color = c.primary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (isToday) "Today" else "Logged",
                style = caption(12),
                color = c.tertiary,
            )
        }
        if (delta != null) {
            Text(
                Units.formatSigned(delta, unit),
                style = body(13, FontWeight.SemiBold),
                color = when {
                    delta > 0.05 -> c.negative
                    delta < -0.05 -> c.positive
                    else -> c.tertiary
                },
                modifier = Modifier.padding(end = 14.dp),
            )
        }
        Text(
            Units.format(entry.kg, unit),
            style = number(17),
            color = c.primary,
            modifier = Modifier.padding(end = 12.dp),
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Delete entry",
                tint = c.tertiary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
