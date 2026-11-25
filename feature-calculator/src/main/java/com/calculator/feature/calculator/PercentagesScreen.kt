package com.calculator.feature.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.VariableInput
import com.calculator.core.ui.theme.AccentPrimary
import com.calculator.core.ui.theme.AccentSecondary
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.core.ui.utils.MathFormatter
import com.calculator.core.ui.utils.NumberFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PercentagesScreen(
    modifier: Modifier = Modifier,
    viewModel: PercentagesViewModel = viewModel()
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val formulas by viewModel.formulas.collectAsState()
    val selectedFormula by viewModel.selectedFormula.collectAsState()
    val inputs by viewModel.inputs.collectAsState()
    val inputUnits by viewModel.inputUnits.collectAsState()
    val targetSymbol by viewModel.targetVariableSymbol.collectAsState()
    val calculationResult by viewModel.calculationResult.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(calculationResult) {
        if (calculationResult != null) {
            kotlinx.coroutines.delay(100)
            val count = listState.layoutInfo.totalItemsCount
            if (count > 0) listState.animateScrollToItem(count - 1)
        }
    }

    val formula = selectedFormula
    if (formula == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentPrimary)
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("percentages_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Проценты", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("Часть от числа, процентный рост и процентный спад.", color = TextSecondary)
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                formulas.forEach { choice ->
                    FilterChip(
                        selected = choice.id == formula.id,
                        onClick = { viewModel.selectFormula(choice) },
                        label = { Text(choice.name) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(formula.subcategory, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    Text(formula.name, style = MaterialTheme.typography.titleLarge)
                    SelectionContainer {
                        Text(
                            MathFormatter.format(formula.latexTemplate.ifBlank { formula.canonicalEquation }),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (formula.description.isNotBlank()) Text(formula.description, color = TextSecondary)
                }
            }
        }
        item {
            Text("Что найти?", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formula.variables.forEach { variable ->
                    FilterChip(
                        selected = variable.symbol == targetSymbol,
                        onClick = { viewModel.setTargetVariable(variable.symbol) },
                        label = { Text("${variable.symbol} · ${variable.name}") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
        item { Text("Известные величины", style = MaterialTheme.typography.titleMedium) }
        items(formula.variables.filter { it.symbol != targetSymbol }, key = { it.symbol }) { variable ->
            VariableInput(
                variable = variable,
                value = inputs[variable.symbol].orEmpty(),
                onValueChange = { viewModel.onInputValueChange(variable.symbol, it) },
                selectedUnit = inputUnits[variable.symbol],
                onUnitSelect = { viewModel.onUnitChange(variable.symbol, it) }
            )
        }
        item {
            Button(
                onClick = { keyboard?.hide(); viewModel.solveFormula() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text("Вычислить ответ", style = MaterialTheme.typography.titleMedium)
            }
        }
        calculationResult?.let { result ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (result.success) "Ответ" else "Проверьте данные", style = MaterialTheme.typography.titleMedium)
                        if (result.success) {
                            SelectionContainer {
                                Text(
                                    "${result.solvedSymbol} = ${NumberFormatter.formatResult(result)} ${result.unitSymbol}".trim(),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        } else {
                            Text(result.errorMessage ?: "Заполните известные величины.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (result.success) {
                item { Text("Как получен ответ", style = MaterialTheme.typography.titleLarge) }
                items(result.steps) { step ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${step.order}. ${step.description}", style = MaterialTheme.typography.titleMedium)
                            if (step.equationLatex.isNotBlank()) {
                                SelectionContainer {
                                    Text(step.equationLatex, fontFamily = FontFamily.Monospace, color = AccentSecondary)
                                }
                            }
                            step.substitutionDetails?.let { Text(it, color = TextSecondary) }
                        }
                    }
                }
            }
        }
    }
}
