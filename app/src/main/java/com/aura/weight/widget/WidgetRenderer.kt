package com.aura.weight.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.aura.weight.MainActivity
import com.aura.weight.R
import com.aura.weight.data.BmiCategory
import com.aura.weight.data.Stats
import com.aura.weight.data.Units
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Builds adaptive, native [RemoteViews] for every widget style.
 *
 * Text stays as real TextViews (crisp at any size and density); only the
 * decorative artwork (sparkline, ring, bars, progress, scale) is drawn into
 * small transparent bitmaps.
 *
 * The style layout is the root RemoteViews layout and carries
 * `@android:id/background` — the id One UI walks to apply its wallpaper blur.
 * Nothing is nested via [RemoteViews.addView], because a host that re-applies
 * RemoteViews on update would append a duplicate copy of the content on every
 * update and leave stale text and artwork stacked on screen.
 */
object WidgetRenderer {

    /** Rough width budget reserved for a trailing "kg"/"lb" label plus its gap. */
    private const val UNIT_DP = 34

    fun build(
        context: Context,
        style: WidgetStyle,
        data: WidgetData,
        dark: Boolean,
        minWidthDp: Int,
        minHeightDp: Int,
    ): RemoteViews {
        val density = context.resources.displayMetrics.density
        val ink = ink(dark)
        val minW = minWidthDp.coerceAtLeast(style.minWidthDp / 2)
        val minH = minHeightDp.coerceAtLeast(style.minHeightDp / 2)

        val views = RemoteViews(context.packageName, layoutFor(style))
        when (style) {
            WidgetStyle.TODAY -> populateToday(views, data, dark, ink, density, minW, minH)
            WidgetStyle.TREND -> populateTrend(views, data, dark, ink, density, minW, minH)
            WidgetStyle.RING -> populateRing(views, data, dark, ink, density, minW, minH)
            WidgetStyle.STREAK -> populateStreak(views, data, dark, ink, density, minW, minH)
            WidgetStyle.BMI -> populateBmi(views, data, dark, ink, density, minW, minH)
            WidgetStyle.COMPACT -> populateCompact(views, data, dark, ink, density, minW, minH)
            WidgetStyle.PROGRESS -> populateProgress(views, data, dark, ink, density, minW, minH)
        }

        // User-adjustable frost; clamped so One UI always sees a valid blur alpha.
        val alpha = data.profile.widgetAlpha.coerceIn(1, 254)
        views.setInt(
            android.R.id.background,
            "setBackgroundColor",
            if (dark) Color.argb(alpha, 10, 14, 22) else Color.argb(alpha, 255, 255, 255),
        )
        views.setOnClickPendingIntent(android.R.id.background, openApp(context, style))
        return views
    }

    private fun layoutFor(style: WidgetStyle): Int = when (style) {
        WidgetStyle.TODAY -> R.layout.widget_content_today
        WidgetStyle.TREND -> R.layout.widget_content_trend
        WidgetStyle.RING -> R.layout.widget_content_ring
        WidgetStyle.STREAK -> R.layout.widget_content_streak
        WidgetStyle.BMI -> R.layout.widget_content_bmi
        WidgetStyle.COMPACT -> R.layout.widget_content_compact
        WidgetStyle.PROGRESS -> R.layout.widget_content_progress
    }

    private fun openApp(context: Context, style: WidgetStyle): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            style.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal data class Ink(
        val primary: Int,
        val secondary: Int,
        val tertiary: Int,
        val accent: Int,
        val accent2: Int,
        val accent3: Int,
        val positive: Int,
        val negative: Int,
        val caution: Int,
    )

    internal fun inkForTest(dark: Boolean): Ink = ink(dark)

