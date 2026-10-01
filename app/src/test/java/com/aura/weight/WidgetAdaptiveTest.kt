package com.aura.weight

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightEntry
import com.aura.weight.widget.WidgetData
import com.aura.weight.widget.WidgetRenderer
import com.aura.weight.widget.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class WidgetAdaptiveTest {

    private fun data(dark: Boolean): WidgetData {
        val today = LocalDate.now().toEpochDay()
        val entries = (40L downTo 0L).map { offset ->
            val i = offset.toInt()
            WeightEntry(today - offset, 74.4 - i * 0.05 + sin(i * 1.7f) * 0.25, recordedAt = 0L)
        }
        return WidgetData(
            entries = entries,
            profile = UserProfile(
                onboarded = true,
                unit = UnitSystem.KG,
                heightCm = 179.0,
                startKg = 78.5,
                targetKg = 68.0,
                darkTheme = dark,
            ),
            today = today,
        )
    }

    private fun applied(
        style: WidgetStyle,
        minW: Int,
        minH: Int,
        dark: Boolean = true,
        data: WidgetData = data(dark),
    ): View {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val views: RemoteViews = WidgetRenderer.build(context, style, data, dark, minW, minH)
        val view = views.apply(context, FrameLayout(context))
        view.measure(
            View.MeasureSpec.makeMeasureSpec((minW * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((minH * density).toInt(), View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        return view
    }

    private fun textOf(view: View, id: Int): CharSequence =
        (view.findViewById<TextView>(id) ?: throw AssertionError("missing TextView $id")).text

    @Test
    fun everyStyleShowsCrispNativeTextAtDefaultSize() {
        val cases = mapOf(
            WidgetStyle.TODAY to Pair(250, 110),
            WidgetStyle.TREND to Pair(250, 150),
            WidgetStyle.RING to Pair(140, 140),
            WidgetStyle.STREAK to Pair(250, 70),
            WidgetStyle.BMI to Pair(140, 140),
            WidgetStyle.COMPACT to Pair(140, 50),
            WidgetStyle.PROGRESS to Pair(250, 110),
        )
        cases.forEach { (style, size) ->
            val view = applied(style, size.first, size.second)
            assertTrue(
                "${style.name} value must be native non-empty text",
                textOf(view, R.id.widget_value).isNotEmpty(),
            )
        }
    }

    @Test
    fun shellBackgroundStaysInOneUiBlurAlphaRange() {
        listOf(true, false).forEach { dark ->
            val view = applied(WidgetStyle.TODAY, 250, 110, dark)
            val root = view.findViewById<View>(android.R.id.background)
            assertNotNull("missing @android:id/background for dark=$dark", root)
            val alpha = (root.background as ColorDrawable).alpha
            assertTrue("background alpha $alpha outside One UI blur range 1..254", alpha in 1..254)
        }
    }

    @Test
    fun todayHidesDeltaAndSubWhenTooNarrowOrShort() {
        val wide = applied(WidgetStyle.TODAY, 250, 110)
        assertEquals(View.VISIBLE, wide.findViewById<View>(R.id.widget_delta).visibility)
        assertEquals(View.VISIBLE, wide.findViewById<View>(R.id.widget_sub).visibility)

        val narrow = applied(WidgetStyle.TODAY, 150, 110)
        assertEquals(View.GONE, narrow.findViewById<View>(R.id.widget_delta).visibility)

        val short = applied(WidgetStyle.TODAY, 250, 60)
        assertEquals(View.GONE, short.findViewById<View>(R.id.widget_sub).visibility)
        assertTrue(textOf(short, R.id.widget_value).isNotEmpty())
    }

    @Test
    fun compactHidesDeltaOnNarrowWidths() {
        val roomy = applied(WidgetStyle.COMPACT, 200, 60)
        assertEquals(View.VISIBLE, roomy.findViewById<View>(R.id.widget_delta).visibility)

        val narrow = applied(WidgetStyle.COMPACT, 110, 50)
        assertEquals(View.GONE, narrow.findViewById<View>(R.id.widget_delta).visibility)
        assertTrue(textOf(narrow, R.id.widget_value).isNotEmpty())
    }

    @Test
    fun trendHidesFooterDatesWhenShort() {
        val tall = applied(WidgetStyle.TREND, 250, 150)
        assertEquals(View.VISIBLE, tall.findViewById<View>(R.id.widget_meta).visibility)

        val short = applied(WidgetStyle.TREND, 250, 90)
        assertEquals(View.GONE, short.findViewById<View>(R.id.widget_meta).visibility)
        assertEquals(View.GONE, short.findViewById<View>(R.id.widget_sub).visibility)
    }

    @Test
    fun chartArtworkIsPresentForVisualStyles() {
        val trend = applied(WidgetStyle.TREND, 250, 150)
        assertNotNull(trend.findViewById<ImageView>(R.id.widget_art))

        val ring = applied(WidgetStyle.RING, 140, 140)
        assertNotNull(ring.findViewById<ImageView>(R.id.widget_art))

        val streak = applied(WidgetStyle.STREAK, 250, 70)
        assertEquals(View.VISIBLE, streak.findViewById<View>(R.id.widget_art).visibility)
    }

    @Test
    fun valueTextGrowsWithWidgetSize() {
        val small = applied(WidgetStyle.TODAY, 150, 70)
        val large = applied(WidgetStyle.TODAY, 320, 180)
        val smallPx = small.findViewById<TextView>(R.id.widget_value).textSize
        val largePx = large.findViewById<TextView>(R.id.widget_value).textSize
        assertTrue("expected larger text on larger widget ($smallPx vs $largePx)", largePx > smallPx)
    }

    @Test
    fun everyStyleAdaptsWhenResized() {
        // Smallest reasonable footprint per style, then a large one. Every style must
        // render without crashing, keep native text, and never shrink its value text
        // when given more room.
        val footprints = mapOf(
            WidgetStyle.TODAY to Pair(Pair(150, 70), Pair(320, 180)),
            WidgetStyle.TREND to Pair(Pair(180, 100), Pair(360, 220)),
            WidgetStyle.RING to Pair(Pair(110, 110), Pair(200, 200)),
            WidgetStyle.STREAK to Pair(Pair(170, 60), Pair(320, 110)),
            WidgetStyle.BMI to Pair(Pair(110, 110), Pair(200, 200)),
            WidgetStyle.COMPACT to Pair(Pair(110, 50), Pair(220, 90)),
            WidgetStyle.PROGRESS to Pair(Pair(170, 80), Pair(320, 180)),
        )
        footprints.forEach { (style, sizes) ->
            val (smallW, smallH) = sizes.first
            val (largeW, largeH) = sizes.second
            val small = applied(style, smallW, smallH)
            val large = applied(style, largeW, largeH)
            assertTrue(
                "${style.name} value must stay non-empty at ${smallW}x$smallH",
                textOf(small, R.id.widget_value).isNotEmpty(),
            )
            assertTrue(
                "${style.name} value must stay non-empty at ${largeW}x$largeH",
                textOf(large, R.id.widget_value).isNotEmpty(),
            )
            val smallPx = small.findViewById<TextView>(R.id.widget_value).textSize
            val largePx = large.findViewById<TextView>(R.id.widget_value).textSize
            assertTrue(
                "${style.name} value text must not shrink on a larger widget ($smallPx vs $largePx)",
                largePx >= smallPx,
            )
            val root = large.findViewById<View>(android.R.id.background)
            assertNotNull("${style.name} missing shell background", root)
        }
    }

    @Test
    fun progressWidgetShowsWeightAndBarAtDefaultSize() {
        val view = applied(WidgetStyle.PROGRESS, 250, 110)
        assertTrue(textOf(view, R.id.widget_value).isNotEmpty())
        assertTrue(textOf(view, R.id.widget_sub).contains("%"))
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_art).visibility)

        val short = applied(WidgetStyle.PROGRESS, 250, 70)
        assertEquals(View.GONE, short.findViewById<View>(R.id.widget_art).visibility)
        assertTrue(textOf(short, R.id.widget_value).isNotEmpty())
    }

    @Test
    fun shellAlphaClampsToOneUiBlurRange() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val base = data(true)
        listOf(0, 300).forEach { alpha ->
            val profile = base.profile.copy(widgetAlpha = alpha)
            val views = WidgetRenderer.build(
                context, WidgetStyle.TODAY, base.copy(profile = profile), true, 250, 110,
            )
            val view = views.apply(context, FrameLayout(context))
            val root = view.findViewById<View>(android.R.id.background)
            val actual = (root.background as ColorDrawable).alpha
            assertTrue("alpha $alpha must clamp into 1..254, was $actual", actual in 1..254)
        }
    }

    @Test
    fun reapplyingUpdatesNeverStacksContent() {
        // One UI re-applies RemoteViews on update. If the widget nested content with
        // addView, every save would append another copy and leave stale text/artwork
        // stacked on screen. A flat layout makes that impossible.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val d = data(true)
        val parent = FrameLayout(context)

        fun views(minW: Int, minH: Int): RemoteViews =
            WidgetRenderer.build(context, WidgetStyle.PROGRESS, d, true, minW, minH)

        fun layout(view: View, minW: Int, minH: Int) {
            val density = context.resources.displayMetrics.density
            view.measure(
                View.MeasureSpec.makeMeasureSpec((minW * density).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((minH * density).toInt(), View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }

        @Suppress("UNUSED_VARIABLE")
        fun build(minW: Int, minH: Int): Pair<RemoteViews, View> {
            val views = WidgetRenderer.build(context, WidgetStyle.PROGRESS, d, true, minW, minH)
            val view = views.apply(context, parent)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(
                    (minW * context.resources.displayMetrics.density).toInt(),
                    View.MeasureSpec.EXACTLY,
                ),
                View.MeasureSpec.makeMeasureSpec(
                    (minH * context.resources.displayMetrics.density).toInt(),
                    View.MeasureSpec.EXACTLY,
                ),
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            return views to view
        }

        val tree = views(250, 110).apply(context, parent)
        assertNotNull("apply produced no view", tree)
        layout(tree, 250, 110)

        // Further updates go through reapply(), exactly like a host refreshing a placed widget.
        views(250, 110).reapply(context, parent)
        layout(tree, 250, 110)
        views(250, 110).reapply(context, parent)
        layout(tree, 250, 110)
        val third = tree

        assertEquals(
            "content must stay flat after repeated updates",
            1,
            countMatches(third, R.id.widget_value),
        )
        assertEquals(
            "artwork must not duplicate after repeated updates",
            1,
            countMatches(third, R.id.widget_art),
        )
    }

    private fun countMatches(root: View, id: Int): Int {
        var count = 0
        fun walk(v: View) {
            if (v.id == id) count++
            if (v is android.view.ViewGroup) {
                for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
        }
        walk(root)
        return count
    }

    @Test
    fun progressBarFillAgreesWithKnobAtEveryProgress() {
        // The screenshot bug: a full-width fill with the knob pinned at the far left,
        // i.e. two different progress values composited at once.
        listOf(0.35f, 0.78f, 1.0f).forEach { fraction ->
            val bmp = WidgetArtForTest.progressBitmap(300, 24, fraction)
            val y = bmp.height / 2
            var fillFirst = -1
            var fillLast = -1
            var knobFirst = -1
            var knobLast = -1
            for (x in 0 until bmp.width) {
                val c = bmp.getPixel(x, y)
                val a = android.graphics.Color.alpha(c)
                val r = android.graphics.Color.red(c)
                val g = android.graphics.Color.green(c)
                val b = android.graphics.Color.blue(c)
                val isFill = a > 200 && b > 150 && b > r + 40
                val isKnob = a > 200 && r > 235 && g > 235 && b > 235
                if (isFill) {
                    if (fillFirst < 0) fillFirst = x
                    fillLast = x
                }
                if (isKnob) {
                    if (knobFirst < 0) knobFirst = x
                    knobLast = x
                }
            }
            assertTrue("progress=$fraction produced no fill", fillFirst >= 0)
            assertTrue("progress=$fraction produced no knob", knobFirst >= 0)
            val knobCenter = (knobFirst + knobLast) / 2
            // knob is kept inside the track at full progress
            val expected = minOf((bmp.width * fraction).toInt(), bmp.width - bmp.height / 2)
            assertTrue(
                "progress=$fraction: knob centred at $knobCenter, expected ~$expected",
                knobCenter - expected in -3..3,
            )
            assertTrue(
                "progress=$fraction: fill must reach the knob (fill=$fillLast knob=$knobFirst)",
                fillLast >= knobFirst - 2,
            )
        }
    }

    @Test
    fun progressBarIsEmptyAtZeroProgress() {
        val bmp = WidgetArtForTest.progressBitmap(300, 24, 0.0f)
        var bright = 0
        for (x in 0 until bmp.width) {
            val c = bmp.getPixel(x, bmp.height / 2)
            if (android.graphics.Color.alpha(c) > 200 &&
                android.graphics.Color.blue(c) > 150 &&
                android.graphics.Color.blue(c) > android.graphics.Color.red(c) + 40
            ) bright++
        }
        assertEquals("0% progress must not paint a fill or knob", 0, bright)
    }

    @Test
    fun unitLabelSitsNextToTheValueWithoutOverflow() {
        listOf(
            WidgetStyle.TODAY to Pair(150, 70),
            WidgetStyle.TODAY to Pair(320, 180),
            WidgetStyle.PROGRESS to Pair(170, 80),
            WidgetStyle.PROGRESS to Pair(320, 180),
            WidgetStyle.COMPACT to Pair(140, 50),
            WidgetStyle.BMI to Pair(110, 110),
        ).forEach { (style, size) ->
            val view = applied(style, size.first, size.second)
            val value = view.findViewById<TextView>(R.id.widget_value)
            val unit = view.findViewById<TextView>(R.id.widget_unit) ?: return@forEach
            if (unit.visibility != View.VISIBLE) return@forEach
            val valueRight = value.left + value.width
            val gap = unit.left - valueRight
            assertTrue(
                "${style.name} ${size.first}x${size.second}: unit must hug the value (gap=$gap)",
                gap in 0..8,
            )
            assertTrue(
                "${style.name} ${size.first}x${size.second}: unit must stay inside the widget (right=${unit.right}, width=${view.width})",
                unit.right <= view.width,
            )
        }
    }
}
