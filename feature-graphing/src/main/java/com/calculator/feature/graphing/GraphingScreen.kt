package com.calculator.feature.graphing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.Background
import com.calculator.core.ui.theme.AccentCyan
import com.calculator.core.ui.theme.AccentViolet
import com.calculator.core.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphingScreen(
    modifier: Modifier = Modifier
) {
    var expressions by remember { mutableStateOf(listOf("sin(x)*x", "", "")) }

    val graphColors = listOf(
        Color(0xFF00FFB2), // Cyber Teal
        Color(0xFF8B5CF6), // Electric Violet
        Color(0xFFFBBF24)  // Accent Amber
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "📈 Графики Функций",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Построение до 3 функций одновременно. Нажмите на холст для просмотра координат.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        // Top section: Input fields for up to 3 functions
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Функции y = f(x)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                expressions.forEachIndexed { index, expr ->
                    val color = graphColors[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Color Indicator Badge
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, RoundedCornerShape(3.dp))
                        )
                        
                        TextField(
                            value = expr,
                            onValueChange = { newVal ->
                                val list = expressions.toMutableList()
                                list[index] = newVal
                                expressions = list
                            },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0x1AFFFFFF),
                                unfocusedContainerColor = Color(0x0DFFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = color,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            placeholder = {
                                Text(
                                    text = "f${index + 1}(x) = ...",
                                    color = Color.White.copy(alpha = 0.2f),
                                    fontSize = 15.sp
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                        )
                    }
                }

                // Quick Presets
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf("sin(x)", "cos(x)*x", "x^2", "sqrt(x)", "tan(x)")
                    items(presets) { preset ->
                        androidx.compose.material3.FilterChip(
                            selected = expressions[0] == preset,
                            onClick = {
                                val list = expressions.toMutableList()
                                list[0] = preset
                                expressions = list
                            },
                            label = { Text(preset, color = Color.White, fontSize = 12.sp) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                containerColor = Color(0x1AFFFFFF),
                                selectedContainerColor = AccentViolet.copy(alpha = 0.6f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Bottom section: Interactive Graph Canvas
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                GraphCanvas(
                    expressions = expressions,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
