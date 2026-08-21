package com.calculator.feature.chemistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.*
import com.calculator.engine.solver.ChemistrySolver

@Composable
fun BalancerSection(
    equationInput: String,
    onEquationChange: (String) -> Unit,
    reactionPresets: List<ReactionPreset>,
    modifier: Modifier = Modifier
) {
    val balanceResult = remember(equationInput) {
        if (equationInput.isNotBlank()) {
            ChemistrySolver.balanceReaction(equationInput)
        } else {
            null
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1-Tap Reaction Presets Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "🔥 Примеры реакций (1 тап):",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(reactionPresets) { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { onEquationChange(preset.equation) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = preset.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Уравнение реакции (реагенты → продукты):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (equationInput.isNotEmpty()) {
                        Text(
                            text = "Очистить",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onEquationChange("") }
                        )
                    }
                }

                TextField(
                    value = equationInput,
                    onValueChange = onEquationChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    placeholder = {
                        Text(
                            text = "H2 + O2 -> H2O",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                )

                // Quick Operators & Elements for Balancer
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    val keys = listOf("+", " -> ", "H", "O", "C", "N", "Fe", "Na", "Cl", "2", "3", "4", "(", ")", "⌫")
                    items(keys) { key ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (key == " -> " || key == "+") Color.White else SurfaceElevated)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (key == "⌫") {
                                        if (equationInput.isNotEmpty()) onEquationChange(equationInput.dropLast(1))
                                    } else {
                                        onEquationChange(equationInput + key)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key.trim(),
                                color = if (key == " -> " || key == "+") Background else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (balanceResult != null) {
            val isSuccess = balanceResult.contains("=")
            if (isSuccess) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "УРАВНЕННАЯ РЕАКЦИЯ:",
                            color = com.calculator.core.ui.theme.AccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = balanceResult,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        HorizontalDivider(color = SurfaceBorder)

                        Text(
                            text = "Коэффициенты реакции расставлены по закону сохранения массы.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = balanceResult,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
