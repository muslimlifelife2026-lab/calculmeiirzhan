package com.calculator.feature.calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calculator.core.data.database.CalculationEntity
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.*
import com.calculator.engine.parser.MathParser

@Composable
fun CalculationSteps(expression: String, result: String, steps: List<MathParser.MathStep>, onExport: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Решение по шагам", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text(expression, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Text("= $result", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
        }
        itemsIndexed(steps) { index, step ->
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}. ${step.description}", color = TextSecondary)
                    Text(step.expression, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item { Button(onClick = onExport, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Сохранить PDF") } }
    }
}

@Composable
fun RecentCalculations(items: List<CalculationEntity>, onSelect: (String) -> Unit, onDelete: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Последние вычисления", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Нажмите на ответ, чтобы использовать его", color = TextSecondary)
        }
        if (items.isEmpty()) item { Text("Здесь появятся ваши расчёты после нажатия «=».", color = TextSecondary, modifier = Modifier.padding(vertical = 24.dp)) }
        items(items, key = { it.id }) { item ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onSelect(item.result) }.padding(vertical = 8.dp)) {
                        Text(item.expression, color = TextSecondary)
                        Text(item.result, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    IconButton(onClick = { onDelete(item.id) }) { Icon(Icons.Rounded.DeleteOutline, "Удалить вычисление", tint = TextMuted) }
                }
            }
        }
    }
}
