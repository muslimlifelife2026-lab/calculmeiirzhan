package com.calculator.feature.physics.simulators

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.calculator.domain.simulation.DampedSmallAnglePendulum

data class PlanetGravity(val name: String, val g: Double)

@OptIn(ExperimentalTextApi::class, ExperimentalLayoutApi::class)
@Composable
fun PendulumSimulator(
    modifier: Modifier = Modifier
) {
    var lengthMeters by rememberSaveable { mutableFloatStateOf(1.0f) }
    var initialAngleDeg by rememberSaveable { mutableFloatStateOf(10f) }
    var isRunning by rememberSaveable { mutableStateOf(true) }

    val planets = listOf(
        PlanetGravity("Земля", 9.81),
        PlanetGravity("Луна", 1.62),
        PlanetGravity("Марс", 3.71),
        PlanetGravity("Юпитер", 24.79)
    )
    var selectedPlanetName by rememberSaveable { mutableStateOf(planets[0].name) }
    val selectedPlanet = planets.first { it.name == selectedPlanetName }

    var timeSeconds by rememberSaveable { mutableDoubleStateOf(0.0) }
    val effectiveAngleDeg = initialAngleDeg.coerceIn(5f, 15f)
    val motion = remember(lengthMeters, selectedPlanetName, effectiveAngleDeg) {
        DampedSmallAnglePendulum(lengthMeters.toDouble(), selectedPlanet.g, Math.toRadians(effectiveAngleDeg.toDouble()))
    }

    LaunchedEffect(isRunning, motion) {
        if (isRunning) {
            var lastFrameTime: Long? = null
            while (isRunning) {
                withFrameNanos { frameTime ->
                    lastFrameTime?.let { previous ->
                        val dt = ((frameTime - previous) / 1_000_000_000.0).coerceIn(0.0, 0.05)
                        timeSeconds += dt
                    }
                    lastFrameTime = frameTime
                }
            }
        }
    }

    // Physics calculations
    val periodT = motion.periodSeconds
    val frequencyHz = 1.0 / periodT
    val state = motion.stateAt(timeSeconds)
    val thetaRad = state.angleRadians
    val currentSpeed = state.speedMetersPerSecond

    val textMeasurer = rememberTextMeasurer()
    val axisDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxHeight < 520.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
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
                    Text(String.format("%.2f Гц", frequencyHz), color = Color(0xFFB9C6D6), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
                }
                Column {
                    Text("Скорость v", color = TextSecondary, fontSize = 11.sp)
                    Text(String.format("%.2f м/с", currentSpeed), color = Color(0xFFACC4B0), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
                }
                Button(
                    onClick = {
                        timeSeconds = 0.0
                        isRunning = !isRunning
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) SurfaceElevated else AccentPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (isRunning) "Сброс" else "Старт", color = if (isRunning) Color.White else Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Live Pendulum Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (compact) Modifier.height(220.dp) else Modifier.weight(1f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF161719))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    val pivotX = width / 2
                    val pivotY = 35.dp.toPx()

                    val visualLength = (height - 90.dp.toPx()).coerceAtLeast(80f) * (lengthMeters / 3.0f).coerceIn(0.35f, 1.0f)

                    val bobX = pivotX + (visualLength * Math.sin(thetaRad)).toFloat()
                    val bobY = pivotY + (visualLength * Math.cos(thetaRad)).toFloat()

                    // Neutral center axis
                    drawLine(
                        color = Color(0xFF1E222D),
                        start = Offset(pivotX, pivotY),
                        end = Offset(pivotX, height - 20f),
                        strokeWidth = 1.5f,
                        pathEffect = axisDashEffect
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
                    drawCircle(color = Color(0xFFB9C6D6).copy(alpha = 0.35f), radius = 22.dp.toPx(), center = Offset(bobX, bobY))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFB9C6D6), Color(0xFF788797)),
                            center = Offset(bobX - 4f, bobY - 4f),
                            radius = 16.dp.toPx()
                        ),
                        radius = 14.dp.toPx(),
                        center = Offset(bobX, bobY)
                    )

                    // Angle label arc
                    val angleText = String.format("%.1f°", Math.toDegrees(thetaRad))
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
                Text("Гравитация:", color = TextSecondary, fontSize = 11.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    planets.forEach { planet ->
                        val isSelected = selectedPlanet == planet
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPlanetName = planet.name
                                timeSeconds = 0.0
                            },
                            label = { Text("${planet.name} (${planet.g})", fontSize = 11.sp) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
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
                        onValueChange = { lengthMeters = it; timeSeconds = 0.0 },
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
                    Text("Угол θ₀: ${effectiveAngleDeg.toInt()}°", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = effectiveAngleDeg,
                        onValueChange = { initialAngleDeg = it; timeSeconds = 0.0 },
                        valueRange = 5f..15f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFB9C6D6), activeTrackColor = Color(0xFFB9C6D6))
                    )
                }
                Text("Модель малых углов (до 15°), θ₀ отпускается без толчка. Постоянное g, линейное затухание β = 0,02 с⁻¹; время 1×. Большие углы эта модель не описывает.",
                    color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
