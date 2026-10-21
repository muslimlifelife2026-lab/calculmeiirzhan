package com.calculator.feature.geometry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.round

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StereometrySection() {
    var shape by rememberSaveable { mutableStateOf(Shape3DType.CUBE) }
    var radius by rememberSaveable { mutableFloatStateOf(5f) }
    var height by rememberSaveable { mutableFloatStateOf(10f) }
    var sideA by rememberSaveable { mutableFloatStateOf(6f) }
    var sideB by rememberSaveable { mutableFloatStateOf(8f) }
    var sideC by rememberSaveable { mutableFloatStateOf(10f) }
    var autoRotate by rememberSaveable { mutableStateOf(false) }
    val (volume, area) = remember(shape, radius, height, sideA, sideB, sideC) {
        // Double arithmetic is ample for these bounded, user-controlled dimensions.
        val r = radius.toDouble(); val h = height.toDouble()
        val a = sideA.toDouble(); val b = sideB.toDouble(); val c = sideC.toDouble()
        when (shape) {
            Shape3DType.CUBE -> a * b * c to 2 * (a * b + b * c + a * c)
            Shape3DType.SPHERE -> 4.0 / 3.0 * PI * r * r * r to 4 * PI * r * r
            Shape3DType.CYLINDER -> PI * r * r * h to 2 * PI * r * (r + h)
            Shape3DType.CONE -> PI * r * r * h / 3 to PI * r * (r + hypot(r, h))
            Shape3DType.PYRAMID -> a * a * h / 3 to a * a + 2 * a * hypot(a / 2, h)
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("geometry_3d"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Форма в пространстве", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("Поверните модель пальцем. Изменения размеров сразу отражаются на фигуре и в расчёте.", color = TextSecondary)
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Shape3DType.entries.forEach { candidate ->
                    FilterChip(selected = shape == candidate, onClick = { shape = candidate },
                        label = { Text(candidate.title) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        item {
            Stereometry3DCanvas(shapeType = shape,
                dimA = if (shape in listOf(Shape3DType.SPHERE, Shape3DType.CYLINDER, Shape3DType.CONE)) radius * 2 else sideA,
                dimB = if (shape in listOf(Shape3DType.CYLINDER, Shape3DType.CONE, Shape3DType.PYRAMID)) height else sideB,
                dimC = sideC, autoRotate = autoRotate, modifier = Modifier.fillMaxWidth().height(280.dp))
        }
        item {
            OutlinedButton(onClick = { autoRotate = !autoRotate }, modifier = Modifier.fillMaxWidth()) {
                Text(if (autoRotate) "Остановить вращение" else "Вращать автоматически")
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Объём", color = TextSecondary)
                    SelectionContainer { Text(String.format(Locale.getDefault(), "%.2f ед³", volume), style = MaterialTheme.typography.headlineMedium) }
                    HorizontalDivider()
                    Text("Полная площадь поверхности", color = TextSecondary)
                    SelectionContainer { Text(String.format(Locale.getDefault(), "%.2f ед²", area), style = MaterialTheme.typography.headlineMedium) }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Размеры фигуры", style = MaterialTheme.typography.titleLarge)
                    Text("Единицы условные. Пропорции сохраняются; масштаб изображения подбирается автоматически.", color = TextSecondary)
                    when (shape) {
                        Shape3DType.SPHERE -> DimensionSlider("Радиус r", radius, 15f) { radius = it }
                        Shape3DType.CYLINDER, Shape3DType.CONE -> {
                            DimensionSlider("Радиус r", radius, 15f) { radius = it }
                            DimensionSlider("Высота h", height, 20f) { height = it }
                        }
                        Shape3DType.CUBE -> {
                            DimensionSlider("Длина a", sideA, 15f) { sideA = it }
                            DimensionSlider("Ширина b", sideB, 15f) { sideB = it }
                            DimensionSlider("Высота c", sideC, 15f) { sideC = it }
                        }
                        Shape3DType.PYRAMID -> {
                            Text("Правильная пирамида с квадратным основанием.", color = TextSecondary)
                            DimensionSlider("Сторона основания a", sideA, 15f) { sideA = it }
                            DimensionSlider("Высота h", height, 20f) { height = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionSlider(title: String, value: Float, maximum: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$title · ${String.format(Locale.getDefault(), "%.1f", value)}", style = MaterialTheme.typography.titleMedium)
        Slider(value = value, onValueChange = { onChange(round(it * 10) / 10) },
            valueRange = 1f..maximum, steps = ((maximum - 1) * 10).toInt() - 1, modifier = Modifier.fillMaxWidth())
    }
}
