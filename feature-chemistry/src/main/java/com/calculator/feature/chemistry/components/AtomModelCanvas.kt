package com.calculator.feature.chemistry.components

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
import androidx.compose.ui.draw.drawWithCache
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
    fun getShellDistribution(atomicNumber: Int): List<Int> = PeriodicTable.shellDistribution(atomicNumber)

    fun getQuantumNotation(atomicNumber: Int): String {
        return PeriodicTable.allElements.getOrNull(atomicNumber - 1)?.electronConfig
            ?.takeIf { it.isNotBlank() } ?: "Конфигурация не указана"
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
    val nucLayout = remember(element.symbol, element.atomicNumber, textMeasurer) {
        textMeasurer.measure(
            text = AnnotatedString("${element.symbol}\n+${element.atomicNumber}"),
            style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
        )
    }
    val shellNames = remember { listOf("K", "L", "M", "N", "O", "P", "Q") }
    val shellLabelLayouts = remember(shells.size, textMeasurer) {
        List(shells.size) { shellIndex ->
            val shellName = shellNames.getOrElse(shellIndex) { "S${shellIndex + 1}" }
            textMeasurer.measure(
                text = AnnotatedString(shellName),
                style = TextStyle(color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
    val orbitStroke = remember {
        Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
    }

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
                Text("${element.nameRu.ifBlank { element.name }} (${element.symbol})", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(quantumNotation, color = Color(0xFFB9C6D6), fontSize = 12.sp, fontFamily = MonospaceFontFamily)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Z = ${element.atomicNumber}", color = TextSecondary, fontSize = 11.sp)
                val massLabel = if (element.atomicMassType == com.calculator.domain.model.AtomicMassType.STANDARD_ATOMIC_WEIGHT) "Aᵣ" else "m (а.е.м.)"
                Text("$massLabel = ${element.atomicMass}", color = TextSecondary, fontSize = 11.sp)
            }
        }

        // Live Bohr-Rutherford 2D/3D Orbital Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF161719))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                .drawWithCache {
                    val width = size.width
                    val height = size.height
                    val center = Offset(width / 2f, height / 2f)
                    val maxRadius = (minOf(width, height) / 2f - 20.dp.toPx()).coerceAtLeast(36.dp.toPx())
                    val shellCount = shells.size.coerceAtLeast(1)
                    val shellRadiusStep = (maxRadius - 30.dp.toPx()) / shellCount.toFloat()
                    val nucleusRadius = 24.dp.toPx()
                    val glowRadius = nucleusRadius + 10.dp.toPx()
                    val electronGlowRadius = 6.dp.toPx()
                    val electronRadius = 3.dp.toPx()
                    val baseShellRadius = 30.dp.toPx()
                    val nucleusBrush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFF991B1B)),
                        center = Offset(center.x - 4f, center.y - 4f),
                        radius = nucleusRadius
                    )
                    val nucleusGlowColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                    val ringColor = Color(0xFFB9C6D6).copy(alpha = 0.25f)
                    val electronGlowColor = Color(0xFFB9C6D6).copy(alpha = 0.45f)
                    val electronCoreColor = Color(0xFFB9C6D6)

                    onDrawBehind {
                        drawCircle(color = nucleusGlowColor, radius = glowRadius, center = center)
                        drawCircle(brush = nucleusBrush, radius = nucleusRadius, center = center)
                        drawText(
                            textLayoutResult = nucLayout,
                            topLeft = Offset(center.x - nucLayout.size.width / 2f, center.y - nucLayout.size.height / 2f)
                        )

                        shells.forEachIndexed { shellIndex, electronCount ->
                            val shellRadius = baseShellRadius + (shellIndex + 1) * shellRadiusStep
                            val orbitSpeed = 1.0f / (shellIndex + 1)

                            drawCircle(
                                color = ringColor,
                                radius = shellRadius,
                                center = center,
                                style = orbitStroke
                            )
                            drawText(
                                textLayoutResult = shellLabelLayouts[shellIndex],
                                topLeft = Offset(center.x + shellRadius + 4f, center.y - 6f)
                            )

                            for (e in 0 until electronCount) {
                                val baseAngle = (2 * Math.PI / electronCount) * e
                                val currentAngle = baseAngle + (animTime * orbitSpeed * (if (shellIndex % 2 == 0) 1 else -1))
                                val electronX = center.x + (shellRadius * Math.cos(currentAngle)).toFloat()
                                val electronY = center.y + (shellRadius * Math.sin(currentAngle)).toFloat()
                                drawCircle(color = electronGlowColor, radius = electronGlowRadius, center = Offset(electronX, electronY))
                                drawCircle(color = electronCoreColor, radius = electronRadius, center = Offset(electronX, electronY))
                            }
                        }
                    }
                }
        )
    }
}
