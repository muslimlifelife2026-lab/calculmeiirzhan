package com.calculator.feature.chemistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    var selectedAtomElement by remember { mutableStateOf(PeriodicTable.getElement("C") ?: Element("C", "Carbon", 6, 12.011)) }
    val sampleElements = remember { PeriodicTable.allElements }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Quick Elements Selector Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(sampleElements) { elem ->
                val isSelected = selectedAtomElement.atomicNumber == elem.atomicNumber
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color.White else SurfaceElevated)
                        .border(1.dp, if (isSelected) Color.White else SurfaceBorder, RoundedCornerShape(14.dp))
                        .clickable { selectedAtomElement = elem }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${elem.symbol} (${elem.name})",
                        color = if (isSelected) Background else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        AtomModelCanvas(
            element = selectedAtomElement,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
