package com.calculator.feature.physics.simulators

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import kotlinx.coroutines.isActive

data class PlanetGravity(val name: String, val g: Float)

@OptIn(ExperimentalTextApi::class)
@Composable
fun PendulumSimulator(
    modifier: Modifier = Modifier
) {
    var lengthMeters by remember { mutableFloatStateOf(1.0f) }
    var initialAngleDeg by remember { mutableFloatStateOf(30f) }
    var damping by remember { mutableFloatStateOf(0.02f) }
    var isRunning by remember { mutableStateOf(true) }

    val planets = listOf(
        PlanetGravity("Земля", 9.81f),
        PlanetGravity("Луна", 1.62f),
        PlanetGravity("Марс", 3.71f),
        PlanetGravity("Юпитер", 24.79f)
    )
    var selectedPlanet by remember { mutableStateOf(planets[0]) }

    var timeSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRunning, lengthMeters, selectedPlanet, damping) {
        var lastFrameTime = System.nanoTime()
        while (isActive) {
            withFrameNanos { frameTime ->
                if (isRunning) {
                    val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    timeSeconds += dt
                }
                lastFrameTime = frameTime
            }
        }
    }

    // Physics calculations
    val omega = Math.sqrt((selectedPlanet.g / lengthMeters).toDouble()).toFloat()
    val periodT = (2 * Math.PI / omega).toFloat()
    val frequencyHz = 1f / periodT

    val thetaRad = (Math.toRadians(initialAngleDeg.toDouble()) *
            Math.exp((-damping * timeSeconds).toDouble()) *
            Math.cos((omega * timeSeconds).toDouble())).toFloat()

    val currentSpeed = Math.abs(lengthMeters * omega * Math.sin((omega * timeSeconds).toDouble())).toFloat()
    val maxSpeed = (lengthMeters * omega * Math.toRadians(initialAngleDeg.toDouble())).toFloat()
    val kineticFraction = if (maxSpeed > 0) (currentSpeed / maxSpeed).coerceIn(0f, 1f) else 0f
    val potentialFraction = (1f - kineticFraction).coerceIn(0f, 1f)

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Physics Telemetry Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Период T", color = TextSecondary, fontSize = 11.sp)
                Text(String.format("%.2f с", periodT), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
            }
            Column {
                Text("Частота ν", color = TextSecondary, fontSize = 11.sp)
                Text(String.format("%.2f Гц", frequencyHz), color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
            }
            Column {
                Text("Скорость v", color = TextSecondary, fontSize = 11.sp)
                Text(String.format("%.2f м/с", currentSpeed), color = Color(0xFF10B981), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
            }
            Button(
                onClick = {
                    timeSeconds = 0f
                    isRunning = !isRunning
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) SurfaceElevated else AccentPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(if (isRunning) "Сброс" else "Старт", color = if (isRunning) Color.White else Background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live Pendulum Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF08090C))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val pivotX = width / 2
                val pivotY = 35.dp.toPx()

                val visualLength = (height - 90.dp.toPx()).coerceAtLeast(80f) * (lengthMeters / 3.0f).coerceIn(0.35f, 1.0f)

                val bobX = pivotX + (visualLength * Math.sin(thetaRad.toDouble())).toFloat()
                val bobY = pivotY + (visualLength * Math.cos(thetaRad.toDouble())).toFloat()

                // Neutral center axis
                drawLine(
                    color = Color(0xFF1E222D),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX, height - 20f),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Pivot mount
                drawCircle(color = Color(0xFF94A3B8), radius = 6.dp.toPx(), center = Offset(pivotX, pivotY))
                drawLine(color = Color(0xFF64748B), start = Offset(pivotX - 30f, pivotY), end = Offset(pivotX + 30f, pivotY), strokeWidth = 3f)

                // String / Rod
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(pivotX, pivotY),
                    end = Offset(bobX, bobY),
                    strokeWidth = 2.5f
                )

                // Bob Mass Glow & Body
                drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.35f), radius = 22.dp.toPx(), center = Offset(bobX, bobY))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFF38BDF8), Color(0xFF0284C7)),
                        center = Offset(bobX - 4f, bobY - 4f),
                        radius = 16.dp.toPx()
                    ),
                    radius = 14.dp.toPx(),
                    center = Offset(bobX, bobY)
                )

                // Angle label arc
                val angleText = String.format("%.1f°", Math.toDegrees(thetaRad.toDouble()))
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(angleText),
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(pivotX + 16f, pivotY + 20f))
            }
        }

        // Interactive Parameter Sliders Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Planet selector pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Гравитация:", color = TextSecondary, fontSize = 11.sp)
                planets.forEach { planet ->
                    val isSelected = selectedPlanet == planet
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White else SurfaceElevated)
                            .border(1.dp, if (isSelected) Color.White else SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedPlanet = planet
                                timeSeconds = 0f
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${planet.name} (${planet.g})",
                            color = if (isSelected) Background else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Length Slider L
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Длина L: ${String.format("%.2f м", lengthMeters)}", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = lengthMeters,
                    onValueChange = { lengthMeters = it; timeSeconds = 0f },
                    valueRange = 0.2f..3.0f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                )
            }

            // Initial Angle Slider θ₀
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Угол θ₀: ${initialAngleDeg.toInt()}°", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = initialAngleDeg,
                    onValueChange = { initialAngleDeg = it; timeSeconds = 0f },
                    valueRange = 5f..60f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                )
            }
        }
    }
}
