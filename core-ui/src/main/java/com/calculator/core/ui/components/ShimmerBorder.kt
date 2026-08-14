package com.calculator.core.ui.components

import android.graphics.Matrix
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A "Magic UI" inspired shimmer border.
 * Creates a rotating sweep gradient around the component's border.
 */
fun Modifier.shimmerBorder(
    width: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    shimmerColor: Color = Color(0xFF00F0FF), // NeonCyan
    durationMillis: Int = 3000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "ShimmerTransition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerAngle"
    )

    val brush = remember(angle, shimmerColor) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val center = size.center
                val shader = SweepGradientShader(
                    center = center,
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        shimmerColor,
                        Color.Transparent,
                        Color.Transparent
                    ),
                    colorStops = listOf(0.0f, 0.4f, 0.5f, 0.6f, 1.0f)
                )
                
                // Rotate the shader around the center
                val matrix = Matrix()
                matrix.postRotate(angle, center.x, center.y)
                shader.setLocalMatrix(matrix)
                
                return shader
            }
        }
    }

    this.border(width = width, brush = brush, shape = shape)
}
