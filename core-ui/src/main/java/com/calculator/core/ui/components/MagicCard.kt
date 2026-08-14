package com.calculator.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A "Magic UI" inspired Card with an ambient spotlight effect.
 */
@Composable
fun MagicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    spotlightColor: Color = Color(0x3300F0FF), // NeonCyan with 20% alpha
    backgroundColor: Color = Color(0x801A1A24), // Glassy Obsidian
    borderColor: Color = Color(0x33FFFFFF),
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SpotlightTransition")
    
    // Pan left to right
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpotlightOffsetX"
    )
    
    // Pan top to bottom
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpotlightOffsetY"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .drawWithCache {
                val brush = Brush.radialGradient(
                    colors = listOf(spotlightColor, Color.Transparent),
                    center = Offset(size.width * offsetX, size.height * offsetY),
                    radius = size.maxDimension * 0.7f
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = brush)
                }
            }
    ) {
        content()
    }
}
