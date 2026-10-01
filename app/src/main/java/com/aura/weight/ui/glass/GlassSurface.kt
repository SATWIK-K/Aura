package com.aura.weight.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.weight.core.AuraBackdrop
import com.aura.weight.core.LocalBackdrop
import com.aura.weight.core.LocalGlass
import com.aura.weight.core.SquircleShape
import com.aura.weight.core.drawFrost
import com.aura.weight.ui.theme.DarkGlass
import com.aura.weight.ui.theme.GlassTint
import com.aura.weight.ui.theme.LocalAuraColors

/**
 * A frosted-glass card. The blur under it is sampled from the real backdrop at
 * the card's exact position, then tinted, grained, and edged.
 *
 * [elevation] adds a soft drop shadow (0 for overlays that sit close to glass).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    corner: Dp = 28.dp,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop: AuraBackdrop? = LocalBackdrop.current
    val glass: GlassTint = LocalGlass.current ?: DarkGlass
    val shape = remember(corner) { SquircleShape(corner) }
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    var pos by remember { mutableStateOf(Offset.Zero) }
    val colors = LocalAuraColors.current
    val shadowColor =
        if (colors.isDark) Color.Black.copy(alpha = 0.55f) else Color(0x2E000000)

    Box(
        modifier
            .onGloballyPositioned { p ->
                val np = p.positionInRoot()
                if (np != pos) pos = np
            }
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        clip = false,
                        ambientColor = shadowColor,
                        spotColor = shadowColor,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .drawBehind {
                drawFrost(backdrop, pos, glass, radius = cornerPx)
            },
    ) {
        content()
    }
}
