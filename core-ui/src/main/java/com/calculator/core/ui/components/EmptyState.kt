package com.calculator.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.TextMuted
import com.calculator.core.ui.theme.TextPrimary
import com.calculator.core.ui.theme.TextSecondary

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun EmptyHistoryState(modifier: Modifier = Modifier) {
    EmptyState(
        emoji = "🕰️",
        title = "История пуста",
        subtitle = "Выполните первый расчёт, и он появится здесь",
        modifier = modifier
    )
}

@Composable
fun EmptyGraphState(modifier: Modifier = Modifier) {
    EmptyState(
        emoji = "📈",
        title = "Нет графиков",
        subtitle = "Введите математическую функцию f(x) для построения",
        modifier = modifier
    )
}
