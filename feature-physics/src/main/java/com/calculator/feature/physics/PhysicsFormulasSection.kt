package com.calculator.feature.physics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.domain.model.Formula
import com.calculator.domain.model.MeasurementUnit
import com.calculator.domain.model.CalculationResult
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.VariableInput
import com.calculator.core.ui.components.PremiumOverlay
import com.calculator.core.ui.components.PremiumTimerBadge
import com.calculator.core.ui.theme.*
import com.calculator.core.ui.R

@Composable
fun PhysicsFormulasSection(
    formulas: List<Formula>,
    selectedFormula: Formula?,
    inputs: Map<String, String>,
    inputUnits: Map<String, MeasurementUnit?>,
    targetSymbol: String,
    calculationResult: CalculationResult?,
    isPremiumActive: Boolean,
    unlockUntilTimestamp: Long?,
    onUnlockPremium: () -> Unit,
    onSelectFormula: (Formula) -> Unit,
    onSetTargetVariable: (String) -> Unit,
    onInputValueChange: (String, String) -> Unit,
    onUnitChange: (String, MeasurementUnit) -> Unit,
    onSolveFormula: () -> Unit,
    onPresetApply: (Formula, Map<String, String>, String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    var showFormulaSelector by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                                        onSelectFormula(formula)
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
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            // 0. Life-Scenarios Quick Presets Row (1-Tap Experience)
            item {
                PhysicsPresetsSection(
                    formulas = formulas,
                    onPresetApply = onPresetApply
                )
            }

            // Unified Compact Formula Hub Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header: Category & Switcher button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFormulaSelector = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formula.subcategory.uppercase(),
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = formula.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceElevated)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Сменить ▾", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Canonical Equation Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formula.canonicalEquation,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Target Variable Selector Row: Compact Capsules
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val activeVar = formula.variables.firstOrNull { it.symbol == targetSymbol }
                            Text(
                                text = "🎯 Ищем: ${activeVar?.symbol ?: targetSymbol} (${activeVar?.name ?: ""})",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                formula.variables.forEach { variable ->
                                    val isSelected = variable.symbol == targetSymbol
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color.White else SurfaceElevated)
                                            .border(1.dp, if (isSelected) Color.White else SurfaceBorder, RoundedCornerShape(6.dp))
                                            .clickable { onSetTargetVariable(variable.symbol) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = variable.symbol,
                                            color = if (isSelected) Background else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
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
                    onValueChange = { onInputValueChange(variable.symbol, it) },
                    selectedUnit = inputUnits[variable.symbol],
                    onUnitSelect = { onUnitChange(variable.symbol, it) }
                )
            }

            // 6. Action Calculate Button
            item {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSolveFormula()
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
                    Text(
                        text = "Вычислить ответ ⚡",
                        color = Color(0xFF08090C),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 7. Evaluation Result Display
            calculationResult?.let { result ->
                if (result.success) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            cornerRadius = 16.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "ОТВЕТ",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
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
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                            if (isPremiumActive && unlockUntilTimestamp != null) {
                                PremiumTimerBadge(unlockUntilTimestamp = unlockUntilTimestamp)
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
                                    onUnlockPremium()
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
