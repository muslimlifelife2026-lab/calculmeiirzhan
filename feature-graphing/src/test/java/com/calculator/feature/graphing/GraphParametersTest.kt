package com.calculator.feature.graphing

import com.calculator.engine.solver.GraphEvaluator
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.tan

class GraphParametersTest {
    @Test fun `function names and scientific notation are not parameters`() {
        val expression = "tan(a*x)+abs(b)+1e-3"
        assertEquals(listOf("a", "b"), GraphParameters.discover(listOf(expression), GraphMode.CARTESIAN))
        assertEquals(tan(1.0) + 3.0 + 0.001,
            GraphEvaluator(expression).evaluate(2.0, mapOf("a" to 0.5, "b" to -3.0)), 1e-12)
    }

    @Test fun `implicit multiplication and negative coefficients are evaluated as variable tokens`() {
        val expression = "2a*x+b"
        assertEquals(listOf("a", "b"), GraphParameters.discover(listOf(expression), GraphMode.CARTESIAN))
        assertEquals(-11.75, GraphEvaluator(expression).evaluate(3.0, mapOf("a" to -2.0, "b" to 0.25)), 1e-12)
    }

    @Test fun `all functions share parameters without exposing coordinate variables`() {
        assertEquals(listOf("a", "b"), GraphParameters.discover(listOf("a*x+b", "b*x", "tan(x)"), GraphMode.CARTESIAN))
        assertEquals(listOf("a"), GraphParameters.discover(listOf("a*cos(theta)", "th"), GraphMode.POLAR))
        assertEquals(listOf("a", "b"), GraphParameters.discover(listOf("a*cos(t)", "b*sin(t)"), GraphMode.PARAMETRIC))
    }

    @Test fun `parameter changes update samples and intersection coordinates`() {
        val line = GraphEvaluator("a*x+b")
        val axis = GraphEvaluator("0")
        val first = mapOf("a" to 1.0, "b" to -1.0)
        val second = mapOf("a" to 2.0, "b" to -4.0)
        assertArrayEquals(doubleArrayOf(-1.0, 0.0, 1.0), line.evaluateRange(0.0, 2.0, 3, first), 1e-12)
        assertArrayEquals(doubleArrayOf(-4.0, -2.0, 0.0), line.evaluateRange(0.0, 2.0, 3, second), 1e-12)
        assertEquals(1.0, line.findIntersections(axis, -5.0, 5.0, params = first).single().first, 1e-5)
        assertEquals(2.0, line.findIntersections(axis, -5.0, 5.0, params = second).single().first, 1e-5)
    }

    @Test fun `slider arithmetic agrees with the displayed tenth`() {
        assertEquals(0.1, GraphParameters.sliderValue(0.10000038f), 0.0)
        assertEquals(-2.3, GraphParameters.sliderValue(-2.2999997f), 0.0)
        assertEquals(5.0, GraphParameters.sliderValue(6f), 0.0)
        assertEquals(0.0, GraphParameters.sliderValue(-0.000001f), 0.0)
    }
}