    private fun ink(dark: Boolean): Ink = if (dark) {
        Ink(
            primary = Color.parseColor("#F2F4F9"),
            secondary = Color.parseColor("#A6ADBF"),
            tertiary = Color.parseColor("#7E859B"),
            accent = Color.parseColor("#6A7BFF"),
            accent2 = Color.parseColor("#3ED0FF"),
            accent3 = Color.parseColor("#4CE0B3"),
            positive = Color.parseColor("#4CE0B3"),
            negative = Color.parseColor("#FF6B81"),
            caution = Color.parseColor("#FFB454"),
        )
    } else {
        Ink(
            primary = Color.parseColor("#111318"),
            secondary = Color.parseColor("#4E5464"),
            tertiary = Color.parseColor("#666D81"),
            accent = Color.parseColor("#4B5BE8"),
            accent2 = Color.parseColor("#0E9DC4"),
            accent3 = Color.parseColor("#0EA97C"),
            positive = Color.parseColor("#0FA274"),
            negative = Color.parseColor("#E0455F"),
            caution = Color.parseColor("#C67C10"),
        )
    }

    /**
     * Picks a text size that fits the room actually available, so the number
     * scales with the widget instead of being clipped, and the unit label can
     * sit right next to it.
     *
     * Width budget assumes a typical value ("80.5") is roughly 3.4 em wide;
     * height budget assumes a line box of about 1.3 em.
     */
    private fun fitSp(availableWidthDp: Int, availableHeightDp: Int, minSp: Float, maxSp: Float): Float {
        val byWidth = availableWidthDp / 3.4f
        val byHeight = availableHeightDp / 1.3f
        return minOf(byWidth, byHeight).coerceIn(minSp, maxSp)
    }

    private fun valueSp(style: WidgetStyle, minW: Int, minH: Int): Float {
        val padding = 28 // 14dp each side
        return when (style) {
            // value + unit sit on one baseline row
            WidgetStyle.TODAY -> fitSp(minW - padding - UNIT_DP, minH - 62, 22f, 44f)
            WidgetStyle.PROGRESS -> {
                val art = if (minH >= 92) 30 else 0
                fitSp(minW - padding - UNIT_DP, minH - 62 - art, 22f, 44f)
            }
            // no unit label on BMI
            WidgetStyle.BMI -> {
                val art = if (minH >= 104) 24 else 0
                fitSp(minW - padding, minH - 56 - art, 22f, 44f)
            }
            // ring content is centred inside the ring
            WidgetStyle.RING -> fitSp(minW - padding - 24, minH - padding - 30, 20f, 36f)
            // streak shares the row with a 7-day bar block
            WidgetStyle.STREAK -> fitSp(minW - padding - 120, minH - padding, 22f, 40f)
            WidgetStyle.COMPACT -> {
                val unit = if (minW >= 110) UNIT_DP else 0
                val delta = if (minW >= 140) 52 else 0
                fitSp(minW - 24 - unit - delta, minH - 24, 16f, 30f)
            }
            WidgetStyle.TREND -> min(minW * 0.07f, minH * 0.12f).coerceIn(13f, 18f)
        }
    }

    private fun titleSp(value: Float): Float = (value * 0.30f).coerceIn(10f, 13f)
    private fun subSp(value: Float): Float = (value * 0.36f).coerceIn(11f, 15f)

    private fun RemoteViews.sp(id: Int, sizeSp: Float) {
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    private fun RemoteViews.show(id: Int, visible: Boolean) {
        setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)
    }

    private fun hasGoal(data: WidgetData): Boolean =
        data.profile.targetKg != null && data.profile.startKg != null

    private fun formatWeight(data: WidgetData, kg: Double): String =
        String.format(Locale.US, "%.1f", Units.toDisplay(kg, data.profile.unit))

    private fun signed(data: WidgetData, delta: Double): String {
        val shown = Units.toDisplay(delta, data.profile.unit)
        val sign = if (shown < 0) "−" else "+"
        return sign + String.format(Locale.US, "%.1f", abs(shown))
    }

    private fun deltaColor(data: WidgetData, ink: Ink, delta: Double): Int =
        if (delta <= 0.0) ink.positive else ink.negative

