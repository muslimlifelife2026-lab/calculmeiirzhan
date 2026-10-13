package com.calculator.domain.simulation

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith

class ConstantForceMotionTest {
    @Test fun zeroForcePreservesUniformMotion() {
        val state = ConstantForceMotion(2.0, 0.0, 3.0, -4.0).stateAt(2.0)
        assertEquals(-5.0, state.positionMeters, 1e-12)
        assertEquals(-4.0, state.velocityMetersPerSecond, 1e-12)
        assertEquals(0.0, state.accelerationMetersPerSecondSquared, 1e-12)
    }

    @Test fun doublingMassHalvesAccelerationAndDisplacement() {
        val light = ConstantForceMotion(2.0, 6.0).stateAt(2.0)
        val heavy = ConstantForceMotion(4.0, 6.0).stateAt(2.0)
        assertEquals(3.0, light.accelerationMetersPerSecondSquared, 1e-12)
        assertEquals(6.0, light.positionMeters, 1e-12)
        assertEquals(light.accelerationMetersPerSecondSquared / 2, heavy.accelerationMetersPerSecondSquared, 1e-12)
        assertEquals(light.positionMeters / 2, heavy.positionMeters, 1e-12)
    }

    @Test fun opposingForceStopsThenReversesAndObeysWorkEnergyRelation() {
        val motion = ConstantForceMotion(2.0, -4.0, 1.0, 4.0)
        val stopped = motion.stateAt(2.0)
        val reversed = motion.stateAt(3.0)
        assertEquals(0.0, stopped.velocityMetersPerSecond, 1e-12)
        assertEquals(-2.0, reversed.velocityMetersPerSecond, 1e-12)
        val initialEnergy = motion.stateAt(0.0).kineticEnergyJoules
        assertEquals(motion.forceNewtons * (reversed.positionMeters - motion.initialPositionMeters),
            reversed.kineticEnergyJoules - initialEnergy, 1e-12)
    }

    @Test fun invalidPhysicalInputsAndOverflowAreRejected() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { mass ->
            assertFailsWith<IllegalArgumentException> { ConstantForceMotion(mass, 1.0) }
        }
        assertFailsWith<IllegalArgumentException> { ConstantForceMotion(1.0, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { ConstantForceMotion(1.0, 1.0).stateAt(-1.0) }
        assertFailsWith<IllegalArgumentException> { ConstantForceMotion(1.0, 1.0).stateAt(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { ConstantForceMotion(Double.MIN_VALUE, 1.0).stateAt(1.0) }
    }
}
