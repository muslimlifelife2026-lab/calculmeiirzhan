package com.calculator.domain.simulation

/** One-dimensional motion in SI units under a constant net force, without friction. */
data class ConstantForceMotion(
    val massKg: Double,
    val forceNewtons: Double,
    val initialPositionMeters: Double = 0.0,
    val initialVelocityMetersPerSecond: Double = 0.0
) {
    init {
        require(massKg.isFinite() && massKg > 0.0) { "Mass must be finite and positive" }
        require(forceNewtons.isFinite()) { "Force must be finite" }
        require(initialPositionMeters.isFinite() && initialVelocityMetersPerSecond.isFinite()) {
            "Initial state must be finite"
        }
    }

    fun stateAt(timeSeconds: Double): MotionState {
        require(timeSeconds.isFinite() && timeSeconds >= 0.0) { "Time must be finite and nonnegative" }
        val acceleration = forceNewtons / massKg
        val velocity = initialVelocityMetersPerSecond + acceleration * timeSeconds
        val displacement = initialVelocityMetersPerSecond * timeSeconds + 0.5 * acceleration * timeSeconds * timeSeconds
        val position = initialPositionMeters + displacement
        val kineticEnergy = 0.5 * massKg * velocity * velocity
        require(listOf(acceleration, velocity, position, kineticEnergy).all { it.isFinite() }) {
            "Motion exceeds the numeric range"
        }
        return MotionState(position, velocity, acceleration, kineticEnergy)
    }
}

data class MotionState(
    val positionMeters: Double,
    val velocityMetersPerSecond: Double,
    val accelerationMetersPerSecondSquared: Double,
    val kineticEnergyJoules: Double
)
