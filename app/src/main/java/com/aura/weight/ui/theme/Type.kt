package com.aura.weight.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aura.weight.R

val Text = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val Numerals = FontFamily(
    Font(R.font.spacegrotesk_medium, FontWeight.Medium),
    Font(R.font.spacegrotesk_bold, FontWeight.Bold),
)

/** Big hero numeral: tight, confident. */
fun hero(size: Int): TextStyle = TextStyle(
    fontFamily = Numerals,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    letterSpacing = (-0.035).em,
    lineHeight = (size * 1.02).sp,
)

/** Medium numeral for tiles. */
fun number(size: Int, weight: FontWeight = FontWeight.Bold): TextStyle = TextStyle(
    fontFamily = Numerals,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = (-0.02).em,
)

/** Screen / card title. */
fun title(size: Int = 26): TextStyle = TextStyle(
    fontFamily = Text,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    letterSpacing = (-0.02).em,
)

/** Small uppercase section label. */
fun label(size: Int = 11, weight: FontWeight = FontWeight.SemiBold): TextStyle = TextStyle(
    fontFamily = Text,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = 0.12.em,
    lineHeight = (size * 1.35).sp,
)

/** Body copy. */
fun body(size: Int = 15, weight: FontWeight = FontWeight.Normal): TextStyle = TextStyle(
    fontFamily = Text,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = (-0.005).em,
    lineHeight = (size * 1.45).sp,
)

/** Tiny caption. */
fun caption(size: Int = 12): TextStyle = TextStyle(
    fontFamily = Text,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    letterSpacing = 0.01.em,
)
