package com.calculator.feature.graphing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*

data class GraphPreset(
    val title: String,
    val category: String,
    val description: String,
    val expressions: List<String>,
    val mode: GraphMode = GraphMode.CARTESIAN
)

enum class GraphMode(val title: String) {
    CARTESIAN("Декартовы y=f(x)"),
    POLAR("Полярные r=f(θ)"),
    PARAMETRIC("Параметрические (x, y)")
}

val PRESET_LIBRARY = listOf(
    // ─── Calculus & Algebra Classics ───
    GraphPreset(
        title = "Распределение Гаусса (Колокол)",
        category = "Математический анализ",
        description = "Нормальное распределение вероятностей с пиком в нуле",
        expressions = listOf("exp(-x^2)", "0.5 * exp(-0.5 * x^2)"),
        mode = GraphMode.CARTESIAN
    ),
    GraphPreset(
        title = "Затухающие гармонические колебания",
        category = "Физика и волны",
        description = "Экспоненциально затухающая синусоида механической системы",
        expressions = listOf("exp(-0.2*x) * sin(3*x)", "exp(-0.2*x)", "-exp(-0.2*x)"),
        mode = GraphMode.CARTESIAN
    ),
    GraphPreset(
        title = "Биения волн (Интерференция)",
        category = "Физика и волны",
        description = "Сложение двух близких по частоте гармонических колебаний",
        expressions = listOf("sin(4*x) + sin(4.5*x)"),
        mode = GraphMode.CARTESIAN
    ),
    GraphPreset(
        title = "Логистическая сигмоида",
        category = "Машинное обучение & Биология",
        description = "Кривая роста популяции и функция активации нейросетей",
        expressions = listOf("1 / (1 + exp(-2*x))"),
        mode = GraphMode.CARTESIAN
    ),
    GraphPreset(
        title = "Кубический сплайн и экстремумы",
        category = "Математический анализ",
        description = "Полином третьей степени с локальными экстремумами",
        expressions = listOf("x^3 - 3*x"),
        mode = GraphMode.CARTESIAN
    ),
    GraphPreset(
        title = "Столкновение параболы и синусоиды",
        category = "Алгебра & Пересечения",
        description = "Идеально для изучения точек пересечения графиков",
        expressions = listOf("sin(x)", "0.2*x^2 - 1"),
        mode = GraphMode.CARTESIAN
    ),

    // ─── Polar Geometry ───
    GraphPreset(
        title = "Четырехлепестковая Роза Гранди",
        category = "Полярная геометрия",
        description = "Кривая r = cos(2θ) в полярных координатах",
        expressions = listOf("cos(2*theta)"),
        mode = GraphMode.POLAR
    ),
    GraphPreset(
        title = "Кардиоида (Сердцевидная кривая)",
        category = "Полярная геометрия",
        description = "Траектория точки окружности, катящейся по равной неподвижной",
        expressions = listOf("1 - sin(theta)"),
        mode = GraphMode.POLAR
    ),
    GraphPreset(
        title = "Спираль Архимеда",
        category = "Полярная геометрия",
        description = "Кривая с постоянным шагом раскручивания r = a*θ",
        expressions = listOf("0.3 * theta"),
        mode = GraphMode.POLAR
    ),

    // ─── Parametric Curves ───
    GraphPreset(
        title = "Фигура Лиссажу 3:2",
        category = "Параметрические траектории",
        description = "Траектория точки при двух ортогональных гармонических колебаниях",
        expressions = listOf("sin(3*t)", "sin(2*t)"),
        mode = GraphMode.PARAMETRIC
    ),
    GraphPreset(
        title = "Астроида (Гипоциклоида)",
        category = "Параметрические траектории",
        description = "Кривая с четырьмя заострениями x = cos³(t), y = sin³(t)",
        expressions = listOf("cos(t)^3", "sin(t)^3"),
        mode = GraphMode.PARAMETRIC
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsSheet(
    onSelectPreset: (GraphPreset) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1117),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SurfaceBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📚 Библиотека математических кривых",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PRESET_LIBRARY) { preset ->
                    PresetCard(
                        preset = preset,
                        onClick = {
                            onSelectPreset(preset)
                            onDismiss()
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: GraphPreset,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = preset.mode.title.split(" ").first(),
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = preset.description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                preset.expressions.forEach { expr ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = expr,
                            color = Color(0xFFA855F7),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
