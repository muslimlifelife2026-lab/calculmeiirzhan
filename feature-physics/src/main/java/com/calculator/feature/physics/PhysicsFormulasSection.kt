package com.calculator.feature.physics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.domain.model.Formula
import com.calculator.domain.model.MeasurementUnit
import com.calculator.domain.model.CalculationResult
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.VariableInput
import com.calculator.core.ui.theme.*
import com.calculator.core.ui.utils.NumberFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhysicsFormulasSection(
    formulas: List<Formula>,
    selectedFormula: Formula?,
    inputs: Map<String, String>,
    inputUnits: Map<String, MeasurementUnit?>,
    targetSymbol: String,
    calculationResult: CalculationResult?,
    onSelectFormula: (Formula) -> Unit,
    onSetTargetVariable: (String) -> Unit,
    onInputValueChange: (String, String) -> Unit,
    onUnitChange: (String, MeasurementUnit) -> Unit,
    onSolveFormula: () -> Unit,
    onPresetApply: (Formula, Map<String, String>, String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    var showFormulaSelector by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showExamples by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    if (showFormulaSelector) {
        ModalBottomSheet(onDismissRequest = { showFormulaSelector = false }, containerColor = Background) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Выберите формулу", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Поиск по теме или названию") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                val filtered = formulas.filter {
                    it.name.contains(searchQuery, true) || it.subcategory.contains(searchQuery, true) || it.description.contains(searchQuery, true)
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    if (filtered.isEmpty()) item { Text("Ничего не найдено. Попробуйте «сила» или «ток».") }
                    items(filtered, key = { it.id }) { formula ->
                        Column(Modifier.fillMaxWidth().clickable {
                            onSelectFormula(formula); showFormulaSelector = false; searchQuery = ""
                        }.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(formula.name, style = MaterialTheme.typography.titleMedium)
                            Text("${formula.subcategory} · ${formula.canonicalEquation}", color = TextSecondary)
                        }
                        HorizontalDivider(color = SurfaceBorder)
                    }
                }
            }
        }
    }

    val formula = selectedFormula ?: return
    LazyColumn(state = listState, modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Расчёт по формуле", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("Выберите величину, которую нужно найти. Единицы можно менять рядом с числами.", color = TextSecondary)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(formula.subcategory, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    Text(formula.name, style = MaterialTheme.typography.titleLarge)
                    SelectionContainer { Text(formula.canonicalEquation, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace) }
                    if (formula.description.isNotBlank()) Text(formula.description, color = TextSecondary)
                    OutlinedButton(onClick = { showFormulaSelector = true }, modifier = Modifier.fillMaxWidth()) { Text("Выбрать другую формулу") }
                }
            }
        }
        item {
            Text("Что найти?", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formula.variables.forEach { variable ->
                    FilterChip(selected = variable.symbol == targetSymbol, onClick = { onSetTargetVariable(variable.symbol) },
                        label = { Text("${variable.symbol} · ${variable.name}") }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        item { Text("Известные величины", style = MaterialTheme.typography.titleMedium) }
        items(formula.variables.filter { it.symbol != targetSymbol }, key = { it.symbol }) { variable ->
            VariableInput(variable = variable, value = inputs[variable.symbol].orEmpty(),
                onValueChange = { onInputValueChange(variable.symbol, it) }, selectedUnit = inputUnits[variable.symbol],
                onUnitSelect = { onUnitChange(variable.symbol, it) })
        }
        item {
            Button(onClick = { keyboard?.hide(); onSolveFormula() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Вычислить ответ", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            TextButton(onClick = { showExamples = !showExamples }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showExamples) "Скрыть примеры" else "Попробовать на примере")
            }
            if (showExamples) PhysicsPresetsSection(formulas, onPresetApply)
        }
        calculationResult?.let { result ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (result.success) "Ответ" else "Проверьте данные", style = MaterialTheme.typography.titleMedium)
                        if (result.success) SelectionContainer {
                            Text("${result.solvedSymbol} = ${NumberFormatter.formatResult(result)} ${result.unitSymbol}".trim(),
                                style = MaterialTheme.typography.headlineMedium)
                        } else Text(result.errorMessage ?: "Заполните известные величины.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (result.success) {
                item { Text("Как получен ответ", style = MaterialTheme.typography.titleLarge) }
                items(result.steps) { step ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${step.order}. ${step.description}", style = MaterialTheme.typography.titleMedium)
                            if (step.equationLatex.isNotBlank()) SelectionContainer {
                                Text(step.equationLatex, fontFamily = FontFamily.Monospace, color = AccentSecondary)
                            }
                            step.substitutionDetails?.let { Text(it, color = TextSecondary) }
                        }
                    }
                }
            }
        }
    }
}
