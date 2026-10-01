package com.aura.weight.core

import android.graphics.Bitmap
import kotlin.math.max

/**
 * CPU blur used for backdrop frosting.
 *
 * Three box passes (each horizontal + vertical) approximate a Gaussian well enough
 * for glass, run on any API level, and work identically in Robolectric renders —
 * which is what makes headless visual QA possible.
 */
object Blur {

    /** Box-blurs an ARGB int array in place-ish, returning a new array. */
    fun blurArgb(src: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3): IntArray {
        if (radius <= 0 || w <= 0 || h <= 0) return src.copyOf()
        var a = src
        var b = IntArray(w * h)
        repeat(passes) {
            boxH(a, b, w, h, radius)
            boxV(b, a, w, h, radius)
        }
        return a
    }

    private fun boxH(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val off = y * w
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            for (i in -r..r) {
                val p = src[off + i.coerceIn(0, w - 1)]
                sa += p ushr 24
                sr += (p shr 16) and 0xFF
                sg += (p shr 8) and 0xFF
                sb += p and 0xFF
            }
            for (x in 0 until w) {
                dst[off + x] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = src[off + (x + r + 1).coerceIn(0, w - 1)]
                val rem = src[off + (x - r).coerceIn(0, w - 1)]
                sa += (add ushr 24) - (rem ushr 24)
                sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                sb += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }

    private fun boxV(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            for (i in -r..r) {
                val p = src[i.coerceIn(0, h - 1) * w + x]
                sa += p ushr 24
                sr += (p shr 16) and 0xFF
                sg += (p shr 8) and 0xFF
                sb += p and 0xFF
            }
            for (y in 0 until h) {
                dst[y * w + x] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = src[(y + r + 1).coerceIn(0, h - 1) * w + x]
                val rem = src[(y - r).coerceIn(0, h - 1) * w + x]
                sa += (add ushr 24) - (rem ushr 24)
                sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                sb += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }

    /** Downscale (box filter via createScaledBitmap) then 3-pass box blur. */
    fun blurBitmap(src: Bitmap, radius: Int): Bitmap {
        val w = max(1, src.width)
        val h = max(1, src.height)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = blurArgb(pixels, w, h, radius)
        val dst = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        dst.setPixels(out, 0, w, 0, 0, w, h)
        return dst
    }
}
