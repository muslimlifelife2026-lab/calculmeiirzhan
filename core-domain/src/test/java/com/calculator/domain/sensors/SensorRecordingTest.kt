package com.calculator.domain.sensors

import java.util.Locale
import kotlin.test.*

class SensorRecordingTest {
    @Test fun axesNormAndBoundedMonotonicRecording() {
        val recording = SensorRecording(2)
        assertTrue(recording.append(AccelerationSample(100, 3.0, 4.0, 12.0)))
        assertEquals(13.0, recording.samples.single().magnitude)
        assertFalse(recording.append(AccelerationSample(100, 0.0, 0.0, 0.0)))
        assertFalse(recording.append(AccelerationSample(99, 0.0, 0.0, 0.0)))
        assertTrue(recording.append(AccelerationSample(200, 0.0, 0.0, 9.81)))
        assertTrue(recording.full)
        assertFalse(recording.append(AccelerationSample(300, 0.0, 0.0, 0.0)))
        assertEquals(2, recording.samples.size)
    }

    @Test fun rejectsInvalidMeasurements() {
        assertFailsWith<IllegalArgumentException> { AccelerationSample(-1, 0.0, 0.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { AccelerationSample(0, Double.NaN, 0.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { AccelerationSample(0, 0.0, Double.POSITIVE_INFINITY, 0.0) }
    }

    @Test fun csvUsesRelativeSecondsAndDecimalPointInRussianLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru-RU"))
            val csv = accelerationCsv(listOf(AccelerationSample(1_000_000_000, 3.0, 4.0, 0.0),
                AccelerationSample(1_500_000_000, -1.5, 0.0, 0.0)), false)
            assertTrue(csv.contains("acceleration_including_gravity,0.0,3.0,4.0,0.0,5.0"))
            assertTrue(csv.contains("acceleration_including_gravity,0.5,-1.5,0.0,0.0,1.5"))
            assertTrue(accelerationCsv(listOf(AccelerationSample(0, 0.0, 0.0, 0.0)), true).contains("linear_acceleration,0.0"))
        } finally { Locale.setDefault(original) }
    }
}
