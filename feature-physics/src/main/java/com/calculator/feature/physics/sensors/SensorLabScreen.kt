package com.calculator.feature.physics.sensors

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.components.GlassTabs
import com.calculator.core.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun SensorLabScreen(modifier: Modifier = Modifier, model: SensorLabViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) model.stop() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); model.stop() }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) model.export(uri)
    }
    val latest = state.samples.lastOrNull()
    val chart = remember(state.samples) { state.samples.takeLast(240) }
    val extent = remember(chart) { max(1.0, chart.maxOfOrNull { max(abs(it.x), max(abs(it.y), abs(it.z))) } ?: 1.0) * 1.1 }
    val duration = if (state.samples.size > 1) (state.samples.last().timestampNanos - state.samples.first().timestampNanos) / 1e9 else 0.0
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Лаборатория датчиков", style = MaterialTheme.typography.headlineSmall)
        Text("Измерьте ускорение телефона и сохраните эксперимент.", color = TextSecondary)
        GlassTabs(listOf("С гравитацией", "Без гравитации"), if (state.linear) 1 else 0, { model.selectMode(it == 1) })
        val available = if (state.linear) model.linearAvailable else model.accelerometerAvailable
        Text(if (state.linear) "Линейное ускорение: оценка без гравитации. Возможны смещение нуля и шум."
            else "Акселерометр включает гравитацию: у неподвижного телефона модуль обычно около 9,8 м/с².", color = TextSecondary)
        if (!available) Text("Этот датчик недоступен на устройстве.", color = MaterialTheme.colorScheme.error)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ускорение, м/с²", style = MaterialTheme.typography.titleMedium)
                Text("X: ${sensorNumber(latest?.x)}   Y: ${sensorNumber(latest?.y)}   Z: ${sensorNumber(latest?.z)}")
                Text("Модуль: ${sensorNumber(latest?.magnitude)}")
                Text("${state.samples.size} отсчётов · ${sensorNumber(duration)} с")
                Text("X — серебристая, Y — голубая, Z — песочная линия. Шкала ±${sensorNumber(extent)} м/с².", style = MaterialTheme.typography.bodySmall)
                Canvas(Modifier.fillMaxWidth().height(160.dp).semantics {
                    contentDescription = "График последних ${chart.size} отсчётов ускорения; текущие значения указаны выше."
                }) {
                    drawLine(TextSecondary.copy(alpha = .4f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
                    val first = chart.firstOrNull()?.timestampNanos ?: return@Canvas
                    val span = ((chart.lastOrNull()?.timestampNanos ?: first) - first).coerceAtLeast(1L)
                    val colors = listOf(Color(0xFFE5E7EB), Color(0xFFA5C6D8), Color(0xFFD5C5A7))
                    for (axis in 0..2) {
                        fun point(index: Int): Offset {
                            val sample = chart[index]
                            val value = when (axis) { 0 -> sample.x; 1 -> sample.y; else -> sample.z }
                            return Offset(((sample.timestampNanos - first).toDouble() / span * size.width).toFloat(),
                                (size.height / 2 - value / extent * size.height / 2).toFloat())
                        }
                        for (index in 1 until chart.size) drawLine(colors[axis], point(index - 1), point(index), 1.5.dp.toPx())
                    }
                }
            }
        }
        Button(onClick = { if (state.running) model.stop() else model.start() },
            enabled = available && !state.exporting, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.running) "Остановить запись" else "Начать новую запись")
        }
        OutlinedButton(onClick = { model.stop(); export.launch("AeroCalc-acceleration.csv") },
            enabled = state.samples.isNotEmpty() && !state.exporting, modifier = Modifier.fillMaxWidth()) { Text("Сохранить CSV") }
        TextButton(onClick = model::clear, enabled = state.samples.isNotEmpty() && !state.exporting,
            modifier = Modifier.fillMaxWidth()) { Text("Очистить измерения") }
        state.message?.let { Text(it, color = TextSecondary) }
        Text("Оси привязаны к естественной ориентации устройства: X вправо, Y вверх, Z из экрана. Поворот интерфейса оси не меняет. Это учебные измерения; точность зависит от датчика телефона.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

private fun sensorNumber(value: Double?): String = value?.let { String.format(Locale.getDefault(), "%.3f", it) } ?: "—"
