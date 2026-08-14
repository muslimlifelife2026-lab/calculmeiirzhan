package com.calculator.feature.physics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
    var isQuizMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Reset steps expand state on formula change
    LaunchedEffect(selectedFormula) {
        showSteps = false
    }

    // Auto-scroll to result card when calculation succeeds
    LaunchedEffect(calculationResult) {
        if (calculationResult?.success == true) {
            delay(100)
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.calculator.core.ui.theme.Background)
            .padding(horizontal = 16.dp)
    ) {
        // Mode Selector: Calculator vs Formula Trainer
        TabRow(
            selectedTabIndex = if (isQuizMode) 1 else 0,
            containerColor = Color.Transparent,
            contentColor = com.calculator.core.ui.theme.AccentViolet,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (isQuizMode) 1 else 0]),
                    color = com.calculator.core.ui.theme.AccentViolet
                )
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Tab(
                selected = !isQuizMode,
                onClick = { isQuizMode = false },
                text = { Text("Калькулятор", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = isQuizMode,
                onClick = { isQuizMode = true },
                text = { Text("Тренажёр", fontWeight = FontWeight.Bold) }
            )
        }

        if (showFormulaSelector) {
            AlertDialog(
                onDismissRequest = { showFormulaSelector = false; searchQuery = "" },
                title = { Text(text = stringResource(R.string.phys_select_formula), color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Search bar inside selector
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск формулы...") },
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
                            it.subcategory.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
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
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Text(text = formula.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(text = formula.subcategory, color = TextSecondary, fontSize = 11.sp)
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

        // FULLY SCROLLABLE LAYOUT
        if (isQuizMode) {
            PhysicsQuizScreen(formulas = formulas)
        } else {
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
                                text = stringResource(R.string.label_select),
                                color = com.calculator.core.ui.theme.AccentAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 1.5 Info Card
                    if (formula.description.isNotBlank()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "ℹ️",
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "О формуле",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = formula.description,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Базовое уравнение: ${formula.canonicalEquation}",
                                        color = com.calculator.core.ui.theme.AccentAmber,
                                        fontSize = 13.sp,
                                        fontFamily = com.calculator.core.ui.theme.MonospaceFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // 2. Formula Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.phys_formula_label, com.calculator.core.ui.utils.MathFormatter.format(formula.latexTemplate)),
                                    color = com.calculator.core.ui.theme.AccentAmber,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formula.name,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // 3. Target Variable Selector
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

                    // 4. Input parameters header
                    item {
                        Text(
                            text = stringResource(R.string.phys_params),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
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
                        val neonCyanViolet = Brush.horizontalGradient(
                            colors = listOf(NeonCyan, ElectricViolet)
                        )
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.solveFormula()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(neonCyanViolet, RoundedCornerShape(12.dp)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.phys_btn_calculate),
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
                                            text = stringResource(R.string.result_title),
                                            color = com.calculator.core.ui.theme.AccentAmber,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${result.solvedSymbol} = ${String.format("%.6f", result.value).trimEnd('0').trimEnd('.')}",
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
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                                            step.substitutionDetails?.let { details ->
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = details,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp
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
                        } else {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = result.errorMessage ?: stringResource(R.string.phys_error),
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
                        Spacer(modifier = Modifier.height(6.dp))
                        com.calculator.core.ui.components.AdmobBannerSimulator()
                    }
                }
            }
        }
    }
}

@Composable
private fun PhysicsQuizScreen(formulas: List<Formula>) {
    if (formulas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = com.calculator.core.ui.theme.AccentViolet)
        }
        return
    }

    var quizIndex by remember { mutableStateOf(0) }
    val formula = formulas[quizIndex % formulas.size]

    // Parse formula symbols
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
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Тренажёр законов физики 🎓",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = com.calculator.core.ui.theme.AccentAmber
        )

        // Task Name
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formula.subcategory,
                    color = com.calculator.core.ui.theme.AccentViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Соберите формулу для закона:",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formula.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Answer board (user inputs)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0AFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (userAnswer.isEmpty()) {
                Text(text = "Нажимайте на блоки внизу", color = TextSecondary.copy(alpha = 0.5f), fontSize = 14.sp)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    userAnswer.forEach { token ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(com.calculator.core.ui.theme.AccentViolet)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = token, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Action Buttons Row (Reset & Next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    userAnswer = emptyList()
                    options = originalTokens.shuffled()
                    isSuccess = null
                    quizMessage = ""
                }
            ) {
                Text("Сбросить 🔄", color = com.calculator.core.ui.theme.AccentAmber)
            }

            if (isSuccess == true) {
                Button(
                    onClick = {
                        quizIndex++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.calculator.core.ui.theme.AccentViolet)
                ) {
                    Text("Далее ➡️", color = Color.White)
                }
            }
        }

        // Quiz options
        Text(
            text = "Доступные величины:",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(options) { token ->
                val isSelected = userAnswer.contains(token) && userAnswer.count { it == token } >= options.count { it == token }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0x0AFFFFFF) else Color(0x1AFFFFFF))
                        .border(1.dp, if (isSelected) Color.Transparent else Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                        .clickable(enabled = !isSelected) {
                            userAnswer = userAnswer + token
                            if (userAnswer.size == originalTokens.size) {
                                // Check answer
                                val correct = userAnswer.joinToString("") == originalTokens.joinToString("")
                                if (correct) {
                                    isSuccess = true
                                    quizMessage = "Отлично! Всё верно! 🎉"
                                } else {
                                    isSuccess = false
                                    quizMessage = "Неправильно. Попробуйте еще раз ❌"
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = token,
                        color = if (isSelected) TextSecondary.copy(alpha = 0.3f) else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Result notification message
        if (quizMessage.isNotEmpty()) {
            Text(
                text = quizMessage,
                color = if (isSuccess == true) Color.Green else Color.Red,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

private suspend fun delay(timeMs: Long) {
    kotlinx.coroutines.delay(timeMs)
}
