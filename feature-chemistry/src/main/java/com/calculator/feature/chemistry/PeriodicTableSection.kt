package com.calculator.feature.chemistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import com.calculator.core.ui.components.GlassCard
import com.calculator.domain.model.Element
import com.calculator.domain.model.PeriodicTable

@Composable
fun PeriodicTableSection(
    onElementClick: (Element) -> Unit,
    modifier: Modifier = Modifier
) {
    val elements = remember { PeriodicTable.allElements }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val fontScale = LocalDensity.current.fontScale

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Элементы", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Найти элемент") },
            placeholder = { Text("Fe, железо или 26") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = SurfaceBorder,
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        val filtered = elements.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.nameRu.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true) ||
            it.atomicNumber.toString().contains(searchQuery)
        }

        if (filtered.isEmpty()) {
            Text("Элемент не найден. Попробуйте символ, название или атомный номер.", color = TextSecondary)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = (100 * fontScale.coerceIn(1f, 1.8f)).dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(filtered, key = { it.atomicNumber }) { elem ->
                GlassCard(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(role = Role.Button) { onElementClick(elem) }
                ) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = elem.atomicNumber.toString(),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = elem.symbol,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = elem.nameRu,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
