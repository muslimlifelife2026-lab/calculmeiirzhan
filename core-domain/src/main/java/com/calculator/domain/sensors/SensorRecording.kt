package com.calculator.domain.sensors

import kotlin.math.hypot

/** Device-coordinate acceleration, in m/s². Timestamps are monotonic sensor nanoseconds. */
data class AccelerationSample(val timestampNanos: Long, val x: Double, val y: Double, val z: Double) {
    init {
        require(timestampNanos >= 0 && x.isFinite() && y.isFinite() && z.isFinite())
    }
    val magnitude: Double get() = hypot(hypot(x, y), z)
}

class SensorRecording(val capacity: Int = 1200) {
    init { require(capacity > 0) }
    private val values = ArrayList<AccelerationSample>()
    val samples: List<AccelerationSample> get() = values.toList()
    val full: Boolean get() = values.size >= capacity

    fun append(sample: AccelerationSample): Boolean {
        if (full || values.lastOrNull()?.let { sample.timestampNanos <= it.timestampNanos } == true) return false
        values.add(sample)
        return true
    }
}

/** Explicit measurement kind and units travel with the exported values. No spreadsheet formulas. */
fun accelerationCsv(samples: List<AccelerationSample>, linear: Boolean): String = buildString {
    append("measurement,time_s,x_m_s2,y_m_s2,z_m_s2,magnitude_m_s2\n")
    val first = samples.firstOrNull()?.timestampNanos ?: return@buildString
    samples.forEach { sample ->
        append(if (linear) "linear_acceleration" else "acceleration_including_gravity")
        append(',').append((sample.timestampNanos - first) / 1_000_000_000.0)
        append(',').append(sample.x).append(',').append(sample.y).append(',').append(sample.z)
        append(',').append(sample.magnitude).append('\n')
    }
}
