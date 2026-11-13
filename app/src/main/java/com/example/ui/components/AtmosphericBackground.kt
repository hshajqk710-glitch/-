package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.example.data.model.AtmosphereMood
import kotlin.math.PI
import kotlin.math.sin

private data class SkyPalette(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val glow: Color,
)

private fun paletteFor(mood: AtmosphereMood): SkyPalette = when (mood) {
    AtmosphereMood.CLEAR_DAY -> SkyPalette(
        top = Color(0xFF0A438A),
        middle = Color(0xFF1A67C7),
        bottom = Color(0xFF479AF0),
        glow = Color(0x55FFD166),
    )
    AtmosphereMood.CLEAR_NIGHT -> SkyPalette(
        top = Color(0xFF040A18),
        middle = Color(0xFF0C1A38),
        bottom = Color(0xFF182F5C),
        glow = Color(0x337CC4FF),
    )
    AtmosphereMood.PARTLY_CLOUDY -> SkyPalette(
        top = Color(0xFF132D4F),
        middle = Color(0xFF284D75),
        bottom = Color(0xFF4D7298),
        glow = Color(0x33FFE082),
    )
    AtmosphereMood.CLOUDY -> SkyPalette(
        top = Color(0xFF1A2838),
        middle = Color(0xFF2F4256),
        bottom = Color(0xFF4B6178),
        glow = Color(0x26C7D8EE),
    )
    AtmosphereMood.FOG -> SkyPalette(
        top = Color(0xFF222D38),
        middle = Color(0xFF3A4958),
        bottom = Color(0xFF5D6E7E),
        glow = Color(0x2AE2ECF5),
    )
    AtmosphereMood.RAIN -> SkyPalette(
        top = Color(0xFF0B1A29),
        middle = Color(0xFF173046),
        bottom = Color(0xFF2A4A64),
        glow = Color(0x2E38BDF8),
    )
    AtmosphereMood.STORM -> SkyPalette(
        top = Color(0xFF080C1B),
        middle = Color(0xFF161D38),
        bottom = Color(0xFF283154),
        glow = Color(0x30818CF8),
    )
    AtmosphereMood.SNOW -> SkyPalette(
        top = Color(0xFF152A44),
        middle = Color(0xFF304E6E),
        bottom = Color(0xFF5C7FA1),
        glow = Color(0x38E0F2FE),
    )
}

private data class ParticleSeed(
    val xFrac: Float,
    val yFrac: Float,
    val speed: Float,
    val size: Float,
    val phase: Float,
)

@Composable
fun AtmosphericBackground(
    mood: AtmosphereMood,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val target = paletteFor(mood)
    val topColor by animateColorAsState(target.top, tween(900), label = "skyTop")
    val midColor by animateColorAsState(target.middle, tween(900), label = "skyMid")
    val botColor by animateColorAsState(target.bottom, tween(900), label = "skyBot")
    val glowColor by animateColorAsState(target.glow, tween(900), label = "skyGlow")

    val infiniteTransition = rememberInfiniteTransition(label = "atmosphereMotion")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particleProgress",
    )

    val seeds = remember {
        List(38) { idx ->
            val a = ((idx * 73 + 19) % 100) / 100f
            val b = ((idx * 41 + 7) % 100) / 100f
            val c = 0.6f + ((idx * 29) % 10) / 10f
            val d = 1.5f + (idx % 4) * 1.1f
            val e = ((idx * 53) % 100) / 100f
            ParticleSeed(a, b, c, d, e)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topColor, midColor, botColor),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle atmospheric radial glow in top-right quadrant
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.14f),
                    radius = size.width * 0.72f,
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.85f, size.height * 0.14f),
            )

            when (mood) {
                AtmosphereMood.CLEAR_NIGHT -> {
                    seeds.forEach { star ->
                        val twinkle = 0.25f + 0.65f * ((sin((progress + star.phase) * 2f * PI.toFloat()) + 1f) * 0.5f)
                        drawCircle(
                            color = Color.White.copy(alpha = twinkle * 0.65f),
                            radius = star.size * 0.65f,
                            center = Offset(star.xFrac * size.width, star.yFrac * size.height * 0.58f),
                        )
                    }
                }
                AtmosphereMood.RAIN, AtmosphereMood.STORM -> {
                    seeds.forEach { drop ->
                        val yPos = ((drop.yFrac + progress * drop.speed * 2.2f) % 1f) * size.height
                        val xPos = (drop.xFrac * size.width) - (yPos * 0.08f)
                        val wrappedX = ((xPos % size.width) + size.width) % size.width
                        drawLine(
                            color = Color(0xFFBAE6FD).copy(alpha = 0.18f),
                            start = Offset(wrappedX, yPos),
                            end = Offset(wrappedX - 7f, yPos + 28f),
                            strokeWidth = 1.6f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                AtmosphereMood.SNOW -> {
                    seeds.forEach { flake ->
                        val yPos = ((flake.yFrac + progress * flake.speed * 0.9f) % 1f) * size.height
                        val drift = sin((progress + flake.phase) * 2f * PI.toFloat()) * 18f
                        val xPos = (flake.xFrac * size.width) + drift
                        drawCircle(
                            color = Color.White.copy(alpha = 0.38f),
                            radius = flake.size * 0.95f,
                            center = Offset(xPos, yPos),
                        )
                    }
                }
                else -> {
                    // Gentle diffused atmospheric cloud halo
                    val driftX = sin(progress * 2f * PI.toFloat()) * 24f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(size.width * 0.2f + driftX, size.height * 0.28f),
                            radius = size.width * 0.5f,
                        ),
                        radius = size.width * 0.5f,
                        center = Offset(size.width * 0.2f + driftX, size.height * 0.28f),
                    )
                }
            }
        }
        content()
    }
}
