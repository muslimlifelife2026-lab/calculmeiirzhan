package com.calculator.engine.algebra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.calculator.engine.parser.MathParser
import java.math.BigDecimal

class LinearSystemSolverTest {
    @Test fun solvesByEliminationAndExplainsSubstitutionAndVerification() {
        val result = solve("2x + y = 7", "x - y = 2")
        assertEquals(LinearSystemKind.UNIQUE, result.kind)
        assertEquals(mapOf("x" to "3", "y" to "1"), result.solution)
        assertTrue(result.steps.any { it.contains("исключ", ignoreCase = true) })
        assertTrue(result.steps.any { it.contains("Подставляем") })
        assertTrue(result.steps.any { it.contains("Проверка") })
    }

    @Test fun decimalsAndFractionsRemainExact() {
        assertEquals(mapOf("x" to "5/3", "y" to "2/3"), solve("0,1x + 0,2y = 0,3", "x - y = 1").solution)
        assertEquals(mapOf("x" to "1/3", "y" to "-2/5"), solve("x+y=-1/15", "2x-y=16/15").solution)
        assertEquals(mapOf("x" to "1", "y" to "1"), solve("1e-3x+2e-3y=3e-3", "x-y=0").solution)
    }

    @Test fun integersBeyondDoublePrecisionAreNotRounded() {
        assertEquals(mapOf("x" to "9007199254740993/2", "y" to "9007199254740993/2"),
            solve("x+y=9007199254740993", "x-y=0").solution)
    }

    @Test fun tinyNonzeroPivotIsNotMistakenForDependentEquations() {
        val result = solve("x+y=2", "x+(1+1e-120)*y=2+1e-120")
        assertEquals(LinearSystemKind.UNIQUE, result.kind)
        assertEquals(mapOf("x" to "1", "y" to "1"), result.solution)
    }

    @Test fun swapsZeroPivotAndSupportsOnlyYConstraints() {
        assertEquals(mapOf("x" to "3", "y" to "2"), solve("y=2", "x+y=5").solution)
        assertEquals(LinearSystemKind.INFINITE, solve("2y=4", "3y=6").kind)
        assertEquals(LinearSystemKind.NONE, solve("2y=4", "3y=7").kind)
    }

    @Test fun distinguishesDependentAndContradictoryRows() {
        val dependent = solve("x+y=1", "2x+2y=2")
        assertEquals(LinearSystemKind.INFINITE, dependent.kind)
        assertTrue(dependent.solution.isEmpty())
        assertTrue(dependent.steps.any { it.contains("t") })
        val contradiction = solve("x+y=1", "2x+2y=3")
        assertEquals(LinearSystemKind.NONE, contradiction.kind)
        assertTrue(contradiction.solution.isEmpty())
        assertTrue(contradiction.steps.any { it.contains("0 = 1") })
    }

    @Test fun handlesIdentitiesAndImpossibleConstantRows() {
        assertEquals(LinearSystemKind.INFINITE, solve("0=0", "0=0").kind)
        assertEquals(LinearSystemKind.INFINITE, solve("x-x=0", "3x+2y=6").kind)
        assertEquals(LinearSystemKind.NONE, solve("0=1", "x+y=2").kind)
        assertEquals(LinearSystemKind.NONE, solve("x+y=2", "0=-1").kind)
    }

    @Test fun expandsParenthesesAndMovesBothSides() {
        assertEquals(mapOf("x" to "3", "y" to "1"), solve("2(X+Y)=8", "y+1=x-1").solution)
        assertEquals(mapOf("x" to "3", "y" to "3"), solve("(x+y)÷2=3", "2×x+1=y+4").solution)
        assertEquals(mapOf("x" to "-1", "y" to "2"), solve("−x+y=3", "x+y=1").solution)
    }

    @Test fun rowOrderAndNonzeroScalingDoNotChangeTheSolution() {
        val expected = solve("2x+y=7", "x-y=2").solution
        assertEquals(expected, solve("x-y=2", "2x+y=7").solution)
        assertEquals(expected, solve("-6x-3y=-21", "x/5-y/5=2/5").solution)
    }

    @Test fun rejectsNonlinearExpressionsAndVariableDenominators() {
        listOf("xy=1", "x*y=1", "x^2+y=1", "1/x+y=2", "x/(y-y+1)=2", "x^0=1", "y^(x-x)=1").forEach {
            assertThrows(it, IllegalArgumentException::class.java) { solve(it, "x+y=3") }
        }
    }

    @Test fun rejectsMalformedEquationsAndUnsupportedSymbols() {
        listOf("x+y", "x=y=2", "x+=2", "=2", "x=", "z+y=1", "sin(x)+y=1", "pi*x+y=1",
            "2e*x+y=1", "x/0+y=1", "1 2+x=1", "x%=1", "0^0+x=1").forEach {
            assertThrows(it, IllegalArgumentException::class.java) { solve(it, "x-y=1") }
        }
    }

    @Test fun rejectsOversizedInputsAndCoefficientsBeforeExpandingThem() {
        listOf("x+".repeat(129) + "y=1", "1".repeat(129) + "x+y=1", "1e129*x+y=1", "1e-129*x+y=1").forEach {
            assertThrows(IllegalArgumentException::class.java) { solve(it, "x-y=0") }
        }
    }

    @Test fun generatedNonsingularIntegerSystemsRecoverTheirKnownSolutions() {
        for (a in -2..2) for (b in -2..2) {
            val c = b + 1
            val d = a - 1
            if (a * d - b * c == 0) continue
            val x = a + 3
            val y = b - 2
            val first = "$a*x+$b*y=${a * x + b * y}".replace("+-", "-")
            val second = "$c*x+$d*y=${c * x + d * y}".replace("+-", "-")
            assertEquals("$first; $second", mapOf("x" to x.toString(), "y" to y.toString()), solve(first, second).solution)
        }
    }

    @Test fun graphExpressionsRepresentBothLinesAndMeetAtTheSolution() {
        val result = solve("2x+y=7", "x-y=2")
        assertEquals(listOf("(7-(2)*x)/(1)", "(2-(1)*x)/(-1)"), result.graphExpressions)
        val values = result.graphExpressions.map {
            MathParser.evaluateRPN(MathParser.shuntingYard(MathParser.tokenize(it)), mapOf("x" to BigDecimal(3)))
        }
        assertTrue(values.all { it.compareTo(BigDecimal.ONE) == 0 })
        assertEquals(2, solve("x+y=1", "2x+2y=3").graphExpressions.size)
        assertEquals(2, solve("x+y=1", "2x+2y=2").graphExpressions.size)
    }

    @Test fun graphExpressionsKeepFractionsAndOmitVerticalOrOversizedLines() {
        val fractional = solve("x/3+y/2=1/6", "x-y=1").graphExpressions
        assertEquals("(1/6-(1/3)*x)/(1/2)", fractional.first())
        val value = MathParser.evaluateRPN(MathParser.shuntingYard(MathParser.tokenize(fractional.first())), mapOf("x" to BigDecimal.ZERO))
        assertTrue(value.subtract(BigDecimal.ONE.divide(BigDecimal(3), java.math.MathContext.DECIMAL128)).abs() < BigDecimal("1e-33"))
        assertTrue(solve("x=2", "x+y=3").graphExpressions.isEmpty())
        assertTrue(solve("(1e128)^12*x+y=1", "x-y=0").graphExpressions.isEmpty())
    }

    private fun solve(first: String, second: String) = LinearSystemSolver.solve(first, second)
}
