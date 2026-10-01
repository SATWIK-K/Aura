package com.aura.weight.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aura.weight.ui.theme.GlassTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/* ------------------------------------------------------------------ *
 *  Backdrop specs — soft drifting color fields ("auras")
 * ------------------------------------------------------------------ */

data class Blob(
    val color: Long,
    val fx: Float,
    val fy: Float,
    val r: Float,
    val dx: Float,
    val dy: Float,
    val speed: Float,
    val seed: Float,
)

data class BackdropSpec(
    val id: String,
    val name: String,
    val base: Long,
    val blobs: List<Blob>,
)

object Backdrops {
    val ember = BackdropSpec(
        id = "ember",
        name = "Ember",
        base = 0xFF1A0E0A,
        blobs = listOf(
            Blob(0xFFFF8A3D, 0.22f, 0.16f, 0.396f, 0.06f, 0.05f, 0.9f, 0.0f),
            Blob(0xFFFF5470, 0.85f, 0.32f, 0.36f, 0.07f, 0.06f, 0.7f, 1.7f),
            Blob(0xFF7A5CFF, 0.15f, 0.78f, 0.432f, 0.05f, 0.07f, 0.8f, 3.1f),
            Blob(0xFFFFB454, 0.70f, 0.88f, 0.324f, 0.06f, 0.05f, 1.1f, 4.4f),
        ),
    )

    val aurora = BackdropSpec(
        id = "aurora",
        name = "Aurora",
        base = 0xFF080B18,
        blobs = listOf(
            Blob(0xFF6A7BFF, 0.20f, 0.20f, 0.396f, 0.07f, 0.06f, 0.8f, 0.6f),
            Blob(0xFF3ED0FF, 0.82f, 0.30f, 0.36f, 0.06f, 0.07f, 1.0f, 2.0f),
            Blob(0xFF4CE0B3, 0.30f, 0.82f, 0.374f, 0.07f, 0.05f, 0.7f, 3.5f),
            Blob(0xFF9F6BFF, 0.88f, 0.85f, 0.346f, 0.05f, 0.06f, 0.9f, 5.0f),
        ),
    )

    val ocean = BackdropSpec(
        id = "ocean",
        name = "Ocean",
        base = 0xFF04101A,
        blobs = listOf(
            Blob(0xFF2D7DFF, 0.25f, 0.25f, 0.396f, 0.06f, 0.06f, 0.8f, 1.1f),
            Blob(0xFF19C3C8, 0.80f, 0.20f, 0.346f, 0.06f, 0.06f, 0.9f, 2.7f),
            Blob(0xFF6A5CFF, 0.20f, 0.80f, 0.396f, 0.06f, 0.06f, 0.7f, 4.2f),
            Blob(0xFF2FE0A6, 0.85f, 0.82f, 0.302f, 0.05f, 0.05f, 1.0f, 5.6f),
        ),
    )

    val bloom = BackdropSpec(
        id = "bloom",
        name = "Bloom",
        base = 0xFF150A16,
        blobs = listOf(
            Blob(0xFFFF6FB5, 0.24f, 0.20f, 0.374f, 0.06f, 0.06f, 0.9f, 0.3f),
            Blob(0xFFFF9A5C, 0.82f, 0.35f, 0.346f, 0.07f, 0.05f, 0.8f, 1.9f),
            Blob(0xFFA06BFF, 0.30f, 0.80f, 0.396f, 0.06f, 0.06f, 0.7f, 3.3f),
            Blob(0xFF3ED0FF, 0.85f, 0.88f, 0.288f, 0.05f, 0.06f, 1.1f, 4.9f),
        ),
    )

    val mono = BackdropSpec(
        id = "mono",
        name = "Mono",
        base = 0xFF0A0B10,
        blobs = listOf(
            Blob(0xFF39415A, 0.25f, 0.22f, 0.396f, 0.06f, 0.05f, 0.7f, 0.9f),
            Blob(0xFF4A5568, 0.82f, 0.35f, 0.36f, 0.06f, 0.06f, 0.8f, 2.2f),
            Blob(0xFF5B6480, 0.25f, 0.80f, 0.374f, 0.05f, 0.06f, 0.6f, 3.8f),
            Blob(0xFF2E3448, 0.85f, 0.85f, 0.324f, 0.05f, 0.05f, 0.9f, 5.2f),
        ),
    )

    val all = listOf(aurora, ember, ocean, bloom, mono)

    fun of(id: String): BackdropSpec = all.firstOrNull { it.id == id } ?: aurora

    /**
     * Bright counterpart of a dark spec: the same mesh lifted onto a cool white
     * field with pastel blobs, so light glass and dark type stay readable.
     */
    fun lightVariant(spec: BackdropSpec): BackdropSpec = spec.copy(
        id = spec.id + "-light",
        name = spec.name + " Light",
        base = mix(spec.base, 0xFFF1F5FC, 0.90f),
        blobs = spec.blobs.map { b -> b.copy(color = mix(b.color, 0xFFFFFFFF, 0.45f)) },
    )

