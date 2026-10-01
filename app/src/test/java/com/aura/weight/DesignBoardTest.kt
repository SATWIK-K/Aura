package com.aura.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.weight.core.TrendPoint
import com.aura.weight.core.drawGlowBar
import com.aura.weight.core.drawTrend
import com.aura.weight.ui.glass.GlassSurface
import com.aura.weight.ui.components.StatTile
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.theme.body
import com.aura.weight.ui.theme.caption
import com.aura.weight.ui.theme.hero
import com.aura.weight.ui.theme.label
import com.aura.weight.ui.theme.number
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * First-pass design board: the material, type, bar and chart together so the
 * whole language can be judged in one glance.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class DesignBoardTest : BaseVisualTest() {

    private fun series(): List<TrendPoint> {
        val noise = floatArrayOf(
            0f, 0.2f, -0.1f, 0.3f, 0.1f, -0.2f, 0.4f, 0.2f, -0.15f, 0.1f,
            0.35f, 0.1f, -0.2f, 0.05f, 0.25f, -0.1f, 0.15f, 0.3f, 0.0f, -0.2f,
            0.1f, 0.2f, -0.05f, 0.15f, 0.3f, 0.1f, -0.1f, 0.2f, 0.05f, 0.12f,
        )
        return noise.mapIndexed { i, n ->
            TrendPoint(i, 74.2f - i * 0.06f + n)
        }
    }

    @Test
    fun boardDark() = capture("board-dark.png", dark = true) { Board(series()) }

    @Test
    fun boardLight() = capture("board-light.png", dark = false) { Board(series()) }

    @Test
    fun materialCloseup() = capture("material-closeup.png", dark = true) { Closeup() }
}

@Composable
private fun Dots(colors: List<Color>, size: Int = 10) {
    Row(horizontalArrangement = Arrangement.spacedBy((-2).dp)) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(c),
            )
        }
    }
}

@Composable
fun Board(points: List<TrendPoint>) {
    val c = LocalAuraColors.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(52.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GOOD EVENING", style = label(11), color = c.tertiary)
                Spacer(Modifier.height(4.dp))
                Text("Tuesday, 30 September", style = body(15), color = c.primary)
            }
            Dots(listOf(c.accent, c.accent2, c.accent3), 11)
        }

        // Hero card
        GlassSurface(corner = 32.dp, elevation = 20.dp) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
                Text("TODAY", style = label(11), color = c.tertiary)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("72.4", style = hero(68), color = c.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "kg",
                        style = number(22, FontWeight.Medium),
                        color = c.tertiary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("62% TO 68 KG", style = label(11), color = c.secondary)
                Spacer(Modifier.height(10.dp))
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                ) {
                    drawGlowBar(fraction = 0.62f)
                }
            }
        }

        // Tiles
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("BMI", "22.4", status = "healthy", modifier = Modifier.weight(1f), accentIndex = 2)
            StatTile("THIS WEEK", "−0.8", "kg", Modifier.weight(1f), accentIndex = 0)
            StatTile("STREAK", "12", "days", Modifier.weight(1f), accentIndex = 1)
        }

        // Chart card
        GlassSurface(corner = 32.dp, elevation = 20.dp) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LAST 30 DAYS", style = label(11), color = c.tertiary, modifier = Modifier.weight(1f))
                    Text("−1.6 kg", style = number(15), color = c.positive)
                }
                Spacer(Modifier.height(16.dp))
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                ) {
                    val vals = points.map { it.v }
                    drawTrend(
                        points = points,
                        range = Pair(vals.max() + 0.4f, vals.min() - 0.4f),
                        lineColor = c.accent2,
                        fillColor = c.accent2.copy(alpha = 0.35f),
                        goal = 68f,
                        goalColor = c.positive.copy(alpha = 0.7f),
                        strokeWidth = 4f,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Text("31 AUG", style = label(9), color = c.quaternary)
                    Spacer(Modifier.weight(1f))
                    Text("30 SEP", style = label(9), color = c.quaternary)
                }
            }
        }

        // Primary action
        Box(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(com.aura.weight.core.SquircleShape(26.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(c.accent, c.accent2),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Log today's weight",
                style = body(17, FontWeight.SemiBold),
                color = Color.White,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Closeup() {
    val c = LocalAuraColors.current
    Box(Modifier.fillMaxSize()) {
        GlassSurface(
            corner = 36.dp,
            elevation = 24.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 36.dp, top = 96.dp)
                .size(width = 300.dp, height = 210.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("FROST", style = label(11), color = c.tertiary)
                Spacer(Modifier.height(8.dp))
                Text("Glass 01", style = hero(34), color = c.primary)
                Spacer(Modifier.height(14.dp))
                Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                    drawGlowBar(fraction = 0.35f)
                }
            }
        }
        GlassSurface(
            corner = 24.dp,
            elevation = 18.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = 140.dp)
                .size(width = 260.dp, height = 170.dp),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("OVERLAP", style = label(11), color = c.tertiary)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("23.1", style = hero(44), color = c.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("bmi", style = caption(13), color = c.tertiary, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Both cards frost the backdrop beneath them independently.",
                    style = caption(12),
                    color = c.secondary,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}
