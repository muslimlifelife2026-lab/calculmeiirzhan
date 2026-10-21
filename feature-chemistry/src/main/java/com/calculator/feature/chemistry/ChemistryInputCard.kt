package com.calculator.feature.chemistry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextSecondary

@Composable
internal fun ChemistryInputCard(label: String, value: String, onChange: (String) -> Unit, placeholder: String, shortcuts: List<String>) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { onChange("") }, enabled = value.isNotEmpty()) { Text("Очистить") }
            }
            OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
                label = { Text(label) }, placeholder = { Text(placeholder) }, shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace), singleLine = true)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shortcuts) { key ->
                    OutlinedButton(onClick = { onChange(if (key == "⌫") value.dropLast(1) else value + key) },
                        modifier = Modifier.heightIn(min = 48.dp).semantics {
                            contentDescription = if (key == "⌫") "Удалить последний символ" else "Вставить ${key.trim()}"
                        }, contentPadding = PaddingValues(horizontal = 14.dp)) { Text(key.trim()) }
                }
            }
        }
    }
}

@Composable
internal fun ChemistryExamples(examples: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Попробуйте пример", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(examples) { (title, formula) ->
                SuggestionChip(onClick = { onSelect(formula) }, label = { Text(title) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

internal fun chemistryErrorMessage(message: String?): String {
    val prefix = "Ошибка в формуле: "
    if (message?.startsWith(prefix) == true) return prefix + chemistryErrorMessage(message.removePrefix(prefix))
    return when (message) {
        "Formula is empty", "Formula has no atoms" -> "Введите формулу вещества, например H2O."
        "Formula is too long" -> "Формула слишком длинная: допустимо до 100 символов."
        "Mismatched parentheses" -> "Проверьте парные скобки в формуле."
        "Empty group" -> "В скобках должны быть атомы, например Ca(OH)2."
        "Invalid atom count" -> "Индекс атома должен быть целым числом от 1 до 1 000 000."
        "Atom count is too large" -> "В формуле слишком много атомов: предел для одного элемента — 1 000 000."
        null -> "Проверьте химическую формулу."
        else -> when {
            message.startsWith("Unknown element: ") -> "Неизвестный элемент: ${message.substringAfter(": ")}. Проверьте регистр букв."
            message.startsWith("Invalid character in formula: ") -> "Недопустимый символ: ${message.substringAfter(": ")}. Используйте символы элементов, индексы и круглые скобки."
            else -> message
        }
    }
}
