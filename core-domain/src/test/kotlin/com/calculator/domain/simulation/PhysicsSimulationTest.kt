package com.calculator.domain.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.assertFailsWith

class PhysicsSimulationTest {
    @Test fun undampedPendulumReturnsToReleasePointAndConservesLinearModelEnergy() {
        val motion = DampedSmallAnglePendulum(1.0, 9.81, Math.toRadians(10.0), 0.0)
        assertEquals(2 * PI / sqrt(9.81), motion.periodSeconds, 1e-12)
        val initial = motion.stateAt(0.0)
        assertEquals(0.0, initial.speedMetersPerSecond, 1e-12)
        val returned = motion.stateAt(motion.periodSeconds)
        assertEquals(initial.angleRadians, returned.angleRadians, 1e-12)
        val quarter = motion.stateAt(motion.periodSeconds / 4)
        assertEquals(0.0, quarter.angleRadians, 1e-12)
        assertEquals(initial.potentialEnergyPerKilogram, quarter.kineticEnergyPerKilogram, 1e-12)
    }

    @Test fun dampedPendulumStartsFromRestAndLosesEnergy() {
        val motion = DampedSmallAnglePendulum(1.0, 9.81, Math.toRadians(10.0), 0.02)
        val initial = motion.stateAt(0.0)
        assertEquals(0.0, initial.angularVelocityRadiansPerSecond, 1e-12)
        var previousEnergy = initial.potentialEnergyPerKilogram
        for (step in 1..100) {
            val state = motion.stateAt(step / 20.0)
            val energy = state.kineticEnergyPerKilogram + state.potentialEnergyPerKilogram
            assertTrue(energy <= previousEnergy + 1e-12)
            previousEnergy = energy
        }
        assertFailsWith<IllegalArgumentException> { DampedSmallAnglePendulum(1.0, 9.81, PI / 3) }
    }

    @Test fun projectileApexLandingAndEnergyMatchIndependentAnalyticReferences() {
        val motion = IdealProjectileMotion(10.0 * sqrt(2.0), PI / 4, 10.0)
        assertEquals(2.0, motion.flightTimeSeconds, 1e-12)
        assertEquals(5.0, motion.maximumHeightMeters, 1e-12)
        assertEquals(20.0, motion.rangeMeters, 1e-12)
        val apex = motion.stateAt(1.0)
        assertEquals(10.0, apex.horizontalMeters, 1e-12)
        assertEquals(5.0, apex.heightMeters, 1e-12)
        assertEquals(0.0, apex.verticalVelocityMetersPerSecond, 1e-12)
        val energy = 0.5 * (apex.horizontalVelocityMetersPerSecond * apex.horizontalVelocityMetersPerSecond +
            apex.verticalVelocityMetersPerSecond * apex.verticalVelocityMetersPerSecond) + 10.0 * apex.heightMeters
        assertEquals(100.0, energy, 1e-12)
        assertEquals(0.0, motion.stateAt(motion.flightTimeSeconds).heightMeters, 1e-12)
        assertFailsWith<IllegalArgumentException> { motion.stateAt(3.0) }
    }

    @Test fun invalidPhysicalParametersAreRejected() {
        assertFailsWith<IllegalArgumentException> { DampedSmallAnglePendulum(0.0, 9.81, 0.1) }
        assertFailsWith<IllegalArgumentException> { DampedSmallAnglePendulum(1.0, 9.81, 0.1, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { IdealProjectileMotion(10.0, 0.5, 0.0) }
        assertFailsWith<IllegalArgumentException> { IdealProjectileMotion(Double.NaN, 0.5, 9.81) }
        assertFailsWith<IllegalArgumentException> { IdealProjectileMotion(10.0, -0.5, 9.81) }
        assertFailsWith<IllegalArgumentException> { IdealProjectileMotion(Double.MAX_VALUE, 0.5, 9.81) }
        assertFailsWith<IllegalArgumentException> { DampedSmallAnglePendulum(Double.MIN_VALUE, 9.81, 0.1) }
    }
}
