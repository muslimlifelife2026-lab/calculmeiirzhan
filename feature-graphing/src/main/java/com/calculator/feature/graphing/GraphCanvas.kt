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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.utils.NumberFormatter
import com.calculator.engine.solver.GraphEvaluator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

data class CriticalPoint(
    val x: Double,
    val y: Double,
    val isRoot: Boolean = false,
    val isMax: Boolean = false,
    val isMin: Boolean = false,
    val isIntersection: Boolean = false
)

private data class GraphCursor(val x: Double, val y: Double)

private data class GraphRenderData(
    val cartesianXMin: Double = 0.0,
    val cartesianXMax: Double = 0.0,
    val cartesianSamples: Map<Int, DoubleArray> = emptyMap(),
    val polarCurves: Map<Int, List<Pair<Double, Double>>> = emptyMap(),
    val parametricPoints: List<Pair<Double, Double>> = emptyList(),
    val criticalPoints: List<CriticalPoint> = emptyList(),
    val integralValue: Double = Double.NaN,
    val integralPoints: List<Pair<Double, Double>> = emptyList()
)

private fun formatAxisTick(value: Double): String {
    val text = String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
    return if (text == "-0") "0" else text
}

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

    var savedScale by rememberSaveable { mutableFloatStateOf(1f) }
    var savedOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var savedOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    val animScale = remember { Animatable(savedScale) }
    val animOffsetX = remember { Animatable(savedOffsetX) }
    val animOffsetY = remember { Animatable(savedOffsetY) }

    var activePoint by remember { mutableStateOf<GraphCursor?>(null) }
    var lastHapticPoint by remember { mutableStateOf<Double?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(expressions, params, mode) {
        activePoint = null
        lastHapticPoint = null
    }

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            savedScale = 1f
            savedOffsetX = 0f
            savedOffsetY = 0f
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
            } catch (_: Exception) {
                null
            }
        }
    }

    var renderData by remember { mutableStateOf(GraphRenderData()) }
    LaunchedEffect(
        evaluators, mode, params, showRoots, showExtremums,
        showIntersections, showIntegral, integralA, integralB, canvasSize
    ) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect
        snapshotFlow { Triple(animScale.value, animOffsetX.value, animOffsetY.value) }
            .collectLatest { (scale, offsetX, offsetY) ->
                val width = canvasSize.width.toFloat()
                val height = canvasSize.height.toFloat()
                val pixelsPerUnit = 50f * scale
                val originX = width / 2f + offsetX
                val visXStart = (-originX / pixelsPerUnit).toDouble()
                val visXEnd = ((width - originX) / pixelsPerUnit).toDouble()
                val viewYSpan = max(1e-6, (height / pixelsPerUnit).toDouble())

                val computed = withContext(Dispatchers.Default) {
                    when (mode) {
                        GraphMode.CARTESIAN -> {
                            val xSpan = (visXEnd - visXStart).coerceAtLeast(1e-6)
                            val sampleMin = visXStart - xSpan * 0.35
                            val sampleMax = visXEnd + xSpan * 0.35
                            val pointsCount = (width * 1.4f).toInt().coerceIn(200, 1200)
                            val sampleStep = (sampleMax - sampleMin) / (pointsCount - 1)

                            val samples = mutableMapOf<Int, DoubleArray>()
                            val criticals = mutableListOf<CriticalPoint>()

                            evaluators.forEachIndexed { index, evaluator ->
                                if (evaluator != null) {
                                    val yValues = evaluator.evaluateRange(sampleMin, sampleMax, pointsCount, params)
                                    samples[index] = yValues
                                    if (index == 0 && showExtremums) {
                                        var lastExtremumX: Double? = null
                                        for (i in 2 until pointsCount - 2) {
                                            val yM2 = yValues[i - 2]
                                            val prevY = yValues[i - 1]
                                            val mathY = yValues[i]
                                            val nextY = yValues[i + 1]
                                            val yP2 = yValues[i + 2]
                                            if (!yM2.isFinite() || !prevY.isFinite() || !mathY.isFinite() ||
                                                !nextY.isFinite() || !yP2.isFinite()
                                            ) continue
                                            if (abs(mathY - prevY) > viewYSpan * 0.5 ||
                                                abs(nextY - mathY) > viewYSpan * 0.5 ||
                                                abs(mathY) > viewYSpan * 4.0
                                            ) continue
                                            val isMax = mathY > prevY && mathY > nextY
                                            val isMin = mathY < prevY && mathY < nextY
                                            if (!isMax && !isMin) continue
                                            val mathX = sampleMin + i * sampleStep
                                            if (lastExtremumX != null && mathX - lastExtremumX <= sampleStep * 4.0) continue
                                            criticals.add(CriticalPoint(mathX, mathY, isMax = isMax, isMin = isMin))
                                            lastExtremumX = mathX
                                        }
                                    }
                                    if (index == 0 && showRoots) {
                                        evaluator.findRoots(visXStart, visXEnd, 400, params).forEach { rx ->
                                            criticals.add(CriticalPoint(rx, 0.0, isRoot = true))
                                        }
                                    }
                                }
                            }

                            if (showIntersections && evaluators.size >= 2) {
                                val ev1 = evaluators.getOrNull(0)
                                val ev2 = evaluators.getOrNull(1)
                                if (ev1 != null && ev2 != null) {
                                    ev1.findIntersections(ev2, visXStart, visXEnd, 300, params).forEach { (cx, cy) ->
                                        criticals.add(CriticalPoint(cx, cy, isIntersection = true))
                                    }
                                }
                            }

                            var intVal = Double.NaN
                            var intSamples = emptyList<Pair<Double, Double>>()
                            val primaryEvaluator = evaluators.firstOrNull { it != null }
                            if (showIntegral && primaryEvaluator != null) {
                                val a = min(integralA, integralB)
                                val b = max(integralA, integralB)
                                intVal = primaryEvaluator.evaluateIntegral(integralA, integralB, params)
                                if (intVal.isFinite()) {
                                    val numSteps = 120
                                    intSamples = (0..numSteps).mapNotNull { s ->
                                        val curX = a + s * (b - a) / numSteps
                                        val curY = primaryEvaluator.evaluate(curX, params)
                                        if (curY.isFinite()) curX to curY else null
                                    }
                                }
                            }

                            GraphRenderData(
                                cartesianXMin = sampleMin,
                                cartesianXMax = sampleMax,
                                cartesianSamples = samples,
                                criticalPoints = criticals,
                                integralValue = intVal,
                                integralPoints = intSamples
                            )
                        }

                        GraphMode.POLAR -> {
                            val polarMap = mutableMapOf<Int, List<Pair<Double, Double>>>()
                            evaluators.forEachIndexed { index, evaluator ->
                                if (evaluator != null) {
                                    polarMap[index] = evaluator.evaluatePolarRange(0.0, 4.0 * Math.PI, 600, params)
                                }
                            }
                            GraphRenderData(polarCurves = polarMap)
                        }

                        GraphMode.PARAMETRIC -> {
                            val evX = evaluators.getOrNull(0)
                            val evY = evaluators.getOrNull(1)
                            val pts = if (evX != null && evY != null) {
                                evX.evaluateParametricRange(evY, -10.0, 10.0, 800, params)
                            } else emptyList()
                            GraphRenderData(parametricPoints = pts)
                        }
                    }
                }
                renderData = computed
            }
    }

    val activeDerivative by produceState<Double?>(null, activePoint, showTangent, mode, evaluators, params) {
        value = if (showTangent && mode == GraphMode.CARTESIAN && activePoint != null) {
            val ev = evaluators.firstOrNull { it != null } ?: return@produceState
            val xVal = activePoint!!.x
            withContext(Dispatchers.Default) {
                ev.evaluateDerivative(xVal, params).takeIf { it.isFinite() }
            }
        } else {
            null
        }
    }

    LaunchedEffect(activePoint, renderData.criticalPoints) {
        val point = activePoint ?: return@LaunchedEffect
        renderData.criticalPoints.firstOrNull { abs(it.x - point.x) < 0.06 && abs(it.y - point.y) < 0.15 }?.let { cp ->
            if (lastHapticPoint == null || abs(lastHapticPoint!! - cp.x) > 0.1) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                lastHapticPoint = cp.x
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()

    val graphColors = remember {
        listOf(
            Color(0xFFFFFFFF), // Titanium White
            Color(0xFFB9C6D6), // Ice Blue
            Color(0xFFB9ADC8), // Cyber Purple
            Color(0xFFACC4B0)  // Emerald
        )
    }
    val dashGridEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }
    val dashIntegralEffect = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) }
    val dashTangentEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) }
    val dashCrosshairEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }
    val integralBrush = remember(canvasSize.height) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFB9C6D6).copy(alpha = 0.35f), Color(0xFFB9C6D6).copy(alpha = 0.05f)),
            startY = 0f,
            endY = canvasSize.height.toFloat().coerceAtLeast(1f)
        )
    }

    val currentEvaluators by rememberUpdatedState(evaluators)
    val currentRenderData by rememberUpdatedState(renderData)
    val currentMode by rememberUpdatedState(mode)
    val currentParams by rememberUpdatedState(params)

    fun updateCursorForPointer(pointer: Offset, widthPx: Int, heightPx: Int, maxDistPx: Float, clearOnInvalidCartesian: Boolean) {
        val scale = animScale.value
        val offsetX = animOffsetX.value
        val offsetY = animOffsetY.value
        val pixelsPerUnit = 50f * scale
        val originX = widthPx / 2f + offsetX
        val originY = heightPx / 2f + offsetY
        when (currentMode) {
            GraphMode.CARTESIAN -> {
                val primaryEvaluator = currentEvaluators.firstOrNull { it != null }
                if (primaryEvaluator != null) {
                    val mathX = ((pointer.x - originX) / pixelsPerUnit).toDouble()
                    val mathY = primaryEvaluator.evaluate(mathX, currentParams)
                    if (mathY.isFinite()) {
                        activePoint = GraphCursor(mathX, mathY)
                    } else if (clearOnInvalidCartesian) {
                        activePoint = null
                    }
                } else if (clearOnInvalidCartesian) {
                    activePoint = null
                }
            }
            GraphMode.POLAR, GraphMode.PARAMETRIC -> {
                val points = if (currentMode == GraphMode.POLAR) {
                    currentRenderData.polarCurves.values.firstOrNull().orEmpty()
                } else {
                    currentRenderData.parametricPoints
                }
                var bestDist = Float.MAX_VALUE
                var bestPoint: GraphCursor? = null
                for ((px, py) in points) {
                    if (!px.isFinite() || !py.isFinite()) continue
                    val sx = originX + (px * pixelsPerUnit).toFloat()
                    val sy = originY - (py * pixelsPerUnit).toFloat()
                    val dist = hypot(sx - pointer.x, sy - pointer.y)
                    if (dist < bestDist) {
                        bestDist = dist
                        bestPoint = GraphCursor(px, py)
                    }
                }
                activePoint = if (bestDist <= maxDistPx) bestPoint else null
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .background(Color(0xFF161719)) // Pure Obsidian Canvas
            .pointerInput(Unit) {
                val snapRadius = 48.dp.toPx()
                detectTapGestures { pressOffset ->
                    updateCursorForPointer(pressOffset, size.width, size.height, snapRadius, clearOnInvalidCartesian = true)
                }
            }
            .pointerInput(Unit) {
                val snapRadius = 48.dp.toPx()
                detectDragGestures(
                    onDragStart = { startOffset ->
                        updateCursorForPointer(startOffset, size.width, size.height, snapRadius, clearOnInvalidCartesian = false)
                    },
                    onDrag = { change, _ ->
                        updateCursorForPointer(change.position, size.width, size.height, snapRadius, clearOnInvalidCartesian = false)
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
                        savedScale = animScale.value
                        savedOffsetX = animOffsetX.value
                        savedOffsetY = animOffsetY.value
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
        val yStartUnit = -(height - originY) / pixelsPerUnit
        val yEndUnit = originY / pixelsPerUnit

        val step: Double = when {
            scale < 0.35f -> 10.0
            scale < 0.75f -> 5.0
            scale > 3.5f -> 0.2
            scale > 1.8f -> 0.5
            else -> 1.0
        }

        // ─── 1. Grid Rendering ────────────────────────────────────────────────
        if (mode == GraphMode.POLAR) {
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
                    text = "r=${formatAxisTick(r)}",
                    style = TextStyle(color = Color(0xFF475569), fontSize = 9.sp),
                    topLeft = Offset(originX + radiusPx + 3f, originY + 2f)
                )
                r += step
            }

            val angles = listOf(0.0, Math.PI / 6, Math.PI / 4, Math.PI / 3, Math.PI / 2, 2 * Math.PI / 3, 3 * Math.PI / 4, 5 * Math.PI / 6, Math.PI)
            val rayLen = max(width, height) * 2f
            for (ang in angles) {
                val cosA = cos(ang).toFloat()
                val sinA = sin(ang).toFloat()
                drawLine(
                    color = Color(0xFF12151E),
                    start = Offset(originX - cosA * rayLen, originY + sinA * rayLen),
                    end = Offset(originX + cosA * rayLen, originY - sinA * rayLen),
                    strokeWidth = 1f,
                    pathEffect = dashGridEffect
                )
            }
        } else {
            var currentX: Double = floor(xStartUnit.toDouble() / step) * step
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
                        text = formatAxisTick(currentX),
                        style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp),
                        topLeft = Offset(screenX + 4f, (originY + 4f).coerceIn(4f, height - 20f))
                    )
                }
                currentX += step
            }

            var currentY: Double = floor(yStartUnit.toDouble() / step) * step
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
                        text = formatAxisTick(currentY),
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
            val integralVal = renderData.integralValue
            val startScreenX = originX + (a * pixelsPerUnit).toFloat()
            val endScreenX = originX + (b * pixelsPerUnit).toFloat()

            if (integralVal.isFinite() && renderData.integralPoints.isNotEmpty()) {
                val polyPath = Path()
                polyPath.moveTo(startScreenX, originY)
                for ((curX, curY) in renderData.integralPoints) {
                    val sX = originX + (curX * pixelsPerUnit).toFloat()
                    val sY = originY - (curY * pixelsPerUnit).toFloat()
                    polyPath.lineTo(sX, sY)
                }
                polyPath.lineTo(endScreenX, originY)
                polyPath.close()
                drawPath(path = polyPath, brush = integralBrush)
            }

            drawLine(
                color = Color(0xFFB9C6D6),
                start = Offset(startScreenX, 0f),
                end = Offset(startScreenX, height),
                strokeWidth = 1.2f,
                pathEffect = dashIntegralEffect
            )
            drawLine(
                color = Color(0xFFB9C6D6),
                start = Offset(endScreenX, 0f),
                end = Offset(endScreenX, height),
                strokeWidth = 1.2f,
                pathEffect = dashIntegralEffect
            )

            val intLabel = "∫[${NumberFormatter.formatCompact(integralA)}, ${NumberFormatter.formatCompact(integralB)}] " +
                if (integralVal.isFinite()) "≈ ${NumberFormatter.formatCompact(integralVal)}" else "не определён"
            val intLayout = textMeasurer.measure(
                text = AnnotatedString(intLabel),
                style = TextStyle(color = Color(0xFFB9C6D6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            val badgeX = ((startScreenX + endScreenX) / 2 - intLayout.size.width / 2)
                .coerceIn(10f, max(10f, width - intLayout.size.width - 20f))
            val badgeY = (originY - 40f).coerceIn(10f, max(10f, height - 30f))

            drawRoundRect(
                color = Color(0xDD0F172A),
                topLeft = Offset(badgeX - 6f, badgeY - 4f),
                size = Size(intLayout.size.width + 12f, intLayout.size.height + 8f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawText(textLayoutResult = intLayout, topLeft = Offset(badgeX, badgeY))
        }

        // ─── 4. Plot graphs ────────────────────────────────────────────────────
        val jumpThreshold = hypot(width, height) * 1.2f
        when (mode) {
            GraphMode.CARTESIAN -> {
                val sampleMin = renderData.cartesianXMin
                val sampleMax = renderData.cartesianXMax
                renderData.cartesianSamples.forEach { (index, yValues) ->
                    val pointsCount = yValues.size
                    if (pointsCount >= 2 && sampleMax > sampleMin) {
                        val color = graphColors[index % graphColors.size]
                        val path = Path()
                        var isFirstPoint = true
                        var prevYScreen = 0f

                        for (i in 0 until pointsCount) {
                            val mathY = yValues[i]
                            if (mathY.isFinite()) {
                                val mathX = sampleMin + i * (sampleMax - sampleMin) / (pointsCount - 1)
                                val screenX = originX + (mathX * pixelsPerUnit).toFloat()
                                val screenY = originY - (mathY * pixelsPerUnit).toFloat()

                                if (isFirstPoint || abs(screenY - prevYScreen) > height * 1.5f) {
                                    path.moveTo(screenX, screenY)
                                    isFirstPoint = false
                                } else {
                                    path.lineTo(screenX, screenY)
                                }
                                prevYScreen = screenY
                            } else {
                                isFirstPoint = true
                            }
                        }

                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            GraphMode.POLAR -> {
                renderData.polarCurves.forEach { (index, polarPoints) ->
                    val color = graphColors[index % graphColors.size]
                    val path = Path()
                    var isFirstPoint = true
                    var prevXScreen = 0f
                    var prevYScreen = 0f

                    for ((x, y) in polarPoints) {
                        if (x.isFinite() && y.isFinite()) {
                            val screenX = originX + (x * pixelsPerUnit).toFloat()
                            val screenY = originY - (y * pixelsPerUnit).toFloat()
                            if (isFirstPoint || hypot(screenX - prevXScreen, screenY - prevYScreen) > jumpThreshold) {
                                path.moveTo(screenX, screenY)
                                isFirstPoint = false
                            } else {
                                path.lineTo(screenX, screenY)
                            }
                            prevXScreen = screenX
                            prevYScreen = screenY
                        } else {
                            isFirstPoint = true
                        }
                    }

                    drawPath(path = path, color = color.copy(alpha = 0.22f), style = Stroke(width = 8.dp.toPx()))
                    drawPath(path = path, color = color, style = Stroke(width = 3.dp.toPx()))
                }
            }

            GraphMode.PARAMETRIC -> {
                val paramPoints = renderData.parametricPoints
                if (paramPoints.isNotEmpty()) {
                    val color = graphColors[0]
                    val path = Path()
                    var isFirstPoint = true
                    var prevXScreen = 0f
                    var prevYScreen = 0f

                    for ((x, y) in paramPoints) {
                        if (x.isFinite() && y.isFinite()) {
                            val screenX = originX + (x * pixelsPerUnit).toFloat()
                            val screenY = originY - (y * pixelsPerUnit).toFloat()
                            if (isFirstPoint || hypot(screenX - prevXScreen, screenY - prevYScreen) > jumpThreshold) {
                                path.moveTo(screenX, screenY)
                                isFirstPoint = false
                            } else {
                                path.lineTo(screenX, screenY)
                            }
                            prevXScreen = screenX
                            prevYScreen = screenY
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
        renderData.criticalPoints.forEach { cp ->
            val screenX = originX + (cp.x * pixelsPerUnit).toFloat()
            val screenY = originY - (cp.y * pixelsPerUnit).toFloat()
            if (!screenX.isFinite() || !screenY.isFinite()) return@forEach

            if (cp.isRoot) {
                drawCircle(color = Color(0xFFB9C6D6).copy(alpha = 0.22f), radius = 10.dp.toPx(), center = Offset(screenX, screenY))
                drawCircle(color = Color(0xFFB9C6D6), radius = 5.dp.toPx(), center = Offset(screenX, screenY))

                val rootLabel = "x≈${NumberFormatter.formatCompact(cp.x)}"
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(rootLabel),
                    style = TextStyle(color = Color(0xFFB9C6D6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, screenY + 12f))
            } else if (cp.isMax || cp.isMin) {
                val extColor = if (cp.isMax) Color(0xFFD0C3AD) else Color(0xFFB9ADC8)
                drawCircle(color = extColor.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = Offset(screenX, screenY))
                drawCircle(color = extColor, radius = 4.dp.toPx(), center = Offset(screenX, screenY))

                val extLabel = if (cp.isMax) "max" else "min"
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(extLabel),
                    style = TextStyle(color = extColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, if (cp.isMax) screenY - 20f else screenY + 10f))
            } else if (cp.isIntersection) {
                val intColor = Color(0xFFB9ADC8)
                drawCircle(color = intColor.copy(alpha = 0.35f), radius = 11.dp.toPx(), center = Offset(screenX, screenY))
                drawCircle(color = intColor, radius = 5.dp.toPx(), center = Offset(screenX, screenY))

                val crossLabel = "≈ (${NumberFormatter.formatCompact(cp.x)}, ${NumberFormatter.formatCompact(cp.y)})"
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(crossLabel),
                    style = TextStyle(color = intColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayoutResult = textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, screenY - 22f))
            }
        }

        // ─── 6. Active Touch / Drag Point Inspector & Tangent ─────────────────
        activePoint?.let { point ->
            val screenX = originX + (point.x * pixelsPerUnit).toFloat()
            val screenY = originY - (point.y * pixelsPerUnit).toFloat()
            if (!screenX.isFinite() || !screenY.isFinite()) return@let

            val derivative = activeDerivative
            if (showTangent && mode == GraphMode.CARTESIAN && derivative != null && derivative.isFinite()) {
                val deltaX = 120.dp.toPx()
                val tanStartX = screenX - deltaX
                val tanEndX = screenX + deltaX
                val tanStartY = screenY + (deltaX * derivative).toFloat()
                val tanEndY = screenY - (deltaX * derivative).toFloat()

                drawLine(
                    color = Color(0xFFD0C3AD),
                    start = Offset(tanStartX, tanStartY),
                    end = Offset(tanEndX, tanEndY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = dashTangentEffect
                )

                val derivText = "dy/dx ≈ ${NumberFormatter.formatCompact(derivative)}"
                val derivLayout = textMeasurer.measure(
                    text = AnnotatedString(derivText),
                    style = TextStyle(color = Color(0xFFD0C3AD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawRoundRect(
                    color = Color(0xEE1E293B),
                    topLeft = Offset(screenX - derivLayout.size.width / 2 - 4f, screenY + 18f),
                    size = Size(derivLayout.size.width + 8f, derivLayout.size.height + 4f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawText(textLayoutResult = derivLayout, topLeft = Offset(screenX - derivLayout.size.width / 2, screenY + 20f))
            }

            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(screenX, 0f),
                end = Offset(screenX, height),
                strokeWidth = 1f,
                pathEffect = dashCrosshairEffect
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, screenY),
                end = Offset(width, screenY),
                strokeWidth = 1f,
                pathEffect = dashCrosshairEffect
            )

            drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = Offset(screenX, screenY))
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(screenX, screenY))

            val tooltipText = "x: ${NumberFormatter.formatCompact(point.x)}\ny: ${NumberFormatter.formatCompact(point.y)}"
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

