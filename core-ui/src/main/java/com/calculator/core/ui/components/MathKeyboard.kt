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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

fun Modifier.kineticBounceClick(
    onClick: () -> Unit,
    onHapticAndSound: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "kinetic_bounce"
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

    val standardKeys = listOf(
        "C", "(", ")", "÷",
        "7", "8", "9", "×",
        "4", "5", "6", "-",
        "1", "2", "3", "+",
        "0", ".", "="
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Scientific functions scrollable row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scientificKeys) { key ->
                val isVariable = key == "x" || key == "y"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (isVariable) SurfaceElevated else KeyOperator)
                        .border(
                            1.dp,
                            if (isVariable) AccentPrimary.copy(alpha = 0.4f) else SurfaceBorder,
                            RoundedCornerShape(percent = 50)
                        )
                        .kineticBounceClick(
                            onClick = { onKeyPressed(key) },
                            onHapticAndSound = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                            }
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = key,
                        color = if (isVariable) AccentPrimary else TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Standard Keyboard Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = standardKeys,
                span = { key ->
                    if (key == "0") GridItemSpan(2) else GridItemSpan(1)
                }
            ) { key ->
                val isOperator = key in listOf("÷", "×", "-", "+")
                val isEqual = key == "="
                val isClear = key == "C"
                val isSecondary = key in listOf("(", ")")

                val shape = if (key == "0") RoundedCornerShape(percent = 50) else CircleShape

                val buttonBg = when {
                    isEqual -> AccentPrimary // Solid Titanium White
                    isClear -> KeyClear      // Subtle Dark Crimson
                    isOperator -> KeyOperator // Dark Slate 800
                    isSecondary -> KeyOperator
                    else -> KeyNumeric        // Deep Charcoal Slate
                }

                val borderStroke = when {
                    isEqual -> AccentPrimary
                    isClear -> ErrorRed.copy(alpha = 0.3f)
                    else -> SurfaceBorder
                }

                val textColor = when {
                    isEqual -> Background    // Pure Obsidian Text on White button
                    isClear -> ErrorRed
                    isOperator -> TextPrimary
                    isSecondary -> TextSecondary
                    else -> TextPrimary
                }

                val boxModifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (key == "0") 2.1f else 1f)
                    .clip(shape)
                    .background(buttonBg)
                    .border(1.dp, borderStroke, shape)
                    .kineticBounceClick(
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
                    )

                Box(
                    modifier = boxModifier,
                    contentAlignment = if (key == "0") Alignment.CenterStart else Alignment.Center
                ) {
                    Text(
                        text = key,
                        color = textColor,
                        fontSize = 26.sp,
                        fontWeight = if (isOperator || isEqual || isClear) FontWeight.Bold else FontWeight.Medium,
                        modifier = if (key == "0") Modifier.padding(start = 28.dp) else Modifier
                    )
                }
            }
        }
    }
}
