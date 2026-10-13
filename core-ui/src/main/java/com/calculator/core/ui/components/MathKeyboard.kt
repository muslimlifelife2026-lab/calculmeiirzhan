package com.calculator.core.ui.components

import android.view.HapticFeedbackConstants
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import kotlinx.coroutines.flow.collect

@Composable
fun MathKeyboard(
    modifier: Modifier = Modifier,
    onKeyPressed: (String) -> Unit,
    showVariables: Boolean = false,
    numbersOnly: Boolean = false
) {
    var scientific by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    val rows = if (numbersOnly) listOf(
        listOf("C", "±", "⌫"), listOf("7", "8", "9"), listOf("4", "5", "6"),
        listOf("1", "2", "3"), listOf("0", ".", "=")
    ) else listOf(
        listOf("C", "⌫", "%", "÷"), listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"), listOf("1", "2", "3", "+"), listOf("0", ".", "=")
    )
    BoxWithConstraints(modifier.fillMaxSize()) {
        val minimum = if (scientific && !numbersOnly) 470.dp else if (numbersOnly) 280.dp else 330.dp
        val contentHeight = maxHeight.coerceAtLeast(minimum)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().height(contentHeight), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!numbersOnly) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            scientific = !scientific
                        }) {
                            Text("Научный", color = TextSecondary, fontSize = 13.sp)
                            Icon(if (scientific) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                if (scientific) "Скрыть научные функции" else "Показать научные функции", tint = TextSecondary)
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKeyPressed("(")
                        }) { Text("(", color = TextSecondary) }
                        TextButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKeyPressed(")")
                        }) { Text(")", color = TextSecondary) }
                    }
                    if (scientific) {
                        val functions = listOf("sin(", "cos(", "tan(", "sqrt(", "^", "fact(", "ln(", "log(", "π", "e") +
                            if (showVariables) listOf("x", "y") else emptyList()
                        functions.chunked(5).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { key ->
                                    KeyboardKey(key, Modifier.weight(1f).height(44.dp), small = true) { onKeyPressed(key) }
                                }
                                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            KeyboardKey(key, Modifier.weight(if (key == "0" && !numbersOnly) 2f else 1f).fillMaxHeight()) {
                                onKeyPressed(key)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(key: String, modifier: Modifier, small: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    var pressPosition by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(interactionSource, view) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                pressPosition = interaction.pressPosition
                view.performHapticFeedback(
                    if (key == "=" && Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.KEYBOARD_TAP
                )
            }
        }
    }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessHigh),
        label = "keyPress"
    )
    val pressGlow by animateFloatAsState(if (pressed) 0.16f else 0f, animationSpec = tween(100), label = "glassPress")
    val equal = key == "="
    val operator = key in listOf("+", "-", "×", "÷")
    val special = key in listOf("C", "⌫", "%", "±")
    val fill = when { equal -> KeyEqual; key == "C" -> KeyClear; operator -> KeyOperator; special || small -> KeySpecial; else -> KeyNumeric }
    val foreground = when { equal -> Background; key == "C" -> ErrorRed; small -> TextSecondary; else -> TextPrimary }
    val cornerRadius = if (small) 16.dp else 28.dp
    val shape = RoundedCornerShape(cornerRadius)
    val label = when(key) {
        "C" -> "Очистить"; "⌫" -> "Удалить последний символ"; "=" -> "Равно"
        "sqrt(" -> "Квадратный корень"; "fact(" -> "Факториал"; "±" -> "Изменить знак"
        else -> key
    }
    Box(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale; translationY = if (pressed) 1.dp.toPx() else 0f }
            .clip(shape).then(if (equal) Modifier else Modifier.glassBackdrop(6.dp, cornerRadius)).background(fill)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (operator) 0.11f else 0.08f), Color.Transparent, Color.Black.copy(alpha = 0.08f))))
            .drawWithCache {
                val contact = if (pressPosition.x.isFinite() && pressPosition.y.isFinite()) pressPosition else Offset(size.width / 2f, size.height / 2f)
                val contactLight = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = pressGlow), Color.Transparent),
                    center = contact, radius = size.maxDimension.coerceAtLeast(1f)
                )
                onDrawBehind { if (pressGlow > 0f) drawRect(contactLight) }
            }
            .border(0.8.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = if (pressed) 0.5f else if (equal) 0.3f else 0.26f), SurfaceBorder, Color.White.copy(alpha = 0.07f))), shape)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = foreground, modifier = Modifier.size(24.dp))
        else Text(
            when(key) { "sqrt(" -> "√"; "fact(" -> "n!"; "sin(" -> "sin"; "cos(" -> "cos"; "tan(" -> "tan"; "ln(" -> "ln"; "log(" -> "log"; "-" -> "−"; else -> key },
            color = foreground, fontSize = if (small) 16.sp else 27.sp, fontWeight = FontWeight.Medium
        )
    }
}
