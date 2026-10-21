package com.calculator.feature.chemistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import com.calculator.domain.model.Element
import com.calculator.domain.model.PeriodicTable
import com.calculator.feature.chemistry.components.AtomModelCanvas

@Composable
fun AtomModelSection(
    modifier: Modifier = Modifier
) {
    var selectedAtomSymbol by rememberSaveable { mutableStateOf("C") }
    val selectedAtomElement = PeriodicTable.getElement(selectedAtomSymbol)
        ?: Element("C", "Carbon", 6, 12.011)
    val sampleElements = remember { PeriodicTable.allElements }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Модель атома", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("Схема электронных оболочек. Положение и движение электронов показаны условно — это не реальные орбиты.",
            color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        // Quick Elements Selector Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(sampleElements) { elem ->
                val isSelected = selectedAtomElement.atomicNumber == elem.atomicNumber
                FilterChip(selected = isSelected, onClick = { selectedAtomSymbol = elem.symbol },
                    label = { Text("${elem.symbol} · ${elem.nameRu}") }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }

        AtomModelCanvas(
            element = selectedAtomElement,
            modifier = Modifier.fillMaxWidth().height(440.dp)
        )
    }
}
