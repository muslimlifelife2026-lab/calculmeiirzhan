package com.calculator.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.engine.algebra.AlgebraOperation
import com.calculator.engine.algebra.AlgebraResult
import com.calculator.engine.algebra.PolynomialAlgebra
import com.calculator.engine.algebra.LinearSystemKind
import com.calculator.engine.algebra.LinearSystemResult
import com.calculator.engine.algebra.LinearSystemSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlgebraScreen(
    modifier: Modifier = Modifier,
    initialExpression: String? = null,
    initialRequest: Long = 0L,
    onGraph: ((List<String>) -> Unit)? = null
) {
    val initialLines = remember(initialExpression) { initialExpression?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty() }
    var expression by rememberSaveable { mutableStateOf(initialLines.singleOrNull() ?: "(x + 1)^2 - x^2") }
    var operation by rememberSaveable { mutableStateOf(if (initialExpression?.contains('=') == true) AlgebraOperation.SOLVE else AlgebraOperation.SIMPLIFY) }
    var systemMode by rememberSaveable { mutableStateOf(initialLines.size > 1) }
    var firstEquation by rememberSaveable { mutableStateOf(if (initialLines.size > 1) initialLines.first() else "x + y = 3") }
    var secondEquation by rememberSaveable { mutableStateOf(if (initialLines.size > 1) initialLines.drop(1).joinToString("\n") else "x - y = 1") }
    var submittedExpression by rememberSaveable { mutableStateOf<String?>(null) }
    var submittedSecondEquation by rememberSaveable { mutableStateOf("") }
    var submittedSystemMode by rememberSaveable { mutableStateOf(false) }
    var submittedOperation by rememberSaveable { mutableStateOf(AlgebraOperation.SIMPLIFY) }
    var result by remember { mutableStateOf<AlgebraResult?>(null) }
    var resultOperation by remember { mutableStateOf(AlgebraOperation.SIMPLIFY) }
    var systemResult by remember { mutableStateOf<LinearSystemResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var appliedRequest by rememberSaveable { mutableLongStateOf(-1L) }

    fun submitCurrent() {
        submittedOperation = operation
        submittedSystemMode = systemMode
        submittedSecondEquation = if (systemMode) secondEquation else ""
        submittedExpression = if (systemMode) firstEquation else expression
    }

    fun clearResult() {
        submittedExpression = null
        result = null
        systemResult = null
        error = null
        busy = false
    }

    LaunchedEffect(initialExpression, initialRequest) {
        if (initialExpression != null && initialRequest != appliedRequest) {
            systemMode = initialLines.size > 1
            if (systemMode) {
                firstEquation = initialLines.first()
                secondEquation = initialLines.drop(1).joinToString("\n")
            } else {
                expression = initialExpression
                operation = if (initialExpression.contains('=')) AlgebraOperation.SOLVE else AlgebraOperation.SIMPLIFY
            }
            clearResult()
            submitCurrent()
            appliedRequest = initialRequest
        }
    }

    LaunchedEffect(submittedExpression, submittedSecondEquation, submittedSystemMode, submittedOperation) {
        val input = submittedExpression ?: return@LaunchedEffect
        val second = submittedSecondEquation
        val isSystem = submittedSystemMode
        val selectedOperation = submittedOperation
        busy = true
        val outcome = withContext(Dispatchers.Default) {
            try {
                Result.success(
                    if (isSystem) AlgebraCalculation(system = LinearSystemSolver.solve(input, second))
                    else AlgebraCalculation(polynomial = PolynomialAlgebra.calculate(input, selectedOperation))
                )
            } catch (exception: IllegalArgumentException) {
                Result.failure(exception)
            } catch (exception: ArithmeticException) {
                Result.failure(exception)
            }
        }
        if (submittedExpression != input || submittedSecondEquation != second || submittedSystemMode != isSystem || submittedOperation != selectedOperation) return@LaunchedEffect
        resultOperation = selectedOperation
        result = outcome.getOrNull()?.polynomial
        systemResult = outcome.getOrNull()?.system
        error = outcome.exceptionOrNull()?.message
        busy = false
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("algebra_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Алгебра", style = MaterialTheme.typography.headlineMedium)
        Text("Уравнения, системы и объяснения · без интернета", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AlgebraOperation.SIMPLIFY to "Упростить",
                        AlgebraOperation.DERIVATIVE to "Производная",
                        AlgebraOperation.SOLVE to "Решить"
                    ).forEach { (value, title) ->
                        FilterChip(selected = !systemMode && operation == value,
                            onClick = { systemMode = false; operation = value; clearResult() },
                            label = { Text(title) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                    FilterChip(selected = systemMode, onClick = { systemMode = true; clearResult() },
                        label = { Text("Система") }, modifier = Modifier.heightIn(min = 48.dp).testTag("algebra_system_mode"))
                }
                if (systemMode) {
                    Text("Два линейных уравнения с x и y", style = MaterialTheme.typography.titleMedium)
                    Text("Укажите обе части каждого уравнения. Например: 2x + y = 5. Ответы вычисляются точными дробями.",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = firstEquation,
                        onValueChange = { if (it.length <= LinearSystemSolver.MAX_INPUT_LENGTH) { firstEquation = it; clearResult() } },
                        modifier = Modifier.fillMaxWidth().testTag("system_first_equation"),
                        label = { Text("Первое уравнение") }, minLines = 2, maxLines = 4,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), isError = error != null
                    )
                    OutlinedTextField(
                        value = secondEquation,
                        onValueChange = { if (it.length <= LinearSystemSolver.MAX_INPUT_LENGTH) { secondEquation = it; clearResult() } },
                        modifier = Modifier.fillMaxWidth().testTag("system_second_equation"),
                        label = { Text("Второе уравнение") }, minLines = 2, maxLines = 4,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), isError = error != null,
                        supportingText = { Text(error ?: "Используйте x, y, числа, дроби и скобки.") }
                    )
                } else {
                    OutlinedTextField(
                        value = expression,
                        onValueChange = { if (it.length <= PolynomialAlgebra.MAX_INPUT_LENGTH) { expression = it; clearResult() } },
                        modifier = Modifier.fillMaxWidth().testTag("algebra_input"),
                        label = { Text(if (operation == AlgebraOperation.SOLVE) "Уравнение с x" else "Выражение с x") },
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        isError = error != null,
                        supportingText = { Text(error ?: "Например: (x + 1)^2, x/3 или x^2 − 5x + 6 = 0") }
                    )
                }
                Button(
                    onClick = { submitCurrent() },
                    enabled = (if (systemMode) firstEquation.isNotBlank() && secondEquation.isNotBlank() else expression.isNotBlank()) && !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("algebra_calculate")
                ) { Text(if (systemMode) "Решить систему" else "Вычислить") }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (systemMode) {
                        listOf(
                            "Простой пример" to ("x + y = 3" to "x - y = 1"),
                            "Точные дроби" to ("2x + 3y = 1" to "x - y = 0"),
                            "Нет решений" to ("x + y = 2" to "2x + 2y = 5"),
                            "Зависимые уравнения" to ("x + y = 3" to "2x + 2y = 6")
                        ).forEach { (title, preset) ->
                            AssistChip(onClick = { firstEquation = preset.first; secondEquation = preset.second; clearResult() }, label = { Text(title) })
                        }
                    } else {
                        listOf(
                            "Раскрыть скобки" to ("(x + 1)^2 - x^2" to AlgebraOperation.SIMPLIFY),
                            "Найти производную" to ("x^3 - 2x/3 + 9" to AlgebraOperation.DERIVATIVE),
                            "Найти корни" to ("x^2 - 5x + 6 = 0" to AlgebraOperation.SOLVE)
                        ).forEach { (title, preset) ->
                            AssistChip(onClick = { expression = preset.first; operation = preset.second; clearResult() }, label = { Text(title) })
                        }
                    }
                }
            }
        }
        result?.let { solution ->
            val solutionOperation = resultOperation
            val graphFunctions = if (solutionOperation == AlgebraOperation.DERIVATIVE) listOf(solution.normalized, solution.answer) else listOf(solution.normalized)
            val fitsGraph = graphFunctions.all { it.length <= 256 }
            GlassCard(Modifier.fillMaxWidth().testTag("algebra_result")) {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Результат", style = MaterialTheme.typography.titleMedium)
                        Text(solution.answer, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                        solution.roots.forEachIndexed { index, root ->
                            Text("x${if (solution.roots.size > 1) index + 1 else ""} = ${root.exact}", fontFamily = FontFamily.Monospace)
                            root.approximate?.let { Text("≈ $it", color = TextSecondary, fontFamily = FontFamily.Monospace) }
                        }
                        if (solution.roots.any { it.approximate != null }) {
                            Text("Приближённые значения: 14 значащих цифр. Дроби и выражения с √ выше — точные.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        if (onGraph != null && fitsGraph) OutlinedButton(onClick = {
                            onGraph(graphFunctions)
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("algebra_show_graph")) { Text("Показать график") }
                        if (onGraph != null && !fitsGraph) {
                            Text("Выражение слишком длинное для графика. Точное решение показано выше.", color = TextSecondary)
                        } else if (onGraph != null && solutionOperation == AlgebraOperation.SOLVE) {
                            Text("На графике показана разность левой и правой частей. Её нули — решения уравнения.",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else if (onGraph != null && solutionOperation == AlgebraOperation.DERIVATIVE) {
                            Text("На графике будут исходная функция и её производная.",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
            AlgebraSolutionSteps(solution.steps)
        }
        systemResult?.let { solution ->
            GlassCard(Modifier.fillMaxWidth().testTag("linear_system_result")) {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(when (solution.kind) {
                            LinearSystemKind.UNIQUE -> "Единственное решение"
                            LinearSystemKind.NONE -> "Решений нет"
                            LinearSystemKind.INFINITE -> "Бесконечно много решений"
                        }, style = MaterialTheme.typography.titleLarge)
                        if (solution.kind == LinearSystemKind.UNIQUE) {
                            listOf("x", "y").forEach { variable ->
                                Text("$variable = ${solution.solution.getValue(variable)}", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                            }
                            Text("Значения точные: дроби не заменены округлёнными числами.", color = TextSecondary)
                        } else Text(
                            if (solution.kind == LinearSystemKind.NONE) "Ни одна пара x и y не удовлетворяет обоим уравнениям. Причина показана в шагах ниже."
                            else "Уравнения не определяют одну пару x и y. Все допустимые пары должны удовлетворять исходным уравнениям.",
                            color = TextSecondary
                        )
                        if (onGraph != null && solution.graphExpressions.isNotEmpty()) {
                            OutlinedButton(onClick = { onGraph(solution.graphExpressions) },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("system_show_graph")) {
                                Text("Показать график")
                            }
                            Text("На графике показаны прямые исходной системы. Общие точки соответствуют её решениям.",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
            AlgebraSolutionSteps(solution.steps)
        }
        Text(
            if (systemMode) "Система: два линейных уравнения с переменными x и y. Поддерживаются числа, дроби, скобки и линейные выражения. " +
                "Произведения неизвестных, их степени выше первой и деление на выражение с неизвестной не поддерживаются."
            else "Поддерживается одна переменная x, числа, дроби, + − * / ^ и скобки. " +
                "Упрощение и производная — до степени 12; решение — до степени 2 в действительных числах. " +
                "Деление на выражения с x и функции sin, log пока не поддерживаются. Без знака = полином приравнивается к 0.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

private data class AlgebraCalculation(val polynomial: AlgebraResult? = null, val system: LinearSystemResult? = null)

@Composable
private fun AlgebraSolutionSteps(steps: List<String>) {
    if (steps.isEmpty()) return
    Text("Решение по шагам", style = MaterialTheme.typography.titleLarge)
    steps.forEachIndexed { index, step ->
        GlassCard(Modifier.fillMaxWidth()) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Шаг ${index + 1}", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
