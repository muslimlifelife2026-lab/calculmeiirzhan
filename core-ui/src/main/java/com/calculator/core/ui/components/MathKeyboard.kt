package com.calculator.core.ui.components

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*

fun Modifier.adaptiveKineticClick(
    onClick: () -> Unit,
    onHapticAndSound: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "adaptive_bounce"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                onHapticAndSound()
                onClick()
            }
        )
}

@Composable
fun MathKeyboard(
    modifier: Modifier = Modifier,
    onKeyPressed: (String) -> Unit,
    showVariables: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val scientificKeys = mutableListOf("sin(", "cos(", "tan(", "sqrt(", "^", "fact(", "ln(", "log(", "%", "π", "e")
    if (showVariables) {
        scientificKeys.add(0, "x")
        scientificKeys.add(1, "y")
    }

    val rows = listOf(
        listOf("C", "(", ")", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Scientific functions compact top strip (34.dp height)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(scientificKeys) { key ->
                val isVariable = key == "x" || key == "y"
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isVariable) SurfaceElevated else KeyOperator)
                        .border(
                            1.dp,
                            if (isVariable) AccentPrimary.copy(alpha = 0.5f) else SurfaceBorder,
                            RoundedCornerShape(18.dp)
                        )
                        .adaptiveKineticClick(
                            onClick = { onKeyPressed(key) },
                            onHapticAndSound = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                            }
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key,
                        color = if (isVariable) AccentPrimary else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5 Fully Responsive & Dynamic Rows that adapt to the exact remaining height
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowKeys.forEach { key ->
                    val isOperator = key in listOf("÷", "×", "-", "+")
                    val isEqual = key == "="
                    val isClear = key == "C"
                    val isSecondary = key in listOf("(", ")")
                    val isZero = key == "0"

                    val weight = if (isZero) 2f else 1f

                    val buttonBg = when {
                        isEqual -> AccentPrimary       // Solid Titanium White
                        isClear -> KeyClear            // Dark Crimson accent
                        isOperator -> KeyOperator       // Dark Slate
                        isSecondary -> KeyOperator
                        else -> KeyNumeric              // Deep Charcoal (#1A1D27)
                    }

                    val borderStroke = when {
                        isEqual -> AccentPrimary
                        isClear -> ErrorRed.copy(alpha = 0.4f)
                        else -> SurfaceBorder
                    }

                    val textColor = when {
                        isEqual -> Background          // Pure Obsidian text on White button
                        isClear -> ErrorRed
                        isOperator -> TextPrimary
                        isSecondary -> TextSecondary
                        else -> TextPrimary
                    }

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(buttonBg)
                            .border(1.dp, borderStroke, RoundedCornerShape(20.dp))
                            .adaptiveKineticClick(
                                onClick = { onKeyPressed(key) },
                                onHapticAndSound = {
                                    when {
                                        isEqual -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            view.playSoundEffect(SoundEffectConstants.CLICK)
                                        }
                                        isClear -> {
                                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                            view.playSoundEffect(SoundEffectConstants.CLICK)
                                        }
                                        isOperator -> {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            view.playSoundEffect(SoundEffectConstants.CLICK)
                                        }
                                        else -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                            view.playSoundEffect(SoundEffectConstants.CLICK)
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = textColor,
                            fontSize = if (isOperator || isEqual || isClear) 24.sp else 22.sp,
                            fontWeight = if (isOperator || isEqual || isClear) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
