package com.aura.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.aura.weight.core.SquircleShape
import com.aura.weight.ui.glass.GlassSurface
import com.aura.weight.ui.theme.hero
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class DiagTest : BaseVisualTest() {

    @Test
    fun variants() = capture("diag-variants.png", dark = true) { Variants() }

    @Test
    fun rawContent() = captureRaw("diag-raw.png", dark = true) { Variants() }
}

@Composable
private fun Variants() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(36.dp))

        // E: plain system text, no clip — baseline
        Text("E system plain", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(10.dp))

        // F: hero font only
        Text("F hero font", style = hero(30), color = Color.White)

        Spacer(Modifier.height(18.dp))

        // A: squircle clip + flat background + text (tests path/clip)
        Box(
            Modifier
                .size(width = 340.dp, height = 80.dp)
                .clip(SquircleShape(32.dp))
                .background(Color(0xFF223344)),
            contentAlignment = Alignment.Center,
        ) {
            Text("A clip", fontSize = 28.sp, color = Color.White)
        }

        Spacer(Modifier.height(14.dp))

        // B: GlassSurface without elevation (tests frost + content, no shadow)
        GlassSurface(corner = 32.dp, elevation = 0.dp, modifier = Modifier.size(width = 340.dp, height = 80.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("B no-elev", fontSize = 28.sp, color = Color.White)
            }
        }

        Spacer(Modifier.height(14.dp))

        // C: GlassSurface with elevation (tests shadow layer)
        GlassSurface(corner = 32.dp, elevation = 18.dp, modifier = Modifier.size(width = 340.dp, height = 80.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("C elev", fontSize = 28.sp, color = Color.White)
            }
        }

        Spacer(Modifier.height(14.dp))

        // D: rounded-corner clip control
        Box(
            Modifier
                .size(width = 340.dp, height = 80.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                .background(Color(0xFF334422)),
            contentAlignment = Alignment.Center,
        ) {
            Text("D round", fontSize = 28.sp, color = Color.White)
        }
    }
}
