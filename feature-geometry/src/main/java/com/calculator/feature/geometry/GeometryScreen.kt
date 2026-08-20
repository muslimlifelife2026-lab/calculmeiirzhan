package com.calculator.feature.geometry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.calculator.core.ui.theme.*
import com.calculator.core.ui.R
import kotlin.math.PI
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeometryScreen(
    modifier: Modifier = Modifier,
    viewModel: GeometryViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val premiumManager = remember { PremiumManager(context) }
    var isPremiumActive by remember { mutableStateOf(premiumManager.isPremiumActive()) }

    var selectedModeTab by remember { mutableStateOf(0) } // 0: 2D Planimetry, 1: 3D Stereometry

    val formulas by viewModel.formulas.collectAsState()
    val selectedFormula by viewModel.selectedFormula.collectAsState()
    val inputs by viewModel.inputs.collectAsState()
    val inputUnits by viewModel.inputUnits.collectAsState()
    val targetSymbol by viewModel.targetVariableSymbol.collectAsState()
    val calculationResult by viewModel.calculationResult.collectAsState()

    var showShapeSelector by remember { mutableStateOf(false) }
    var showSteps by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // 3D State
    var selected3DShape by remember { mutableStateOf(Shape3DType.CUBE) }
    var dimRadius by remember { mutableFloatStateOf(5.0f) }
    var dimHeight by remember { mutableFloatStateOf(10.0f) }
    var dimSideA by remember { mutableFloatStateOf(6.0f) }
    var dimSideB by remember { mutableFloatStateOf(8.0f) }
    var dimSideC by remember { mutableFloatStateOf(10.0f) }
    var autoRotate3D by remember { mutableStateOf(false) }

    LaunchedEffect(selectedFormula) {
        showSteps = false
    }

    LaunchedEffect(calculationResult) {
        if (calculationResult?.success == true) {
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mode Selector: 2D vs 3D Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedModeTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (selectedModeTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedModeTab]),
                        color = Color.White
                    )
                }
            },
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            val tabs = listOf("📐 2D Планиметрия", "🧊 3D Стереометрия (360°)")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedModeTab == index,
                    onClick = { selectedModeTab = index },
                    text = { Text(title, fontWeight = if (selectedModeTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                )
            }
        }

        if (selectedModeTab == 1) {
            // ─── 3D STEREOMETRY MODE (100% Functional & Bug-Free) ──────────────
            val (vol3D, area3D) = remember(selected3DShape, dimRadius, dimHeight, dimSideA, dimSideB, dimSideC) {
                when (selected3DShape) {
                    Shape3DType.CUBE -> {
                        val v = dimSideA * dimSideB * dimSideC
                        val s = 2 * (dimSideA * dimSideB + dimSideB * dimSideC + dimSideA * dimSideC)
                        v to s
                    }
                    Shape3DType.SPHERE -> {
                        val v = (4.0 / 3.0) * PI * Math.pow(dimRadius.toDouble(), 3.0)
                        val s = 4.0 * PI * Math.pow(dimRadius.toDouble(), 2.0)
                        v.toFloat() to s.toFloat()
                    }
                    Shape3DType.CYLINDER -> {
                        val v = PI * Math.pow(dimRadius.toDouble(), 2.0) * dimHeight
                        val s = 2 * PI * dimRadius * (dimRadius + dimHeight)
                        v.toFloat() to s.toFloat()
                    }
                    Shape3DType.CONE -> {
                        val l = sqrt(dimRadius * dimRadius + dimHeight * dimHeight)
                        val v = (1.0 / 3.0) * PI * dimRadius * dimRadius * dimHeight
                        val s = PI * dimRadius * (dimRadius + l)
                        v.toFloat() to s.toFloat()
                    }
                    Shape3DType.PYRAMID -> {
                        val v = (1.0 / 3.0) * dimSideA * dimSideA * dimHeight
                        val apothem = sqrt((dimSideA / 2f) * (dimSideA / 2f) + dimHeight * dimHeight)
                        val s = dimSideA * dimSideA + 2 * dimSideA * apothem
                        v.toFloat() to s.toFloat()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3D Shapes Selector Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(Shape3DType.values()) { shape ->
                        val isSelected = selected3DShape == shape
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color.White else SurfaceCard)
                                .border(1.dp, if (isSelected) Color.White else SurfaceBorder, RoundedCornerShape(14.dp))
                            .clickable { selected3DShape = shape }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = shape.title,
                                color = if (isSelected) Background else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // 3D Interactive Canvas
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    Stereometry3DCanvas(
                        shapeType = selected3DShape,
                        dimA = if (selected3DShape == Shape3DType.SPHERE || selected3DShape == Shape3DType.CYLINDER || selected3DShape == Shape3DType.CONE) dimRadius * 2f else dimSideA,
                        dimB = if (selected3DShape == Shape3DType.CYLINDER || selected3DShape == Shape3DType.CONE || selected3DShape == Shape3DType.PYRAMID) dimHeight else dimSideB,
                        dimC = dimSideC,
                        autoRotate = autoRotate3D,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Calculations Card (Volume & Area)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Объем (V):", color = TextSecondary, fontSize = 11.sp)
                        Text(String.format("%.2f ед³", vol3D), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("Полная площадь (S):", color = TextSecondary, fontSize = 11.sp)
                        Text(String.format("%.2f ед²", area3D), color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (autoRotate3D) Color(0xFF38BDF8).copy(alpha = 0.2f) else SurfaceElevated)
                            .border(1.dp, if (autoRotate3D) Color(0xFF38BDF8) else SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { autoRotate3D = !autoRotate3D }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(if (autoRotate3D) "⏸ Пауза" else "▶ Авто", color = if (autoRotate3D) Color(0xFF38BDF8) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Interactive Dimension Controls Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Параметры фигуры:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    when (selected3DShape) {
                        Shape3DType.SPHERE -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Радиус (r): ${String.format("%.1f", dimRadius)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = dimRadius, onValueChange = { dimRadius = it }, valueRange = 1f..15f, modifier = Modifier.weight(1f))
                            }
                        }
                        Shape3DType.CYLINDER, Shape3DType.CONE -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Радиус (r): ${String.format("%.1f", dimRadius)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = dimRadius, onValueChange = { dimRadius = it }, valueRange = 1f..15f, modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Высота (h): ${String.format("%.1f", dimHeight)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = dimHeight, onValueChange = { dimHeight = it }, valueRange = 1f..20f, modifier = Modifier.weight(1f))
                            }
                        }
                        Shape3DType.CUBE -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Длина (a): ${String.format("%.1f", dimSideA)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = dimSideA, onValueChange = { dimSideA = it }, valueRange = 1f..15f, modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ширина (b): ${String.format("%.1f", dimSideB)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = dimSideB, onValueChange = { dimSideB = it }, valueRange = 1f..15f, modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Высота (c): ${String.format("%.1f", dimSideC)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = dimSideC, onValueChange = { dimSideC = it }, valueRange = 1f..15f, modifier = Modifier.weight(1f))
                            }
                        }
                        Shape3DType.PYRAMID -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Основание (a): ${String.format("%.1f", dimSideA)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = dimSideA, onValueChange = { dimSideA = it }, valueRange = 1f..15f, modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Высота (h): ${String.format("%.1f", dimHeight)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = dimHeight, onValueChange = { dimHeight = it }, valueRange = 1f..20f, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            // ─── 2D PLANIMETRY MODE ────────────────────────────────────────────
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
                    containerColor = SurfaceCard
                )
            }

            selectedFormula?.let { formula ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
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
                                text = "Выбрать ▾",
                                color = com.calculator.core.ui.theme.AccentAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Equation Card
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Формула:", color = TextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formula.canonicalEquation,
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 3. Target Variable Selector
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯 Что ищем:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                formula.variables.forEach { variable ->
                                    val isSelected = variable.symbol == targetSymbol
                                    val bg = if (isSelected) Color.White else SurfaceElevated
                                    val border = if (isSelected) Color.White else SurfaceBorder
                                    val tc = if (isSelected) Background else TextSecondary
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bg)
                                            .border(1.dp, border, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setTargetVariable(variable.symbol) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = "${variable.symbol} (${variable.name})", color = tc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Input Variables
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

                    // 5. Calculate Button
                    item {
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.solveFormula()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF08090C)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Рассчитать ⚡", color = Color(0xFF08090C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 6. Result
                    calculationResult?.let { result ->
                        if (result.success) {
                            item {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                    cornerRadius = 16.dp
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("ОТВЕТ", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "СИ: ${result.unitSymbol}".trim(),
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${result.solvedSymbol} = ${String.format(java.util.Locale.US, "%.4f", result.value).trimEnd('0').trimEnd('.')} ${result.unitSymbol}".trim(),
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
