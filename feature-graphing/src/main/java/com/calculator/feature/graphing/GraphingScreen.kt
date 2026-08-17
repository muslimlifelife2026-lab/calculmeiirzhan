package com.calculator.feature.graphing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.calculator.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphingScreen(
    modifier: Modifier = Modifier
) {
    var expressions by remember { mutableStateOf(listOf("sin(x)", "0.5*x - 1")) }
    var showRoots by remember { mutableStateOf(true) }
    var showExtremums by remember { mutableStateOf(true) }

    val graphColors = listOf(
        Color(0xFFFFFFFF), // Titanium White
        Color(0xFF38BDF8), // Ice Blue
        Color(0xFFA855F7)  // Purple
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Function Inputs Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Функции y = f(x)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Roots Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (showRoots) Color(0xFF06B6D4).copy(alpha = 0.2f) else SurfaceElevated)
                            .border(1.dp, if (showRoots) Color(0xFF06B6D4) else SurfaceBorder, RoundedCornerShape(14.dp))
                            .clickable { showRoots = !showRoots }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🎯 Корни",
                            color = if (showRoots) Color(0xFF06B6D4) else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Extremums Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (showExtremums) Color(0xFFF59E0B).copy(alpha = 0.2f) else SurfaceElevated)
                            .border(1.dp, if (showExtremums) Color(0xFFF59E0B) else SurfaceBorder, RoundedCornerShape(14.dp))
                            .clickable { showExtremums = !showExtremums }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📊 Экстремумы",
                            color = if (showExtremums) Color(0xFFF59E0B) else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            expressions.forEachIndexed { index, expr ->
                val color = graphColors[index % graphColors.size]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(color)
                    )

                    OutlinedTextField(
                        value = expr,
                        onValueChange = { newText ->
                            val updated = expressions.toMutableList()
                            updated[index] = newText
                            expressions = updated
                        },
                        placeholder = { Text("y${index + 1} = ...", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceElevated,
                            unfocusedContainerColor = SurfaceElevated,
                            focusedBorderColor = color,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    if (expressions.size > 1) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevated)
                                .clickable {
                                    val updated = expressions.toMutableList()
                                    updated.removeAt(index)
                                    expressions = updated
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (expressions.size < 3) {
                Button(
                    onClick = { expressions = expressions + "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Добавить функцию", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        // Full Responsive Graph Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
        ) {
            GraphCanvas(
                expressions = expressions.filter { it.isNotBlank() },
                showRoots = showRoots,
                showExtremums = showExtremums,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
