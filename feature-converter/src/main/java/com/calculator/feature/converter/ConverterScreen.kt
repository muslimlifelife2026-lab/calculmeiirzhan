package com.calculator.feature.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.math.MathContext

@Composable
fun ConverterScreen(
    modifier: Modifier = Modifier
) {
    var categoryName by rememberSaveable { mutableStateOf(ConversionCategory.LENGTH.name) }
    val selectedCategory = ConversionCategory.valueOf(categoryName)
    
    val units = UnitConverter.getUnitsForCategory(selectedCategory)
    var fromIndex by rememberSaveable(categoryName) { mutableIntStateOf(0) }
    var toIndex by rememberSaveable(categoryName) { mutableIntStateOf(1) }
    val fromUnit = units[fromIndex.coerceIn(units.indices)]
    val toUnit = units[toIndex.coerceIn(units.indices)]
    
    var inputValue by rememberSaveable { mutableStateOf("1") }
    var temperatureDifference by rememberSaveable { mutableStateOf(false) }
    
    val conversion = remember(inputValue, fromUnit, toUnit, temperatureDifference) {
        runCatching {
            val value = inputValue.toBigDecimalOrNull()
                ?: throw IllegalArgumentException("Завершите ввод числа")
            UnitConverter.convert(value, fromUnit, toUnit, selectedCategory, temperatureDifference)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
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
                            categoryName = category.name
                            inputValue = "1"
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category.title,
                        color = if (isSelected) Color(0xFF161719) else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (selectedCategory == ConversionCategory.TEMPERATURE) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Изменение температуры (ΔT)", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = temperatureDifference, onCheckedChange = { temperatureDifference = it })
            }
        }

        // 2. Conversion Bento Cards with Interactive Swap Button
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        ) {
            // FROM field
            ConversionField(
                label = "ИЗНАЧАЛЬНО",
                value = inputValue,
                unit = fromUnit,
                units = units,
                onUnitSelected = { fromIndex = units.indexOf(it) },
                isResult = false
            )

            // Interactive Swap Button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                        .border(1.dp, Color(0xFFB9C6D6).copy(alpha = 0.4f), CircleShape)
                        .bounceClick {
                            val temp = fromIndex
                            fromIndex = toIndex
                            toIndex = temp
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = "Поменять единицы местами",
                        tint = Color(0xFFB9C6D6),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // TO field (Result with Glowing Border)
            ConversionField(
                label = "РЕЗУЛЬТАТ",
                value = conversion.getOrNull()?.let {
                    com.calculator.core.ui.utils.NumberFormatter.formatExact(it.round(MathContext(14)))
                } ?: "—",
                unit = toUnit,
                units = units,
                onUnitSelected = { toIndex = units.indexOf(it) },
                isResult = true
            )
            conversion.exceptionOrNull()?.let {
                Text(it.message ?: "Проверьте введённое число", color = ErrorRed, fontSize = 13.sp)
            }
            Text(
                if (selectedCategory == ConversionCategory.TEMPERATURE && temperatureDifference)
                    "ΔT — изменение, без смещения шкалы: 1 °C = 1 K = 1,8 °F."
                else "Расчёт: до 34 значащих цифр. Показ: до 14. Точность измерения зависит от исходных данных.",
                color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp
            )
        }

        // 3. High-precision tactile keypad
        MathKeyboard(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            numbersOnly = true,
            onKeyPressed = { key ->
                inputValue = when (key) {
                    "C" -> ""
                    "⌫" -> if (inputValue.isNotEmpty()) inputValue.dropLast(1) else ""
                    "." -> if (!inputValue.contains(".")) inputValue + "." else inputValue
                    "=" -> inputValue
                    "±" -> if (inputValue.startsWith("-")) inputValue.removePrefix("-") else "-$inputValue"
                    else -> if (key.all { it.isDigit() }) { if (inputValue == "0") key else inputValue + key } else inputValue
                }
                if (inputValue.isEmpty()) inputValue = "0"
                inputValue = inputValue.take(48)
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
                if (isResult) Modifier.border(1.dp, Color(0xFFB9C6D6).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                else Modifier
            ),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Input / Output
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = if (isResult) Color(0xFFB9C6D6) else TextSecondary,
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
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
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
                            color = Color(0xFFB9C6D6),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Выбрать единицу",
                            tint = Color(0xFFB9C6D6),
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
                                    color = if (u == unit) Color(0xFFB9C6D6) else Color.White,
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
