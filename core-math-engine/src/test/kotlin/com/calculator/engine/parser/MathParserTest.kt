package com.calculator.engine.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.math.MathContext

class MathParserTest {

    @Test fun tangentPolesReportUndefinedInsteadOfHugeFiniteAnswers() {
        listOf("tan(90)", "tan(270)", "tan(-90)", "tan(450)", "tan(-270)",
            "tan(180000000000000000000000000090)").forEach { input ->
            assertThrows(input, ArithmeticException::class.java) {
                MathParser.evaluateRPN(MathParser.shuntingYard(MathParser.tokenize(input)), isDegrees = true)
            }
        }
        assertThrows(ArithmeticException::class.java) { calculate("tan(pi/2)") }
        val nearPole = MathParser.evaluateRPN(MathParser.shuntingYard(MathParser.tokenize("tan(89.99)")), isDegrees = true)
        org.junit.Assert.assertTrue(nearPole > BigDecimal(5000))
        val pastPole = MathParser.evaluateRPN(MathParser.shuntingYard(MathParser.tokenize("tan(270.01)")), isDegrees = true)
        org.junit.Assert.assertTrue(pastPole < BigDecimal(-5000))
        val period = MathParser.evaluateRPN(MathParser.shuntingYard(MathParser.tokenize("tan(180000000000000000000000000045)")), isDegrees = true)
        org.junit.Assert.assertTrue(period.subtract(BigDecimal.ONE).abs() < BigDecimal("1e-30"))
    }

    @Test fun degreeAnglesAreReducedBeforeConvertingToApproximateRadians() {
        val result = MathParser.evaluateRPN(MathParser.shuntingYard(MathParser.tokenize("cos(360000000000000000000000000000000000000000000000000000000000000000)")), isDegrees = true)
        assertEquals(0, result.compareTo(BigDecimal.ONE))
    }

    @Test fun malformedInfixInputCannotBorrowOperandsFromAnotherExpression() {
        listOf("1 2 +", "sin(1,2)+", "1+", "()", "2+sin()", "sin 2").forEach { expression ->
            assertThrows(expression, IllegalArgumentException::class.java) { calculate(expression) }
        }
    }

    @Test fun multiplicationAfterParenthesesAndSignedPowers() {
        mapOf("(2+3)4" to "20", "2^-2^2" to "0.0625", "(-2)^-3" to "-0.125")
            .forEach { (expression, expected) ->
                assertEquals(expression, 0, calculate(expression).compareTo(BigDecimal(expected)))
            }
    }

    private fun calculate(expression: String): BigDecimal =
        MathParser.evaluateRPN(MathParser.shuntingYard(MathParser.tokenize(expression)))

    @Test fun exponentAndUnaryMinusPrecedence() {
        mapOf("-2^2" to "-4", "(-2)^2" to "4", "2^-2" to "0.25", "2^3^2" to "512")
            .forEach { (expression, expected) ->
                assertEquals(expression, 0, calculate(expression).compareTo(BigDecimal(expected)))
            }
    }

    @Test fun scientificNotationAndImplicitMultiplication() {
        assertEquals(0, calculate("1e-3").compareTo(BigDecimal("0.001")))
        assertEquals(0, calculate("2(3+4)").compareTo(BigDecimal("14")))
    }

    @Test fun contextSensitivePercent() {
        assertEquals(0, calculate("200+10%").compareTo(BigDecimal("220")))
        assertEquals(0, calculate("200-10%").compareTo(BigDecimal("180")))
        assertEquals(0, calculate("200*10%").compareTo(BigDecimal("20")))
        assertEquals(0, calculate("200+10%+5%").compareTo(BigDecimal("231")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun hugeFactorialIsRejectedBeforeLongConversion() {
        calculate("fact(18446744073709551616)")
    }

    @Test
    fun testSimpleArithmetic() {
        val expr = "2 + 3 * 4"
        val tokens = MathParser.tokenize(expr)
        val rpn = MathParser.shuntingYard(tokens)
        val result = MathParser.evaluateRPN(rpn)
        assertEquals(BigDecimal("14"), result.stripTrailingZeros())
    }

    @Test
    fun testUnaryMinus() {
        val expr = "-5 + 3 * -2"
        val tokens = MathParser.tokenize(expr)
        val rpn = MathParser.shuntingYard(tokens)
        val result = MathParser.evaluateRPN(rpn)
        assertEquals(BigDecimal("-11"), result.stripTrailingZeros())
    }

    @Test
    fun testVariablesAndParentheses() {
        val expr = "(x + y) * 2"
        val tokens = MathParser.tokenize(expr)
        val rpn = MathParser.shuntingYard(tokens)
        val vars = mapOf(
            "x" to BigDecimal("3.5"),
            "y" to BigDecimal("1.5")
        )
        val result = MathParser.evaluateRPN(rpn, vars)
        assertEquals(0, result.compareTo(BigDecimal("10")))
    }

    @Test
    fun testTrigonometryAndConstants() {
        val expr = "sin(pi / 2)"
        val tokens = MathParser.tokenize(expr)
        val rpn = MathParser.shuntingYard(tokens)
        val result = MathParser.evaluateRPN(rpn)
        assertEquals(BigDecimal("1"), BigDecimal(result.toDouble()).setScale(0, java.math.RoundingMode.HALF_UP))
    }
}
