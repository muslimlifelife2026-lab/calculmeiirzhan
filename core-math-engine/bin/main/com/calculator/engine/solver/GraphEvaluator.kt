package com.calculator.engine.solver

import com.calculator.engine.parser.MathParser
import com.calculator.engine.parser.MathToken
import java.util.Stack
import kotlin.math.*

/**
 * Fast Double-based evaluator for plotting 2D graphs.
 * Optimizes the evaluation of y = f(x) by avoiding BigDecimal allocations.
 */
class GraphEvaluator(expression: String) {
    private val rpn: List<MathToken>

    init {
        val tokens = MathParser.tokenize(expression)
        rpn = MathParser.shuntingYard(tokens)
    }

    /**
     * Evaluates the function for a given x value.
     */
    fun evaluate(x: Double): Double {
        val stack = DoubleArray(rpn.size)
        var sp = 0

        for (token in rpn) {
            when (token) {
                is MathToken.Number -> {
                    stack[sp++] = token.value.toDouble()
                }
                is MathToken.Variable -> {
                    if (token.symbol == "x") {
                        stack[sp++] = x
                    } else {
                        // For now, treat unknown variables as 0.0 or throw
                        stack[sp++] = 0.0
                    }
                }
                is MathToken.Operator -> {
                    if (token.symbol == "u-") {
                        if (sp < 1) return Double.NaN
                        val a = stack[--sp]
                        stack[sp++] = -a
                    } else {
                        if (sp < 2) return Double.NaN
                        val b = stack[--sp]
                        val a = stack[--sp]
                        val res = when (token.symbol) {
                            "+" -> a + b
                            "-" -> a - b
                            "*" -> a * b
                            "/" -> a / b
                            "^" -> a.pow(b)
                            else -> Double.NaN
                        }
                        stack[sp++] = res
                    }
                }
                is MathToken.Func -> {
                    if (sp < 1) return Double.NaN
                    val a = stack[--sp]
                    val res = when (token.name) {
                        "sin" -> sin(a)
                        "cos" -> cos(a)
                        "tan" -> tan(a)
                        "asin" -> asin(a)
                        "acos" -> acos(a)
                        "atan" -> atan(a)
                        "sinh" -> sinh(a)
                        "cosh" -> cosh(a)
                        "tanh" -> tanh(a)
                        "sqrt" -> sqrt(a)
                        "ln" -> ln(a)
                        "log" -> log10(a)
                        "exp" -> exp(a)
                        "abs" -> abs(a)
                        "fact" -> {
                            Double.NaN
                        }
                        else -> Double.NaN
                    }
                    stack[sp++] = res
                }
                else -> {
                    // Ignore
                }
            }
        }
        return if (sp == 1) stack[0] else Double.NaN
    }

    /**
     * Bulk evaluates y values for a range of x values to improve plotting performance.
     */
    fun evaluateRange(xMin: Double, xMax: Double, pointsCount: Int): DoubleArray {
        val yValues = DoubleArray(pointsCount)
        if (pointsCount <= 1) return yValues
        
        val step = (xMax - xMin) / (pointsCount - 1)
        for (i in 0 until pointsCount) {
            val x = xMin + i * step
            yValues[i] = evaluate(x)
        }
        return yValues
    }
}
