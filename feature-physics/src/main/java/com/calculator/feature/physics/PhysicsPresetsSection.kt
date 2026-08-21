package com.calculator.feature.physics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.domain.model.Formula
import com.calculator.core.ui.theme.SurfaceCard
import com.calculator.core.ui.theme.SurfaceBorder
import com.calculator.core.ui.theme.TextSecondary

data class PhysicsPreset(
    val title: String,
    val formulaName: String,
    val target: String,
    val inputs: Map<String, String>,
    val description: String
)

@Composable
fun PhysicsPresetsSection(
    formulas: List<Formula>,
    onPresetApply: (Formula, Map<String, String>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember {
        listOf(
            PhysicsPreset("🏎️ Разгон авто", "velocity_kinematics", "v_f", mapOf("v_i" to "0", "a" to "5", "t" to "6"), "Разгон с 0 до скорости за 6 сек при a=5 м/с²"),
            PhysicsPreset("📱 Падение яблока", "potential_energy", "Ep", mapOf("m" to "0.2", "g" to "9.81", "h" to "3"), "Энергия яблока 200г на высоте 3м"),
            PhysicsPreset("🚗 Торможение", "force_newton", "F", mapOf("m" to "1500", "a" to "8"), "Сила для торможения авто 1.5т"),
            PhysicsPreset("💧 Масса воды", "density", "m", mapOf("d" to "1000", "V" to "0.2"), "Масса 200 литров (0.2м³) воды"),
            PhysicsPreset("💡 Закон Ома", "ohm_law", "I", mapOf("U" to "220", "R" to "44"), "Сила тока при R=44 Ом"),
            PhysicsPreset("🏃‍♂️ Энергия бега", "kinetic_energy", "E_k", mapOf("m" to "70", "v" to "8"), "Кинетическая энергия человека 70кг при скорости 8м/с"),
            PhysicsPreset("🏋️ Работа силы", "work_force", "A", mapOf("F" to "500", "s" to "20"), "Работа при перемещении груза силой 500Н на 20м")
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "🚀 Примеры из жизни (1 клик):",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(presets) { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val matched = formulas.firstOrNull { it.id == preset.formulaName }
                                ?: formulas.firstOrNull { it.name.contains(preset.formulaName, ignoreCase = true) }
                            if (matched != null) {
                                onPresetApply(matched, preset.inputs, preset.target)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
