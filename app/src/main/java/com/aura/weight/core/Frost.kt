package com.aura.weight.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.aura.weight.ui.theme.GlassTint
import kotlin.math.roundToInt

/**
 * The glass material, drawn into a card already clipped to its squircle:
 *   blurred backdrop slice → tint → grain → specular → hairline border.
 *
 * The slice is sampled from the region *directly beneath* the card, so the
 * frost lines up with what is actually behind it.
 */
fun DrawScope.drawFrost(
    backdrop: AuraBackdrop?,
    cardPos: Offset,
    tint: GlassTint,
    radius: Float,
    n: Float = 5f,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val bl = backdrop?.blurred
    val screen = backdrop?.screen
    if (bl != null && screen != null && screen.width > 0 && screen.height > 0) {
        drawBlurSlice(bl, screen, cardPos, w, h)
    } else {
        drawRect(tint.top)
    }

    // Tint: darker toward the bottom, like light falling through glass.
    drawRect(Brush.verticalGradient(listOf(tint.top, tint.bottom), 0f, h))

    // Grain.
    if (tint.grainAlpha > 0f) {
        drawRect(Grain.brush(), alpha = tint.grainAlpha)
    }

    // Specular: soft sheen along the top edge.
    if (tint.specular.alpha > 0.001f) {
        drawRect(
            Brush.verticalGradient(
                listOf(tint.specular, Color.Transparent),
                0f,
                h * 0.28f,
            ),
        )
    }

    // Hairline border: light on top, weight on the bottom.
    val path = buildSquircle(Path(), w, h, radius, n)
    drawPath(
        path = path,
        brush = Brush.verticalGradient(listOf(tint.borderTop, tint.borderBottom), 0f, h),
        style = Stroke(width = 1.5f),
    )
}

private fun DrawScope.drawBlurSlice(
    bl: ImageBitmap,
    screen: IntSize,
    pos: Offset,
    w: Float,
    h: Float,
) {
    val bw = bl.width.toFloat()
    val bh = bl.height.toFloat()
    if (bw < 1f || bh < 1f) return
    val kx = bw / screen.width
    val ky = bh / screen.height

    val x0 = pos.x * kx
    val y0 = pos.y * ky
    val x1 = (pos.x + w) * kx
    val y1 = (pos.y + h) * ky

    val cx0 = x0.coerceIn(0f, bw)
    val cy0 = y0.coerceIn(0f, bh)
    val cx1 = x1.coerceIn(0f, bw)
    val cy1 = y1.coerceIn(0f, bh)
    if (cx1 - cx0 < 0.5f || cy1 - cy0 < 0.5f) return

    val fx0 = if (x1 > x0) (cx0 - x0) / (x1 - x0) else 0f
    val fy0 = if (y1 > y0) (cy0 - y0) / (y1 - y0) else 0f
    val fx1 = if (x1 > x0) (cx1 - x0) / (x1 - x0) else 1f
    val fy1 = if (y1 > y0) (cy1 - y0) / (y1 - y0) else 1f

    drawImage(
        image = bl,
        srcOffset = IntOffset(cx0.roundToInt(), cy0.roundToInt()),
        srcSize = IntSize(
            (cx1 - cx0).roundToInt().coerceAtLeast(1),
            (cy1 - cy0).roundToInt().coerceAtLeast(1),
        ),
        dstOffset = IntOffset((fx0 * w).roundToInt(), (fy0 * h).roundToInt()),
        dstSize = IntSize(
            ((fx1 - fx0) * w).roundToInt().coerceAtLeast(1),
            ((fy1 - fy0) * h).roundToInt().coerceAtLeast(1),
        ),
        filterQuality = FilterQuality.Medium,
    )
}

/** Convenience for drawing glass onto a plain rect (used by widgets/tests). */
fun DrawScope.drawFrostRect(
    backdrop: AuraBackdrop?,
    rect: Offset,
    size: Size,
    tint: GlassTint,
    radius: Float,
    n: Float = 5f,
) {
    drawFrost(backdrop, rect, tint, radius, n)
}
