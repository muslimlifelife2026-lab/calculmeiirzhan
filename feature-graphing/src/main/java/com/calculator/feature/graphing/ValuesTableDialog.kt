package com.calculator.feature.graphing

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.SurfaceElevated
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.core.ui.utils.NumberFormatter
import com.calculator.engine.solver.GraphEvaluator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ValuesTableDialog(
    expressions: List<String>,
    mode: GraphMode,
    params: Map<String, Double> = emptyMap(),
    onDismiss: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(0.5) }
    val evaluators = remember(expressions) {
        // Preserve each column's position, including a blank or invalid expression.
        expressions.map { expression ->
            if (expression.isBlank()) null else try { GraphEvaluator(expression) } catch (_: IllegalArgumentException) { null }
        }
    }
    val rows = remember(evaluators, mode, step, params) {
        fun shown(value: Double?) = if (value != null && value.isFinite()) NumberFormatter.formatCompact(value) else "—"
        val symbol = when (mode) { GraphMode.PARAMETRIC -> "t"; GraphMode.POLAR -> "theta"; GraphMode.CARTESIAN -> "x" }
        List((10.0 / step).toInt() + 1) { index ->
            val coordinate = -5.0 + index * step
            listOf(shown(coordinate)) + evaluators.map { shown(it?.evaluate(coordinate, params, symbol)) }
        }
    }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.86f
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassCard(Modifier.padding(16.dp).widthIn(max = 600.dp).fillMaxWidth().heightIn(max = maxHeight)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    when (mode) {
                        GraphMode.CARTESIAN -> "Таблица значений X–Y"
                        GraphMode.POLAR -> "Таблица значений θ–r"
                        GraphMode.PARAMETRIC -> "Таблица значений t–X–Y"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text("Диапазон от −5 до 5. Прочерк — выражение не задано или значение не определено.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.1, 0.25, 0.5, 1.0, 2.0).forEach { value ->
                        FilterChip(selected = step == value, onClick = { step = value }, label = { Text("Шаг $value") })
                    }
                }
                Column(Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState())) {
                    val headers = listOf(when (mode) { GraphMode.CARTESIAN -> "X"; GraphMode.POLAR -> "θ"; GraphMode.PARAMETRIC -> "t" }) +
                        evaluators.indices.map { index -> when (mode) {
                            GraphMode.CARTESIAN -> "Y${index + 1}"
                            GraphMode.POLAR -> "r${index + 1}"
                            GraphMode.PARAMETRIC -> if (index == 0) "X" else "Y"
                        } }
                    TableRow(headers, header = true)
                    LazyColumn(Modifier.width((headers.size * 120).dp).heightIn(max = 340.dp)) {
                        items(rows) { row -> TableRow(row) }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
            }
        }
    }
}

@Composable
private fun TableRow(values: List<String>, header: Boolean = false) {
    Row(Modifier.background(if (header) SurfaceElevated else Color.Transparent).padding(vertical = 8.dp)) {
        values.forEachIndexed { index, value ->
            Text(value, Modifier.width(120.dp).padding(horizontal = 8.dp),
                style = if (header) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace, color = if (index == 0) TextSecondary else MaterialTheme.colorScheme.onSurface)
        }
    }
}
