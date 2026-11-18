package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepRoyalPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MagentaPulse
import com.example.ui.theme.NeonLilac
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SoftLavender
import com.example.voice.VoiceOrbState
import kotlin.math.sin

@Composable
fun RaniOrb(
    orbState: VoiceOrbState,
    rmsLevel: Float,
    wakeWordActive: Boolean,
    onTapOrb: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 186.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (orbState) {
                    VoiceOrbState.THINKING -> 2200
                    VoiceOrbState.LISTENING -> 3400
                    VoiceOrbState.SPEAKING -> 2800
                    VoiceOrbState.IDLE -> 9000
                },
                easing = LinearEasing
            )
        ),
        label = "orb_rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val animatedRms by animateFloatAsState(
        targetValue = if (orbState == VoiceOrbState.LISTENING || orbState == VoiceOrbState.SPEAKING) {
            rmsLevel.coerceIn(0.15f, 1f)
        } else {
            0.1f
        },
        animationSpec = spring(),
        label = "rms_anim"
    )

    val coreAccent = when (orbState) {
        VoiceOrbState.IDLE -> if (wakeWordActive) CyberCyan else ElectricViolet
        VoiceOrbState.LISTENING -> CyberCyan
        VoiceOrbState.THINKING -> MagentaPulse
        VoiceOrbState.SPEAKING -> NeonLilac
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable(onClick = onTapOrb)
                .testTag("rani_orb_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = (this.size.minDimension / 2f) * 0.62f
                val dynamicScale = pulse + (animatedRms * 0.14f)

                // Outer radial halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreAccent.copy(alpha = 0.42f),
                            ElectricViolet.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.65f * dynamicScale
                    ),
                    radius = baseRadius * 1.65f * dynamicScale,
                    center = center
                )

                // Rotating orbital rings
                rotate(degrees = rotation, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                CyberCyan.copy(alpha = 0.85f),
                                NeonLilac,
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 250f,
                        useCenter = false,
                        style = Stroke(width = 5f, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - baseRadius * 1.24f, center.y - baseRadius * 1.24f),
                        size = androidx.compose.ui.geometry.Size(
                            baseRadius * 2.48f,
                            baseRadius * 2.48f
                        )
                    )
                }

                rotate(degrees = -rotation * 1.35f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                MagentaPulse.copy(alpha = 0.8f),
                                SoftLavender,
                                Color.Transparent
                            )
                        ),
                        startAngle = 60f,
                        sweepAngle = 200f,
                        useCenter = false,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - baseRadius * 1.08f, center.y - baseRadius * 1.08f),
                        size = androidx.compose.ui.geometry.Size(
                            baseRadius * 2.16f,
                            baseRadius * 2.16f
                        )
                    )
                }

                // Core sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SoftLavender,
                            coreAccent,
                            DeepRoyalPurple
                        ),
                        center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f),
                        radius = baseRadius * 1.15f
                    ),
                    radius = baseRadius * (0.94f + animatedRms * 0.12f),
                    center = center
                )
            }

            Icon(
                imageVector = when (orbState) {
                    VoiceOrbState.SPEAKING -> Icons.Rounded.Stop
                    VoiceOrbState.LISTENING -> Icons.Rounded.GraphicEq
                    else -> Icons.Rounded.Mic
                },
                contentDescription = "RANI AI Voice Orb",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val stateLabel = when (orbState) {
            VoiceOrbState.IDLE -> if (wakeWordActive) "WAKE-WORD READY • \"HEY RANI\"" else "TAP ORB TO SPEAK"
            VoiceOrbState.LISTENING -> "LISTENING (HINDI / EN / HINGLISH)..."
            VoiceOrbState.THINKING -> "PROCESSING..."
            VoiceOrbState.SPEAKING -> "SPEAKING • TAP TO INTERRUPT"
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = ObsidianCard,
            border = BorderStroke(1.dp, coreAccent.copy(alpha = 0.5f))
        ) {
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = SoftLavender,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun VoiceWaveform(
    active: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing)
        ),
        label = "wave_phase"
    )

    Row(
        modifier = modifier.height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 16
        for (i in 0 until barCount) {
            val waveVal = if (active) {
                val amp = (0.35f + rmsLevel * 0.65f).coerceIn(0.25f, 1f)
                val sinFactor = ((sin(phase + i * 0.55f) + 1f) / 2f)
                (6f + 26f * amp * sinFactor)
            } else {
                4f + (i % 3) * 2f
            }
            val barColor = if (i % 2 == 0) NeonLilac else CyberCyan
            Canvas(
                modifier = Modifier
                    .width(4.dp)
                    .height(waveVal.dp)
            ) {
                drawLine(
                    color = if (active) barColor else barColor.copy(alpha = 0.3f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = size.width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCard.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 4.dp
    ) {
        content()
    }
}
