package com.calculator.feature.geometry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.VariableInput
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.core.ui.utils.NumberFormatter

@Composable
fun GeometryScreen(modifier: Modifier = Modifier, viewModel: GeometryViewModel = viewModel()) {
    var section by rememberSaveable { mutableIntStateOf(-1) }
    val sectionStates = rememberSaveableStateHolder()
    BackHandler(section >= 0) { section = -1 }
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
        if (section >= 0) TextButton(onClick = { section = -1 }) { Text("‹  Разделы геометрии") }
        sectionStates.SaveableStateProvider(section) {
            when (section) {
                0 -> GeometryFormulas(viewModel)
                1 -> StereometrySection()
                2 -> PlaneConstructionScreen()
                else -> GeometryLibrary { section = it }
            }
        }
    }
}

private data class GeometryDestination(val title: String, val detail: String, val mark: String)

@Composable
private fun GeometryLibrary(onSelect: (Int) -> Unit) {
    val destinations = listOf(
        GeometryDestination("Формулы", "Площади, периметры и длины. Расчёт с единицами и объяснением шагов.", "ƒ"),
        GeometryDestination("3D-фигуры", "Вращайте фигуру, меняйте размеры и сравнивайте объём и площадь.", "3D"),
        GeometryDestination("Построения", "Создавайте точки, отрезки и окружности на координатной плоскости.", "xy")
    )
    LazyColumn(Modifier.fillMaxSize().testTag("geometry_library"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Увидеть форму", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text("Исследуйте фигуры на плоскости и в пространстве.", color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(destinations.indices.toList()) { index ->
            val destination = destinations[index]
            GlassCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button) { onSelect(index) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(destination.mark, style = MaterialTheme.typography.headlineSmall, color = TextSecondary,
                        modifier = Modifier.width(44.dp).padding(top = 3.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(destination.title, style = MaterialTheme.typography.titleLarge)
                        Text(destination.detail, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeometryFormulas(viewModel: GeometryViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current
    val formulas by viewModel.formulas.collectAsState()
    val formula by viewModel.selectedFormula.collectAsState()
    val inputs by viewModel.inputs.collectAsState()
    val units by viewModel.inputUnits.collectAsState()
    val target by viewModel.targetVariableSymbol.collectAsState()
    val result by viewModel.calculationResult.collectAsState()
    val listState = rememberLazyListState()
    var selector by rememberSaveable { mutableStateOf(false) }
    // Scrolling only happens while the formulas screen exists.
    LaunchedEffect(result) {
        if (result != null) {
            kotlinx.coroutines.delay(100)
            val count = listState.layoutInfo.totalItemsCount
            if (count > 0) listState.animateScrollToItem(count - 1)
        }
    }
    if (selector) AlertDialog(
        onDismissRequest = { selector = false },
        title = { Text("Выберите формулу") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                items(formulas, key = { it.id }) { choice ->
                    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) {
                        viewModel.selectFormula(choice); selector = false
                    }.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(choice.name, style = MaterialTheme.typography.titleMedium)
                        Text("${choice.subcategory} · ${choice.canonicalEquation}", color = TextSecondary)
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = { selector = false }) { Text("Закрыть") } }
    )
    val selected = formula ?: return
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("geometry_formulas"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Расчёт по формуле", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("Выберите неизвестную величину и введите остальные размеры.", color = TextSecondary)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(selected.subcategory, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    Text(selected.name, style = MaterialTheme.typography.titleLarge)
                    SelectionContainer {
                        Text(selected.canonicalEquation, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
                    }
                    if (selected.description.isNotBlank()) Text(selected.description, color = TextSecondary)
                    OutlinedButton(onClick = { selector = true }, modifier = Modifier.fillMaxWidth()) { Text("Выбрать другую формулу") }
                }
            }
        }
        item {
            Text("Что найти?", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.variables.forEach { variable ->
                    FilterChip(selected = variable.symbol == target, onClick = { viewModel.setTargetVariable(variable.symbol) },
                        label = { Text("${variable.symbol} · ${variable.name}") }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        item { Text("Известные размеры", style = MaterialTheme.typography.titleMedium) }
        items(selected.variables.filter { it.symbol != target }, key = { it.symbol }) { variable ->
            VariableInput(variable, inputs[variable.symbol].orEmpty(), { viewModel.onInputValueChange(variable.symbol, it) },
                units[variable.symbol], { viewModel.onUnitChange(variable.symbol, it) })
        }
        item {
            Button(onClick = { keyboard?.hide(); viewModel.solveFormula() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Вычислить ответ", style = MaterialTheme.typography.titleMedium)
            }
        }
        result?.let { answer ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (answer.success) "Ответ" else "Проверьте данные", style = MaterialTheme.typography.titleMedium)
                        if (answer.success) SelectionContainer {
                            Text("${answer.solvedSymbol} = ${NumberFormatter.formatResult(answer)} ${answer.unitSymbol}".trim(),
                                style = MaterialTheme.typography.headlineMedium)
                        } else Text(answer.errorMessage ?: "Заполните известные размеры.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (answer.success && answer.steps.isNotEmpty()) {
                item { Text("Как получен ответ", style = MaterialTheme.typography.titleLarge) }
                items(answer.steps) { step ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${step.order}. ${step.description}", style = MaterialTheme.typography.titleMedium)
                            if (step.equationLatex.isNotBlank()) SelectionContainer { Text(step.equationLatex, fontFamily = FontFamily.Monospace) }
                            step.substitutionDetails?.let { Text(it, color = TextSecondary) }
                        }
                    }
                }
            }
        }
    }
}
