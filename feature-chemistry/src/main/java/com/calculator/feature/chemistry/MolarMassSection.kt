package com.calculator.feature.chemistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.util.Locale

@Composable
fun MolarMassSection(
    formulaInput: String,
    onFormulaChange: (String) -> Unit,
    molarPresets: List<ChemicalPreset>,
    modifier: Modifier = Modifier
) {
    val massResult = remember(formulaInput) {
        if (formulaInput.isNotBlank()) {
            ChemistrySolver.solveMolarMass(formulaInput)
        } else {
            null
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1-Tap Substance Presets Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "✨ Популярные вещества (1 тап):",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(molarPresets) { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { onFormulaChange(preset.formula) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${preset.title} (${preset.formula})",
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
                        text = "Химическая формула:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (formulaInput.isNotEmpty()) {
                        Text(
                            text = "Очистить",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onFormulaChange("") }
                        )
                    }
                }
                
                TextField(
                    value = formulaInput,
                    onValueChange = onFormulaChange,
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    placeholder = {
                        Text(
                            text = "Например: H2SO4 или C6H12O6",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                )

                // Quick Chemistry Keyboard Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    val keys = listOf("H", "O", "C", "N", "Na", "Cl", "Fe", "Ca", "S", "K", "Al", "Cu", "(", ")", "2", "3", "4", "5", "6", "⌫")
                    items(keys) { key ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (key == "⌫") ErrorRed.copy(alpha = 0.2f) else SurfaceElevated)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (key == "⌫") {
                                        if (formulaInput.isNotEmpty()) onFormulaChange(formulaInput.dropLast(1))
                                    } else {
                                        onFormulaChange(formulaInput + key)
                                    }
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                color = if (key == "⌫") ErrorRed else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (massResult != null) {
            if (massResult.success) {
                GlassCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "МОЛЯРНАЯ МАССА:",
                            color = com.calculator.core.ui.theme.AccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = String.format(Locale.US, "%.3f г/моль", massResult.molarMass),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        HorizontalDivider(color = SurfaceBorder)

                        Text(
                            text = "Состав и массовые доли:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(massResult.elementCounts.toList()) { (element, count) ->
                                val elementTotalMass = element.atomicMass * count
                                val fractionPercent = (elementTotalMass / massResult.molarMass) * 100.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SurfaceElevated)
                                                .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = element.symbol,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Text(
                                            text = "${element.name} (×$count)",
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", fractionPercent)}%",
                                        color = Color(0xFF10B981),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = massResult.errorMessage ?: "Проверьте правильность химической формулы",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
