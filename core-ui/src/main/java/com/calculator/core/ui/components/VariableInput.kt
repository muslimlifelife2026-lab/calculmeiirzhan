package com.calculator.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.domain.model.MeasurementUnit
import com.calculator.domain.model.Variable

/** Stacked labels keep the numeric editor usable with large fonts and on narrow phones. */
@Composable
fun VariableInput(
    variable: Variable,
    value: String,
    onValueChange: (String) -> Unit,
    selectedUnit: MeasurementUnit?,
    onUnitSelect: (MeasurementUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember(variable.symbol) { mutableStateOf(false) }
    GlassCard(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(variable.name, style = MaterialTheme.typography.titleMedium)
            if (variable.description.isNotBlank()) {
                Text(variable.description, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(variable.symbol) },
                placeholder = { Text("Введите число") },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            )
            if (variable.supportedUnits.isNotEmpty()) {
                Box {
                    OutlinedButton(onClick = { dropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Единица: ${selectedUnit?.symbol ?: variable.defaultUnitSymbol}  ▾")
                    }
                    DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        variable.supportedUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.name} (${unit.symbol})") },
                                onClick = { onUnitSelect(unit); dropdownExpanded = false }
                            )
                        }
                    }
                }
            } else if (variable.defaultUnitSymbol.isNotBlank()) {
                Text("Единица: ${variable.defaultUnitSymbol}", color = TextSecondary)
            }
        }
    }
}