    private fun dateLabel(day: Long, pattern: String): String =
        LocalDate.ofEpochDay(day)
            .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))

    private fun categoryColor(ink: Ink, category: BmiCategory): Int = when (category) {
        BmiCategory.UNDER -> ink.caution
        BmiCategory.HEALTHY -> ink.positive
        BmiCategory.OVER -> ink.caution
        BmiCategory.OBESE -> ink.negative
    }

    // ------------------------------------------------------------------ //
    // Per-style content                                                    //
    // ------------------------------------------------------------------ //

    private fun populateToday(
        views: RemoteViews,
        data: WidgetData,
        dark: Boolean,
        ink: Ink,
        density: Float,
        minW: Int,
        minH: Int,
    ) {
        val v = valueSp(WidgetStyle.TODAY, minW, minH)
        val latest = data.latest
        val delta = data.weekDelta

        views.setTextViewText(R.id.widget_title, "TODAY")
        views.setTextColor(R.id.widget_title, ink.tertiary)
        views.sp(R.id.widget_title, titleSp(v))

        if (delta != null && minW >= 180) {
            views.setTextViewText(R.id.widget_delta, signed(data, delta))
            views.setTextColor(R.id.widget_delta, deltaColor(data, ink, delta))
            views.sp(R.id.widget_delta, subSp(v))
            views.show(R.id.widget_delta, true)
        } else {
            views.show(R.id.widget_delta, false)
        }

        views.setTextViewText(R.id.widget_value, latest?.let { formatWeight(data, it.kg) } ?: "—")
        views.setTextColor(R.id.widget_value, ink.primary)
        views.sp(R.id.widget_value, v)

        if (latest != null) {
            views.setTextViewText(R.id.widget_unit, Units.label(data.profile.unit))
            views.setTextColor(R.id.widget_unit, ink.secondary)
            views.sp(R.id.widget_unit, subSp(v))
            views.show(R.id.widget_unit, true)
        } else {
            views.show(R.id.widget_unit, false)
        }

        views.setTextViewText(
            R.id.widget_sub,
            latest?.let { dateLabel(data.today, "EEE d MMM").uppercase(Locale.US) }
                ?: "Log your first weight",
        )
        views.setTextColor(R.id.widget_sub, ink.secondary)
        views.sp(R.id.widget_sub, subSp(v))
        views.show(R.id.widget_sub, minH >= 64)
    }

    private fun populateTrend(
        views: RemoteViews,
        data: WidgetData,
        dark: Boolean,
        ink: Ink,
        density: Float,
        minW: Int,
        minH: Int,
    ) {
        val v = valueSp(WidgetStyle.TREND, minW, minH)
        val latest = data.latest
        val window = data.window30

        views.setTextViewText(R.id.widget_title, "LAST 30 DAYS")
        views.setTextColor(R.id.widget_title, ink.tertiary)
        views.sp(R.id.widget_title, titleSp(v))

        views.setTextViewText(
            R.id.widget_value,
            latest?.let { "${formatWeight(data, it.kg)} ${Units.label(data.profile.unit)}" } ?: "—",
        )
        views.setTextColor(R.id.widget_value, ink.primary)
        views.sp(R.id.widget_value, v)

        val showFooter = minH >= 112
        val reservedDp = 28 + 34 + if (showFooter) 30 else 0
        val wPx = ((minW - 28) * density).toInt().coerceAtLeast(1)
        val hPx = ((minH - reservedDp) * density).toInt()
            .coerceAtLeast((32 * density).toInt().coerceAtLeast(1))
        views.setImageViewBitmap(
            R.id.widget_art,
            sparklineBitmap(
                wPx = wPx,
                hPx = hPx,
                density = density,
                values = window.map { it.kg.toFloat() },
                lineColor = ink.accent2,
                fillColor = withAlpha(ink.accent2, 0x59),
                goal = data.profile.targetKg?.toFloat(),
                goalColor = withAlpha(ink.positive, 0xB3),
            ),
        )

        if (showFooter) {
            val first = window.firstOrNull()?.day ?: (data.today - 29)
            val last = window.lastOrNull()?.day ?: data.today
            views.setTextViewText(R.id.widget_meta, dateLabel(first, "d MMM").uppercase(Locale.US))
            views.setTextViewText(R.id.widget_sub, dateLabel(last, "d MMM").uppercase(Locale.US))
            views.setTextColor(R.id.widget_meta, ink.tertiary)
            views.setTextColor(R.id.widget_sub, ink.tertiary)
            views.sp(R.id.widget_meta, titleSp(v))
            views.sp(R.id.widget_sub, titleSp(v))
            views.show(R.id.widget_meta, true)
            views.show(R.id.widget_sub, true)
        } else {
            views.show(R.id.widget_meta, false)
            views.show(R.id.widget_sub, false)
        }
    }

    private fun populateRing(
        views: RemoteViews,
        data: WidgetData,
        dark: Boolean,
        ink: Ink,
        density: Float,
        minW: Int,
        minH: Int,
    ) {
        val v = valueSp(WidgetStyle.RING, minW, minH)
        val latest = data.latest
        val goal = hasGoal(data)

        val headline = when {
            goal && latest != null -> "${Math.round(data.progress * 100)}%"
            latest != null -> formatWeight(data, latest.kg)
            else -> null
        }
        val caption = when {
            goal && latest != null -> "TO ${formatWeight(data, data.profile.targetKg!!)} " +
                Units.label(data.profile.unit).uppercase(Locale.US)
            latest != null -> Units.label(data.profile.unit).uppercase(Locale.US)
            else -> "NO DATA"
        }

        val sidePx = ((min(minW, minH) - 30) * density).toInt()
            .coerceAtLeast((48 * density).toInt().coerceAtLeast(1))
        views.setImageViewBitmap(
            R.id.widget_art,
            ringBitmap(
                sidePx = sidePx,
                density = density,
                fraction = if (goal) data.progress.coerceIn(0f, 1f) else 0f,
                ink = ink,
                dark = dark,
                headline = headline,
                caption = if (minH >= 120) caption else null,
            ),
        )
        views.setContentDescription(
            R.id.widget_art,
            if (headline == null) caption else "$headline. $caption",
        )

        // Headline and caption are painted into the artwork; the text nodes stay as
        // the accessible description of the widget and are not drawn.
        views.setTextViewText(R.id.widget_value, headline ?: "—")
        views.setTextColor(R.id.widget_value, ink.primary)
        views.setTextViewText(R.id.widget_sub, caption)
        views.setTextColor(R.id.widget_sub, ink.secondary)
        views.sp(R.id.widget_sub, subSp(v))
        views.show(R.id.widget_value, false)
        views.show(R.id.widget_sub, false)
    }

    private fun populateStreak(
        views: RemoteViews,
        data: WidgetData,
        dark: Boolean,
        ink: Ink,
        density: Float,
        minW: Int,
        minH: Int,
    ) {
        val v = valueSp(WidgetStyle.STREAK, minW, minH)

        views.setTextViewText(R.id.widget_value, data.streakDays.toString())
        views.setTextColor(R.id.widget_value, ink.primary)
        views.sp(R.id.widget_value, v)

        views.setTextViewText(R.id.widget_unit, if (data.streakDays == 1) "day" else "days")
        views.setTextColor(R.id.widget_unit, ink.secondary)
        views.sp(R.id.widget_unit, subSp(v))

        val showTitle = minH >= 60
        views.setTextViewText(R.id.widget_title, "STREAK")
        views.setTextColor(R.id.widget_title, ink.tertiary)
        views.sp(R.id.widget_title, titleSp(v))
        views.show(R.id.widget_title, showTitle)

        if (minW >= 160 && minH >= 58) {
            val barsDp = (minW * 0.44f).coerceIn(96f, 168f)
            val wPx = (barsDp * density).toInt().coerceAtLeast(1)
            val hPx = (12 * density).toInt().coerceAtLeast(1)
            val logged = (6 downTo 0).map { (data.today - it) in data.logDaysLast7() }
            views.setImageViewBitmap(
                R.id.widget_art,
                barsBitmap(wPx, hPx, density, logged, ink.accent3, trackColor(dark)),
            )
            views.show(R.id.widget_art, true)
        } else {
            views.show(R.id.widget_art, false)
        }
    }

    private fun populateBmi(
        views: RemoteViews,
        data: WidgetData,
        dark: Boolean,
        ink: Ink,
        density: Float,
        minW: Int,
        minH: Int,
    ) {
        val v = valueSp(WidgetStyle.BMI, minW, minH)
        val value = data.bmiValue

        views.setTextViewText(R.id.widget_title, "BMI")
        views.setTextColor(R.id.widget_title, ink.tertiary)
        views.sp(R.id.widget_title, titleSp(v))

        views.setTextViewText(
            R.id.widget_value,
            if (value > 0) String.format(Locale.US, "%.1f", value) else "—",
        )
        views.setTextColor(R.id.widget_value, ink.primary)
        views.sp(R.id.widget_value, v)

        if (value > 0) {
            val category = Stats.bmiCategory(value)
            views.setTextViewText(R.id.widget_sub, category.label.uppercase(Locale.US))
            views.setTextColor(R.id.widget_sub, categoryColor(ink, category))
        } else {
            views.setTextViewText(R.id.widget_sub, "ADD HEIGHT & WEIGHT")
            views.setTextColor(R.id.widget_sub, ink.secondary)
        }
        views.sp(R.id.widget_sub, subSp(v))
        views.show(R.id.widget_sub, minH >= 84)

        if (value > 0 && minH >= 104) {
            val wPx = ((minW - 28) * density).toInt().coerceAtLeast(1)
            val hPx = (10 * density).toInt().coerceAtLeast(1)
            val fraction = ((value - 15.0) / 20.0).coerceIn(0.0, 1.0).toFloat()
            views.setImageViewBitmap(
                R.id.widget_art,
                scaleBitmap(wPx, hPx, density, fraction, ink),
            )
            views.show(R.id.widget_art, true)
        } else {
            views.show(R.id.widget_art, false)
        }
    }

    private fun populateCompact(
        views: RemoteViews,
        data: WidgetData,
        dark: Boolean,
        ink: Ink,
        density: Float,
        minW: Int,
        minH: Int,
    ) {
        val v = valueSp(WidgetStyle.COMPACT, minW, minH)
        val latest = data.latest
        val delta = data.weekDelta

        views.setTextViewText(R.id.widget_value, latest?.let { formatWeight(data, it.kg) } ?: "—")
        views.setTextColor(R.id.widget_value, ink.primary)
        views.sp(R.id.widget_value, v)

        if (latest != null && minW >= 110) {
            views.setTextViewText(R.id.widget_unit, Units.label(data.profile.unit))
            views.setTextColor(R.id.widget_unit, ink.secondary)
            views.sp(R.id.widget_unit, subSp(v))
            views.show(R.id.widget_unit, true)
        } else {
            views.show(R.id.widget_unit, false)
        }

        if (latest != null && delta != null && minW >= 140) {
            views.setTextViewText(R.id.widget_delta, signed(data, delta))
            views.setTextColor(R.id.widget_delta, deltaColor(data, ink, delta))
            views.sp(R.id.widget_delta, subSp(v))
            views.show(R.id.widget_delta, true)
        } else {
            views.show(R.id.widget_delta, false)
        }
    }

    private fun populateProgress(
        views: RemoteViews,
        data: WidgetData,
        dark: Boolean,
        ink: Ink,
        density: Float,
        minW: Int,
        minH: Int,
    ) {
        val v = valueSp(WidgetStyle.PROGRESS, minW, minH)
        val latest = data.latest
        val goal = hasGoal(data)

        views.setTextViewText(R.id.widget_title, "GOAL PROGRESS")
        views.setTextColor(R.id.widget_title, ink.tertiary)
        views.sp(R.id.widget_title, titleSp(v))

        views.setTextViewText(R.id.widget_value, latest?.let { formatWeight(data, it.kg) } ?: "—")
        views.setTextColor(R.id.widget_value, ink.primary)
        views.sp(R.id.widget_value, v)

        if (latest != null) {
            views.setTextViewText(R.id.widget_unit, Units.label(data.profile.unit))
            views.setTextColor(R.id.widget_unit, ink.secondary)
            views.sp(R.id.widget_unit, subSp(v))
            views.show(R.id.widget_unit, true)
        } else {
            views.show(R.id.widget_unit, false)
        }

        if (goal && latest != null) {
            views.setTextViewText(
                R.id.widget_sub,
                "${Math.round(data.progress * 100)}% TO " +
                    "${formatWeight(data, data.profile.targetKg!!)} " +
                    Units.label(data.profile.unit).uppercase(Locale.US),
            )
            views.setTextColor(R.id.widget_sub, ink.secondary)
        } else if (latest == null) {
            views.setTextViewText(R.id.widget_sub, "Log your first weight")
            views.setTextColor(R.id.widget_sub, ink.secondary)
        } else {
            views.setTextViewText(R.id.widget_sub, "Set a goal in settings")
            views.setTextColor(R.id.widget_sub, ink.secondary)
        }
        views.sp(R.id.widget_sub, subSp(v))
        views.show(R.id.widget_sub, minH >= 64)

        if (goal && latest != null && minH >= 92) {
            val wPx = ((minW - 28) * density).toInt().coerceAtLeast(1)
            val hPx = (12 * density).toInt().coerceAtLeast(1)
            views.setImageViewBitmap(
                R.id.widget_art,
                progressBitmap(wPx, hPx, data.progress.coerceIn(0f, 1f), ink),
            )
            views.show(R.id.widget_art, true)
        } else {
            views.show(R.id.widget_art, false)
        }
    }

    // ------------------------------------------------------------------ //
    // Transparent decorative artwork                                       //
    // ------------------------------------------------------------------ //

    private fun trackColor(dark: Boolean): Int =
        if (dark) Color.argb(0x2E, 0xFF, 0xFF, 0xFF) else Color.argb(0x2A, 0x00, 0x00, 0x00)

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha shl 24)

    private fun sparklineBitmap(
        wPx: Int,
        hPx: Int,
        density: Float,
        values: List<Float>,
        lineColor: Int,
        fillColor: Int,
        goal: Float?,
        goalColor: Int,
    ): Bitmap {
        val w = wPx.coerceAtLeast(1)
        val h = hPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

        if (values.size < 2) {
            val faint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(lineColor, 0x55)
                strokeWidth = 1.5f * density
                pathEffect = DashPathEffect(floatArrayOf(6f * density, 5f * density), 0f)
            }
            canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), faint)
            return bitmap
        }

        val minV = values.min()
        val maxV = values.max()
        val span = max(0.0001f, maxV - minV)
        val padV = span * 0.12f
        val top = minV - padV
        val bottom = maxV + padV
        val range = max(0.0001f, bottom - top)
        val n = values.size
        fun xOf(i: Int): Float = rect.left + rect.width() * i / (n - 1).toFloat()
        fun yOf(v: Float): Float = rect.top + (bottom - v) / range * rect.height()

        if (goal != null && goal >= top && goal <= bottom) {
            val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = goalColor
                strokeWidth = 1.5f * density
                pathEffect = DashPathEffect(floatArrayOf(6f * density, 5f * density), 0f)
            }
            canvas.drawLine(rect.left, yOf(goal), rect.right, yOf(goal), goalPaint)
        }

        val line = Path()
        line.moveTo(xOf(0), yOf(values[0]))
        for (i in 1 until n) {
            val prevX = xOf(i - 1)
            val prevY = yOf(values[i - 1])
            val currX = xOf(i)
            val currY = yOf(values[i])
            line.quadTo(prevX, prevY, (prevX + currX) / 2f, (prevY + currY) / 2f)
            if (i == n - 1) line.lineTo(currX, currY)
        }

        val fill = Path(line)
        fill.lineTo(xOf(n - 1), rect.bottom)
        fill.lineTo(xOf(0), rect.bottom)
        fill.close()
        canvas.drawPath(
            fill,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, rect.top, 0f, rect.bottom,
                    fillColor,
                    withAlpha(fillColor, 0x00),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawPath(
            line,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = lineColor
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * density
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            },
        )

        val endX = xOf(n - 1)
        val endY = yOf(values[n - 1])
        canvas.drawCircle(
            endX, endY, 10f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(
                    endX, endY, 10f * density,
                    intArrayOf(lineColor, withAlpha(lineColor, 0x00)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawCircle(
            endX, endY, 2.6f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE },
        )
        canvas.drawCircle(
            endX, endY, 1.8f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lineColor },
        )
        return bitmap
    }

    internal fun ringBitmap(
        sidePx: Int,
        density: Float,
        fraction: Float,
        ink: Ink,
        dark: Boolean,
        headline: String? = null,
        caption: String? = null,
    ): Bitmap {
        val side = sidePx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = side / 2f
        val stroke = max(6f * density, side * 0.10f)
        val radius = center - stroke / 2f - 2f * density

        canvas.drawCircle(
            center, center, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                color = trackColor(dark)
            },
        )

        val sweep = 360f * fraction
        if (sweep > 0.5f) {
            canvas.drawArc(
                RectF(center - radius, center - radius, center + radius, center + radius),
                -90f,
                sweep,
                false,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                    strokeCap = Paint.Cap.ROUND
                    shader = SweepGradient(
                        center, center,
                        intArrayOf(ink.accent, ink.accent2, ink.accent3),
                        null,
                    )
                },
            )
        }

        if (headline != null) {
            val size = side * 0.26f
            val paint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                textSize = size
                color = ink.primary
                textAlign = Paint.Align.CENTER
                letterSpacing = -0.02f
            }
            val base = center -
                if (caption != null) size * 0.06f else (paint.ascent() + paint.descent()) / 2f
            canvas.drawText(headline, center, base, paint)
            if (caption != null) {
                val sub = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    textSize = (size * 0.34f).coerceAtLeast(8f)
                    color = ink.secondary
                    textAlign = Paint.Align.CENTER
                    letterSpacing = 0.12f
                }
                canvas.drawText(caption, center, base + size * 0.46f, sub)
            }
        }
        return bitmap
    }

    private fun barsBitmap(
        wPx: Int,
        hPx: Int,
        density: Float,
        logged: List<Boolean>,
        onColor: Int,
        offColor: Int,
    ): Bitmap {
        val w = wPx.coerceAtLeast(1)
        val h = hPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val n = max(1, logged.size)
        val gap = 4f * density
        val barW = (w - gap * (n - 1)) / n
        if (barW <= 0f) return bitmap
        val barH = h.toFloat()
        logged.forEachIndexed { i, on ->
            val left = i * (barW + gap)
            canvas.drawRoundRect(
                RectF(left, 0f, left + barW, barH),
                barH / 2f,
                barH / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (on) onColor else offColor },
            )
        }
        return bitmap
    }

    internal fun progressBitmap(
        wPx: Int,
        hPx: Int,
        fraction: Float,
        ink: Ink,
    ): Bitmap {
        val w = wPx.coerceAtLeast(1)
        val h = hPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

        canvas.drawRoundRect(
            rect, h / 2f, h / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(ink.accent2, 0x2E) },
        )
        val fillW = (w * fraction).coerceIn(0f, w.toFloat())
        if (fillW > 1f) {
            val fill = RectF(0f, 0f, fillW, h.toFloat())
            canvas.drawRoundRect(
                fill, h / 2f, h / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, fillW, 0f,
                        ink.accent, ink.accent2,
                        Shader.TileMode.CLAMP,
                    )
                },
            )
            canvas.drawCircle(
                fillW.coerceAtMost(w - h / 2f).coerceAtLeast(h / 2f), h / 2f, h * 0.42f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE },
            )
        }
        return bitmap
    }

    private fun scaleBitmap(
        wPx: Int,
        hPx: Int,
        density: Float,
        fraction: Float,
        ink: Ink,
    ): Bitmap {
        val w = wPx.coerceAtLeast(1)
        val h = hPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

        canvas.drawRoundRect(
            rect, h / 2f, h / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), 0f,
                    intArrayOf(
                        withAlpha(ink.accent3, 0xD9),
                        withAlpha(ink.caution, 0xD9),
                        withAlpha(ink.negative, 0xD9),
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
            },
        )
        val mx = (w * fraction).coerceIn(0f, w.toFloat())
        canvas.drawCircle(
            mx, h / 2f, 5f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(4f * density, 0f, 0f, ink.accent2)
            },
        )
        return bitmap
    }
}
