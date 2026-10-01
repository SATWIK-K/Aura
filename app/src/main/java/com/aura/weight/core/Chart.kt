package com.aura.weight.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.min

data class TrendPoint(
    /** Index position (0 until n). */
    val i: Int,
    /** Weight value in kg (or any unit). */
    val v: Float,
)

/**
 * Trend chart: smooth area + line, dashed goal line, glowing end dot.
 *
 * [range] maps value → y: top = range.first (max), bottom = range.second (min).
 */
fun DrawScope.drawTrend(
    points: List<TrendPoint>,
    range: Pair<Float, Float>,
    size: Size = this.size,
    lineColor: Color,
    fillColor: Color,
    goal: Float? = null,
    goalColor: Color = Color.White.copy(alpha = 0.35f),
    strokeWidth: Float = 5f,
    padTop: Float = 14f,
    padBottom: Float = 14f,
) {
    if (points.size < 2 || size.width <= 0f || size.height <= 0f) return
    val n = points.size
    val top = padTop
    val bottom = size.height - padBottom
    val (maxV, minV) = range
    val span = max(0.0001f, maxV - minV)

    fun xOf(idx: Int): Float = size.width * idx / (n - 1).toFloat()
    fun yOf(v: Float): Float = top + (maxV - v) / span * (bottom - top)

    // Smooth path through midpoints (quadratic), reads softer than polylines.
    val line = Path()
    val pts = points.mapIndexed { idx, p -> Offset(xOf(idx), yOf(p.v)) }
    line.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        val prev = pts[i - 1]
        val curr = pts[i]
        val mid = Offset((prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
        line.quadraticTo(prev.x, prev.y, mid.x, mid.y)
    }
    line.lineTo(pts.last().x, pts.last().y)

    // Area fill under the line
    val area = Path().apply {
        addPath(line)
        lineTo(pts.last().x, bottom)
        lineTo(pts.first().x, bottom)
        close()
    }
    drawPath(
        path = area,
        brush = Brush.verticalGradient(
            listOf(fillColor, fillColor.copy(alpha = 0f)),
            startY = top,
            endY = bottom,
        ),
    )

    // Goal line
    if (goal != null && goal >= minV && goal <= maxV) {
        val gy = yOf(goal)
        drawLine(
            color = goalColor,
            start = Offset(0f, gy),
            end = Offset(size.width, gy),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f),
            cap = StrokeCap.Round,
        )
    }

    // Line
    drawPath(
        path = line,
        color = lineColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    // End dot with glow
    val end = pts.last()
    drawCircle(
        brush = Brush.radialGradient(
            listOf(lineColor.copy(alpha = 0.5f), Color.Transparent),
            center = end,
            radius = strokeWidth * 4f,
        ),
        radius = strokeWidth * 4f,
        center = end,
    )
    drawCircle(color = Color.White, radius = strokeWidth * 0.85f, center = end)
    drawCircle(color = lineColor, radius = strokeWidth * 0.55f, center = end)
}

/** Horizontal sparkline for widgets: line only, no axes. */
fun DrawScope.drawSparkline(
    values: List<Float>,
    size: Size = this.size,
    lineColor: Color,
    fillColor: Color? = null,
    strokeWidth: Float = 4f,
) {
    if (values.size < 2) return
    val maxV = values.max()
    val minV = values.min()
    val span = max(0.0001f, maxV - minV)
    val n = values.size
    fun xOf(i: Int) = size.width * i / (n - 1).toFloat()
    fun yOf(v: Float) = size.height - 2f - (v - minV) / span * (size.height - 4f)

    val path = Path()
    path.moveTo(xOf(0), yOf(values[0]))
    for (i in 1 until n) {
        val px = xOf(i - 1)
        val py = yOf(values[i - 1])
        val cx = xOf(i)
        val cy = yOf(values[i])
        path.quadraticTo(px, py, (px + cx) / 2f, (py + cy) / 2f)
        if (i == n - 1) path.lineTo(cx, cy)
    }

    if (fillColor != null) {
        val area = Path().apply {
            addPath(path)
            lineTo(xOf(n - 1), size.height)
            lineTo(xOf(0), size.height)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                listOf(fillColor, fillColor.copy(alpha = 0f)),
            ),
        )
    }
    drawPath(path, color = lineColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

/** Small round progress ring (BMI gauge / goal ring). */
fun DrawScope.drawRing(
    fraction: Float,
    size: Size = this.size,
    trackColor: Color,
    colors: List<Color>,
    strokeWidth: Float,
) {
    val f = fraction.coerceIn(0f, 1f)
    val d = min(size.width, size.height)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = (d - strokeWidth) / 2f

    drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
    if (f > 0.001f) {
        drawArc(
            brush = Brush.sweepGradient(colors),
            startAngle = -90f,
            sweepAngle = 360f * f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
