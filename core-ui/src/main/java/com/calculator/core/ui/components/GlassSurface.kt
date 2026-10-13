package com.calculator.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.theme.*

/** Layered glass styling without blurring text or requiring a newer Android API. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    elevation: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(GlassTint)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.11f), Color.Transparent, Color.Black.copy(alpha = 0.12f))))
            .border(1.dp, Brush.verticalGradient(listOf(GlassHighlight, SurfaceBorder, Color.White.copy(alpha = 0.12f))), shape),
        content = content
    )
}
