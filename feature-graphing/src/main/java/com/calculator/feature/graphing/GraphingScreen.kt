package com.calculator.feature.graphing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import com.calculator.engine.solver.GraphEvaluator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphingScreen(
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(GraphMode.CARTESIAN) }
    var expressions by remember { mutableStateOf(listOf("sin(x)", "0.5*x - 1")) }

    // Analysis Toggles
    var showRoots by rememberSaveable { mutableStateOf(true) }
    var showExtremums by rememberSaveable { mutableStateOf(true) }
    var showIntersections by rememberSaveable { mutableStateOf(true) }
    var showTangent by rememberSaveable { mutableStateOf(false) }
    var showIntegral by rememberSaveable { mutableStateOf(false) }
    var integralA by remember { mutableStateOf(0.0) }
    var integralB by remember { mutableStateOf(2.0) }

    // Controls and Modals
    var resetTrigger by remember { mutableStateOf(0) }
    var showPresetsSheet by remember { mutableStateOf(false) }
    var showValuesTable by remember { mutableStateOf(false) }

    // Dynamic Parameter Slider State (e.g., 'a', 'b', 'c', 'k')
    var paramValues by remember {
        mutableStateOf(
            mapOf("a" to 1.0, "b" to 1.0, "c" to 0.0, "k" to 1.0)
        )
    }

    // Auto-detect any unknown variable parameters across all active expressions
    val detectedParams = remember(expressions, mode) {
        val list = mutableListOf<String>()
        val exclude = when (mode) {
            GraphMode.CARTESIAN -> setOf("x", "pi", "e")
            GraphMode.POLAR -> setOf("theta", "th", "pi", "e")
            GraphMode.PARAMETRIC -> setOf("t", "pi", "e")
        }
        for (expr in expressions) {
            try {
                if (expr.isNotBlank()) {
                    val ev = GraphEvaluator(expr)
                    ev.extractParameters(exclude).forEach { p ->
                        if (p !in list) list.add(p)
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors while typing
            }
        }
        list
    }

    val graphColors = listOf(
        Color(0xFFFFFFFF), // Titanium White
        Color(0xFF38BDF8), // Ice Blue
        Color(0xFFA855F7), // Purple
        Color(0xFF10B981)  // Emerald
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ─── 1. Header Card: Mode Selector & Calculus Toggles ──────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Coordinate System Mode Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Pair(GraphMode.CARTESIAN, "y = f(x)"),
                        Pair(GraphMode.POLAR, "r = f(θ)"),
                        Pair(GraphMode.PARAMETRIC, "x(t), y(t)")
                    ).forEach { (m, title) ->
                        val isSelected = mode == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White else SurfaceElevated)
                                .clickable {
                                    mode = m
                                    expressions = when (m) {
                                        GraphMode.CARTESIAN -> listOf("sin(x)", "0.5*x - 1")
                                        GraphMode.POLAR -> listOf("cos(2*theta)")
                                        GraphMode.PARAMETRIC -> listOf("sin(3*t)", "sin(2*t)")
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Background else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Presets & Table Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated)
                            .clickable { showPresetsSheet = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CollectionsBookmark,
                                contentDescription = "Пресеты",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text("Библиотека", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated)
                            .clickable { showValuesTable = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TableChart,
                                contentDescription = "Таблица",
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(13.dp)
                            )
                            Text("Таблица", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Function Expression Inputs
            expressions.forEachIndexed { index, expr ->
                val color = graphColors[index % graphColors.size]
                val placeholderText = when (mode) {
                    GraphMode.CARTESIAN -> "y${index + 1} = ..."
                    GraphMode.POLAR -> "r${index + 1}(θ) = ..."
                    GraphMode.PARAMETRIC -> if (index == 0) "x(t) = ..." else "y(t) = ..."
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(color)
                    )

                    OutlinedTextField(
                        value = expr,
                        onValueChange = { newText ->
                            val updated = expressions.toMutableList()
                            updated[index] = newText
                            expressions = updated
                        },
                        placeholder = { Text(placeholderText, color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceElevated,
                            unfocusedContainerColor = SurfaceElevated,
                            focusedBorderColor = color,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    if (expressions.size > 1 && mode != GraphMode.PARAMETRIC) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevated)
                                .clickable {
                                    val updated = expressions.toMutableList()
                                    updated.removeAt(index)
                                    expressions = updated
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (expressions.size < 3 && mode != GraphMode.PARAMETRIC) {
                Button(
                    onClick = { expressions = expressions + "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Добавить функцию", color = Color.White, fontSize = 11.sp)
                }
            }

            // ─── 2. Calculus Feature Toggles Bar ──────────────────────────────
            if (mode == GraphMode.CARTESIAN) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Roots
                    item {
                        CalculusTogglePill(
                            label = "🎯 Корни",
                            isActive = showRoots,
                            activeColor = Color(0xFF06B6D4),
                            onClick = { showRoots = !showRoots }
                        )
                    }
                    // Extremums
                    item {
                        CalculusTogglePill(
                            label = "📊 Экстремумы",
                            isActive = showExtremums,
                            activeColor = Color(0xFFF59E0B),
                            onClick = { showExtremums = !showExtremums }
                        )
                    }
                    // Intersections
                    item {
                        CalculusTogglePill(
                            label = "✖ Пересечения",
                            isActive = showIntersections,
                            activeColor = Color(0xFFA855F7),
                            onClick = { showIntersections = !showIntersections }
                        )
                    }
                    // Tangent
                    item {
                        CalculusTogglePill(
                            label = "📐 Касательная",
                            isActive = showTangent,
                            activeColor = Color(0xFFF59E0B),
                            onClick = { showTangent = !showTangent }
                        )
                    }
                    // Definite Integral
                    item {
                        CalculusTogglePill(
                            label = "🧮 Интеграл ∫",
                            isActive = showIntegral,
                            activeColor = Color(0xFF38BDF8),
                            onClick = { showIntegral = !showIntegral }
                        )
                    }
                }
            }

            // ─── 3. Real-Time Parameter Sliders (Dynamic a, b, c) ──────────────
            AnimatedVisibility(
                visible = detectedParams.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🎛 Интерактивные коэффициенты (Sliders):",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    detectedParams.forEach { paramName ->
                        val currentVal = paramValues[paramName] ?: 1.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$paramName = ${String.format("%.1f", currentVal)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(62.dp)
                            )
                            Slider(
                                value = currentVal.toFloat(),
                                onValueChange = { newVal ->
                                    paramValues = paramValues + (paramName to newVal.toDouble())
                                },
                                valueRange = -5f..5f,
                                steps = 99,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = SurfaceBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        // ─── 4. Graph Canvas Viewport with Float Controls ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
        ) {
            GraphCanvas(
                expressions = expressions.filter { it.isNotBlank() },
                mode = mode,
                params = paramValues,
                showRoots = showRoots,
                showExtremums = showExtremums,
                showIntersections = showIntersections,
                showTangent = showTangent,
                showIntegral = showIntegral,
                integralA = integralA,
                integralB = integralB,
                resetTrigger = resetTrigger,
                modifier = Modifier.fillMaxSize()
            )

            // Floating Home / Reset Origin Button
            FloatingSnapOriginButton(
                onClick = { resetTrigger++ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }
    }

    // ─── 5. Presets Bottom Sheet ───────────────────────────────────────────────
    if (showPresetsSheet) {
        PresetsSheet(
            onSelectPreset = { preset ->
                mode = preset.mode
                expressions = preset.expressions
            },
            onDismiss = { showPresetsSheet = false }
        )
    }

    // ─── 6. X-Y Values Table Dialog ───────────────────────────────────────────
    if (showValuesTable) {
        ValuesTableDialog(
            expressions = expressions.filter { it.isNotBlank() },
            params = paramValues,
            onDismiss = { showValuesTable = false }
        )
    }
}

@Composable
private fun CalculusTogglePill(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.2f) else SurfaceElevated)
            .border(1.dp, if (isActive) activeColor else SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) activeColor else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FloatingSnapOriginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xEE1E293B))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CenterFocusStrong,
            contentDescription = "Сброс к центру (0,0)",
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(20.dp)
        )
    }
}

