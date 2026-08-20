package com.calculator.feature.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.MathKeyboard
import com.calculator.core.ui.components.bounceClick
import com.calculator.core.ui.theme.*
import java.util.Locale

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
            .background(Background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Bento Category Selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ConversionCategory.entries) { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White else SurfaceCard)
                        .border(1.dp, if (isSelected) Color.White else SurfaceBorder, RoundedCornerShape(12.dp))
                        .bounceClick {
                            selectedCategory = category
                            inputValue = "1"
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category.title,
                        color = if (isSelected) Color(0xFF08090C) else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 2. Conversion Bento Cards with Interactive Swap Button
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            // FROM field
            ConversionField(
                label = "ИЗНАЧАЛЬНО",
                value = inputValue,
                unit = fromUnit,
                units = units,
                onUnitSelected = { fromUnit = it },
                isResult = false
            )

            // Interactive Swap Button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), CircleShape)
                        .bounceClick {
                            val temp = fromUnit
                            fromUnit = toUnit
                            toUnit = temp
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = "Swap Units",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // TO field (Result with Glowing Border)
            ConversionField(
                label = "РЕЗУЛЬТАТ",
                value = String.format(Locale.US, "%.6f", resultValue).trimEnd('0').trimEnd('.'),
                unit = toUnit,
                units = units,
                onUnitSelected = { toUnit = it },
                isResult = true
            )
        }

        // 3. High-precision tactile keypad
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
fun ConversionField(
    label: String,
    value: String,
    unit: UnitItem,
    units: List<UnitItem>,
    onUnitSelected: (UnitItem) -> Unit,
    isResult: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isResult) Modifier.border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                else Modifier
            ),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Input / Output
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = if (isResult) Color(0xFF38BDF8) else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (value.isEmpty()) "0" else value,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontFamily = MonospaceFontFamily
                )
            }

            // Unit Selector Pill Dropdown
            Box {
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = unit.symbol,
                            color = Color(0xFF38BDF8),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Select unit",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(SurfaceCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                ) {
                    units.forEach { u ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${u.name} (${u.symbol})",
                                    color = if (u == unit) Color(0xFF38BDF8) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (u == unit) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onUnitSelected(u)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
