package com.aura.weight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aura.weight.core.SquircleShape
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.theme.hero
import com.aura.weight.ui.theme.label
import com.aura.weight.ui.theme.number

fun filterNumber(input: String): String {
    val out = StringBuilder()
    var seenDot = false
    for (ch in input) {
        when {
            ch.isDigit() -> out.append(ch)
            (ch == '.' || ch == ',') && !seenDot -> {
                out.append('.')
                seenDot = true
            }
        }
        if (out.length >= 7) break
    }
    return out.toString()
}

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    unitLabel: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = hero(56),
    placeholder: String = "0.0",
) {
    val c = LocalAuraColors.current
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(filterNumber(it)) },
            textStyle = textStyle.copy(color = c.primary),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.width(IntrinsicSize.Min),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, style = textStyle, color = c.quaternary)
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(10.dp))
        Text(
            unitLabel,
            style = number(24, FontWeight.Medium),
            color = c.tertiary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
fun LabeledField(heading: String, content: @Composable () -> Unit) {
    val c = LocalAuraColors.current
    Column(Modifier.fillMaxWidth()) {
        Text(heading, style = label(10), color = c.tertiary)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(SquircleShape(22.dp))
                .background(Color.White.copy(alpha = if (c.isDark) 0.06f else 0.6f))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}
