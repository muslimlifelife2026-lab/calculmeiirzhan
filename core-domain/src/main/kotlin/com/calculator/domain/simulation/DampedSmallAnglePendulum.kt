package com.calculator.domain.simulation

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** theta'' + 2*beta*theta' + (g/L)*theta = 0, initially released from rest. */
data class DampedSmallAnglePendulum(
    val lengthMeters: Double,
    val gravityMetersPerSecondSquared: Double,
    val initialAngleRadians: Double,
    val dampingPerSecond: Double = 0.02
) {
    init {
        require(lengthMeters.isFinite() && lengthMeters > 0)
        require(gravityMetersPerSecondSquared.isFinite() && gravityMetersPerSecondSquared > 0)
        require((gravityMetersPerSecondSquared / lengthMeters).isFinite()) { "Frequency exceeds the numeric range" }
        require(initialAngleRadians.isFinite() && abs(initialAngleRadians) <= Math.toRadians(15.0)) {
            "Small-angle model is limited to 15 degrees"
        }
        require(dampingPerSecond.isFinite() && dampingPerSecond >= 0)
        require(dampingPerSecond * dampingPerSecond < gravityMetersPerSecondSquared / lengthMeters) {
            "This model supports only underdamped oscillation"
        }
    }

    private val naturalFrequencySquared get() = gravityMetersPerSecondSquared / lengthMeters
    private val dampedFrequency get() = sqrt(naturalFrequencySquared - dampingPerSecond * dampingPerSecond)
    val periodSeconds get() = 2 * Math.PI / dampedFrequency

    fun stateAt(timeSeconds: Double): PendulumState {
        require(timeSeconds.isFinite() && timeSeconds >= 0)
        val phase = dampedFrequency * timeSeconds
        val decay = exp(-dampingPerSecond * timeSeconds)
        val angle = initialAngleRadians * decay * (cos(phase) + dampingPerSecond / dampedFrequency * sin(phase))
        val angularVelocity = -initialAngleRadians * decay * naturalFrequencySquared / dampedFrequency * sin(phase)
        val kinetic = 0.5 * lengthMeters * lengthMeters * angularVelocity * angularVelocity
        val potential = 0.5 * gravityMetersPerSecondSquared * lengthMeters * angle * angle
        require(listOf(angle, angularVelocity, kinetic, potential).all { it.isFinite() })
        return PendulumState(angle, angularVelocity, abs(lengthMeters * angularVelocity), kinetic, potential)
    }
}

data class PendulumState(
    val angleRadians: Double,
    val angularVelocityRadiansPerSecond: Double,
    val speedMetersPerSecond: Double,
    val kineticEnergyPerKilogram: Double,
    val potentialEnergyPerKilogram: Double
)
