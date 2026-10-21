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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val cellSize = (52 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Таблица растворимости",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Прокрутите таблицу и выберите пару ионов. ? — нет проверенных данных.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
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
                            .size(cellSize)
                            .background(Color(0x22FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Кат\\Ан", color = TextSecondary, fontSize = 9.sp)
                    }
                    anions.forEach { anion ->
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(Color(0x1AFFFFFF))
                                .border(0.5.dp, Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(anion, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                    .size(cellSize)
                                    .background(Color(0x1AFFFFFF))
                                    .border(0.5.dp, Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cation, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            // Status cells
                            anions.forEach { anion ->
                                val status = SolubilityData.getStatus(cation, anion)
                                val badgeBg = Color(status.hexColor).copy(alpha = 0.22f)

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .background(badgeBg)
                                        .border(0.5.dp, Color(0x33FFFFFF))
                                        .clickable(role = Role.Button) { onPairClick(cation to anion) }
                                        .semantics { contentDescription = "$cation и $anion: ${status.title}" },
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
