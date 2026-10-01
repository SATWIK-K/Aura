package com.aura.weight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.weight.core.SquircleShape
import com.aura.weight.ui.glass.GlassSurface
import com.aura.weight.ui.theme.LocalAuraColors
import com.aura.weight.ui.theme.body
import com.aura.weight.ui.theme.label
import com.aura.weight.ui.theme.title

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 48.dp,
) {
    val c = LocalAuraColors.current
    GlassSurface(
        corner = size / 2f,
        elevation = 10.dp,
        modifier = Modifier.size(size),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(size)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = c.secondary,
                modifier = Modifier.size(size * 0.44f),
            )
        }
    }
}

@Composable
fun AuraTopBar(
    title: String,
    onBack: () -> Unit,
    backIcon: ImageVector,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = LocalAuraColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(backIcon, "Back", onBack, 48.dp)
        Spacer(Modifier.width(14.dp))
        Text(title, style = title(22), color = c.primary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SectionCard(
    heading: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = LocalAuraColors.current
    GlassSurface(corner = 28.dp, elevation = 16.dp, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(heading, style = label(10), color = c.tertiary)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun ChoiceRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val c = LocalAuraColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SquircleShape(18.dp))
            .background(if (c.isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, text) ->
            val active = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(SquircleShape(16.dp))
                    .background(
                        if (active) {
                            if (c.isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.14f)
                            else androidx.compose.ui.graphics.Color.White
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        },
                    )
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text,
                    style = body(14, androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = if (active) c.primary else c.tertiary,
                )
            }
        }
    }
}

@Composable
fun AccentButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 64.dp,
    onClick: () -> Unit,
) {
    val c = LocalAuraColors.current
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(SquircleShape(24.dp))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    if (enabled) listOf(c.accent, c.accent2)
                    else listOf(c.quaternary, c.quaternary),
                ),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = body(16, androidx.compose.ui.text.font.FontWeight.SemiBold), color = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    onClick: () -> Unit,
) {
    val c = LocalAuraColors.current
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(SquircleShape(24.dp))
            .border(1.5.dp, c.quaternary.copy(alpha = if (c.isDark) 0.5f else 0.35f), SquircleShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = body(16, androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.secondary)
    }
}

@Composable
fun QuoteCard(
    text: String,
    author: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalAuraColors.current
    GlassSurface(corner = 28.dp, elevation = 14.dp, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                "“",
                style = title(30),
                color = c.accent2.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                style = body(15),
                color = c.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                author.uppercase(),
                style = label(10),
                color = c.tertiary,
            )
        }
    }
}
