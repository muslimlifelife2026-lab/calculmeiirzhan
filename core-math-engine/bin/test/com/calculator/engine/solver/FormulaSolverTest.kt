package com.calculator.engine.solver

import com.calculator.domain.model.Formula
import com.calculator.domain.model.Variable
import com.calculator.domain.model.MeasurementUnit
import com.calculator.domain.model.DimensionVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FormulaSolverTest {

    @Test
    fun testSolveVelocityFormulaSymbolic() {
        val meterPerSecond = MeasurementUnit("m/s", "Meters per second", 1.0)
        val kmPerHour = MeasurementUnit("km/h", "Kilometers per hour", 1.0 / 3.6)
        val meterPerSecondSquared = MeasurementUnit("m/s^2", "Meters per second squared", 1.0)
        val seconds = MeasurementUnit("s", "Seconds", 1.0)
        val hours = MeasurementUnit("h", "Hours", 3600.0)

        // v_f = v_i + a * t
        val formula = Formula(
            id = "velocity_kinematics",
            name = "Velocity Formula",
            category = "PHYSICS",
            canonicalEquation = "v_f - v_i - a * t",
            latexTemplate = "v_f = v_i + a \\cdot t",
            variables = listOf(
                Variable("v_f", "Final Velocity", defaultUnitSymbol = "m/s", supportedUnits = listOf(meterPerSecond, kmPerHour)),
                Variable("v_i", "Initial Velocity", defaultUnitSymbol = "m/s", supportedUnits = listOf(meterPerSecond, kmPerHour)),
                Variable("a", "Acceleration", defaultUnitSymbol = "m/s^2", supportedUnits = listOf(meterPerSecondSquared)),
                Variable("t", "Time", defaultUnitSymbol = "s", supportedUnits = listOf(seconds, hours))
            ),
            solvedEquations = mapOf(
                "v_f" to "v_i + a * t",
                "v_i" to "v_f - a * t",
                "a" to "(v_f - v_i) / t",
                "t" to "(v_f - v_i) / a"
            )
        )

        // Inputs: v_i = 10 m/s, a = 2 m/s^2, t = 5 s. Solve for v_f.
        val result = FormulaSolver.solve(
            formula = formula,
            inputs = mapOf(
                "v_i" to BigDecimal("10"),
                "a" to BigDecimal("2"),
                "t" to BigDecimal("5")
            ),
            inputUnits = mapOf(
                "v_i" to meterPerSecond,
                "a" to meterPerSecondSquared,
                "t" to seconds
            ),
            targetSymbol = "v_f"
        )

        assertTrue(result.success)
        assertEquals("v_f", result.solvedSymbol)
        assertEquals(20.0, result.value, 1e-9)
        assertEquals("m/s", result.unitSymbol)
        assertEquals(4, result.steps.size)
    }

    @Test
    fun testSolveWithUnitConversions() {
        val meterPerSecond = MeasurementUnit("m/s", "Meters per second", 1.0)
        val kmPerHour = MeasurementUnit("km/h", "Kilometers per hour", 1.0 / 3.6)
        val meterPerSecondSquared = MeasurementUnit("m/s^2", "Meters per second squared", 1.0)
        val seconds = MeasurementUnit("s", "Seconds", 1.0)
        val hours = MeasurementUnit("h", "Hours", 3600.0)

        val formula = Formula(
            id = "velocity_kinematics",
            name = "Velocity Formula",
            category = "PHYSICS",
            canonicalEquation = "v_f - v_i - a * t",
            latexTemplate = "v_f = v_i + a \\cdot t",
            variables = listOf(
                Variable("v_f", "Final Velocity", defaultUnitSymbol = "m/s", supportedUnits = listOf(meterPerSecond, kmPerHour)),
                Variable("v_i", "Initial Velocity", defaultUnitSymbol = "m/s", supportedUnits = listOf(meterPerSecond, kmPerHour)),
                Variable("a", "Acceleration", defaultUnitSymbol = "m/s^2", supportedUnits = listOf(meterPerSecondSquared)),
                Variable("t", "Time", defaultUnitSymbol = "s", supportedUnits = listOf(seconds, hours))
            ),
            solvedEquations = mapOf(
                "v_f" to "v_i + a * t",
                "v_i" to "v_f - a * t",
                "a" to "(v_f - v_i) / t",
                "t" to "(v_f - v_i) / a"
            )
        )

        // Inputs: v_i = 36 km/h (10 m/s), a = 2 m/s^2, t = 1 hour (3600 s)
        // Solve for v_f, expecting output in km/h
        val result = FormulaSolver.solve(
            formula = formula,
            inputs = mapOf(
                "v_i" to BigDecimal("36"),
                "a" to BigDecimal("2"),
                "t" to BigDecimal("1")
            ),
            inputUnits = mapOf(
                "v_i" to kmPerHour,
                "a" to meterPerSecondSquared,
                "t" to hours
            ),
            targetSymbol = "v_f",
            targetUnit = kmPerHour
        )

        assertTrue(result.success)
        assertEquals("v_f", result.solvedSymbol)
        
        // Expected v_f in SI: 10 + 2 * 3600 = 7210 m/s
        // Expected v_f in km/h: 7210 * 3.6 = 25956 km/h
        assertEquals(25956.0, result.value, 1e-9)
        assertEquals("km/h", result.unitSymbol)
    }
}
