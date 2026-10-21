package com.calculator.engine.solver

import com.calculator.domain.model.Formula
import com.calculator.domain.model.Variable
import com.calculator.domain.model.MeasurementUnit
import com.calculator.domain.model.DimensionVector
import com.calculator.domain.utils.FormulaLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FormulaSolverTest {
    @Test fun numericalFallbackHandlesExcludedZeroAndSmallFunctionScale() {
        val formula = Formula("domain", "Domain", "MATHEMATICS", canonicalEquation = "ln(x)-1",
            latexTemplate = "", variables = listOf(Variable("x", "x", defaultUnitSymbol = "")))
        val logarithm = FormulaSolver.solve(formula, emptyMap(), emptyMap(), "x")
        assertTrue(logarithm.errorMessage, logarithm.success)
        assertEquals(kotlin.math.E, logarithm.value, 1e-12)
        val subunit = FormulaSolver.solve(formula.copy(canonicalEquation = "ln(x)+1"), emptyMap(), emptyMap(), "x")
        assertTrue(subunit.errorMessage, subunit.success)
        assertEquals(1.0 / kotlin.math.E, subunit.value, 1e-12)
        val scaled = FormulaSolver.solve(formula.copy(canonicalEquation = "1e-30*(x-0.37)"), emptyMap(), emptyMap(), "x")
        assertTrue(scaled.errorMessage, scaled.success)
        assertEquals(0.37, scaled.value, 1e-12)
    }

    @Test fun substitutionPreservesFunctionsAndOverlappingVariableNames() {
        val formula = Formula("names", "Names", "MATHEMATICS", canonicalEquation = "y-sin(a)-a_a",
            latexTemplate = "", variables = listOf("y", "a", "a_a").map { Variable(it, it, defaultUnitSymbol = "") },
            solvedEquations = mapOf("y" to "sin(a)+a_a"))
        val result = FormulaSolver.solve(formula, mapOf("a" to BigDecimal.ZERO, "a_a" to BigDecimal.TEN), emptyMap(), "y")
        assertTrue(result.success)
        assertEquals("y = sin((0))+(10)", result.steps[2].equationLatex)
    }

    @Test fun newlyIndexedFormulasSolveEveryTarget() {
        val ids = setOf("cone_volume", "electric_power", "friction_force", "gravity_accel",
            "parallelogram_area", "percent_decrease", "percent_increase", "potential_energy",
            "torque_force", "trapezoid_area", "wave_frequency")
        FormulaLoader.formulas.filter { it.id in ids }.forEach { formula ->
            formula.variables.forEach { target ->
                val inputs = formula.variables.filter { it.symbol != target.symbol }
                    .associate { it.symbol to BigDecimal("2") }
                val result = FormulaSolver.solve(formula, inputs, emptyMap(), target.symbol)
                assertTrue("${formula.id}:${target.symbol} ${result.errorMessage}", result.success)
            }
        }
    }

    @Test fun numericalFallbackVerifiesRoot() {
        val target = Variable("S", "Area", defaultUnitSymbol = "")
        val cubic = Formula("cubic", "Cubic", "MATHEMATICS", canonicalEquation = "S^3 - 8",
            latexTemplate = "", variables = listOf(target))
        val square = cubic.copy(canonicalEquation = "S^2 - 4")
        assertEquals(2.0, FormulaSolver.solve(cubic, emptyMap(), emptyMap(), "S").value, 1e-12)
        assertEquals(2.0, FormulaSolver.solve(square, emptyMap(), emptyMap(), "S").value, 1e-12)
    }

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
