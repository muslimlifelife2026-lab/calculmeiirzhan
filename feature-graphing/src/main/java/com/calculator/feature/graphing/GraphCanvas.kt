package com.calculator.feature.graphing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.engine.solver.GraphEvaluator
import kotlinx.coroutines.launch
import kotlin.math.*

data class CriticalPoint(
    val x: Double,
    val y: Double,
    val isRoot: Boolean = false,
    val isMax: Boolean = false,
    val isMin: Boolean = false,
    val isIntersection: Boolean = false
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun GraphCanvas(
    expressions: List<String>,
    mode: GraphMode = GraphMode.CARTESIAN,
    params: Map<String, Double> = emptyMap(),
    showRoots: Boolean = true,
    showExtremums: Boolean = true,
    showIntersections: Boolean = true,
    showTangent: Boolean = false,
    showIntegral: Boolean = false,
    integralA: Double = 0.0,
    integralB: Double = 2.0,
    resetTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val animScale = remember { Animatable(1f) }
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }

    var activePoint by remember { mutableStateOf<Offset?>(null) }
    var lastHapticPoint by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            launch { animScale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
            launch { animOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
            launch { animOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
            activePoint = null
        }
    }

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

    val graphColors = listOf(
        Color(0xFFFFFFFF), // Titanium White
        Color(0xFF38BDF8), // Ice Blue
        Color(0xFFA855F7), // Cyber Purple
        Color(0xFF10B981)  // Emerald
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080B)) // Pure Obsidian Canvas
            .pointerInput(expressions, params, mode) {
                detectTapGestures { pressOffset ->
                    val scale = animScale.value
                    val offsetX = animOffsetX.value
                    val offsetY = animOffsetY.value
                    val pixelsPerUnit = 50f * scale
                    val originX = size.width / 2 + offsetX
                    val originY = size.height / 2 + offsetY
                    val mathX = (pressOffset.x - originX) / pixelsPerUnit

                    val primaryEvaluator = evaluators.firstOrNull { it != null }
                    if (primaryEvaluator != null && mode == GraphMode.CARTESIAN) {
                        val mathY = primaryEvaluator.evaluate(mathX.toDouble(), params)
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
            .pointerInput(expressions, params, mode) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val scale = animScale.value
                        val offsetX = animOffsetX.value
                        val pixelsPerUnit = 50f * scale
                        val originX = size.width / 2 + offsetX
                        val mathX = (startOffset.x - originX) / pixelsPerUnit
                        val primaryEvaluator = evaluators.firstOrNull { it != null }
                        if (primaryEvaluator != null && mode == GraphMode.CARTESIAN) {
                            val mathY = primaryEvaluator.evaluate(mathX.toDouble(), params)
                            if (!mathY.isNaN() && !mathY.isInfinite()) {
                                activePoint = Offset(mathX, mathY.toFloat())
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val scale = animScale.value
                        val offsetX = animOffsetX.value
                        val pixelsPerUnit = 50f * scale
                        val originX = size.width / 2 + offsetX
                        val mathX = (change.position.x - originX) / pixelsPerUnit
                        val primaryEvaluator = evaluators.firstOrNull { it != null }
                        if (primaryEvaluator != null && mode == GraphMode.CARTESIAN) {
                            val mathY = primaryEvaluator.evaluate(mathX.toDouble(), params)
                            if (!mathY.isNaN() && !mathY.isInfinite()) {
                                activePoint = Offset(mathX, mathY.toFloat())
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    coroutineScope.launch {
                        val newScale = (animScale.value * zoom).coerceIn(0.12f, 18f)
                        animScale.snapTo(newScale)
                        animOffsetX.snapTo(animOffsetX.value + pan.x)
                        animOffsetY.snapTo(animOffsetY.value + pan.y)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val scale = animScale.value
        val offsetX = animOffsetX.value
        val offsetY = animOffsetY.value

        val pixelsPerUnit = 50f * scale
        val originX = width / 2 + offsetX
        val originY = height / 2 + offsetY

        val xStartUnit = -originX / pixelsPerUnit
        val xEndUnit = (width - originX) / pixelsPerUnit
        val yStartUnit = -originY / pixelsPerUnit
        val yEndUnit = (height - originY) / pixelsPerUnit

        val step: Double = when {
            scale < 0.35f -> 10.0
            scale < 0.75f -> 5.0
            scale > 3.5f -> 0.2
            scale > 1.8f -> 0.5
            else -> 1.0
        }

        // ─── 1. Grid Rendering ────────────────────────────────────────────────
        if (mode == GraphMode.POLAR) {
            // Polar concentric rings & angle rays
            val maxRadius = sqrt((width / 2 + abs(offsetX)).pow(2) + (height / 2 + abs(offsetY)).pow(2)) / pixelsPerUnit
            var r = step
            while (r <= maxRadius) {
                val radiusPx = (r * pixelsPerUnit).toFloat()
                drawCircle(
                    color = Color(0xFF131722),
                    radius = radiusPx,
                    center = Offset(originX, originY),
                    style = Stroke(width = 1f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "r=${String.format("%.1f", r).trimEnd('0').trimEnd('.')}",
                    style = TextStyle(color = Color(0xFF475569), fontSize = 9.sp),
                    topLeft = Offset(originX + radiusPx + 3f, originY + 2f)
                )
                r += step
            }

            // Radial angle rays (0, π/6, π/4, π/3, π/2, etc.)
            val angles = listOf(
                Pair(0.0, "0°"),
                Pair(Math.PI / 6, "30°"),
                Pair(Math.PI / 4, "45°"),
                Pair(Math.PI / 3, "60°"),
                Pair(Math.PI / 2, "90°"),
                Pair(2 * Math.PI / 3, "120°"),
                Pair(3 * Math.PI / 4, "135°"),
                Pair(5 * Math.PI / 6, "150°"),
                Pair(Math.PI, "180°")
            )
            val rayLen = max(width, height) * 2f
            for ((ang, label) in angles) {
                val cosA = cos(ang).toFloat()
                val sinA = sin(ang).toFloat()
                drawLine(
                    color = Color(0xFF12151E),
                    start = Offset(originX - cosA * rayLen, originY + sinA * rayLen),
                    end = Offset(originX + cosA * rayLen, originY - sinA * rayLen),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }
        } else {
            // Cartesian Vertical Grid
            var currentX: Double = Math.floor(xStartUnit.toDouble() / step) * step
            while (currentX <= xEndUnit) {
                val screenX = originX + (currentX * pixelsPerUnit).toFloat()
                drawLine(
                    color = Color(0xFF131620),
                    start = Offset(screenX, 0f),
                    end = Offset(screenX, height),
                    strokeWidth = 1f
                )
                if (abs(currentX) > 0.001) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = String.format("%.1f", currentX).trimEnd('0').trimEnd('.'),
                        style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp),
                        topLeft = Offset(screenX + 4f, (originY + 4f).coerceIn(4f, height - 20f))
                    )
                }
                currentX += step
            }

            // Cartesian Horizontal Grid
            var currentY: Double = Math.floor(yStartUnit.toDouble() / step) * step
            while (currentY <= yEndUnit) {
                val screenY = originY - (currentY * pixelsPerUnit).toFloat()
                drawLine(
                    color = Color(0xFF131620),
                    start = Offset(0f, screenY),
                    end = Offset(width, screenY),
                    strokeWidth = 1f
                )
                if (abs(currentY) > 0.001) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = String.format("%.1f", currentY).trimEnd('0').trimEnd('.'),
                        style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp),
                        topLeft = Offset((originX + 6f).coerceIn(4f, width - 40f), screenY - 14f)
                    )
                }
                currentY += step
            }
        }

        // ─── 2. Main Axes (X and Y) ───────────────────────────────────────────
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

        // ─── 3. Integral Area Shading ─────────────────────────────────────────
        val primaryEvaluator = evaluators.firstOrNull { it != null }
        if (showIntegral && mode == GraphMode.CARTESIAN && primaryEvaluator != null) {
            val a = min(integralA, integralB)
            val b = max(integralA, integralB)
            val integralVal = primaryEvaluator.evaluateIntegral(a, b, params)

            val polyPath = Path()
            val numSteps = 120
            val startScreenX = originX + (a * pixelsPerUnit).toFloat()
            val endScreenX = originX + (b * pixelsPerUnit).toFloat()

            polyPath.moveTo(startScreenX, originY)

            for (s in 0..numSteps) {
                val curX = a + s * (b - a) / numSteps
                val curY = primaryEvaluator.evaluate(curX, params)
                if (!curY.isNaN() && !curY.isInfinite()) {
                    val sX = originX + (curX * pixelsPerUnit).toFloat()
                    val sY = originY - (curY * pixelsPerUnit).toFloat()
                    polyPath.lineTo(sX, sY)
                }
            }
            polyPath.lineTo(endScreenX, originY)
            polyPath.close()

            // Draw Area Gradient
            drawPath(
                path = polyPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF38BDF8).copy(alpha = 0.05f)),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw Integral Bounds Markers
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(startScreenX, 0f),
                end = Offset(startScreenX, height),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(endScreenX, 0f),
                end = Offset(endScreenX, height),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )

            // Integral Result Badge
            val intLabel = String.format("∫[%.1f, %.1f] = %.3f", a, b, integralVal)
            val intLayout = textMeasurer.measure(
                text = AnnotatedString(intLabel),
                style = TextStyle(color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            val badgeX = ((startScreenX + endScreenX) / 2 - intLayout.size.width / 2).coerceIn(10f, width - intLayout.size.width - 20f)
            val badgeY = (originY - 40f).coerceIn(10f, height - 30f)

            drawRoundRect(
                color = Color(0xDD0F172A),
                topLeft = Offset(badgeX - 6f, badgeY - 4f),
                size = Size(intLayout.size.width + 12f, intLayout.size.height + 8f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawText(textLayoutResult = intLayout, topLeft = Offset(badgeX, badgeY))
        }

        // ─── 4. Plot Graphs with Neon Glow ────────────────────────────────────
        val criticalPoints = mutableListOf<CriticalPoint>()

        when (mode) {
            GraphMode.CARTESIAN -> {
                evaluators.forEachIndexed { index, evaluator ->
                    if (evaluator != null) {
                        val color = graphColors[index % graphColors.size]
                        val pointsCount = width.toInt().coerceAtLeast(100)
                        val yValues = evaluator.evaluateRange(xStartUnit.toDouble(), xEndUnit.toDouble(), pointsCount, params)

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
                                    if (abs(screenY - prevYScreen) > height * 1.5f) {
                                        path.moveTo(screenX, screenY)
                                    } else {
                                        path.lineTo(screenX, screenY)
                                    }
                                }
                                prevYScreen = screenY

                                // Roots & Extremums for Primary Graph
                                if (index == 0 && i > 0 && i < pointsCount - 1) {
                                    val prevY = yValues[i - 1]
                                    val nextY = yValues[i + 1]

                                    if (showRoots && ((prevY < 0 && mathY >= 0) || (prevY > 0 && mathY <= 0))) {
                                        if (criticalPoints.none { it.isRoot && abs(it.x - mathX) < (xEndUnit - xStartUnit) / 30.0 }) {
                                            criticalPoints.add(CriticalPoint(mathX.toDouble(), 0.0, isRoot = true))
                                        }
                                    }

                                    if (showExtremums) {
                                        if (mathY > prevY && mathY > nextY && abs(mathY) > 0.01) {
                                            criticalPoints.add(CriticalPoint(mathX.toDouble(), mathY, isMax = true))
                                        } else if (mathY < prevY && mathY < nextY && abs(mathY) > 0.01) {
                                            criticalPoints.add(CriticalPoint(mathX.toDouble(), mathY, isMin = true))
                                        }
                                    }
                                }
                            } else {
                                isFirstPoint = true
                            }
                        }

                        // Layer 1: Ambient Neon Glow Path
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.22f),
                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        // Layer 2: Core Crisp Line
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                // Intersections between graphs
                if (showIntersections && evaluators.size >= 2) {
                    val ev1 = evaluators.getOrNull(0)
                    val ev2 = evaluators.getOrNull(1)
                    if (ev1 != null && ev2 != null) {
                        val crossPoints = ev1.findIntersections(ev2, xStartUnit.toDouble(), xEndUnit.toDouble(), 300, params)
                        crossPoints.forEach { (cx, cy) ->
                            criticalPoints.add(CriticalPoint(cx, cy, isIntersection = true))
                        }
                    }
                }
            }

            GraphMode.POLAR -> {
                evaluators.forEachIndexed { index, evaluator ->
                    if (evaluator != null) {
                        val color = graphColors[index % graphColors.size]
                        val polarPoints = evaluator.evaluatePolarRange(0.0, 4.0 * Math.PI, 600, params)

                        val path = Path()
                        var isFirstPoint = true

                        for ((x, y) in polarPoints) {
                            if (!x.isNaN() && !y.isNaN()) {
                                val screenX = originX + (x * pixelsPerUnit).toFloat()
                                val screenY = originY - (y * pixelsPerUnit).toFloat()
                                if (isFirstPoint) {
                                    path.moveTo(screenX, screenY)
                                    isFirstPoint = false
                                } else {
                                    path.lineTo(screenX, screenY)
                                }
                            } else {
                                isFirstPoint = true
                            }
                        }

                        drawPath(path = path, color = color.copy(alpha = 0.22f), style = Stroke(width = 8.dp.toPx()))
                        drawPath(path = path, color = color, style = Stroke(width = 3.dp.toPx()))
                    }
                }
            }

            GraphMode.PARAMETRIC -> {
                if (evaluators.size >= 2 && evaluators[0] != null && evaluators[1] != null) {
                    val evX = evaluators[0]!!
                    val evY = evaluators[1]!!
                    val color = graphColors[0]
                    val paramPoints = evX.evaluateParametricRange(evY, -10.0, 10.0, 800, params)

                    val path = Path()
                    var isFirstPoint = true

                    for ((x, y) in paramPoints) {
                        if (!x.isNaN() && !y.isNaN()) {
                            val screenX = originX + (x * pixelsPerUnit).toFloat()
                            val screenY = originY - (y * pixelsPerUnit).toFloat()
                            if (isFirstPoint) {
                                path.moveTo(screenX, screenY)
                                isFirstPoint = false
                            } else {
                                path.lineTo(screenX, screenY)
                            }
                        } else {
                            isFirstPoint = true
                        }
                    }

                    drawPath(path = path, color = color.copy(alpha = 0.25f), style = Stroke(width = 8.dp.toPx()))
                    drawPath(path = path, color = color, style = Stroke(width = 3.dp.toPx()))
                }
            }
        }

        // ─── 5. Render Critical Points & Intersections ─────────────────────────
        criticalPoints.forEach { cp ->
            val screenX = originX + (cp.x * pixelsPerUnit).toFloat()
            val screenY = originY - (cp.y * pixelsPerUnit).toFloat()

            if (cp.isRoot) {
                drawCircle(color = Color(0xFF06B6D4).copy(alpha = 0.35f), radius = 10.dp.toPx(), center = Offset(screenX, screenY))
                drawCircle(color = Color(0xFF06B6D4), radius = 5.dp.toPx(), center = Offset(screenX, screenY))

                val rootLabel = String.format("x=%.2f", cp.x)
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(rootLabel),
                    style = TextStyle(color = Color(0xFF06B6D4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, screenY + 12f))
            } else if (cp.isMax || cp.isMin) {
                val extColor = if (cp.isMax) Color(0xFFF59E0B) else Color(0xFFEC4899)
                drawCircle(color = extColor.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = Offset(screenX, screenY))
                drawCircle(color = extColor, radius = 4.dp.toPx(), center = Offset(screenX, screenY))

                val extLabel = if (cp.isMax) "max" else "min"
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(extLabel),
                    style = TextStyle(color = extColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, if (cp.isMax) screenY - 20f else screenY + 10f))
            } else if (cp.isIntersection) {
                // Purple/Gold Glowing Diamond Intersection
                val intColor = Color(0xFFA855F7)
                drawCircle(color = intColor.copy(alpha = 0.35f), radius = 11.dp.toPx(), center = Offset(screenX, screenY))
                drawCircle(color = intColor, radius = 5.dp.toPx(), center = Offset(screenX, screenY))

                val crossLabel = String.format("(%.2f, %.2f)", cp.x, cp.y)
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(crossLabel),
                    style = TextStyle(color = intColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, screenY - 22f))
            }
        }

        // ─── 6. Active Touch / Drag Point Inspector & Tangent ─────────────────
        activePoint?.let { point ->
            val screenX = originX + point.x * pixelsPerUnit
            val screenY = originY - point.y * pixelsPerUnit

            // Tangent Line at Inspector Point
            if (showTangent && mode == GraphMode.CARTESIAN && primaryEvaluator != null) {
                val derivative = primaryEvaluator.evaluateDerivative(point.x.toDouble(), params)
                if (!derivative.isNaN() && !derivative.isInfinite()) {
                    val deltaX = 120.dp.toPx()
                    val tanStartX = screenX - deltaX
                    val tanEndX = screenX + deltaX
                    val tanStartY = screenY + (deltaX * derivative).toFloat()
                    val tanEndY = screenY - (deltaX * derivative).toFloat()

                    drawLine(
                        color = Color(0xFFF59E0B),
                        start = Offset(tanStartX, tanStartY),
                        end = Offset(tanEndX, tanEndY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    // Derivative Badge
                    val derivText = String.format("dy/dx = %.3f", derivative)
                    val derivLayout = textMeasurer.measure(
                        text = AnnotatedString(derivText),
                        style = TextStyle(color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    drawRoundRect(
                        color = Color(0xEE1E293B),
                        topLeft = Offset(screenX - derivLayout.size.width / 2 - 4f, screenY + 18f),
                        size = Size(derivLayout.size.width + 8f, derivLayout.size.height + 4f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawText(textLayoutResult = derivLayout, topLeft = Offset(screenX - derivLayout.size.width / 2, screenY + 20f))
                }
            }

            // Crosshair Guidelines
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

            // Inspector Cursor Dot
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

            // Haptic check near critical points
            criticalPoints.firstOrNull { abs(it.x - point.x) < 0.06 }?.let { cp ->
                if (lastHapticPoint == null || abs(lastHapticPoint!! - cp.x) > 0.1) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    lastHapticPoint = cp.x
                }
            }
        }
    }
}

