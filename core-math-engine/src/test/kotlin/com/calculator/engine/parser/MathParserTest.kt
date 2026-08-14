package com.calculator.engine.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.MathContext

class MathParserTest {

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
        assertEquals(BigDecimal("10"), result.stripTrailingZeros())
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
