package com.calculator.feature.geometry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.domain.model.Formula
import com.calculator.domain.model.Variable
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.VariableInput
import com.calculator.core.ui.components.PremiumOverlay
import com.calculator.core.ui.components.PremiumTimerBadge
import com.calculator.core.ui.premium.PremiumManager
import com.calculator.core.ui.theme.NeonCyan
import com.calculator.core.ui.theme.ElectricViolet
import com.calculator.core.ui.theme.TextPrimary
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.core.ui.R

@Composable
fun GeometryScreen(
    modifier: Modifier = Modifier,
    viewModel: GeometryViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val premiumManager = remember { PremiumManager(context) }
    var isPremiumActive by remember { mutableStateOf(premiumManager.isPremiumActive()) }

    val formulas by viewModel.formulas.collectAsState()
    val selectedFormula by viewModel.selectedFormula.collectAsState()
    val inputs by viewModel.inputs.collectAsState()
    val inputUnits by viewModel.inputUnits.collectAsState()
    val targetSymbol by viewModel.targetVariableSymbol.collectAsState()
    val calculationResult by viewModel.calculationResult.collectAsState()

    var showShapeSelector by remember { mutableStateOf(false) }
    var showSteps by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedFormula) {
        showSteps = false
    }

    LaunchedEffect(calculationResult) {
        if (calculationResult?.success == true) {
            delay(100)
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Deep Indigo
            Color(0xFF020617)  // Obsidian Black
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.calculator.core.ui.theme.Background)
            .padding(horizontal = 16.dp)
    ) {
        if (showShapeSelector) {
            AlertDialog(
                onDismissRequest = { showShapeSelector = false },
                title = { Text(text = stringResource(R.string.geom_select_shape), color = TextPrimary) },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        items(formulas) { formula ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectFormula(formula)
                                        showShapeSelector = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            ) {
                                Text(text = formula.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(text = formula.subcategory, color = TextSecondary, fontSize = 11.sp)
                            }
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showShapeSelector = false }) {
                        Text(stringResource(R.string.btn_close), color = NeonCyan)
                    }
                },
                containerColor = com.calculator.core.ui.theme.SurfaceCard
            )
        }

        // FULLY SCROLLABLE LAYOUT
        selectedFormula?.let { formula ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
            ) {
                // 1. Selector Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showShapeSelector = true }
                            .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.geom_title, formula.subcategory),
                                color = com.calculator.core.ui.theme.AccentViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formula.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(R.string.label_select),
                            color = com.calculator.core.ui.theme.AccentAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Interactive Dynamic Geometry Canvas (2D vs 3D Stereometry)
                item {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    
                    val is3DShape = formula.id.contains("sphere") || formula.id.contains("cylinder") || formula.id.contains("cone") || formula.id.contains("cube") || formula.id.contains("pyramid")

                    if (is3DShape) {
                        val shape3DType = when {
                            formula.id.contains("sphere") -> Shape3DType.SPHERE
                            formula.id.contains("cylinder") -> Shape3DType.CYLINDER
                            formula.id.contains("cone") -> Shape3DType.CONE
                            formula.id.contains("pyramid") -> Shape3DType.PYRAMID
                            else -> Shape3DType.CUBE
                        }

                        val paramA = (inputs["r"] ?: inputs["a"] ?: "80").toFloatOrNull() ?: 80f
                        val paramB = (inputs["h"] ?: inputs["b"] ?: "80").toFloatOrNull() ?: 80f
                        val paramC = (inputs["c"] ?: "80").toFloatOrNull() ?: 80f

                        Stereometry3DCanvas(
                            shapeType = shape3DType,
                            paramA = paramA * 10f,
                            paramB = paramB * 10f,
                            paramC = paramC * 10f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // State to track control point offsets
                        var circleRadiusState by remember(formula.id) { mutableStateOf(50.dp) }
                        var rectWidthState by remember(formula.id) { mutableStateOf(120.dp) }
                        var rectHeightState by remember(formula.id) { mutableStateOf(80.dp) }
                        var triangleBaseState by remember(formula.id) { mutableStateOf(120.dp) }
                        var triangleHeightState by remember(formula.id) { mutableStateOf(90.dp) }

                        // Sync state from manual text inputs
                        LaunchedEffect(inputs, targetSymbol) {
                            inputs["r"]?.toFloatOrNull()?.let { circleRadiusState = (it * 10).coerceIn(20f, 120f).dp }
                            inputs["R"]?.toFloatOrNull()?.let { circleRadiusState = (it * 10).coerceIn(20f, 120f).dp }
                            inputs["a"]?.toFloatOrNull()?.let { 
                                rectWidthState = (it * 15).coerceIn(40f, 200f).dp
                                triangleBaseState = (it * 15).coerceIn(40f, 200f).dp
                            }
                            inputs["b"]?.toFloatOrNull()?.let { rectHeightState = (it * 15).coerceIn(30f, 120f).dp }
                            inputs["h"]?.toFloatOrNull()?.let { triangleHeightState = (it * 15).coerceIn(30f, 120f).dp }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x0AFFFFFF))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .pointerInput(formula.id, targetSymbol) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val dragX = dragAmount.x / density.density
                                            val dragY = dragAmount.y / density.density

                                            if (formula.id.contains("circle")) {
                                                val newRadius = (circleRadiusState.value + dragX).coerceIn(20f, 120f)
                                                circleRadiusState = newRadius.dp
                                                val mathVal = String.format("%.2f", newRadius / 10f).replace(",", ".")
                                                if (targetSymbol != "r" && targetSymbol != "R") {
                                                    if (formula.variables.any { it.symbol == "r" }) viewModel.onInputValueChange("r", mathVal)
                                                    if (formula.variables.any { it.symbol == "R" }) viewModel.onInputValueChange("R", mathVal)
                                                }
                                            } else if (formula.id.contains("rectangle") || formula.id.contains("parallelogram")) {
                                                val newW = (rectWidthState.value + dragX).coerceIn(40f, 200f)
                                                val newH = (rectHeightState.value - dragY).coerceIn(30f, 120f)
                                                rectWidthState = newW.dp
                                                rectHeightState = newH.dp
                                                val valA = String.format("%.2f", newW / 15f).replace(",", ".")
                                                val valB = String.format("%.2f", newH / 15f).replace(",", ".")
                                                if (targetSymbol != "a") viewModel.onInputValueChange("a", valA)
                                                if (targetSymbol != "b") viewModel.onInputValueChange("b", valB)
                                            } else {
                                                val newBase = (triangleBaseState.value + dragX).coerceIn(40f, 200f)
                                                val newHeight = (triangleHeightState.value - dragY).coerceIn(30f, 120f)
                                                triangleBaseState = newBase.dp
                                                triangleHeightState = newHeight.dp
                                                val valA = String.format("%.2f", newBase / 15f).replace(",", ".")
                                                val valH = String.format("%.2f", newHeight / 15f).replace(",", ".")
                                                if (targetSymbol != "a" && formula.variables.any { it.symbol == "a" }) viewModel.onInputValueChange("a", valA)
                                                if (targetSymbol != "h" && formula.variables.any { it.symbol == "h" }) viewModel.onInputValueChange("h", valH)
                                            }
                                        }
                                    }
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val centerX = canvasWidth / 2
                                val centerY = canvasHeight / 2

                                if (formula.id.contains("circle")) {
                                    val radiusPx = circleRadiusState.toPx()
                                    drawCircle(
                                        color = com.calculator.core.ui.theme.AccentAmber.copy(alpha = 0.8f),
                                        radius = radiusPx,
                                        center = Offset(centerX, centerY),
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                    drawLine(
                                        color = com.calculator.core.ui.theme.AccentViolet,
                                        start = Offset(centerX, centerY),
                                        end = Offset(centerX + radiusPx, centerY),
                                        strokeWidth = 2.5.dp.toPx()
                                    )
                                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(centerX, centerY))
                                    drawCircle(color = com.calculator.core.ui.theme.AccentViolet, radius = 8.dp.toPx(), center = Offset(centerX + radiusPx, centerY))
                                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(centerX + radiusPx, centerY))
                                } else if (formula.id.contains("rectangle") || formula.id.contains("parallelogram")) {
                                    val wPx = rectWidthState.toPx()
                                    val hPx = rectHeightState.toPx()
                                    val topLeftX = centerX - wPx / 2
                                    val topLeftY = centerY - hPx / 2
                                    drawRect(
                                        color = com.calculator.core.ui.theme.AccentAmber.copy(alpha = 0.8f),
                                        topLeft = Offset(topLeftX, topLeftY),
                                        size = androidx.compose.ui.geometry.Size(wPx, hPx),
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                    drawCircle(color = com.calculator.core.ui.theme.AccentViolet, radius = 8.dp.toPx(), center = Offset(topLeftX + wPx, topLeftY + hPx))
                                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(topLeftX + wPx, topLeftY + hPx))
                                } else {
                                    val wPx = triangleBaseState.toPx()
                                    val hPx = triangleHeightState.toPx()
                                    val bottomY = centerY + hPx / 2
                                    val topY = centerY - hPx / 2
                                    val leftX = centerX - wPx / 2
                                    val rightX = centerX + wPx / 2
                                    drawLine(color = com.calculator.core.ui.theme.AccentAmber.copy(alpha = 0.8f), start = Offset(leftX, bottomY), end = Offset(rightX, bottomY), strokeWidth = 3.dp.toPx())
                                    drawLine(color = com.calculator.core.ui.theme.AccentAmber.copy(alpha = 0.8f), start = Offset(rightX, bottomY), end = Offset(centerX, topY), strokeWidth = 3.dp.toPx())
                                    drawLine(color = com.calculator.core.ui.theme.AccentAmber.copy(alpha = 0.8f), start = Offset(centerX, topY), end = Offset(leftX, bottomY), strokeWidth = 3.dp.toPx())
                                    drawCircle(color = com.calculator.core.ui.theme.AccentViolet, radius = 8.dp.toPx(), center = Offset(centerX, topY))
                                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(centerX, topY))
                                    drawCircle(color = com.calculator.core.ui.theme.AccentViolet, radius = 8.dp.toPx(), center = Offset(rightX, bottomY))
                                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(rightX, bottomY))
                                }
                            }
                        }
                    }
                }

                // 3. Formula template
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = stringResource(R.string.phys_formula_label, com.calculator.core.ui.utils.MathFormatter.format(formula.latexTemplate)),
                                color = com.calculator.core.ui.theme.AccentAmber,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formula.description,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // 4. Solve target selection
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_find),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            formula.variables.forEach { variable ->
                                val isSelected = variable.symbol == targetSymbol
                                val bg = if (isSelected) com.calculator.core.ui.theme.AccentViolet else Color(0x1AFFFFFF)
                                val border = if (isSelected) Color(0x448B5CF6) else Color(0x1AFFFFFF)
                                val tc = if (isSelected) Color.White else TextSecondary
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .border(1.dp, border, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setTargetVariable(variable.symbol) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = variable.symbol,
                                        color = tc,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Inputs Header
                item {
                    Text(
                        text = stringResource(R.string.geom_params),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 6. Inputs List
                val inputsList = formula.variables.filter { it.symbol != targetSymbol }
                items(inputsList) { variable ->
                    val rawVal = inputs[variable.symbol] ?: ""
                    VariableInput(
                        variable = variable,
                        value = rawVal,
                        onValueChange = { viewModel.onInputValueChange(variable.symbol, it) },
                        selectedUnit = inputUnits[variable.symbol],
                        onUnitSelect = { viewModel.onUnitChange(variable.symbol, it) }
                    )
                }

                // 7. Solve button
                item {
                    val neonCyanViolet = Brush.horizontalGradient(
                        colors = listOf(NeonCyan, ElectricViolet)
                    )

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.solveFormula()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(neonCyanViolet),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.geom_btn_calculate),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 8. Result Card
                calculationResult?.let { result ->
                    if (result.success) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                            ) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.5f), ElectricViolet.copy(alpha = 0.3f))), RoundedCornerShape(16.dp)),
                                    cornerRadius = 16.dp
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = stringResource(R.string.result_title),
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Text(
                                            text = "${result.solvedSymbol} = ${String.format("%.6f", result.value).trimEnd('0').trimEnd('.')}",
                                            color = com.calculator.core.ui.theme.AccentAmber,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        Text(
                                            text = result.unitSymbol,
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Step solution toggle
                                        Row(
                                            modifier = Modifier
                                                .clickable { showSteps = !showSteps }
                                                .background(Color(0x0AFFFFFF), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (showSteps) stringResource(R.string.btn_hide_solution) else stringResource(R.string.btn_show_solution),
                                                color = com.calculator.core.ui.theme.AccentViolet,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Detailed step solutions
                        if (showSteps) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_step_breakdown),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isPremiumActive) {
                                        PremiumTimerBadge(unlockUntilTimestamp = premiumManager.getUnlockUntilTimestamp())
                                    }
                                }
                            }

                            if (isPremiumActive) {
                                items(result.steps) { step ->
                                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = stringResource(R.string.label_step_num, step.order, step.description),
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (step.equationLatex.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = step.equationLatex,
                                                    color = com.calculator.core.ui.theme.AccentAmber,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    PremiumOverlay(
                                        onUnlockSuccess = {
                                            premiumManager.unlockFor24Hours()
                                            isPremiumActive = true
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = result.errorMessage ?: stringResource(R.string.geom_error),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 9. Banner advertisement
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    com.calculator.core.ui.components.AdmobBannerSimulator()
                }
            }
        }
    }
}

private suspend fun delay(timeMs: Long) {
    kotlinx.coroutines.delay(timeMs)
}

