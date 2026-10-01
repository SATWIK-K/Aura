package com.aura.weight.core

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Continuous-curvature "squircle" shape (superellipse corners).
 *
 * [n] controls squareness: 2 = very round, 5 = iOS-like, higher = boxier.
 */
class SquircleShape(
    private val corner: Dp,
    private val n: Float = 5f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { corner.toPx() }.coerceAtMost(min(size.width, size.height) / 2f)
        if (r <= 0.75f) return Outline.Rectangle(Rect(Offset.Zero, size))
        return Outline.Generic(buildSquircle(Path(), size.width, size.height, r, n))
    }
}

/**
 * Builds a clockwise squircle path inside a w×h box.
 * Works for both outlines and direct Canvas drawing.
 */
fun buildSquircle(path: Path, w: Float, h: Float, r: Float, n: Float = 5f): Path {
    path.rewind()
    if (r <= 0f) {
        path.addRect(Rect(0f, 0f, w, h))
        return path
    }
    val radius = min(r, min(w, h) / 2f)
    val samples = 16
    val exponent = 2f / n

    // Unit quarter: u(θ) = (1 - cos^e θ, 1 - sin^e θ), θ: 0 → π/2 goes (0,1) → (1,0)
    // cos/sin are clamped to ≥ 0 first: at the exact quarter angles Float trig can
    // return a tiny negative value (≈ -4e-8) and (−ε)^0.4 is NaN, which would
    // poison the whole path.
    fun ux(t: Float): Float = 1f - kotlin.math.max(0f, cos(t)).pow(exponent)
    fun uy(t: Float): Float = 1f - kotlin.math.max(0f, sin(t)).pow(exponent)

    // Start on the top edge, walk clockwise: TL(arc) → TR(arc) → BR(arc) → BL(arc)
    // TL corner at (0,0): world = (ux*r, uy*r); θ 0→π/2 = (0,r) → (r,0)
    // We begin at TL's end (r,0), so TL is drawn last (closing the loop).
    fun arc(cornerX: Float, cornerY: Float, sx: Float, sy: Float, dir: Float) {
        // sx/sy = ±1 map local (ux,uy) to world relative to corner
        for (i in 0..samples) {
            val t = if (dir > 0f) (i.toFloat() / samples) * (PI.toFloat() / 2f)
            else (1f - i.toFloat() / samples) * (PI.toFloat() / 2f)
            val x = cornerX + sx * ux(t) * radius
            val y = cornerY + sy * uy(t) * radius
            path.lineTo(x, y)
        }
    }

    // TL: corner (0,0), interior +x/+y, drawn θ π/2→0 so it runs (r,0)…(0,r) when closing.
    // Order: start at (r,0) [TL θ=π/2] → TR → BR → BL → back up left edge → close.
    // TR corner (w,0): world = (w - ux*r, uy*r); start θ=π/2 at (w-r,0), end θ=0 at (w,r)
    path.moveTo(radius, 0f)
    arc(w, 0f, -1f, +1f, dir = -1f)          // TR: (w-r,0) → (w,r)
    path.lineTo(w, h - radius)
    arc(w, h, -1f, -1f, dir = +1f)           // BR: (w,h-r) → (w-r,h)
    path.lineTo(radius, h)
    arc(0f, h, +1f, -1f, dir = -1f)          // BL: (r,h) → (0,h-r)
    path.lineTo(0f, radius)
    arc(0f, 0f, +1f, +1f, dir = +1f)         // TL: (0,r) → (r,0)
    path.close()
    return path
}
