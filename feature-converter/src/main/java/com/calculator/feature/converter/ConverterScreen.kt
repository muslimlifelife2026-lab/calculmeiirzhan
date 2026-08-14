package com.calculator.feature.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.MathKeyboard
import com.calculator.core.ui.theme.NeonCyan

@Composable
fun ConverterScreen(
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ConversionCategory.LENGTH) }
    
    val units = UnitConverter.getUnitsForCategory(selectedCategory)
    var fromUnit by remember(selectedCategory) { mutableStateOf(units.first()) }
    var toUnit by remember(selectedCategory) { mutableStateOf(units.getOrNull(1) ?: units.first()) }
    
    var inputValue by remember { mutableStateOf("1") }
    
    val resultValue = remember(inputValue, fromUnit, toUnit) {
        val value = inputValue.toDoubleOrNull() ?: 0.0
        UnitConverter.convert(value, fromUnit, toUnit, selectedCategory)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.calculator.core.ui.theme.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category Selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ConversionCategory.entries) { category ->
                CategoryChip(
                    title = category.title,
                    isSelected = category == selectedCategory,
                    onClick = {
                        selectedCategory = category
                        inputValue = "1" // Reset on category change
                    }
                )
            }
        }

        // Conversion Fields
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            // FROM field
            ConversionField(
                label = "Из",
                value = inputValue,
                unit = fromUnit,
                units = units,
                onUnitSelected = { fromUnit = it },
                onValueChanged = { inputValue = it },
                isReadOnly = true // Assuming we use custom keyboard
            )

            // Arrow down
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "To",
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            // TO field
            ConversionField(
                label = "В",
                value = String.format("%.6f", resultValue).trimEnd('0').trimEnd('.', ','),
                unit = toUnit,
                units = units,
                onUnitSelected = { toUnit = it },
                onValueChanged = {}, // Read only
                isReadOnly = true
            )
        }

        // Custom Numpad Keyboard
        MathKeyboard(
            onKeyPressed = { key ->
                inputValue = when (key) {
                    "C" -> ""
                    "⌫" -> if (inputValue.isNotEmpty()) inputValue.dropLast(1) else ""
                    "." -> if (!inputValue.contains(".")) inputValue + "." else inputValue
                    "=" -> inputValue
                    "+", "-", "×", "÷", "(", ")", "^", "sin", "cos", "tan", "ln", "log", "π", "e", "x", "y" -> inputValue
                    else -> if (inputValue == "0") key else inputValue + key
                }
                if (inputValue.isEmpty()) inputValue = "0"
            },
            showVariables = false
        )
    }
}

@Composable
fun CategoryChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x1AFFFFFF)
    val textColor = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionField(
    label: String,
    value: String,
    unit: UnitItem,
    units: List<UnitItem>,
    onUnitSelected: (UnitItem) -> Unit,
    onValueChanged: (String) -> Unit,
    isReadOnly: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Input
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Unit Selector
            Box {
                Surface(
                    color = Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = unit.symbol,
                            color = NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Select unit",
                            tint = NeonCyan,
                            modifier = Modifier.padding(start = 4.dp).size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF1E1E2E))
                ) {
                    units.forEach { item ->
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(item.name, color = Color.White)
                                    Text(item.symbol, color = Color.White.copy(alpha = 0.5f))
                                }
                            },
                            onClick = {
                                onUnitSelected(item)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
