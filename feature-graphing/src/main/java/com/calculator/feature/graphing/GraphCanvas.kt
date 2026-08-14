package com.calculator.feature.graphing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

@OptIn(ExperimentalTextApi::class)
@Composable
fun GraphCanvas(
    expressions: List<String>,
    modifier: Modifier = Modifier
) {
    // Current viewport bounds
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Track last tap coordinates in math coordinates
    var lastTapPoint by remember { mutableStateOf<Offset?>(null) }

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
                            lastTapPoint = Offset(mathX, mathY.toFloat())
                        } else {
                            lastTapPoint = null
                        }
                    } else {
                        val mathY = (originY - pressOffset.y) / pixelsPerUnit
                        lastTapPoint = Offset(mathX, mathY)
                    }
                }
            }
            .pointerInput(expressions) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.1f, 10f)
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

        val step: Double = if (scale < 0.5f) 5.0 else if (scale > 2f) 0.5 else 1.0

        // Vertical lines & X Axis Labels
        var currentX: Double = Math.floor(xStartUnit.toDouble() / step) * step
        while (currentX <= xEndUnit) {
            val screenX = originX + (currentX * pixelsPerUnit).toFloat()
            drawLine(
                color = Color(0xFF1A1D27),
                start = Offset(screenX, 0f),
                end = Offset(screenX, height),
                strokeWidth = 1f
            )
            if (Math.abs(currentX) > 0.01) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format("%.1f", currentX).trimEnd('0').trimEnd('.'),
                    style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp),
                    topLeft = Offset(screenX + 4f, originY + 4f)
                )
            }
            currentX += step
        }

        // Horizontal lines & Y Axis Labels
        var currentY: Double = Math.floor(yStartUnit.toDouble() / step) * step
        while (currentY <= yEndUnit) {
            val screenY = originY - (currentY * pixelsPerUnit).toFloat()
            drawLine(
                color = Color(0xFF1A1D27),
                start = Offset(0f, screenY),
                end = Offset(width, screenY),
                strokeWidth = 1f
            )
            if (Math.abs(currentY) > 0.01) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format("%.1f", currentY).trimEnd('0').trimEnd('.'),
                    style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp),
                    topLeft = Offset(originX + 6f, screenY - 14f)
                )
            }
            currentY += step
        }

        // Draw Main Axes
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

        // Plot all graphs with high-contrast minimalist colors
        val graphColors = listOf(
            Color(0xFFFFFFFF), // Pure Titanium White
            Color(0xFF38BDF8), // Ice Blue
            Color(0xFFA855F7)  // Minimalist Purple
        )

        evaluators.forEachIndexed { index, evaluator ->
            if (evaluator != null) {
                val color = graphColors[index % graphColors.size]
                val pointsCount = width.toInt()
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
                            if (Math.abs(screenY - prevYScreen) > height) {
                                path.moveTo(screenX, screenY)
                            } else {
                                path.lineTo(screenX, screenY)
                            }
                        }
                        prevYScreen = screenY
                    } else {
                        isFirstPoint = true
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 4f)
                )
            }
        }

        // Draw tapped point inspector (GeoGebra style tooltips)
        lastTapPoint?.let { point ->
            val screenX = originX + point.x * pixelsPerUnit
            val screenY = originY - point.y * pixelsPerUnit

            // Target point crosshair
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(screenX, 0f),
                end = Offset(screenX, height),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(0f, screenY),
                end = Offset(width, screenY),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Outer ring
            drawCircle(
                color = Color(0xFF00FFB2).copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = Offset(screenX, screenY)
            )
            // Inner point
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(screenX, screenY)
            )

            // Text tooltip block
            val tooltipText = String.format("x: %.2f\ny: %.2f", point.x, point.y)
            val textLayout = textMeasurer.measure(
                text = AnnotatedString(tooltipText),
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // Draw tooltip backdrop
            val rectWidth = textLayout.size.width + 16f
            val rectHeight = textLayout.size.height + 12f
            val rectLeft = (screenX + 16f).coerceAtMost(width - rectWidth - 8f)
            val rectTop = (screenY - rectHeight - 16f).coerceAtLeast(8f)

            drawRoundRect(
                color = Color(0xCCFFFFFF),
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Draw text inside tooltip
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(rectLeft + 8f, rectTop + 6f)
            )
        }
    }
}
