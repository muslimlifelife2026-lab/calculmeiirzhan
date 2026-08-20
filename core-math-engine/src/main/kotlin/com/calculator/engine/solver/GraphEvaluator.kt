package com.calculator.engine.solver

import com.calculator.engine.parser.MathParser
import com.calculator.engine.parser.MathToken
import kotlin.math.*

/**
 * Fast Double-based evaluator for plotting 2D graphs and numerical calculus analysis.
 * Optimizes the evaluation of y = f(x), r = f(θ), and x(t), y(t) by avoiding object allocations.
 */
class GraphEvaluator(val expression: String) {
    private val rpn: List<MathToken>

    init {
        val tokens = MathParser.tokenize(expression)
        rpn = MathParser.shuntingYard(tokens)
    }

    /**
     * Extracts any parameter names in the formula that aren't coordinate variables or math constants.
     * Useful for automatically generating parameter slider controls (e.g. 'a', 'b', 'c', 'k').
     */
    fun extractParameters(excludeSymbols: Set<String> = setOf("x", "t", "theta", "th", "pi", "e")): List<String> {
        val params = mutableListOf<String>()
        for (token in rpn) {
            if (token is MathToken.Variable) {
                val sym = token.symbol.lowercase()
                if (sym !in excludeSymbols && sym !in params) {
                    params.add(token.symbol)
                }
            }
        }
        return params
    }

