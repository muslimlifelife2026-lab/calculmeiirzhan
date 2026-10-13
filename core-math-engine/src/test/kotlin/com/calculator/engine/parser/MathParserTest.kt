package com.calculator.engine.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.MathContext

class MathParserTest {

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
