package com.aura.weight.ui.theme

import androidx.compose.ui.graphics.Color

/** Accent ramp: the aura — indigo → cyan → mint. */
val AccentIndigo = Color(0xFF6A7BFF)
val AccentCyan = Color(0xFF3ED0FF)
val AccentMint = Color(0xFF4CE0B3)

val Positive = Color(0xFF4CE0B3)
val Caution = Color(0xFFFFB454)
val Negative = Color(0xFFFF6B81)

/** Solid surfaces (used where glass cannot be used: dialogs, widgets fallback). */
val InkBlack = Color(0xFF0A0B0F)
val InkWhite = Color(0xFFF4F6FB)

/** All colors a screen needs, for one theme. */
data class AuraColors(
    val isDark: Boolean,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val quaternary: Color,
    val positive: Color,
    val caution: Color,
    val negative: Color,
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val onAccent: Color,
)

val DarkColors = AuraColors(
    isDark = true,
    primary = Color(0xFFF2F4F9),
    secondary = Color(0xFFA6ADBF),
    tertiary = Color(0xFF7E859B),
    quaternary = Color(0xFF4B5165),
    positive = Positive,
    caution = Caution,
    negative = Negative,
    accent = AccentIndigo,
    accent2 = AccentCyan,
    accent3 = AccentMint,
    surface = Color(0xFF12141B),
    surfaceHigh = Color(0xFF1B1E28),
    onAccent = Color(0xFF07080C),
)

val LightColors = AuraColors(
    isDark = false,
    primary = Color(0xFF111318),
    secondary = Color(0xFF4E5464),
    tertiary = Color(0xFF666D81),
    quaternary = Color(0xFFB7BCC9),
    positive = Color(0xFF0FA274),
    caution = Color(0xFFC67C10),
    negative = Color(0xFFE0455F),
    accent = Color(0xFF4B5BE8),
    accent2 = Color(0xFF0E9DC4),
    accent3 = Color(0xFF0EA97C),
    surface = Color(0xFFF3F4F8),
    surfaceHigh = Color(0xFFFFFFFF),
    onAccent = Color(0xFFFFFFFF),
)

/** The frosted material for one theme. Alpha values are pre-multiplied tints over the blur. */
data class GlassTint(
    val top: Color,
    val bottom: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val specular: Color,
    val grainAlpha: Float,
    val shadow: Color,
)

val DarkGlass = GlassTint(
    top = Color(0x7A141722),
    bottom = Color(0xB30C0E14),
    borderTop = Color.White.copy(alpha = 0.11f),
    borderBottom = Color.Black.copy(alpha = 0.35f),
    specular = Color.White.copy(alpha = 0.07f),
    grainAlpha = 0.055f,
    shadow = Color.Black.copy(alpha = 0.55f),
)

val LightGlass = GlassTint(
    top = Color(0x66FFFFFF),
    bottom = Color(0x99FFFFFF),
    borderTop = Color.White.copy(alpha = 0.85f),
    borderBottom = Color.Black.copy(alpha = 0.10f),
    specular = Color.White.copy(alpha = 0.65f),
    grainAlpha = 0.045f,
    shadow = Color(0x33000000),
)
