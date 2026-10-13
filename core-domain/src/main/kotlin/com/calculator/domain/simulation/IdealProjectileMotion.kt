package com.calculator.domain.simulation

import kotlin.math.cos
import kotlin.math.sin

/** Launch and landing at the same height, uniform gravity, no air resistance. SI units. */
data class IdealProjectileMotion(
    val initialSpeedMetersPerSecond: Double,
    val launchAngleRadians: Double,
    val gravityMetersPerSecondSquared: Double
) {
    init {
        require(initialSpeedMetersPerSecond.isFinite() && initialSpeedMetersPerSecond >= 0)
        require(launchAngleRadians.isFinite() && launchAngleRadians in 0.0..(Math.PI / 2))
        require(gravityMetersPerSecondSquared.isFinite() && gravityMetersPerSecondSquared > 0)
        val verticalVelocity = initialSpeedMetersPerSecond * sin(launchAngleRadians)
        val flightTime = 2 * verticalVelocity / gravityMetersPerSecondSquared
        val maximumHeight = verticalVelocity * verticalVelocity / (2 * gravityMetersPerSecondSquared)
        val range = initialSpeedMetersPerSecond * cos(launchAngleRadians) * flightTime
        require(listOf(flightTime, maximumHeight, range).all { it.isFinite() }) { "Flight exceeds the numeric range" }
    }

    private val initialHorizontalVelocity get() = initialSpeedMetersPerSecond * cos(launchAngleRadians)
    private val initialVerticalVelocity get() = initialSpeedMetersPerSecond * sin(launchAngleRadians)
    val flightTimeSeconds get() = 2 * initialVerticalVelocity / gravityMetersPerSecondSquared
    val maximumHeightMeters get() = initialVerticalVelocity * initialVerticalVelocity / (2 * gravityMetersPerSecondSquared)
    val rangeMeters get() = initialHorizontalVelocity * flightTimeSeconds

    fun stateAt(timeSeconds: Double): ProjectileState {
        require(timeSeconds.isFinite() && timeSeconds in 0.0..flightTimeSeconds) { "Time must lie within the flight" }
        val horizontal = initialHorizontalVelocity * timeSeconds
        val vertical = (initialVerticalVelocity * timeSeconds - 0.5 * gravityMetersPerSecondSquared * timeSeconds * timeSeconds).coerceAtLeast(0.0)
        val verticalVelocity = initialVerticalVelocity - gravityMetersPerSecondSquared * timeSeconds
        require(listOf(horizontal, vertical, verticalVelocity).all { it.isFinite() })
        return ProjectileState(horizontal, vertical, initialHorizontalVelocity, verticalVelocity)
    }
}

data class ProjectileState(
    val horizontalMeters: Double,
    val heightMeters: Double,
    val horizontalVelocityMetersPerSecond: Double,
    val verticalVelocityMetersPerSecond: Double
)
