package com.aerocalc.smartcalculator.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.*

private data class Lesson(val title: String, val example: String, val explanation: String)

private val lessons = listOf(
    Lesson("Порядок действий", "2 + 3 × 4 = 14", "Сначала умножьте 3 × 4 = 12, затем прибавьте 2. Скобки меняют порядок: (2 + 3) × 4 = 20."),
    Lesson("Проценты", "15% от 200 = 30", "Переведите процент в дробь: 15 / 100 = 0,15. Умножьте 200 × 0,15 = 30."),
    Lesson("Перевод единиц", "1,5 км = 1500 м", "В одном километре 1000 метров. Умножьте 1,5 × 1000 и проверьте результат в конвертере."),
    Lesson("Второй закон Ньютона", "F = ma", "Сила равна произведению массы на ускорение. При m = 2 кг и a = 3 м/с² получаем F = 6 Н.")
)

@Composable
fun LearningScreen(onOpenPhysicsPractice: () -> Unit, onOpenConverter: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 12.dp)) {
                Text("Обучение", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Короткие объяснения и практика", color = TextSecondary, fontSize = 15.sp)
            }
        }
        items(lessons, key = { it.title }) { lesson ->
            var expanded by rememberSaveable(lesson.title) { mutableStateOf(false) }
            GlassCard(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClickLabel = "Открыть урок ${lesson.title}") { expanded = !expanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(lesson.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(lesson.example, color = AccentSecondary, style = MaterialTheme.typography.titleLarge)
                    if (expanded) Text(lesson.explanation, color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                    else Text("Нажмите, чтобы увидеть объяснение", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClickLabel = "Открыть тренажёр по физике", onClick = onOpenPhysicsPractice)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.AutoStories, null, tint = AccentSecondary)
                    Column(Modifier.weight(1f)) {
                        Text("Тренажёр по физике", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Собирайте формулы и проверяйте ответ", color = TextSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.Rounded.ArrowForward, null, tint = TextMuted)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClickLabel = "Открыть конвертер", onClick = onOpenConverter)) {
                Text("Проверить перевод единиц в конвертере", color = TextPrimary)
            }
        }
    }
}
