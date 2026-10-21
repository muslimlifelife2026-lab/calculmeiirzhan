package com.calculator.feature.geometry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.domain.geometry.PlaneViewport
import kotlin.math.round

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaneConstructionScreen(model: PlaneConstructionViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val drawing = state.drawing
    var x by rememberSaveable { mutableStateOf("0") }
    var y by rememberSaveable { mutableStateOf("0") }
    var first by rememberSaveable { mutableIntStateOf(0) }
    var second by rememberSaveable { mutableIntStateOf(0) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(drawing.points) {
        if (drawing.point(first) == null) first = drawing.points.firstOrNull()?.id ?: 0
        if (drawing.point(second) == null) second = drawing.points.firstOrNull { it.id != first }?.id ?: first
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)
        .testTag("geometry_construction"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Построения на плоскости", style = MaterialTheme.typography.headlineSmall)
        Text("Коснитесь сетки, чтобы поставить точку с шагом 0,5. Для точного положения введите координаты ниже.", color = TextSecondary)
        GlassCard(Modifier.fillMaxWidth()) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f).clipToBounds().testTag("construction_canvas")
                .semantics { contentDescription = "Координатная сетка от −10 до 10. Точек: ${drawing.points.size}; отрезков: ${drawing.segments.size}; окружностей: ${drawing.circles.size}." }
                .pointerInput(model) {
                    detectTapGestures { position ->
                        val viewport = PlaneViewport(size.width.toDouble(), size.height.toDouble())
                        val px = round(viewport.worldX(position.x.toDouble()) * 2) / 2
                        val py = round(viewport.worldY(position.y.toDouble()) * 2) / 2
                        model.addPoint(px, py)
                    }
                }) {
                val viewport = PlaneViewport(size.width.toDouble(), size.height.toDouble())
                fun point(px: Double, py: Double) = Offset(viewport.screenX(px).toFloat(), viewport.screenY(py).toFloat())
                for (value in -10..10) {
                    val color = if (value == 0) Color(0xFFB9C4CF) else Color.White.copy(alpha = .10f)
                    drawLine(color, point(value.toDouble(), -10.0), point(value.toDouble(), 10.0))
                    drawLine(color, point(-10.0, value.toDouble()), point(10.0, value.toDouble()))
                }
                drawing.circles.forEach { circle ->
                    val center = drawing.point(circle.center) ?: return@forEach
                    drawCircle(Color(0xFFD5C5A7), (drawing.distance(circle.center, circle.through) * viewport.pixelsPerUnit).toFloat(),
                        point(center.x, center.y), style = Stroke(1.5.dp.toPx()))
                }
                drawing.segments.forEach { segment ->
                    val a = drawing.point(segment.from) ?: return@forEach
                    val b = drawing.point(segment.to) ?: return@forEach
                    drawLine(Color(0xFFA5C6D8), point(a.x, a.y), point(b.x, b.y), 2.dp.toPx())
                }
                drawing.points.forEach { drawCircle(Color.White, 4.dp.toPx(), point(it.x, it.y)) }
            }
        }
        Text("Одинаковый масштаб X и Y · центр (0; 0) · шаг сетки 1", color = TextSecondary,
            style = MaterialTheme.typography.bodySmall)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Добавить точку", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(x, { x = it.take(24) }, label = { Text("X: −10…10") }, singleLine = true,
                        modifier = Modifier.weight(1f).testTag("construction_x"))
                    OutlinedTextField(y, { y = it.take(24) }, label = { Text("Y: −10…10") }, singleLine = true,
                        modifier = Modifier.weight(1f).testTag("construction_y"))
                }
                Button(onClick = { model.addPoint(x, y) }, enabled = drawing.points.size < 24,
                    modifier = Modifier.fillMaxWidth().testTag("construction_add")) { Text("Добавить точку") }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (drawing.points.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Первая точка / центр", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        drawing.points.forEach { p -> FilterChip(selected = first == p.id, onClick = { first = p.id },
                            label = { Text("${p.label} (${p.x}; ${p.y})") }, modifier = Modifier.heightIn(min = 48.dp)) }
                    }
                    Text("Вторая точка / точка окружности", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        drawing.points.forEach { p -> FilterChip(selected = second == p.id, onClick = { second = p.id },
                            label = { Text(p.label) }, modifier = Modifier.heightIn(min = 48.dp)) }
                    }
                    Button(onClick = { model.addSegment(first, second) }, enabled = first != second,
                        modifier = Modifier.fillMaxWidth()) { Text("Построить отрезок") }
                    OutlinedButton(onClick = { model.addCircle(first, second) }, enabled = first != second,
                        modifier = Modifier.fillMaxWidth()) { Text("Построить окружность") }
                    TextButton(onClick = { model.removePoint(first) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Удалить первую точку и связанные фигуры")
                    }
                    if (first != second && drawing.point(first) != null && drawing.point(second) != null)
                        Text("Расстояние: ${drawing.distance(first, second)}", color = TextSecondary)
                }
            }
        }
        if (drawing.segments.isNotEmpty() || drawing.circles.isNotEmpty()) GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("На чертеже", style = MaterialTheme.typography.titleMedium)
                drawing.segments.forEach { Text("${drawing.point(it.from)?.label}${drawing.point(it.to)?.label}: длина ${drawing.distance(it.from, it.to)}") }
                drawing.circles.forEach { Text("Центр ${drawing.point(it.center)?.label}, через ${drawing.point(it.through)?.label}: радиус ${drawing.distance(it.center, it.through)}") }
            }
        }
        OutlinedButton(onClick = model::undo, enabled = state.canUndo, modifier = Modifier.fillMaxWidth()) { Text("Отменить построение") }
        TextButton(onClick = { confirmClear = true }, enabled = drawing.points.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Очистить чертёж") }
        Text("Черновик сохраняется на устройстве. До 24 точек и 32 фигур. Подписи точек и точные координаты доступны в списке выше.", color = TextSecondary,
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("Очистить чертёж?") },
        text = { Text("Все точки и фигуры будут удалены. Можно отменить кнопкой «Отменить построение».") },
        confirmButton = { TextButton(onClick = { model.clear(); confirmClear = false }) { Text("Очистить") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Оставить") } })
}