    /**
     * Evaluates the function for a given coordinate variable value and dynamic parameters.
     */
    fun evaluate(
        x: Double,
        params: Map<String, Double> = emptyMap(),
        varSymbol: String = "x"
    ): Double {
        if (rpn.isEmpty()) return Double.NaN
        val stack = DoubleArray(rpn.size)
        var sp = 0

        for (token in rpn) {
            when (token) {
                is MathToken.Number -> {
                    stack[sp++] = token.value.toDouble()
                }
                is MathToken.Variable -> {
                    val sym = token.symbol
                    when {
                        sym.equals(varSymbol, ignoreCase = true) -> {
                            stack[sp++] = x
                        }
                        params.containsKey(sym) -> {
                            stack[sp++] = params[sym] ?: 0.0
                        }
                        sym.equals("pi", ignoreCase = true) -> {
                            stack[sp++] = Math.PI
                        }
                        sym.equals("e", ignoreCase = true) -> {
                            stack[sp++] = Math.E
                        }
                        else -> {
                            stack[sp++] = 0.0
                        }
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
                            "/" -> if (b == 0.0) Double.NaN else a / b
                            "^" -> a.pow(b)
                            else -> Double.NaN
                        }
                        stack[sp++] = res
                    }
                }
                is MathToken.Func -> {
                    if (sp < 1) return Double.NaN
                    val a = stack[--sp]
                    val res = when (token.name.lowercase()) {
                        "sin" -> sin(a)
                        "cos" -> cos(a)
                        "tan" -> {
                            val cosVal = cos(a)
                            if (abs(cosVal) < 1e-10) Double.NaN else tan(a)
                        }
                        "asin" -> if (a in -1.0..1.0) asin(a) else Double.NaN
                        "acos" -> if (a in -1.0..1.0) acos(a) else Double.NaN
                        "atan" -> atan(a)
                        "sinh" -> sinh(a)
                        "cosh" -> cosh(a)
                        "tanh" -> tanh(a)
                        "sqrt" -> if (a >= 0.0) sqrt(a) else Double.NaN
                        "ln" -> if (a > 0.0) ln(a) else Double.NaN
                        "log" -> if (a > 0.0) log10(a) else Double.NaN
                        "exp" -> exp(a)
                        "abs" -> abs(a)
                        else -> Double.NaN
                    }
                    stack[sp++] = res
                }
                else -> {
                    // Ignore comma, parens
                }
            }
        }
        return if (sp == 1) stack[0] else Double.NaN
    }

    /**
     * Bulk evaluates y values for a range of x values to maximize canvas rendering performance.
     */
    fun evaluateRange(
        xMin: Double,
        xMax: Double,
        pointsCount: Int,
        params: Map<String, Double> = emptyMap(),
        varSymbol: String = "x"
    ): DoubleArray {
        val yValues = DoubleArray(pointsCount)
        if (pointsCount <= 1) return yValues

        val step = (xMax - xMin) / (pointsCount - 1)
        for (i in 0 until pointsCount) {
            val x = xMin + i * step
            yValues[i] = evaluate(x, params, varSymbol)
        }
        return yValues
    }

    /**
     * Calculates the numerical derivative f'(x) using central difference formula:
     * f'(x) ≈ (f(x + h) - f(x - h)) / (2h)
     */
    fun evaluateDerivative(
        x: Double,
        params: Map<String, Double> = emptyMap(),
        varSymbol: String = "x",
        h: Double = 1e-5
    ): Double {
        val yPlus = evaluate(x + h, params, varSymbol)
        val yMinus = evaluate(x - h, params, varSymbol)
        if (yPlus.isNaN() || yMinus.isNaN() || yPlus.isInfinite() || yMinus.isInfinite()) {
            return Double.NaN
        }
        return (yPlus - yMinus) / (2.0 * h)
    }

    /**
     * Evaluates the definite integral ∫[a, b] f(x)dx using Simpson's Composite 1/3 Rule.
     */
    fun evaluateIntegral(
        a: Double,
        b: Double,
        params: Map<String, Double> = emptyMap(),
        varSymbol: String = "x",
        intervals: Int = 100
    ): Double {
        if (a == b) return 0.0
        val n = max(2, if (intervals % 2 == 0) intervals else intervals + 1)
        val h = (b - a) / n

        val fa = evaluate(a, params, varSymbol)
        val fb = evaluate(b, params, varSymbol)
        if (fa.isNaN() || fb.isNaN()) return Double.NaN

        var sumOdd = 0.0
        var sumEven = 0.0

        for (i in 1 until n) {
            val x = a + i * h
            val fx = evaluate(x, params, varSymbol)
            if (fx.isNaN() || fx.isInfinite()) return Double.NaN
            if (i % 2 != 0) {
                sumOdd += fx
            } else {
                sumEven += fx
            }
        }

        return (h / 3.0) * (fa + fb + 4.0 * sumOdd + 2.0 * sumEven)
    }

    /**
     * Evaluates polar coordinates r = f(θ) and returns a list of (x, y) Cartesian points:
     * x = r * cos(θ), y = r * sin(θ)
     */
    fun evaluatePolarRange(
        thetaMin: Double,
        thetaMax: Double,
        pointsCount: Int,
        params: Map<String, Double> = emptyMap()
    ): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val step = (thetaMax - thetaMin) / max(1, pointsCount - 1)

        for (i in 0 until pointsCount) {
            val theta = thetaMin + i * step
            val r = evaluate(theta, params, varSymbol = "theta")
            if (!r.isNaN() && !r.isInfinite()) {
                val x = r * cos(theta)
                val y = r * sin(theta)
                points.add(Pair(x, y))
            } else {
                points.add(Pair(Double.NaN, Double.NaN))
            }
        }
        return points
    }

    /**
     * Evaluates parametric curve where this is x(t) and other is y(t).
     */
    fun evaluateParametricRange(
        otherY: GraphEvaluator,
        tMin: Double,
        tMax: Double,
        pointsCount: Int,
        params: Map<String, Double> = emptyMap()
    ): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val step = (tMax - tMin) / max(1, pointsCount - 1)

        for (i in 0 until pointsCount) {
            val t = tMin + i * step
            val x = evaluate(t, params, varSymbol = "t")
            val y = otherY.evaluate(t, params, varSymbol = "t")
            if (!x.isNaN() && !y.isNaN() && !x.isInfinite() && !y.isInfinite()) {
                points.add(Pair(x, y))
            } else {
                points.add(Pair(Double.NaN, Double.NaN))
            }
        }
        return points
    }

    /**
     * Finds intersections between this function f1(x) and another function f2(x).
     */
    fun findIntersections(
        other: GraphEvaluator,
        xMin: Double,
        xMax: Double,
        pointsCount: Int = 300,
        params: Map<String, Double> = emptyMap()
    ): List<Pair<Double, Double>> {
        val intersections = mutableListOf<Pair<Double, Double>>()
        val step = (xMax - xMin) / (pointsCount - 1)

        var prevDiff = evaluate(xMin, params) - other.evaluate(xMin, params)
        var prevX = xMin

        for (i in 1 until pointsCount) {
            val currX = xMin + i * step
            val y1 = evaluate(currX, params)
            val y2 = other.evaluate(currX, params)
            val currDiff = y1 - y2

            if (!prevDiff.isNaN() && !currDiff.isNaN()) {
                if ((prevDiff <= 0 && currDiff >= 0) || (prevDiff >= 0 && currDiff <= 0)) {
                    // Linear interpolation zero-crossing refinement
                    val denom = abs(currDiff - prevDiff)
                    val crossX = if (denom > 1e-12) {
                        prevX + step * (abs(prevDiff) / denom)
                    } else {
                        (prevX + currX) / 2.0
                    }
                    val crossY = (evaluate(crossX, params) + other.evaluate(crossX, params)) / 2.0

                    if (!crossY.isNaN() && !crossY.isInfinite()) {
                        if (intersections.none { abs(it.first - crossX) < (xMax - xMin) / 40.0 }) {
                            intersections.add(Pair(crossX, crossY))
                        }
                    }
                }
            }
            prevDiff = currDiff
            prevX = currX
        }

        return intersections
    }
}

