package com.calculator.feature.graphing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.engine.solver.GraphEvaluator

data class CriticalPoint(val x: Double, val y: Double, val isRoot: Boolean, val isMax: Boolean = false, val isMin: Boolean = false)

@OptIn(ExperimentalTextApi::class)
@Composable
fun GraphCanvas(
    expressions: List<String>,
    showRoots: Boolean = true,
    showExtremums: Boolean = true,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Track active inspector point (from drag or tap)
    var activePoint by remember { mutableStateOf<Offset?>(null) }

    // Re-evaluate RPNs when expressions change
    val evaluators = remember(expressions) {
        expressions.map { expr ->
            try {
                if (expr.isNotBlank()) GraphEvaluator(expr) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08090C)) // Pure Obsidian Minimalist Canvas
            .pointerInput(expressions) {
                detectTapGestures { pressOffset ->
                    val pixelsPerUnit = 50f * scale
                    val originX = size.width / 2 + offsetX
                    val originY = size.height / 2 + offsetY
                    val mathX = (pressOffset.x - originX) / pixelsPerUnit
                    
                    val primaryEvaluator = evaluators.firstOrNull { it != null }
                    if (primaryEvaluator != null) {
                        val mathY = primaryEvaluator.evaluateRange(mathX.toDouble(), mathX.toDouble(), 1).firstOrNull() ?: Double.NaN
                        if (!mathY.isNaN() && !mathY.isInfinite()) {
                            activePoint = Offset(mathX, mathY.toFloat())
                        } else {
                            activePoint = null
                        }
                    } else {
                        val mathY = (originY - pressOffset.y) / pixelsPerUnit
                        activePoint = Offset(mathX, mathY)
                    }
                }
            }
            .pointerInput(expressions) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val pixelsPerUnit = 50f * scale
                        val originX = size.width / 2 + offsetX
                        val mathX = (startOffset.x - originX) / pixelsPerUnit
                        val primaryEvaluator = evaluators.firstOrNull { it != null }
                        if (primaryEvaluator != null) {
                            val mathY = primaryEvaluator.evaluateRange(mathX.toDouble(), mathX.toDouble(), 1).firstOrNull() ?: Double.NaN
                            if (!mathY.isNaN() && !mathY.isInfinite()) {
                                activePoint = Offset(mathX, mathY.toFloat())
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val pixelsPerUnit = 50f * scale
                        val originX = size.width / 2 + offsetX
                        val mathX = (change.position.x - originX) / pixelsPerUnit
                        val primaryEvaluator = evaluators.firstOrNull { it != null }
                        if (primaryEvaluator != null) {
                            val mathY = primaryEvaluator.evaluateRange(mathX.toDouble(), mathX.toDouble(), 1).firstOrNull() ?: Double.NaN
                            if (!mathY.isNaN() && !mathY.isInfinite()) {
                                activePoint = Offset(mathX, mathY.toFloat())
                            }
                        }
                    }
                )
            }
            .pointerInput(expressions) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.15f, 15f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        val width = size.width
        val height = size.height

        val pixelsPerUnit = 50f * scale
        val originX = width / 2 + offsetX
        val originY = height / 2 + offsetY

        // Draw Grid Lines & Numbers
        val xStartUnit = -originX / pixelsPerUnit
        val xEndUnit = (width - originX) / pixelsPerUnit
        val yStartUnit = -originY / pixelsPerUnit
        val yEndUnit = (height - originY) / pixelsPerUnit

        val step: Double = when {
            scale < 0.4f -> 10.0
            scale < 0.8f -> 5.0
            scale > 3.5f -> 0.2
            scale > 1.8f -> 0.5
            else -> 1.0
        }

        // Vertical Grid Lines
        var currentX: Double = Math.floor(xStartUnit.toDouble() / step) * step
        while (currentX <= xEndUnit) {
            val screenX = originX + (currentX * pixelsPerUnit).toFloat()
            drawLine(
                color = Color(0xFF161922),
                start = Offset(screenX, 0f),
                end = Offset(screenX, height),
                strokeWidth = 1f
            )
            if (Math.abs(currentX) > 0.001) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format("%.1f", currentX).trimEnd('0').trimEnd('.'),
                    style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp),
                    topLeft = Offset(screenX + 4f, (originY + 4f).coerceIn(4f, height - 20f))
                )
            }
            currentX += step
        }

        // Horizontal Grid Lines
        var currentY: Double = Math.floor(yStartUnit.toDouble() / step) * step
        while (currentY <= yEndUnit) {
            val screenY = originY - (currentY * pixelsPerUnit).toFloat()
            drawLine(
                color = Color(0xFF161922),
                start = Offset(0f, screenY),
                end = Offset(width, screenY),
                strokeWidth = 1f
            )
            if (Math.abs(currentY) > 0.001) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format("%.1f", currentY).trimEnd('0').trimEnd('.'),
                    style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp),
                    topLeft = Offset((originX + 6f).coerceIn(4f, width - 40f), screenY - 14f)
                )
            }
            currentY += step
        }

        // Main Coordinate Axes
        drawLine(
            color = Color(0xFF475569),
            start = Offset(0f, originY),
            end = Offset(width, originY),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0xFF475569),
            start = Offset(originX, 0f),
            end = Offset(originX, height),
            strokeWidth = 1.5f
        )

        // Plot Graphs
        val graphColors = listOf(
            Color(0xFFFFFFFF), // Pure Titanium White
            Color(0xFF38BDF8), // Ice Blue
            Color(0xFFA855F7)  // Purple
        )

        val criticalPoints = mutableListOf<CriticalPoint>()

        evaluators.forEachIndexed { index, evaluator ->
            if (evaluator != null) {
                val color = graphColors[index % graphColors.size]
                val pointsCount = width.toInt().coerceAtLeast(100)
                val yValues = evaluator.evaluateRange(xStartUnit.toDouble(), xEndUnit.toDouble(), pointsCount)

                val path = Path()
                var isFirstPoint = true
                var prevYScreen = 0f

                for (i in 0 until pointsCount) {
                    val mathX = xStartUnit + i * (xEndUnit - xStartUnit) / (pointsCount - 1)
                    val mathY = yValues[i]

                    if (!mathY.isNaN() && !mathY.isInfinite()) {
                        val screenX = originX + mathX * pixelsPerUnit
                        val screenY = originY - (mathY * pixelsPerUnit).toFloat()

                        if (isFirstPoint) {
                            path.moveTo(screenX, screenY)
                            isFirstPoint = false
                        } else {
                            if (Math.abs(screenY - prevYScreen) > height * 1.5f) {
                                path.moveTo(screenX, screenY)
                            } else {
                                path.lineTo(screenX, screenY)
                            }
                        }
                        prevYScreen = screenY

                        // Root & Extremum detection for primary graph (index 0)
                        if (index == 0 && i > 0 && i < pointsCount - 1) {
                            val prevY = yValues[i - 1]
                            val nextY = yValues[i + 1]

                            // Root Zero-Crossing Detection: y(x) changes sign
                            if (showRoots && ((prevY < 0 && mathY >= 0) || (prevY > 0 && mathY <= 0))) {
                                if (criticalPoints.none { Math.abs(it.x - mathX) < (xEndUnit - xStartUnit) / 30.0 }) {
                                    criticalPoints.add(CriticalPoint(mathX.toDouble(), 0.0, isRoot = true))
                                }
                            }

                            // Extremum Detection: local peak or valley
                            if (showExtremums) {
                                if (mathY > prevY && mathY > nextY && Math.abs(mathY) > 0.01) {
                                    criticalPoints.add(CriticalPoint(mathX.toDouble(), mathY, isRoot = false, isMax = true))
                                } else if (mathY < prevY && mathY < nextY && Math.abs(mathY) > 0.01) {
                                    criticalPoints.add(CriticalPoint(mathX.toDouble(), mathY, isRoot = false, isMin = true))
                                }
                            }
                        }
                    } else {
                        isFirstPoint = true
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.5f)
                )
            }
        }

        // Render Critical Points (Roots & Extremums)
        criticalPoints.forEach { cp ->
            val screenX = originX + (cp.x * pixelsPerUnit).toFloat()
            val screenY = originY - (cp.y * pixelsPerUnit).toFloat()

            if (cp.isRoot) {
                // Glowing Cyan Root Dot
                drawCircle(color = Color(0xFF06B6D4).copy(alpha = 0.35f), radius = 10.dp.toPx(), center = Offset(screenX, screenY))
                drawCircle(color = Color(0xFF06B6D4), radius = 5.dp.toPx(), center = Offset(screenX, screenY))

                val rootLabel = String.format("x=%.2f", cp.x)
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(rootLabel),
                    style = TextStyle(color = Color(0xFF06B6D4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, screenY + 12f))
            } else if (cp.isMax || cp.isMin) {
                // Glowing Gold Extremum Dot
                val extColor = if (cp.isMax) Color(0xFFF59E0B) else Color(0xFFEC4899)
                drawCircle(color = extColor.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = Offset(screenX, screenY))
                drawCircle(color = extColor, radius = 4.dp.toPx(), center = Offset(screenX, screenY))

                val extLabel = if (cp.isMax) "max" else "min"
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(extLabel),
                    style = TextStyle(color = extColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, if (cp.isMax) screenY - 20f else screenY + 10f))
            }
        }

        // Active Touch / Drag Point Inspector Crosshair
        activePoint?.let { point ->
            val screenX = originX + point.x * pixelsPerUnit
            val screenY = originY - point.y * pixelsPerUnit

            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(screenX, 0f),
                end = Offset(screenX, height),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, screenY),
                end = Offset(width, screenY),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = Offset(screenX, screenY))
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(screenX, screenY))

            // Floating Coordinate Badge
            val tooltipText = String.format("x: %.3f\ny: %.3f", point.x, point.y)
            val textLayout = textMeasurer.measure(
                text = AnnotatedString(tooltipText),
                style = TextStyle(color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )

            val rectWidth = textLayout.size.width + 16f
            val rectHeight = textLayout.size.height + 12f
            val rectLeft = (screenX + 16f).coerceAtMost(width - rectWidth - 10f)
            val rectTop = (screenY - rectHeight - 16f).coerceAtLeast(10f)

            drawRoundRect(
                color = Color(0xEEFFFFFF),
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawText(textLayoutResult = textLayout, topLeft = Offset(rectLeft + 8f, rectTop + 6f))
        }
    }
}