    private fun mix(from: Long, to: Long, t: Float): Long {
        val r = ((from shr 16 and 0xFF) + (((to shr 16 and 0xFF) - (from shr 16 and 0xFF)) * t)).toInt()
        val g = ((from shr 8 and 0xFF) + (((to shr 8 and 0xFF) - (from shr 8 and 0xFF)) * t)).toInt()
        val b = ((from and 0xFF) + (((to and 0xFF) - (from and 0xFF)) * t)).toInt()
        return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
}

/* ------------------------------------------------------------------ *
 *  Rendering
 * ------------------------------------------------------------------ */

/** Renders the drifting mesh gradient into a bitmap. */
fun renderBackdrop(spec: BackdropSpec, w: Int, h: Int, phase: Float): Bitmap {
    val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(spec.base.toInt())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val maxDim = max(w, h).toFloat()
    for (b in spec.blobs) {
        val cx = (b.fx + b.dx * sin(phase * b.speed + b.seed)) * w
        val cy = (b.fy + b.dy * cos(phase * b.speed * 0.83f + b.seed)) * h
        val radius = (b.r * maxDim).coerceAtLeast(1f)
        paint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(b.color.toInt() and 0x00FFFFFF or 0xC0000000.toInt(), b.color.toInt() and 0x00FFFFFF),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }
    paint.shader = null
    // Unify the field: scrim keeps type and cards readable in either polarity.
    val baseLum =
        (0.299f * (spec.base shr 16 and 0xFF) + 0.587f * (spec.base shr 8 and 0xFF) +
            0.114f * (spec.base and 0xFF)) / 255f
    paint.shader = if (baseLum > 0.55f) {
        // light field: gentle white lift at top and bottom
        android.graphics.LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x4AFFFFFF, 0x10FFFFFF, 0x5AFFFFFF),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    } else {
        android.graphics.LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x52000000, 0x1A000000, 0x66000000),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    paint.shader = null
    return bmp
}

/* ------------------------------------------------------------------ *
 *  State shared with glass surfaces
 * ------------------------------------------------------------------ */

@Stable
class AuraBackdrop {
    /** Display copy of the backdrop (moderate resolution — gradients upscale cleanly). */
    var sharp: ImageBitmap? by mutableStateOf(null)

    /** Strongly blurred copy used by every glass surface for frosting. */
    var blurred: ImageBitmap? by mutableStateOf(null)

    /** Root size in px — maps card positions into backdrop space. */
    var screen: IntSize by mutableStateOf(IntSize.Zero)

    /** Display scale: backdrop bitmap size = screen * DISPLAY_SCALE. */
    var displayScale = 0.45f
        private set

    /** Blur sampling scale: blurred bitmap size = screen * BLUR_SCALE. */
    var blurScale = 0.16f
        private set

    private var lastKey = ""
    private var lastPhase = -1f

    fun render(
        spec: BackdropSpec,
        screenPx: IntSize,
        phase: Float,
        blurRadiusPx: Float,
        inline: Boolean,
        onDone: () -> Unit = {},
    ) {
        if (screenPx.width <= 0 || screenPx.height <= 0) return
        val key = "${spec.id}:${screenPx.width}x${screenPx.height}:${blurRadiusPx.toInt()}"
        val keyChanged = key != lastKey
        if (!keyChanged && kotlin.math.abs(phase - lastPhase) < 0.0005f) return
        lastKey = key
        lastPhase = phase

        val scale = displayScale
        val bw = (screenPx.width * scale).toInt().coerceAtLeast(2)
        val bh = (screenPx.height * scale).toInt().coerceAtLeast(2)
        val fs = blurScale
        val fw = (screenPx.width * fs).toInt().coerceAtLeast(2)
        val fh = (screenPx.height * fs).toInt().coerceAtLeast(2)

        val work: () -> Unit = {
            val sharpBmp = renderBackdrop(spec, bw, bh, phase)
            // blur radius expressed in blur-bitmap pixels (blur bitmap ≈ screen*fs)
            val r = (blurRadiusPx * fs / 1.6f).toInt().coerceIn(1, 48)
            val blurBmp = Blur.blurBitmap(
                android.graphics.Bitmap.createScaledBitmap(sharpBmp, fw, fh, true),
                r,
            )
            sharp = sharpBmp.asImageBitmap()
            blurred = blurBmp.asImageBitmap()
            screen = screenPx
            onDone()
        }

        if (inline) {
            work()
        } else {
            // caller launches within a coroutine scope
            pendingWork = work
        }
    }

    private var pendingWork: (() -> Unit)? = null

    suspend fun runPending() {
        val w = pendingWork ?: return
        pendingWork = null
        withContext(Dispatchers.Default) { w() }
    }
}

val LocalBackdrop = staticCompositionLocalOf<AuraBackdrop?> { null }
val LocalGlass = staticCompositionLocalOf<GlassTint?> { null }

/* ------------------------------------------------------------------ *
 *  Host: paints the backdrop and provides frost state to the tree
 * ------------------------------------------------------------------ */

@Composable
fun AuraBackdropHost(
    spec: BackdropSpec,
    glass: GlassTint,
    blurRadius: Dp = 46.dp,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val backdrop = remember { AuraBackdrop() }
    var phase by remember { mutableFloatStateOf(0.35f) }
    var screenPx by remember { mutableStateOf(IntSize.Zero) }

    val blurPx = with(density) { blurRadius.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { s ->
                if (s.width > 0 && s.height > 0 && s != screenPx) {
                    screenPx = IntSize(s.width, s.height)
                    backdrop.render(spec, screenPx, phase, blurPx, inline = true)
                }
            },
    ) {
        val sharp = backdrop.sharp
        if (sharp != null) {
            Image(
                bitmap = sharp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            )
        } else {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(spec.base)),
            )
        }

        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalGlass provides glass,
        ) {
            content()
        }
    }

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        while (isActive) {
            kotlinx.coroutines.delay(220)
            phase += 0.025f
            backdrop.render(spec, screenPx, phase, blurPx, inline = false)
            backdrop.runPending()
        }
    }

    LaunchedEffect(spec.id, blurPx) {
        if (screenPx.width > 0) {
            phase = 0.35f
            backdrop.render(spec, screenPx, phase, blurPx, inline = false)
            backdrop.runPending()
        }
    }
}
