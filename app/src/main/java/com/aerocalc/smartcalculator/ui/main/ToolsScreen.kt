package com.aerocalc.smartcalculator.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.*

data class ScienceTool(val id: String, val title: String, val description: String, val icon: ImageVector)
val scienceTools = listOf(
    ScienceTool("converter", "Конвертер", "Длина, масса, температура и другие единицы", Icons.Rounded.SwapHoriz),
    ScienceTool("physics", "Физика", "Формулы, движение и учебные задачи", Icons.Rounded.Science),
    ScienceTool("geometry", "Геометрия", "Площади, объёмы и трёхмерные фигуры", Icons.Rounded.Architecture),
    ScienceTool("graphs", "Графики", "Функции, пересечения и таблица значений", Icons.AutoMirrored.Rounded.ShowChart),
    ScienceTool("chemistry", "Химия", "Молярная масса, реакции и элементы", Icons.Rounded.Biotech)
)

@Composable
fun ToolsScreen(
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    privacyOptionsRequired: Boolean = false,
    onPrivacyOptions: () -> Unit = {},
    onChangeAgeGroup: () -> Unit = {}
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 12.dp)) {
                Text("Инструменты", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Выберите, что нужно посчитать", color = TextSecondary, fontSize = 15.sp)
            }
        }
        items(scienceTools, key = { it.id }) { tool ->
            GlassCard(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClickLabel = "Открыть ${tool.title}") { onSelect(tool.id) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceElevated), contentAlignment = Alignment.Center) {
                        Icon(tool.icon, null, tint = AccentSecondary, modifier = Modifier.size(25.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tool.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(tool.description, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (privacyOptionsRequired) {
            item {
                TextButton(onClick = onPrivacyOptions, modifier = Modifier.fillMaxWidth()) {
                    Text("Настройки конфиденциальности")
                }
            }
        }
        item {
            TextButton(onClick = onChangeAgeGroup, modifier = Modifier.fillMaxWidth()) {
                Text("Изменить возрастную группу")
            }
        }
    }
}
