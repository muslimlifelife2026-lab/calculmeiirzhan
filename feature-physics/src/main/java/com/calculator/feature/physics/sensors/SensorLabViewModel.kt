package com.calculator.feature.physics.sensors

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.domain.sensors.AccelerationSample
import com.calculator.domain.sensors.SensorRecording
import com.calculator.domain.sensors.accelerationCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SensorLabState(
    val linear: Boolean = false,
    val running: Boolean = false,
    val samples: List<AccelerationSample> = emptyList(),
    val message: String? = null,
    val exporting: Boolean = false
)

class SensorLabViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val manager = application.getSystemService(SensorManager::class.java)
    private val mutableState = MutableStateFlow(SensorLabState())
    val state = mutableState.asStateFlow()
    private var recording = SensorRecording()
    private var lastTimestamp: Long? = null
    val accelerometerAvailable: Boolean get() = sensor(false) != null
    val linearAvailable: Boolean get() = sensor(true) != null

    private fun sensor(linear: Boolean) = manager?.getDefaultSensor(
        if (linear) Sensor.TYPE_LINEAR_ACCELERATION else Sensor.TYPE_ACCELEROMETER)

    fun selectMode(linear: Boolean) {
        if (state.value.linear == linear) return
        stop()
        clear()
        mutableState.value = state.value.copy(linear = linear)
    }

    fun start() {
        if (state.value.running) return
        val selected = sensor(state.value.linear)
        if (selected == null) {
            mutableState.value = state.value.copy(message = "Этот датчик недоступен на устройстве.")
            return
        }
        clear()
        // The requested period is a hint. Export uses actual sensor timestamps.
        val registered = try { manager?.registerListener(this, selected, 50_000) == true }
            catch (_: RuntimeException) { false }
        mutableState.value = state.value.copy(running = registered,
            message = if (registered) "Запись: до 1200 отсчётов. При выходе она остановится." else "Не удалось запустить датчик.")
    }

    fun stop() {
        manager?.unregisterListener(this)
        mutableState.value = state.value.copy(running = false)
    }

    fun clear() {
        stop()
        recording = SensorRecording()
        lastTimestamp = null
        mutableState.value = state.value.copy(samples = emptyList(), message = null)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!state.value.running || event.values.size < 3) return
        if (lastTimestamp?.let { event.timestamp - it < 50_000_000L } == true) return
        val sample = runCatching { AccelerationSample(event.timestamp,
            event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble()) }.getOrNull() ?: return
        if (!recording.append(sample)) return
        lastTimestamp = sample.timestampNanos
        mutableState.value = state.value.copy(samples = recording.samples)
        if (recording.full) {
            stop()
            mutableState.value = state.value.copy(message = "Записано 1200 отсчётов. Сохраните CSV или начните новую запись.")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE && state.value.running)
            mutableState.value = state.value.copy(message = "Датчик сообщает о низкой достоверности. Проверьте положение телефона.")
    }

    fun export(uri: Uri) {
        val snapshot = state.value
        if (snapshot.samples.isEmpty() || snapshot.exporting) return
        mutableState.value = snapshot.copy(exporting = true)
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                        ?: error("Output unavailable")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(accelerationCsv(snapshot.samples, snapshot.linear)) }
                }.isSuccess
            }
            mutableState.value = state.value.copy(exporting = false,
                message = if (success) "CSV сохранён." else "Не удалось сохранить CSV. Выберите другое место.")
        }
    }

    override fun onCleared() { manager?.unregisterListener(this) }
}
