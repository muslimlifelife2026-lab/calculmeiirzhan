package com.calculator.feature.physics.simulators

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.domain.simulation.ConstantForceMotion
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/** An original educational experiment; all calculations use SI and real elapsed time. */
@Composable
fun NewtonSimulator(modifier: Modifier = Modifier) {
    val durationSeconds = 10.0
    var massKg by rememberSaveable { mutableFloatStateOf(2f) }
    var forceNewtons by rememberSaveable { mutableFloatStateOf(4f) }
    var timeSeconds by rememberSaveable { mutableDoubleStateOf(0.0) }
    // Rotation/navigation pauses the experiment rather than restarting it unexpectedly.
    var running by remember { mutableStateOf(false) }
    val motion = remember(massKg, forceNewtons) { ConstantForceMotion(massKg.toDouble(), forceNewtons.toDouble()) }
    val state = motion.stateAt(timeSeconds)

    LaunchedEffect(running, massKg, forceNewtons) {
        if (running) {
            var previousFrame: Long? = null
            while (running && timeSeconds < durationSeconds) {
                withFrameNanos { now ->
                    previousFrame?.let { previous ->
                        timeSeconds = (timeSeconds + (now - previous) / 1_000_000_000.0).coerceAtMost(durationSeconds)
                    }
                    previousFrame = now
                }
            }
            running = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Сила и движение", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Измените массу и силу: сравните ускорение и путь за 10 секунд.", color = TextSecondary)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("a = F / m", style = MaterialTheme.typography.titleLarge)
                Text("a = ${number(forceNewtons.toDouble())} Н / ${number(massKg.toDouble())} кг = ${number(state.accelerationMetersPerSecondSquared)} м/с²")
                val extentMeters = max(1.0, abs(motion.stateAt(durationSeconds).positionMeters) * 1.12)
                Canvas(Modifier.fillMaxWidth().height(110.dp).semantics {
                    contentDescription = "Тело: координата ${number(state.positionMeters)} метров. Положительное направление вправо."
                }) {
                    val centerX = size.width / 2
                    val axisY = size.height * 0.72f
                    drawLine(TextSecondary.copy(alpha = 0.4f), Offset(12.dp.toPx(), axisY), Offset(size.width - 12.dp.toPx(), axisY), 1.dp.toPx())
                    drawLine(TextSecondary, Offset(centerX, axisY - 4.dp.toPx()), Offset(centerX, axisY + 4.dp.toPx()), 1.dp.toPx())
                    val travelPixels = size.width / 2 - 28.dp.toPx()
                    val bodyX = centerX + (state.positionMeters / extentMeters * travelPixels).toFloat()
                    val block = 26.dp.toPx()
                    drawRoundRect(Color(0xFFD7DEE7), Offset(bodyX - block / 2, axisY - block - 6.dp.toPx()), Size(block, block), CornerRadius(6.dp.toPx()))
                    if (forceNewtons != 0f) {
                        val direction = if (forceNewtons > 0f) 1f else -1f
                        val arrowY = axisY - block - 18.dp.toPx()
                        val start = Offset(bodyX, arrowY)
                        val end = Offset(bodyX + direction * 24.dp.toPx(), arrowY)
                        drawLine(Color.White, start, end, 2.dp.toPx())
                        drawLine(Color.White, end, Offset(end.x - direction * 6.dp.toPx(), end.y - 4.dp.toPx()), 2.dp.toPx())
                        drawLine(Color.White, end, Offset(end.x - direction * 6.dp.toPx(), end.y + 4.dp.toPx()), 2.dp.toPx())
                    }
                }
                Text("Шкала: от −${number(extentMeters)} до +${number(extentMeters)} м; ноль в центре.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("t = ${number(timeSeconds)} с  ·  x = ${number(state.positionMeters)} м")
                Text("v = ${number(state.velocityMetersPerSecond)} м/с  ·  Eₖ = ${number(state.kineticEnergyJoules)} Дж")
            }
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Масса: ${number(massKg.toDouble())} кг")
                Slider(value = massKg, onValueChange = { massKg = it; running = false; timeSeconds = 0.0 },
                    valueRange = 0.5f..10f, modifier = Modifier.semantics { contentDescription = "Масса тела в килограммах" })
                Text("Равнодействующая сила: ${number(forceNewtons.toDouble())} Н")
                Slider(value = forceNewtons, onValueChange = { forceNewtons = it; running = false; timeSeconds = 0.0 },
                    valueRange = -20f..20f, steps = 39, modifier = Modifier.semantics { contentDescription = "Равнодействующая сила в ньютонах" })
                Text("Время: ${number(timeSeconds)} с")
                Slider(value = timeSeconds.toFloat(), onValueChange = { timeSeconds = it.toDouble(); running = false },
                    valueRange = 0f..durationSeconds.toFloat(), modifier = Modifier.semantics { contentDescription = "Время эксперимента в секундах" })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (!running && timeSeconds >= durationSeconds) timeSeconds = 0.0
                        running = !running
                    }, modifier = Modifier.weight(1f)) {
                        Text(if (running) "Пауза" else if (timeSeconds > 0 && timeSeconds < durationSeconds) "Продолжить" else "Начать")
                    }
                    OutlinedButton(onClick = { running = false; timeSeconds = 0.0 }, modifier = Modifier.weight(1f)) { Text("Сбросить") }
                }
            }
        }
        Text("Модель: материальная точка движется по прямой из состояния покоя; x₀ = 0. Сила постоянна, трения и сопротивления воздуха нет. Минус означает направление влево. Изменение массы или силы начинает новый опыт.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text("Проверьте: удвойте массу при той же силе — ускорение и путь уменьшатся вдвое. При F = 0 тело остаётся в покое.",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
    }
}

private fun number(value: Double): String = String.format(Locale.getDefault(), "%.2f", if (abs(value) < 0.005) 0.0 else value)
