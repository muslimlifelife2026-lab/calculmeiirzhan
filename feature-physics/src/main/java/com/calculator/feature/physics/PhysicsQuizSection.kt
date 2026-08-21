package com.calculator.feature.physics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.domain.model.Formula
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextPrimary
import com.calculator.core.ui.theme.TextSecondary

@Composable
fun PhysicsQuizSection(formulas: List<Formula>, modifier: Modifier = Modifier) {
    if (formulas.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    var quizIndex by remember { mutableStateOf(0) }
    val formula = formulas[quizIndex % formulas.size]

    val cleanEquation = formula.canonicalEquation.replace(" ", "")
    val originalTokens = remember(cleanEquation) {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < cleanEquation.length) {
            val c = cleanEquation[i]
            if (c.isLetter()) {
                val start = i
                while (i < cleanEquation.length && (cleanEquation[i].isLetterOrDigit() || cleanEquation[i] == '_')) {
                    i++
                }
                tokens.add(cleanEquation.substring(start, i))
            } else if (c == '*' || c == '+' || c == '-' || c == '/' || c == '=' || c == '^') {
                tokens.add(c.toString())
                i++
            } else if (c.isDigit()) {
                val start = i
                while (i < cleanEquation.length && cleanEquation[i].isDigit()) {
                    i++
                }
                tokens.add(cleanEquation.substring(start, i))
            } else {
                tokens.add(c.toString())
                i++
            }
        }
        tokens
    }

    var options by remember(originalTokens) { mutableStateOf(originalTokens.shuffled()) }
    var userAnswer by remember(originalTokens) { mutableStateOf<List<String>>(emptyList()) }
    var quizMessage by remember(originalTokens) { mutableStateOf("") }
    var isSuccess by remember(originalTokens) { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Соберите формулу:",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${formula.name} (${formula.subcategory})",
            color = com.calculator.core.ui.theme.AccentViolet,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        // User Answer Box
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (userAnswer.isEmpty()) {
                    Text("Нажимайте на блоки снизу...", color = TextSecondary, fontSize = 14.sp)
                } else {
                    userAnswer.forEachIndexed { index, token ->
                        Button(
                            onClick = {
                                userAnswer = userAnswer.toMutableList().also { it.removeAt(index) }
                                options = options + token
                                isSuccess = null
                                quizMessage = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = com.calculator.core.ui.theme.AccentViolet),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(token, color = Color.White)
                        }
                    }
                }
            }
        }

        // Available Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            options.forEachIndexed { index, token ->
                Button(
                    onClick = {
                        userAnswer = userAnswer + token
                        options = options.toMutableList().also { it.removeAt(index) }
                        isSuccess = null
                        quizMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(token, color = TextPrimary)
                }
            }
        }

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    userAnswer = emptyList()
                    options = originalTokens.shuffled()
                    isSuccess = null
                    quizMessage = ""
                }
            ) {
                Text("Сброс", color = TextSecondary)
            }

            Button(
                onClick = {
                    if (userAnswer == originalTokens) {
                        isSuccess = true
                        quizMessage = "🎉 Правильно! Отличная работа!"
                    } else {
                        isSuccess = false
                        quizMessage = "❌ Неверно. Попробуйте еще раз!"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = com.calculator.core.ui.theme.AccentAmber)
            ) {
                Text("Проверить", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (quizMessage.isNotEmpty()) {
            Text(
                text = quizMessage,
                color = if (isSuccess == true) Color(0xFF00FFB2) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (isSuccess == true) {
            Button(
                onClick = {
                    quizIndex++
                    userAnswer = emptyList()
                    isSuccess = null
                    quizMessage = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFB2))
            ) {
                Text("Следующая формула ➡️", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
