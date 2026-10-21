package com.calculator.engine.algebra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PolynomialAlgebraTest {
    @Test fun expandsCollectsAndKeepsCoefficientsExact() {
        assertEquals("2·x + 1", calculate("(x+1)^2-x^2").answer)
        assertEquals("1/3·x + 3/10", calculate("x/6+x/6+0.1+0.2").answer)
        assertEquals("−x^2 + 1", calculate("-x^2+1").answer)
        assertEquals("9007199254740993·x", calculate("9007199254740993x").answer)
    }

    @Test fun differentiatesTermsAndConstants() {
        assertEquals("3·x^2 − 2/3", calculate("x^3-2x/3+9", AlgebraOperation.DERIVATIVE).answer)
        assertEquals("0", calculate("3/7", AlgebraOperation.DERIVATIVE).answer)
    }

    @Test fun solvesLinearEquationsAndDegenerateEquations() {
        assertEquals("1/3", calculate("3x=1", AlgebraOperation.SOLVE).roots.single().exact)
        assertEquals("Любое действительное x", calculate("x+1=x+1", AlgebraOperation.SOLVE).answer)
        assertEquals("Решений нет", calculate("x+1=x+2", AlgebraOperation.SOLVE).answer)
        assertEquals("2", calculate("0*x^2+2x-4", AlgebraOperation.SOLVE).roots.single().exact)
    }

    @Test fun exactQuadraticRootsAndMultiplicityAndRealDomain() {
        val two = calculate("(x-2)*(x+3)=0", AlgebraOperation.SOLVE)
        assertEquals(listOf("-3", "2"), two.roots.map { it.exact })
        val repeated = calculate("4x^2-4x+1=0", AlgebraOperation.SOLVE).roots.single()
        assertEquals("1/2", repeated.exact)
        assertEquals(2, repeated.multiplicity)
        assertEquals("Действительных корней нет", calculate("x^2+1=0", AlgebraOperation.SOLVE).answer)
    }

    @Test fun irrationalRootsRetainExactFormAndDoNotLoseSmallRootToCancellation() {
        val sqrtTwo = calculate("x^2=2", AlgebraOperation.SOLVE).roots
        assertTrue(sqrtTwo.all { "√(8)" in it.exact })
        assertEquals(-kotlin.math.sqrt(2.0), sqrtTwo[0].approximate!!.toDouble(), 1e-13)
        assertEquals(kotlin.math.sqrt(2.0), sqrtTwo[1].approximate!!.toDouble(), 1e-13)
        val disparate = calculate("x^2+1e30*x-1=0", AlgebraOperation.SOLVE).roots
        assertEquals(0, BigDecimal(disparate[1].approximate).compareTo(BigDecimal("1e-30")))
        val negativeLeading = calculate("-x^2+2=0", AlgebraOperation.SOLVE).roots
        assertEquals(kotlin.math.sqrt(2.0), negativeLeading[0].approximate!!.toDouble(), 1e-13)
    }

    @Test fun preservesDomainByRejectingVariableDenominatorsAndInvalidInput() {
        listOf("x/x", "1/(x-1)", "0/0", "0^0", "x^0", "(x-1)^0", "x^(x-x)", "x^-1", "x^1.5", "x^13", "(x^7)^2",
            "sin(x)", "pi*x", "e*x", "y+1", "x%", "1 2 +", "x/0", "1e1000*x").forEach { expression ->
            assertThrows(expression, IllegalArgumentException::class.java) { calculate(expression) }
        }
        assertThrows(IllegalArgumentException::class.java) { calculate("x^3=2", AlgebraOperation.SOLVE) }
        assertThrows(IllegalArgumentException::class.java) { calculate("x=2=3", AlgebraOperation.SOLVE) }
    }

    @Test fun supportsKeyboardSymbolsDecimalCommaAndScientificNotation() {
        assertEquals("1/2·x^2 − 1/1000", calculate("0,5×X²−1e-3").answer)
        assertEquals("12·x^11", calculate("x^12", AlgebraOperation.DERIVATIVE).answer)
    }

    private fun calculate(input: String, operation: AlgebraOperation = AlgebraOperation.SIMPLIFY) =
        PolynomialAlgebra.calculate(input, operation)
}
