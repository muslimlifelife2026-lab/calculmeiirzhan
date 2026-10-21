package com.calculator.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlgebraScreen(modifier: Modifier = Modifier) {
    var expression by rememberSaveable { mutableStateOf("(x + 1)^2 - x^2") }
    var operation by rememberSaveable { mutableStateOf(AlgebraOperation.SIMPLIFY) }
    var submittedExpression by rememberSaveable { mutableStateOf<String?>(null) }
    var submittedOperation by rememberSaveable { mutableStateOf(AlgebraOperation.SIMPLIFY) }
    var result by remember { mutableStateOf<AlgebraResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun clearResult() {
        submittedExpression = null
        result = null
        error = null
        busy = false
    }

    LaunchedEffect(submittedExpression, submittedOperation) {
        val input = submittedExpression ?: return@LaunchedEffect
        busy = true
        val outcome = withContext(Dispatchers.Default) {
            try {
                Result.success(PolynomialAlgebra.calculate(input, submittedOperation))
            } catch (exception: IllegalArgumentException) {
                Result.failure(exception)
            } catch (exception: ArithmeticException) {
                Result.failure(exception)
            }
        }
        result = outcome.getOrNull()
        error = outcome.exceptionOrNull()?.message
        busy = false
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("algebra_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Алгебра", style = MaterialTheme.typography.headlineMedium)
        Text("Полиномы и уравнения · без интернета", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AlgebraOperation.SIMPLIFY to "Упростить",
                        AlgebraOperation.DERIVATIVE to "Производная",
                        AlgebraOperation.SOLVE to "Решить"
                    ).forEach { (value, title) ->
                        FilterChip(selected = operation == value, onClick = { operation = value; clearResult() }, label = { Text(title) })
                    }
                }
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
                Button(
                    onClick = { submittedOperation = operation; submittedExpression = expression },
                    enabled = expression.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth().testTag("algebra_calculate")
                ) { Text("Вычислить") }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        result?.let { solution ->
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
                        solution.steps.forEachIndexed { index, step -> Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
        Text(
            "Поддерживается одна переменная x, числа, дроби, + − * / ^ и скобки. " +
                "Упрощение и производная — до степени 12; решение — до степени 2 в действительных числах. " +
                "Деление на выражения с x и функции sin, log пока не поддерживаются. Без знака = полином приравнивается к 0.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
