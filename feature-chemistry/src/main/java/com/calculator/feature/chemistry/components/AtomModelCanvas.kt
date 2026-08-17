package com.calculator.feature.chemistry.components

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
import com.calculator.domain.model.Element
import com.calculator.domain.model.PeriodicTable
import kotlinx.coroutines.isActive

object ElectronShellCalculator {
    // Standard Bohr orbital shell distribution for elements 1-118
    fun getShellDistribution(atomicNumber: Int): List<Int> {
        val maxPerShell = listOf(2, 8, 18, 32, 32, 18, 8)
        val shells = mutableListOf<Int>()
        var remaining = atomicNumber

        for (max in maxPerShell) {
            if (remaining <= 0) break
            val count = Math.min(remaining, max)
            shells.add(count)
            remaining -= count
        }
        return shells
    }

    fun getQuantumNotation(atomicNumber: Int): String {
        return when (atomicNumber) {
            1 -> "1s¹"
            2 -> "1s²"
            6 -> "[He] 2s² 2p²"
            7 -> "[He] 2s² 2p³"
            8 -> "[He] 2s² 2p⁴"
            11 -> "[Ne] 3s¹"
            17 -> "[Ne] 3s² 3p⁵"
            26 -> "[Ar] 3d⁶ 4s²"
            29 -> "[Ar] 3d¹⁰ 4s¹"
            79 -> "[Xe] 4f¹⁴ 5d¹⁰ 6s¹"
            else -> {
                val shells = getShellDistribution(atomicNumber)
                "Электроны по слоям: " + shells.joinToString(", ")
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun AtomModelCanvas(
    element: Element = PeriodicTable.getElement("C") ?: Element("C", "Carbon", 6, 12.011),
    modifier: Modifier = Modifier
) {
    val shells = remember(element.atomicNumber) {
        ElectronShellCalculator.getShellDistribution(element.atomicNumber)
    }
    val quantumNotation = remember(element.atomicNumber) {
        ElectronShellCalculator.getQuantumNotation(element.atomicNumber)
    }

    var animTime by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNanos = System.nanoTime()
        while (isActive) {
            withFrameNanos { frameNanos ->
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                animTime += dt
                lastNanos = frameNanos
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Element Summary Card
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
                Text("${element.name} (${element.symbol})", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(quantumNotation, color = Color(0xFF38BDF8), fontSize = 12.sp, fontFamily = MonospaceFontFamily)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Z = ${element.atomicNumber}", color = TextSecondary, fontSize = 11.sp)
                Text("A = ${element.atomicMass}", color = TextSecondary, fontSize = 11.sp)
            }
        }

        // Live Bohr-Rutherford 2D/3D Orbital Canvas
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
                val center = Offset(width / 2, height / 2)

                val maxRadius = Math.min(width, height) / 2 - 20.dp.toPx()
                val shellCount = shells.size.coerceAtLeast(1)
                val shellRadiusStep = (maxRadius - 30.dp.toPx()) / shellCount.toFloat()

                // Draw Central Glowing Nucleus
                val nucleusRadius = 24.dp.toPx()
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.3f),
                    radius = nucleusRadius + 10.dp.toPx(),
                    center = center
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFF991B1B)),
                        center = Offset(center.x - 4f, center.y - 4f),
                        radius = nucleusRadius
                    ),
                    radius = nucleusRadius,
                    center = center
                )

                // Nucleus text (Symbol & Z)
                val nucleusText = "${element.symbol}\n+${element.atomicNumber}"
                val nucLayout = textMeasurer.measure(
                    text = AnnotatedString(nucleusText),
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
                )
                drawText(textLayoutResult = nucLayout, topLeft = Offset(center.x - nucLayout.size.width / 2, center.y - nucLayout.size.height / 2))

                // Draw Electron Shells and Orbiting Electrons
                shells.forEachIndexed { shellIndex, electronCount ->
                    val shellRadius = 30.dp.toPx() + (shellIndex + 1) * shellRadiusStep
                    val orbitSpeed = 1.0f / (shellIndex + 1) // Inner electrons orbit faster

                    // Orbital Ring
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                        radius = shellRadius,
                        center = center,
                        style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                    )

                    // Shell Label (K, L, M, N, etc.)
                    val shellNames = listOf("K", "L", "M", "N", "O", "P", "Q")
                    val shellName = shellNames.getOrElse(shellIndex) { "S${shellIndex + 1}" }
                    val shellLabelLayout = textMeasurer.measure(
                        text = AnnotatedString(shellName),
                        style = TextStyle(color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    drawText(textLayoutResult = shellLabelLayout, topLeft = Offset(center.x + shellRadius + 4f, center.y - 6f))

                    // Draw Electrons on this Shell
                    for (e in 0 until electronCount) {
                        val baseAngle = (2 * Math.PI / electronCount) * e
                        val currentAngle = baseAngle + (animTime * orbitSpeed * (if (shellIndex % 2 == 0) 1 else -1))

                        val electronX = center.x + (shellRadius * Math.cos(currentAngle)).toFloat()
                        val electronY = center.y + (shellRadius * Math.sin(currentAngle)).toFloat()

                        // Glowing Electron Dot
                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                            radius = 6.dp.toPx(),
                            center = Offset(electronX, electronY)
                        )
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = 3.dp.toPx(),
                            center = Offset(electronX, electronY)
                        )
                    }
                }
            }
        }
    }
}
