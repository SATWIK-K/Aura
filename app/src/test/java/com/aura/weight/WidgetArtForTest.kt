package com.aura.weight

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aura.weight.widget.WidgetRenderer

/** Thin wrapper so tests can exercise the artwork drawing directly. */
object WidgetArtForTest {
    fun progressBitmap(widthPx: Int, heightPx: Int, fraction: Float) =
        WidgetRenderer.progressBitmap(
            widthPx, heightPx, fraction, WidgetRenderer.inkForTest(true),
        )
}
