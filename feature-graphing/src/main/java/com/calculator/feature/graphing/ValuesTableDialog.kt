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

@Composable
fun ValuesTableDialog(
    expressions: List<String>,
    params: Map<String, Double> = emptyMap(),
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0.5) }
    var rangeMin by remember { mutableStateOf(-5.0) }
    var rangeMax by remember { mutableStateOf(5.0) }

    val validEvaluators = remember(expressions) {
        expressions.mapNotNull { expr ->
            try {
                if (expr.isNotBlank()) GraphEvaluator(expr) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    val rows = remember(validEvaluators, step, rangeMin, rangeMax, params) {
        val list = mutableListOf<List<String>>()
        var x = rangeMin
        while (x <= rangeMax + 1e-6) {
            val rowValues = mutableListOf(String.format("%.2f", x).trimEnd('0').trimEnd('.'))
            for (evaluator in validEvaluators) {
                val y = evaluator.evaluate(x, params)
                rowValues.add(
                    if (y.isNaN() || y.isInfinite()) "—" else String.format("%.4f", y).trimEnd('0').trimEnd('.')
                )
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
                .background(Color(0xFF0F1117))
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
                        text = "📋 Таблица значений X-Y",
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
                    Text("Шаг Δx:", color = TextSecondary, fontSize = 12.sp)
                    listOf(0.1, 0.25, 0.5, 1.0, 2.0).forEach { s ->
                        val isSelected = step == s
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF38BDF8) else SurfaceElevated)
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
                        text = "X",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    validEvaluators.forEachIndexed { i, _ ->
                        Text(
                            text = "Y${i + 1}",
                            color = when (i) {
                                0 -> Color.White
                                1 -> Color(0xFF38BDF8)
                                else -> Color(0xFFA855F7)
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
