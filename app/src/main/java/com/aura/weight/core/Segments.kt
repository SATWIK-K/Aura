package com.aura.weight.core

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.min

/**
 * The glow bar: a rounded track with a gradient fill and a bright leading bead.
 * Optional [segments] draws gaps that split the fill into milestones.
 */
fun DrawScope.drawGlowBar(
    fraction: Float,
    colors: List<Color> = listOf(Color(0xFF6A7BFF), Color(0xFF3ED0FF), Color(0xFF4CE0B3)),
    trackColor: Color = Color.White.copy(alpha = 0.10f),
    height: Float = size.height,
    segments: Int = 0,
    bead: Boolean = true,
) {
    val w = size.width
    val h = min(height, size.height)
    val top = (size.height - h) / 2f
    val r = h / 2f
    val frac = fraction.coerceIn(0f, 1f)

    // Track
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(0f, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r),
    )

    if (frac <= 0.001f) return

    val fillW = (w * frac).coerceAtLeast(h)
    val fillPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                0f, top, fillW, top + h,
                CornerRadius(min(r, fillW / 2f), min(r, fillW / 2f)),
            ),
        )
    }

    // Gradient across the full width so partial fills show the ramp start.
    drawPath(
        path = fillPath,
        brush = Brush.horizontalGradient(
            colors,
            startX = 0f,
            endX = w,
        ),
    )

    // Milestone gaps
    if (segments > 1) {
        val gap = 3f
        for (i in 1 until segments) {
            val x = w * i / segments
            if (x < fillW) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(x - gap / 2f, top + 1f),
                    size = Size(gap, h - 2f),
                )
            }
        }
    }

    // Top sheen on the fill
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
            startY = top,
            endY = top + h * 0.55f,
        ),
        style = Fill,
        alpha = 1f,
    )

    // Leading bead: hot dot + halo
    if (bead && frac > 0.02f && frac < 0.999f) {
        val cx = fillW - h * 0.5f
        val cy = top + h / 2f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(colors.last().copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx, cy),
                radius = h * 1.15f,
            ),
            radius = h * 1.15f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = Color.White,
            radius = h * 0.26f,
            center = Offset(cx, cy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.9f), colors.last().copy(alpha = 0.8f)),
                center = Offset(cx, cy),
                radius = h * 0.5f,
            ),
            radius = h * 0.42f,
            center = Offset(cx, cy),
        )
    }
}

/** Dashed horizontal line (goal markers). */
fun DrawScope.drawDashedLine(
    startY: Float,
    startX: Float,
    endX: Float,
    color: Color,
    strokeWidth: Float = 3f,
    dash: Float = 18f,
    gap: Float = 14f,
) {
    drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(endX, startY),
        strokeWidth = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f),
        cap = StrokeCap.Round,
    )
}

/** Thin gradient hairline used as a divider on glass. */
fun DrawScope.drawHairline(
    startY: Float,
    color: Color = Color.White.copy(alpha = 0.10f),
) {
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, color, Color.Transparent),
        ),
        topLeft = Offset(0f, startY),
        size = Size(size.width, 1f),
    )
}
