package com.aura.weight

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.aura.weight.core.BackdropSpec
import com.aura.weight.core.Backdrops
import com.aura.weight.core.AuraBackdropHost
import com.aura.weight.ui.theme.AuraTheme
import com.aura.weight.ui.theme.DarkGlass
import com.aura.weight.ui.theme.LightGlass
import org.junit.Rule
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream

/**
 * Renders real composables to PNGs under build/visual so the design can be
 * reviewed headlessly. Robolectric runs with NATIVE graphics, so our CPU blur,
 * shaders, fonts and paths all draw exactly as they would on a device.
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseVisualTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    val outDir: File = File("build/visual").apply { mkdirs() }

    fun capture(
        name: String,
        dark: Boolean = true,
        spec: BackdropSpec = Backdrops.aurora,
        beforeShot: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        rule.setContent {
            AuraTheme(dark) {
                AuraBackdropHost(
                    spec = if (dark) spec else Backdrops.lightVariant(spec),
                    glass = if (dark) DarkGlass else LightGlass,
                    animate = false,
                ) {
                    Box(Modifier.fillMaxSize()) { content() }
                }
            }
        }
        beforeShot()
        shot(name)
    }

    /** Capture without the backdrop host — isolates content from frost/backdrop. */
    fun captureRaw(
        name: String,
        dark: Boolean = true,
        background: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF101018),
        content: @Composable () -> Unit,
    ) {
        rule.setContent {
            AuraTheme(dark) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(background),
                ) { content() }
            }
        }
        shot(name)
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        rule.waitForIdle()

        val view = rule.activity.window.decorView
        val w = view.width.coerceAtLeast(1)
        val h = view.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        FileOutputStream(File(outDir, name)).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
