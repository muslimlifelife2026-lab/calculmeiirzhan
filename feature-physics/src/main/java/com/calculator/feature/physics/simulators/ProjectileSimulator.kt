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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import kotlinx.coroutines.isActive

@OptIn(ExperimentalTextApi::class)
@Composable
fun ProjectileSimulator(
    modifier: Modifier = Modifier
) {
    var initialVelocity by remember { mutableFloatStateOf(35f) } // v0 in m/s
    var launchAngleDeg by remember { mutableFloatStateOf(45f) }   // alpha in deg
    var isSimulating by remember { mutableStateOf(false) }

    val planets = listOf(
        PlanetGravity("Земля", 9.81f),
        PlanetGravity("Луна", 1.62f),
        PlanetGravity("Марс", 3.71f)
    )
    var selectedPlanet by remember { mutableStateOf(planets[0]) }

    var animTime by remember { mutableFloatStateOf(0f) }

    val g = selectedPlanet.g
    val alphaRad = Math.toRadians(launchAngleDeg.toDouble()).toFloat()

    val totalFlightTime = (2 * initialVelocity * Math.sin(alphaRad.toDouble()) / g).toFloat()
    val maxHeight = (Math.pow(initialVelocity * Math.sin(alphaRad.toDouble()), 2.0) / (2 * g)).toFloat()
    val maxRange = (Math.pow(initialVelocity.toDouble(), 2.0) * Math.sin(2 * alphaRad.toDouble()) / g).toFloat()

    LaunchedEffect(isSimulating) {
        if (isSimulating) {
            animTime = 0f
            var lastNanos = System.nanoTime()
            while (isActive && animTime < totalFlightTime) {
                withFrameNanos { frameNanos ->
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f) * 1.5f // Slight speedup for fun
                    animTime += dt
                    lastNanos = frameNanos
                }
            }
            isSimulating = false
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Telemetry readout
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
                Text("Дальность L", color = TextSecondary, fontSize = 11.sp)
                Text(String.format("%.1f м", maxRange), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
            }
            Column {
                Text("Высота H_max", color = TextSecondary, fontSize = 11.sp)
                Text(String.format("%.1f м", maxHeight), color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
            }
            Column {
                Text("Время t_полёта", color = TextSecondary, fontSize = 11.sp)
                Text(String.format("%.2f с", totalFlightTime), color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
            }
            Button(
                onClick = {
                    isSimulating = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Запуск 🚀", color = Background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live Ballistics Trajectory Canvas
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

                val originX = 40.dp.toPx()
                val originY = height - 35.dp.toPx()

                // Calculate visual scale
                val maxVisibleX = (maxRange * 1.15f).coerceAtLeast(30f)
                val maxVisibleY = (maxHeight * 1.35f).coerceAtLeast(20f)

                val scaleX = (width - originX - 30.dp.toPx()) / maxVisibleX
                val scaleY = (originY - 30.dp.toPx()) / maxVisibleY

                // Ground line
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(0f, originY),
                    end = Offset(width, originY),
                    strokeWidth = 2f
                )

                // Cannon Base
                drawCircle(color = Color(0xFF64748B), radius = 10.dp.toPx(), center = Offset(originX, originY))

                // Cannon Barrel
                val barrelLength = 22.dp.toPx()
                val barrelEndX = originX + (barrelLength * Math.cos(alphaRad.toDouble())).toFloat()
                val barrelEndY = originY - (barrelLength * Math.sin(alphaRad.toDouble())).toFloat()
                drawLine(color = Color.White, start = Offset(originX, originY), end = Offset(barrelEndX, barrelEndY), strokeWidth = 5f)

                // Theoretical Parabolic Curve
                val curvePath = Path()
                val steps = 80
                for (i in 0..steps) {
                    val t = totalFlightTime * (i.toFloat() / steps)
                    val mathX = (initialVelocity * Math.cos(alphaRad.toDouble()) * t).toFloat()
                    val mathY = (initialVelocity * Math.sin(alphaRad.toDouble()) * t - 0.5f * g * t * t).toFloat()

                    val sx = originX + mathX * scaleX
                    val sy = originY - mathY * scaleY

                    if (i == 0) curvePath.moveTo(sx, sy) else curvePath.lineTo(sx, sy)
                }

                drawPath(
                    path = curvePath,
                    color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                )

                // Current projectile position
                val currentT = animTime.coerceIn(0f, totalFlightTime)
                val currentMathX = (initialVelocity * Math.cos(alphaRad.toDouble()) * currentT).toFloat()
                val currentMathY = (initialVelocity * Math.sin(alphaRad.toDouble()) * currentT - 0.5f * g * currentT * currentT).toFloat().coerceAtLeast(0f)

                val projScreenX = originX + currentMathX * scaleX
                val projScreenY = originY - currentMathY * scaleY

                // Render Projectile
                drawCircle(color = Color(0xFFF59E0B).copy(alpha = 0.4f), radius = 12.dp.toPx(), center = Offset(projScreenX, projScreenY))
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(projScreenX, projScreenY))

                // Landing Target Indicator
                val targetScreenX = originX + maxRange * scaleX
                drawCircle(color = Color(0xFF10B981), radius = 4.dp.toPx(), center = Offset(targetScreenX, originY))
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString("🎯 ${String.format("%.1fм", maxRange)}"),
                    style = TextStyle(color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    topLeft = Offset(targetScreenX - 25f, originY + 6f)
                )
            }
        }

        // Sliders Card
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
                                animTime = 0f
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

            // Angle Slider alpha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Угол α: ${launchAngleDeg.toInt()}°", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = launchAngleDeg,
                    onValueChange = { launchAngleDeg = it; animTime = 0f },
                    valueRange = 10f..85f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                )
            }

            // Velocity Slider v0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Скорость v₀: ${initialVelocity.toInt()} м/с", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = initialVelocity,
                    onValueChange = { initialVelocity = it; animTime = 0f },
                    valueRange = 10f..80f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B))
                )
            }
        }
    }
}
