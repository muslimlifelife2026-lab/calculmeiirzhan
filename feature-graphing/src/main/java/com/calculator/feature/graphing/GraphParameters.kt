package com.calculator.feature.graphing

import com.calculator.engine.solver.GraphEvaluator
import kotlin.math.round

/** Names come from variable tokens, so tan/abs/scientific notation are never rewritten. */
object GraphParameters {
    fun discover(expressions: List<String>, mode: GraphMode): List<String> {
        val coordinates = when (mode) {
            GraphMode.CARTESIAN -> setOf("x", "pi", "e")
            GraphMode.POLAR -> setOf("theta", "th", "pi", "e")
            GraphMode.PARAMETRIC -> setOf("t", "pi", "e")
        }
        return expressions.flatMap { expression ->
            runCatching { GraphEvaluator(expression).extractParameters(coordinates) }.getOrDefault(emptyList())
        }.distinct()
    }

    /** Matches the slider's displayed 0.1 increments, instead of leaking Float noise into calculations. */
    fun sliderValue(value: Float): Double {
        require(value.isFinite())
        return round(value.coerceIn(-5f, 5f).toDouble() * 10.0) / 10.0
    }
}
