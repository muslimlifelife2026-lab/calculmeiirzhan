package com.calculator.feature.physics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.calculator.domain.model.MeasurementUnit
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.VariableInput
import com.calculator.core.ui.components.PremiumOverlay
import com.calculator.core.ui.components.PremiumTimerBadge
import com.calculator.core.ui.premium.PremiumManager
import com.calculator.core.ui.theme.NeonCyan
import com.calculator.core.ui.theme.ElectricViolet
import com.calculator.core.ui.theme.TextPrimary
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.core.ui.theme.SurfaceCard
import com.calculator.core.ui.theme.SurfaceBorder
import com.calculator.core.ui.theme.SurfaceElevated
import com.calculator.core.ui.R
import com.calculator.feature.physics.simulators.PendulumSimulator
import com.calculator.feature.physics.simulators.ProjectileSimulator

data class PhysicsPreset(
    val title: String,
    val formulaName: String,
    val target: String,
    val inputs: Map<String, String>,
    val description: String
)

@Composable
fun PhysicsScreen(
    modifier: Modifier = Modifier,
    viewModel: PhysicsViewModel = viewModel()
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

    var showFormulaSelector by remember { mutableStateOf(false) }
    var showSteps by remember { mutableStateOf(false) }
    var selectedPhysicsTab by remember { mutableStateOf(0) } // 0: Formulas, 1: Pendulum, 2: Projectile, 3: Quiz
    var searchQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    val presets = remember {
        listOf(
            PhysicsPreset("🏎️ Разгон авто", "Скорость (кинематика)", "v", mapOf("v_0" to "0", "a" to "5", "t" to "6"), "Разгон с 0 до скорости за 6 сек при a=5 м/с²"),
            PhysicsPreset("📱 Падение яблока", "Потенциальная энергия", "E_p", mapOf("m" to "0.2", "g" to "9.81", "h" to "3"), "Энергия яблока 200г на высоте 3м"),
            PhysicsPreset("⚡ Электрочайник", "Электрическая мощность", "P", mapOf("U" to "220", "I" to "10"), "Мощность в сети 220В при токе 10А"),
            PhysicsPreset("🚗 Торможение", "Второй закон Ньютона", "F", mapOf("m" to "1500", "a" to "8"), "Сила для торможения авто 1.5т"),
            PhysicsPreset("💧 Масса воды", "Плотность вещества", "m", mapOf("rho" to "1000", "V" to "0.2"), "Масса 200 литров (0.2м³) воды"),
            PhysicsPreset("💡 Закон Ома", "Закон Ома для участка цепи", "I", mapOf("U" to "220", "R" to "44"), "Сила тока при R=44 Ом")
        )
    }

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
            .background(com.calculator.core.ui.theme.Background)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Mode Selector: 4 Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedPhysicsTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (selectedPhysicsTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedPhysicsTab]),
                        color = Color.White
                    )
                }
            },
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            val tabs = listOf("📐 Формулы", "🕹️ Маятник", "🚀 Баллистика", "🎯 Тренажёр")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedPhysicsTab == index,
                    onClick = { selectedPhysicsTab = index },
                    text = { Text(title, fontWeight = if (selectedPhysicsTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                )
            }
        }

        when (selectedPhysicsTab) {
            1 -> {
                PendulumSimulator()
            }
            2 -> {
                ProjectileSimulator()
            }
            3 -> {
                PhysicsQuizSection(formulas = formulas)
            }
            else -> {
                // Formula Selector Dialog with Human Keyword Search
                if (showFormulaSelector) {
                    AlertDialog(
                        onDismissRequest = { showFormulaSelector = false; searchQuery = "" },
                        title = { Text(text = stringResource(R.string.phys_select_formula), color = TextPrimary) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Поиск: ток, скорость, сила, масса...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = com.calculator.core.ui.theme.AccentViolet,
                                        focusedContainerColor = Color(0x0AFFFFFF),
                                        unfocusedContainerColor = Color(0x0AFFFFFF)
                                    ),
                                    singleLine = true
                                )
                                
                                val filteredFormulas = formulas.filter {
                                    it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.subcategory.contains(searchQuery, ignoreCase = true) ||
                                    it.description.contains(searchQuery, ignoreCase = true)
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 260.dp)
                                ) {
                                    items(filteredFormulas) { formula ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.selectFormula(formula)
                                                    showFormulaSelector = false
                                                    searchQuery = ""
                                                }
                                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                        ) {
                                            Text(text = formula.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text(text = "${formula.subcategory} • ${formula.canonicalEquation}", color = Color(0xFF38BDF8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        HorizontalDivider(color = Color(0x1AFFFFFF))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFormulaSelector = false; searchQuery = "" }) {
                                Text(stringResource(R.string.btn_close), color = com.calculator.core.ui.theme.AccentAmber)
                            }
                        },
                        containerColor = com.calculator.core.ui.theme.SurfaceCard
                    )
                }

                selectedFormula?.let { formula ->
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
                    ) {
                        // 0. Life-Scenarios Quick Presets Row (1-Tap Experience)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "🚀 Примеры из жизни (1 клик):",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(presets) { preset ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceCard)
                                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    val matched = formulas.firstOrNull { it.name.contains(preset.formulaName, ignoreCase = true) }
                                                    if (matched != null) {
                                                        viewModel.selectFormula(matched)
                                                        viewModel.applyPreset(preset.inputs, preset.target)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = preset.title,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 1. Selector Bar
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFormulaSelector = true }
                                    .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.phys_category, formula.subcategory),
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
                                    text = "Выбрать другую ▾",
                                    color = com.calculator.core.ui.theme.AccentAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 1.5 Quick Explanation & Tip
                        if (formula.description.isNotBlank()) {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "ℹ️ ${formula.description}", color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }

                        // 2. Main Equation Card
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Каноническая формула:",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formula.canonicalEquation,
                                        color = TextPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // 3. Target Variable Selector with Clear Guidance
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎯 Что ищем:",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    formula.variables.forEach { variable ->
                                        val isSelected = variable.symbol == targetSymbol
                                        val bg = if (isSelected) Color.White else SurfaceElevated
                                        val border = if (isSelected) Color.White else SurfaceBorder
                                        val tc = if (isSelected) com.calculator.core.ui.theme.Background else TextSecondary
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

                        // 4. Input parameters header
                        item {
                            Text(
                                text = "📝 Введите известные числа:",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // 5. Input variables fields list
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

                        // 6. Action Calculate Button
                        item {
                            val neonCyanViolet = Brush.horizontalGradient(colors = listOf(NeonCyan, ElectricViolet))
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.solveFormula()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .background(neonCyanViolet, RoundedCornerShape(12.dp)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Вычислить ответ ⚡",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 7. Evaluation Result Display
                        calculationResult?.let { result ->
                            if (result.success) {
                                item {
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "ОТВЕТ",
                                                color = com.calculator.core.ui.theme.AccentAmber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${result.solvedSymbol} = ${String.format("%.4f", result.value).trimEnd('0').trimEnd('.')} ${result.unitSymbol}".trim(),
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Steps Breakdown Card Header
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Пошаговое решение:",
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
                                        GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
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
                                                step.substitutionDetails?.let { details ->
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(text = details, color = TextSecondary, fontSize = 10.sp)
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
                            } else {
                                item {
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = result.errorMessage ?: "Заполните параметры для расчета",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // 8. Banner advertisement
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            com.calculator.core.ui.components.AdmobBannerSimulator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhysicsQuizSection(formulas: List<Formula>) {
    if (formulas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    var quizIndex by remember { mutableStateOf(0) }
    val formula = formulas[quizIndex % formulas.size]

    val cleanEquation = formula.canonicalEquation.replace(" ", "")
    val originalTokens = remember(cleanEquation) {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < cleanEquation.length) {
            val c = cleanEquation[i]
            if (c.isLetter()) {
                val start = i
                while (i < cleanEquation.length && (cleanEquation[i].isLetterOrDigit() || cleanEquation[i] == '_')) {
                    i++
                }
                tokens.add(cleanEquation.substring(start, i))
            } else if (c == '*' || c == '+' || c == '-' || c == '/' || c == '=' || c == '^') {
                tokens.add(c.toString())
                i++
            } else if (c.isDigit()) {
                val start = i
                while (i < cleanEquation.length && cleanEquation[i].isDigit()) {
                    i++
                }
                tokens.add(cleanEquation.substring(start, i))
            } else {
                tokens.add(c.toString())
                i++
            }
        }
        tokens
    }

    var options by remember(originalTokens) { mutableStateOf(originalTokens.shuffled()) }
    var userAnswer by remember(originalTokens) { mutableStateOf<List<String>>(emptyList()) }
    var quizMessage by remember(originalTokens) { mutableStateOf("") }
    var isSuccess by remember(originalTokens) { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Соберите формулу:",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${formula.name} (${formula.subcategory})",
            color = com.calculator.core.ui.theme.AccentViolet,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        // User Answer Box
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (userAnswer.isEmpty()) {
                    Text("Нажимайте на блоки снизу...", color = TextSecondary, fontSize = 14.sp)
                } else {
                    userAnswer.forEachIndexed { index, token ->
                        Button(
                            onClick = {
                                userAnswer = userAnswer.toMutableList().also { it.removeAt(index) }
                                options = options + token
                                isSuccess = null
                                quizMessage = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = com.calculator.core.ui.theme.AccentViolet),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(token, color = Color.White)
                        }
                    }
                }
            }
        }

        // Available Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            options.forEachIndexed { index, token ->
                Button(
                    onClick = {
                        userAnswer = userAnswer + token
                        options = options.toMutableList().also { it.removeAt(index) }
                        isSuccess = null
                        quizMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(token, color = TextPrimary)
                }
            }
        }

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    userAnswer = emptyList()
                    options = originalTokens.shuffled()
                    isSuccess = null
                    quizMessage = ""
                }
            ) {
                Text("Сброс", color = TextSecondary)
            }

            Button(
                onClick = {
                    if (userAnswer == originalTokens) {
                        isSuccess = true
                        quizMessage = "🎉 Правильно! Отличная работа!"
                    } else {
                        isSuccess = false
                        quizMessage = "❌ Неверно. Попробуйте еще раз!"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = com.calculator.core.ui.theme.AccentAmber)
            ) {
                Text("Проверить", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (quizMessage.isNotEmpty()) {
            Text(
                text = quizMessage,
                color = if (isSuccess == true) Color(0xFF00FFB2) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (isSuccess == true) {
            Button(
                onClick = {
                    quizIndex++
                    userAnswer = emptyList()
                    isSuccess = null
                    quizMessage = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFB2))
            ) {
                Text("Следующая формула ➡️", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
