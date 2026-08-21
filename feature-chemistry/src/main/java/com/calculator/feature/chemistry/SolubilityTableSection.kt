package com.calculator.feature.chemistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import com.calculator.domain.model.SolubilityData
import com.calculator.domain.model.SolubilityStatus

@Composable
fun SolubilityTableSection(
    onPairClick: (Pair<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val cations = SolubilityData.cations
    val anions = SolubilityData.anions
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Таблица Растворимости Солей",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Нажмите на ячейку для просмотра подробностей",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column {
                // Header Row: Anions
                Row {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x22FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Кат\\Ан", color = TextSecondary, fontSize = 9.sp)
                    }
                    anions.forEach { anion ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0x1AFFFFFF))
                                .border(0.5.dp, Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(anion, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Data Rows: Cation x Anions
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cations) { cation ->
                        Row {
                            // Left Cation Header
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x1AFFFFFF))
                                    .border(0.5.dp, Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cation, color = com.calculator.core.ui.theme.AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Status cells
                            anions.forEach { anion ->
                                val status = SolubilityData.getStatus(cation, anion)
                                val badgeBg = Color(status.hexColor).copy(alpha = 0.85f)

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(badgeBg)
                                        .border(0.5.dp, Color(0x33FFFFFF))
                                        .clickable { onPairClick(cation to anion) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status.code,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
