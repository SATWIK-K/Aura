package com.aura.weight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aura.weight.data.UnitSystem
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightEntry
import com.aura.weight.widget.WidgetData
import com.aura.weight.widget.WidgetRenderer
import com.aura.weight.widget.WidgetStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WidgetRenderTest {

    private val outDir: File = File("build/visual").apply { mkdirs() }

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

    @Test
    fun widgetsDark() = sheet("widgets-dark.png", dark = true)

    @Test
    fun widgetsLight() = sheet("widgets-light.png", dark = false)

    fun renderLive(
        context: Context,
        style: WidgetStyle,
        minWidthDp: Int,
        minHeightDp: Int,
        d: WidgetData,
        dark: Boolean,
        outputScale: Float = 1f,
    ): android.graphics.Bitmap {
        val density = context.resources.displayMetrics.density
        val views = WidgetRenderer.build(context, style, d, dark, minWidthDp, minHeightDp)
        val wPx = (minWidthDp * density * outputScale).toInt().coerceAtLeast(1)
        val hPx = (minHeightDp * density * outputScale).toInt().coerceAtLeast(1)
        val parent = android.widget.FrameLayout(context)
        val view = views.apply(context, parent)
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(wPx, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(hPx, android.view.View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, wPx, hPx)
        val bmp = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        return bmp
    }

    private fun sheet(name: String, dark: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val d = data(dark)

        val cases = listOf(
            WidgetStyle.TODAY to Pair(250, 110),
            WidgetStyle.TREND to Pair(250, 150),
            WidgetStyle.RING to Pair(140, 140),
            WidgetStyle.STREAK to Pair(250, 70),
            WidgetStyle.BMI to Pair(140, 140),
            WidgetStyle.COMPACT to Pair(140, 50),
            WidgetStyle.PROGRESS to Pair(250, 110),
        )

        val pad = (24 * density).toInt()
        val gap = (20 * density).toInt()
        val labelH = (22 * density).toInt()

        val rendered = cases.map { (style, size) ->
            style to renderLive(context, style, size.first, size.second, d, dark)
        }
        val rows = rendered.chunked(4)
        val rowWidths = rows.map { row -> row.sumOf { it.second.width } + gap * (row.size - 1) }
        val rowHeights = rows.map { row -> row.maxOf { it.second.height } + labelH }
        val sheetW = (rowWidths.maxOrNull() ?: 0) + pad * 2

        val sheetH = pad * 2 + rowHeights.sum() + gap * (rows.size - 1)
        val sheet = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)

        val wallTop = if (dark) 0xFF121A2EL.toInt() else 0xFFBFD3E8L.toInt()
        val wallBottom = if (dark) 0xFF2A1B3AL.toInt() else 0xFFE7DCF2L.toInt()
        canvas.drawPaint(
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, sheetW.toFloat(), sheetH.toFloat(),
                    wallTop, wallBottom,
                    Shader.TileMode.CLAMP,
                )
            },
        )
        // wallpaper blobs so the glass has something to frost over
        val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        blobPaint.shader = android.graphics.RadialGradient(
            sheetW * 0.75f, sheetH * 0.25f, sheetW * 0.45f,
            if (dark) 0x666A7BFF else 0x88FFFFFFL.toInt(),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(sheetW * 0.75f, sheetH * 0.25f, sheetW * 0.45f, blobPaint)
        blobPaint.shader = android.graphics.RadialGradient(
            sheetW * 0.2f, sheetH * 0.8f, sheetW * 0.4f,
            if (dark) 0x554CE0B3 else 0x88B7E8D8L.toInt(),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(sheetW * 0.2f, sheetH * 0.8f, sheetW * 0.4f, blobPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dark) Color.WHITE else Color.BLACK
            textSize = 11f * density
            isFakeBoldText = true
        }

        var y = pad
        for ((row, rowH) in rows.zip(rowHeights)) {
            var x = pad
            for ((style, bmp) in row) {
                canvas.drawText(style.label, x.toFloat(), y + 14f * density, labelPaint)
                canvas.drawBitmap(bmp, x.toFloat(), (y + labelH).toFloat(), null)
                x += bmp.width + gap
            }
            y += rowH + gap
        }

        FileOutputStream(File(outDir, name)).use {
            sheet.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun resizeMatrix() {
        // Same widgets at several user-resizable sizes, to prove they reflow freely.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val d = data(dark = true)
        val widths = listOf(110, 180, 250, 340)
        val heights = listOf(50, 80, 120, 180)

        val cells = mutableListOf<Pair<WidgetStyle, android.graphics.Bitmap>>()
        WidgetStyle.entries.forEach { style ->
            widths.forEachIndexed { i, w ->
                cells += style to renderLive(context, style, w, heights[i], d, true)
            }
        }

        val gap = 16
        val pad = 16
        val rowW = cells.sumOf { it.second.width } + gap * (cells.size - 1) + pad * 2
        val rowH = cells.maxOf { it.second.height } + pad * 2
        val sheet = Bitmap.createBitmap(rowW, rowH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawPaint(Paint().apply { color = Color.parseColor("#FF12141B") })
        var x = pad
        for ((_, bmp) in cells) {
            canvas.drawBitmap(bmp, x.toFloat(), pad.toFloat(), null)
            x += bmp.width + gap
        }
        FileOutputStream(File(outDir, "widgets-resize-matrix.png")).use {
            sheet.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun previews() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val previewDensity = 2f
        val d = data(dark = true)
        File("src/main/res/drawable-nodpi").mkdirs()
        val sizes = mapOf(
            WidgetStyle.TODAY to Pair(250, 110),
            WidgetStyle.TREND to Pair(250, 150),
            WidgetStyle.RING to Pair(140, 140),
            WidgetStyle.STREAK to Pair(250, 70),
            WidgetStyle.BMI to Pair(140, 140),
            WidgetStyle.COMPACT to Pair(140, 50),
            WidgetStyle.PROGRESS to Pair(250, 110),
        )
        sizes.forEach { (style, size) ->
            val bmp = renderLive(context, style, size.first, size.second, d, true, previewDensity)
            val file = File("src/main/res/drawable-nodpi/widget_preview_${style.name.lowercase()}.png")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
