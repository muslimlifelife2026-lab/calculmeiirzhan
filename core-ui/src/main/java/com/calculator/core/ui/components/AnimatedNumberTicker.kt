package com.calculator.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * A "Magic UI" inspired number ticker. 
 * Animates character changes by sliding them like an odometer.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedNumberTicker(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Row(modifier = modifier) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (targetState.isDigit() && initialState.isDigit()) {
                        val targetDigit = targetState.digitToInt()
                        val initialDigit = initialState.digitToInt()
                        if (targetDigit > initialDigit) {
                            (slideInVertically(animationSpec = tween(300)) { height -> height } + fadeIn(animationSpec = tween(300))) togetherWith 
                            (slideOutVertically(animationSpec = tween(300)) { height -> -height } + fadeOut(animationSpec = tween(300)))
                        } else {
                            (slideInVertically(animationSpec = tween(300)) { height -> -height } + fadeIn(animationSpec = tween(300))) togetherWith 
                            (slideOutVertically(animationSpec = tween(300)) { height -> height } + fadeOut(animationSpec = tween(300)))
                        }
                    } else {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }
                },
                label = "TickerAnimation_$index"
            ) { currentChar ->
                Text(
                    text = currentChar.toString(),
                    style = style,
                    color = color,
                    softWrap = false
                )
            }
        }
    }
}
