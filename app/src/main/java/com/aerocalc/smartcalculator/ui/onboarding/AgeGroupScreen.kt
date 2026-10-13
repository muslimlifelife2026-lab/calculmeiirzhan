package com.aerocalc.smartcalculator.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.*

enum class AdAgeGroup { UNDER_13, TEEN, ADULT }

@Composable
fun AgeGroupScreen(onSelect: (AdAgeGroup) -> Unit) {
    AeroCalcTheme {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(SurfaceElevated, Background)))
                .statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Перед началом", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Выберите вашу возрастную группу. Она нужна только для настройки рекламы.",
                color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(28.dp))
            listOf(
                AdAgeGroup.UNDER_13 to "Младше 13 лет",
                AdAgeGroup.TEEN to "13–17 лет",
                AdAgeGroup.ADULT to "18 лет и старше"
            ).forEach { (group, title) ->
                GlassCard(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClickLabel = title) { onSelect(group) }
                ) {
                    Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("Для пользователей младше 13 лет реклама отключена.",
                color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
