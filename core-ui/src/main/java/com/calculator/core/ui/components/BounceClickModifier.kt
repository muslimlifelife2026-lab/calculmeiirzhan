package com.calculator.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 21st.dev / iOS inspired kinetic spring bounce effect for clickable elements.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.92f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "bounceScale"
    )

    val modifierWithScale = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    if (onClick != null) {
        modifierWithScale.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        modifierWithScale
    }
}
