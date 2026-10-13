package com.aerocalc.smartcalculator.ui.main

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassSurface
import com.calculator.core.ui.theme.*

data class NavigationItem(val title: String, val icon: ImageVector)

@Composable
fun GlassNavigation(
    selected: Int,
    items: List<NavigationItem>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val view = LocalView.current
    GlassSurface(modifier.fillMaxWidth(), radius = 32.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(5.dp)) {
            val gap = 4.dp
            val itemWidth = (maxWidth - gap * (items.size - 1)) / items.size
            val indicatorX by animateDpAsState(
                (itemWidth + gap) * selected.coerceIn(0, items.lastIndex),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                label = "glassTabPosition"
            )
            Box(Modifier.fillMaxWidth().drawBehind {
                val topLeft = Offset(indicatorX.toPx(), 0f)
                val indicatorSize = Size(itemWidth.toPx(), size.height)
                val corners = CornerRadius(27.dp.toPx())
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.23f), Color.White.copy(alpha = 0.10f))),
                    topLeft = topLeft, size = indicatorSize, cornerRadius = corners
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.23f), topLeft = topLeft,
                    size = indicatorSize, cornerRadius = corners, style = Stroke(1.dp.toPx())
                )
            }) {
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    items.forEachIndexed { index, item ->
                        val active = index == selected
                        val tint by animateColorAsState(if (active) TextPrimary else TextSecondary, label = "tabTint")
                        val shape = RoundedCornerShape(27.dp)
                        Column(
                            Modifier.weight(1f).clip(shape)
                                .selectable(active, role = Role.Tab, onClick = {
                                    if (!active) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onSelect(index)
                                    }
                                })
                                .padding(horizontal = 4.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(item.icon, null, tint = tint, modifier = Modifier.size(23.dp))
                            Text(item.title, color = tint,
                                fontSize = 11.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
