package com.calculator.feature.graphing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.calculator.core.ui.theme.*
import com.calculator.engine.solver.GraphEvaluator
import com.calculator.core.ui.utils.NumberFormatter

@Composable
fun ValuesTableDialog(
    expressions: List<String>,
    mode: GraphMode,
    params: Map<String, Double> = emptyMap(),
    onDismiss: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(0.5) }
    var rangeMin by rememberSaveable { mutableStateOf(-5.0) }
    var rangeMax by rememberSaveable { mutableStateOf(5.0) }

    val validEvaluators = remember(expressions) {
        expressions.mapNotNull { expr ->
            try {
                if (expr.isNotBlank()) GraphEvaluator(expr) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    val rows = remember(validEvaluators, mode, step, rangeMin, rangeMax, params) {
        fun shown(value: Double) = if (value.isFinite()) NumberFormatter.formatCompact(value) else "—"
        val list = mutableListOf<List<String>>()
        var x = rangeMin
        while (x <= rangeMax + 1e-6) {
            val rowValues = mutableListOf(shown(x))
            if (mode == GraphMode.PARAMETRIC && validEvaluators.size >= 2) {
                rowValues += shown(validEvaluators[0].evaluate(x, params, "t"))
                rowValues += shown(validEvaluators[1].evaluate(x, params, "t"))
            } else for (evaluator in validEvaluators) {
                val y = evaluator.evaluate(x, params, if (mode == GraphMode.POLAR) "theta" else "x")
                rowValues += shown(y)
            }
            list.add(rowValues)
            x += step
        }
        list
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF27292D))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (mode) {
                            GraphMode.CARTESIAN -> "Таблица значений X–Y"
                            GraphMode.POLAR -> "Таблица значений θ–r"
                            GraphMode.PARAMETRIC -> "Таблица значений t–X–Y"
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Step Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Шаг:", color = TextSecondary, fontSize = 12.sp)
                    listOf(0.1, 0.25, 0.5, 1.0, 2.0).forEach { s ->
                        val isSelected = step == s
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFB9C6D6) else SurfaceElevated)
                                .clickable { step = s }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$s",
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (mode) { GraphMode.CARTESIAN -> "X"; GraphMode.POLAR -> "θ"; GraphMode.PARAMETRIC -> "t" },
                        color = Color(0xFFB9C6D6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    validEvaluators.forEachIndexed { i, _ ->
                        Text(
                            text = when (mode) { GraphMode.CARTESIAN -> "Y${i + 1}"; GraphMode.POLAR -> "r${i + 1}"; GraphMode.PARAMETRIC -> if (i == 0) "X" else "Y" },
                            color = when (i) {
                                0 -> Color.White
                                1 -> Color(0xFFB9C6D6)
                                else -> Color(0xFFB9ADC8)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Table Rows
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(rows) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCard)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row.forEachIndexed { colIdx, value ->
                                Text(
                                    text = value,
                                    color = if (colIdx == 0) Color(0xFF94A3B8) else Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
