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
        require(expression.length <= 256) { "График: слишком длинное выражение" }
        val tokens = MathParser.tokenize(expression)
        require(tokens.size <= 256) { "График: слишком сложное выражение" }
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
    ): Double = evaluateWithStack(x, params, varSymbol, DoubleArray(rpn.size), BooleanArray(rpn.size))

    private fun evaluateWithStack(
        x: Double,
        params: Map<String, Double>,
        varSymbol: String,
        stack: DoubleArray,
        percentFlags: BooleanArray
    ): Double {
        if (rpn.isEmpty()) return Double.NaN
        var sp = 0

        for (token in rpn) {
            when (token) {
                is MathToken.Number -> {
                    stack[sp++] = token.value.toDouble()
                    percentFlags[sp - 1] = false
                }
                is MathToken.Variable -> {
                    val sym = token.symbol
                    when {
                        sym.equals(varSymbol, ignoreCase = true) ||
                            (varSymbol == "theta" && sym.equals("th", ignoreCase = true)) -> {
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
                            stack[sp++] = Double.NaN
                        }
                    }
                    percentFlags[sp - 1] = false
                }
                is MathToken.Operator -> {
                    if (token.symbol == "%") {
                        if (sp < 1) return Double.NaN
                        stack[sp - 1] /= 100.0
                        percentFlags[sp - 1] = true
                    } else if (token.symbol == "u-") {
                        if (sp < 1) return Double.NaN
                        val a = stack[--sp]
                        stack[sp++] = -a
                    } else {
                        if (sp < 2) return Double.NaN
                        val b = stack[--sp]
                        val a = stack[--sp]
                        val bIsPercent = percentFlags[sp + 1]
                        val res = when (token.symbol) {
                            "+" -> a + if (bIsPercent) a * b else b
                            "-" -> a - if (bIsPercent) a * b else b
                            "*" -> a * b
                            "/" -> if (b == 0.0) Double.NaN else a / b
                            "^" -> a.pow(b)
                            else -> Double.NaN
                        }
                        stack[sp++] = res
                        percentFlags[sp - 1] = false
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
                        "fact" -> if (a.isFinite() && a >= 0.0 && a <= 170.0 && a == floor(a)) {
                            var factorial = 1.0
                            for (factor in 2..a.toInt()) factorial *= factor
                            factorial
                        } else Double.NaN
                        else -> Double.NaN
                    }
                    stack[sp++] = res
                    percentFlags[sp - 1] = false
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
        require(pointsCount in 0..1200) { "Слишком много точек графика" }
        val yValues = DoubleArray(pointsCount)
        if (pointsCount == 0) return yValues
        if (pointsCount == 1) return doubleArrayOf(evaluate(xMin, params, varSymbol))

        val step = (xMax - xMin) / (pointsCount - 1)
        val stack = DoubleArray(rpn.size)
        val flags = BooleanArray(rpn.size)
        for (i in 0 until pointsCount) {
            val x = xMin + i * step
            yValues[i] = evaluateWithStack(x, params, varSymbol, stack, flags)
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
        require(h.isFinite() && h > 0.0) { "Шаг производной должен быть положительным" }
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
        require(intervals in 2..10_000) { "Число интервалов должно быть от 2 до 10000" }
        if (a == b) return 0.0
        val n = max(2, if (intervals % 2 == 0) intervals else intervals + 1)
        val h = (b - a) / n

        val fa = evaluate(a, params, varSymbol)
        val fb = evaluate(b, params, varSymbol)
        if (!fa.isFinite() || !fb.isFinite()) return Double.NaN

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
        require(pointsCount in 0..1200) { "Слишком много точек графика" }
        val points = mutableListOf<Pair<Double, Double>>()
        val step = (thetaMax - thetaMin) / max(1, pointsCount - 1)
        val stack = DoubleArray(rpn.size)
        val flags = BooleanArray(rpn.size)

        for (i in 0 until pointsCount) {
            val theta = thetaMin + i * step
            val r = evaluateWithStack(theta, params, "theta", stack, flags)
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
        require(pointsCount in 0..1200) { "Слишком много точек графика" }
        val points = mutableListOf<Pair<Double, Double>>()
        val step = (tMax - tMin) / max(1, pointsCount - 1)
        val xStack = DoubleArray(rpn.size)
        val yStack = DoubleArray(otherY.rpn.size)
        val xFlags = BooleanArray(rpn.size)
        val yFlags = BooleanArray(otherY.rpn.size)

        for (i in 0 until pointsCount) {
            val t = tMin + i * step
            val x = evaluateWithStack(t, params, "t", xStack, xFlags)
            val y = otherY.evaluateWithStack(t, params, "t", yStack, yFlags)
            if (!x.isNaN() && !y.isNaN() && !x.isInfinite() && !y.isInfinite()) {
                points.add(Pair(x, y))
            } else {
                points.add(Pair(Double.NaN, Double.NaN))
            }
        }
        return points
    }

    fun findRoots(xMin: Double, xMax: Double, pointsCount: Int = 400,
                  params: Map<String, Double> = emptyMap()): List<Double> {
        val stack = DoubleArray(rpn.size)
        val flags = BooleanArray(rpn.size)
        return findRootsOf(xMin, xMax, pointsCount) { evaluateWithStack(it, params, "x", stack, flags) }
    }

    private fun findRootsOf(xMin: Double, xMax: Double, pointsCount: Int, f: (Double) -> Double): List<Double> {
        require(pointsCount <= 1200) { "Слишком много точек графика" }
        if (pointsCount < 3 || !xMin.isFinite() || !xMax.isFinite() || xMax <= xMin) return emptyList()
        val step = (xMax - xMin) / (pointsCount - 1)
        val values = DoubleArray(pointsCount) { f(xMin + it * step) }
        // A constant function has either no zeros or infinitely many, not isolated roots.
        if (values.all { it == values[0] }) return emptyList()
        val roots = mutableListOf<Double>()
        fun addIfValid(x: Double, localScale: Double = 0.0) {
            val y = f(x)
            if (y.isFinite() && abs(y) <= localScale * 1e-10 &&
                roots.none { abs(it - x) < 1e-7 * max(1.0, abs(x)) }) roots += x
        }
        for (i in 0 until pointsCount - 1) {
            val y0 = values[i]
            val y1 = values[i + 1]
            if (!y0.isFinite() || !y1.isFinite()) continue
            val x0 = xMin + i * step
            if (y0 == 0.0) addIfValid(x0)
            if (y0.sign != y1.sign) {
                var left = x0
                var right = x0 + step
                var leftY = y0
                repeat(50) {
                    val mid = (left + right) / 2
                    val midY = f(mid)
                    if (midY.isFinite()) {
                        if (midY == 0.0 || midY.sign == leftY.sign) {
                            left = mid
                            leftY = midY
                        } else right = mid
                    }
                }
                addIfValid((left + right) / 2, max(abs(y0), abs(y1)))
            }
            if (i > 0 && values[i - 1].isFinite() && abs(y0) < abs(values[i - 1]) && abs(y0) < abs(y1)) {
                var left = x0 - step
                var right = x0 + step
                repeat(40) {
                    val third = (right - left) / 3
                    val a = abs(f(left + third))
                    val b = abs(f(right - third))
                    if (a < b) right -= third else left += third
                }
                addIfValid((left + right) / 2, max(abs(values[i - 1]), abs(y1)))
            }
        }
        addIfValid(xMax)
        return roots.sorted()
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
        val firstStack = DoubleArray(rpn.size)
        val secondStack = DoubleArray(other.rpn.size)
        val firstFlags = BooleanArray(rpn.size)
        val secondFlags = BooleanArray(other.rpn.size)
        val roots = findRootsOf(xMin, xMax, pointsCount) {
            evaluateWithStack(it, params, "x", firstStack, firstFlags) -
                other.evaluateWithStack(it, params, "x", secondStack, secondFlags)
        }
        return roots.mapNotNull { x ->
            val y1 = evaluate(x, params)
            val y2 = other.evaluate(x, params)
            if (y1.isFinite() && y2.isFinite() && abs(y1 - y2) <= 1e-5 * max(1.0, abs(y1))) {
                x to (y1 + y2) / 2
            } else null
        }
    }
}

