package com.aura.weight.core

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.random.Random

/**
 * Procedural film grain — a tiny tiled noise texture drawn over glass so the
 * frost reads as a material rather than flat alpha.
 */
object Grain {
    private const val TILE = 128

    private val bitmap: Bitmap by lazy {
        val rnd = Random(1337)
        val pixels = IntArray(TILE * TILE)
        for (i in pixels.indices) {
            val light = rnd.nextBoolean()
            val a = rnd.nextInt(48)
            val v = if (light) 0xFF else 0x00
            pixels[i] = (a shl 24) or (v shl 16) or (v shl 8) or v
        }
        Bitmap.createBitmap(TILE, TILE, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, TILE, 0, 0, TILE, TILE)
        }
    }

    private val brush: ShaderBrush by lazy {
        ShaderBrush(
            BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT),
        )
    }

    fun brush(): ShaderBrush = brush
}
