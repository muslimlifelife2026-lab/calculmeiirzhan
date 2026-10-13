package com.aerocalc.smartcalculator.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassSurface
import com.calculator.core.ui.theme.*

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    AeroCalcTheme {
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF36383D), Background)))
                .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("AeroCalc", color = TextSecondary, fontSize = 18.sp, modifier = Modifier.padding(top = 24.dp))
            Text("Сложные задачи.\nПонятные решения.", color = TextPrimary, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
            Text("Начните с обычного примера. Всё остальное — в инструментах.", color = TextSecondary, fontSize = 17.sp, lineHeight = 25.sp)
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.End) {
                    Text("12 × 8", color = TextSecondary, fontSize = 22.sp)
                    Text("96", color = TextPrimary, fontSize = 56.sp, fontWeight = FontWeight.Light)
                }
            }
            WelcomeRow(Icons.Rounded.Calculate, "Считайте сразу", "Обычные и научные вычисления в одном месте.")
            WelcomeRow(Icons.Rounded.GridView, "Находите нужный инструмент", "Формулы, графики, геометрия, химия и единицы.")
            WelcomeRow(Icons.Rounded.History, "Возвращайтесь к ответам", "Завершённые расчёты сохраняются в истории.")
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Начать", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WelcomeRow(icon: ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = AccentSecondary, modifier = Modifier.padding(top = 3.dp).size(25.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
