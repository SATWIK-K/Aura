package com.aura.weight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.weight.core.SquircleShape
import com.aura.weight.ui.glass.GlassSurface
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.theme.caption
import com.aura.weight.ui.theme.label
import com.aura.weight.ui.theme.number

@Composable
fun StatTile(
    title: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
    accentIndex: Int = 0,
    status: String? = null,
) {
    val c = LocalAuraColors.current
    val accent = when (accentIndex) {
        0 -> c.accent
        1 -> c.accent2
        else -> c.accent3
    }
    GlassSurface(corner = 26.dp, elevation = 16.dp, modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = label(10),
                    color = c.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (status != null) {
                    Text(
                        status.uppercase(),
                        style = label(10),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = number(30),
                    color = c.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        unit,
                        style = caption(11),
                        color = c.tertiary,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .size(width = 34.dp, height = 5.dp)
                    .clip(SquircleShape(3.dp))
                    .background(accent),
            )
        }
    }
}
