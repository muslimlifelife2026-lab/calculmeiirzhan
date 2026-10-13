package com.calculator.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.theme.SurfaceBorder
import com.calculator.core.ui.theme.GlassTint

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 0.5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .shadow(5.dp, shape, clip = false)
            .clip(shape)
            .glassBackdrop(12.dp, cornerRadius)
            .background(GlassTint)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent, Color.Black.copy(alpha = 0.07f))))
            .border(borderWidth, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), SurfaceBorder, Color.White.copy(alpha = 0.10f))), shape)
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.CenterStart,
        content = content
    )
}
