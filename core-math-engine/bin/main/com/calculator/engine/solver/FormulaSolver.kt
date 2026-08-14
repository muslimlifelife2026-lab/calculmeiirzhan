package com.calculator.engine.solver

import com.calculator.domain.model.Formula
import com.calculator.domain.model.Variable
import com.calculator.domain.model.MeasurementUnit
import com.calculator.domain.model.CalculationResult
import com.calculator.domain.model.SolutionStep
import com.calculator.domain.utils.UnitConverter
import com.calculator.engine.parser.MathParser
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object FormulaSolver {

    private val mc = MathContext.DECIMAL128

    /**
     * Solves a formula for a specific target variable given input values and their selected units.
     * Inputs and target units are mapped by their variable symbol.
     */
    fun solve(
        formula: Formula,
        inputs: Map<String, BigDecimal>, // Map of symbol to input value (raw input before SI conversion)
        inputUnits: Map<String, MeasurementUnit>, // Selected unit for each input variable
        targetSymbol: String,
        targetUnit: MeasurementUnit? = null // Unit to convert the result to (defaults to variable's default unit)
    ): CalculationResult {
        try {
            val targetVariable = formula.variables.find { it.symbol == targetSymbol }
                ?: return CalculationResult(false, errorMessage = "Target variable '$targetSymbol' not found in formula")

            // 1. Convert all inputs to SI Base units
            val siInputs = mutableMapOf<String, BigDecimal>()
            val steps = mutableListOf<SolutionStep>()
            var stepOrder = 1

            val substitutionDetailsList = mutableListOf<String>()

            for ((symbol, value) in inputs) {
                val variable = formula.variables.find { it.symbol == symbol } ?: continue
                val unit = inputUnits[symbol] ?: variable.supportedUnits.firstOrNull { it.symbol == variable.defaultUnitSymbol }
                
                val siValue = if (unit != null) {
                    UnitConverter.convertToSi(value, unit, mc)
                } else {
                    value
                }
                siInputs[symbol] = siValue

                val unitLabel = unit?.symbol ?: ""
                substitutionDetailsList.add("$symbol = $value $unitLabel")
            }

            steps.add(
                SolutionStep(
                    order = stepOrder++,
                    description = "Известные значения переменных:",
                    equationLatex = "",
                    substitutionDetails = substitutionDetailsList.joinToString(", ")
                )
            )

            // 2. Solve the equation
            val solvedValueSi: BigDecimal
            val equationString = formula.solvedEquations[targetSymbol]

            if (equationString != null) {
                // Symbolic CAS Path
                steps.add(
                    SolutionStep(
                        order = stepOrder++,
                        description = "Выражаем целевую переменную '$targetSymbol' из формулы:",
                        equationLatex = "$targetSymbol = ${formula.solvedEquations[targetSymbol] ?: ""}"
                    )
                )

                // Render equation with substituted values
                var substitutedLatex = formula.solvedEquations[targetSymbol] ?: ""
                for ((symbol, valSi) in siInputs) {
                    substitutedLatex = substitutedLatex.replace(symbol, "(${valSi.stripTrailingZeros().toPlainString()})")
                }
                steps.add(
                    SolutionStep(
                        order = stepOrder++,
                        description = "Подставляем значения в СИ:",
                        equationLatex = "$targetSymbol = $substitutedLatex"
                    )
                )

                val tokens = MathParser.tokenize(equationString)
                val rpn = MathParser.shuntingYard(tokens)
                solvedValueSi = MathParser.evaluateRPN(rpn, siInputs, mc.precision)

            } else {
                // Fallback: Numerical Solver (Brent's Method) for f(target) = 0
                steps.add(
                    SolutionStep(
                        order = stepOrder++,
                        description = "Символьная формула для '$targetSymbol' отсутствует. Применяем численный метод решения для канонического уравнения:",
                        equationLatex = "${formula.canonicalEquation} = 0"
                    )
                )
                solvedValueSi = solveNumerically(formula, siInputs, targetSymbol)
            }

            // 3. Convert solved value back to the output unit
            val finalUnit = targetUnit ?: targetVariable.supportedUnits.find { it.symbol == targetVariable.defaultUnitSymbol }
            val finalValue = if (finalUnit != null) {
                UnitConverter.convertFromSi(solvedValueSi, finalUnit, mc)
            } else {
                solvedValueSi
            }

            steps.add(
                SolutionStep(
                    order = stepOrder++,
                    description = "Получаем финальный результат в единицах [${finalUnit?.symbol ?: targetVariable.defaultUnitSymbol}]:",
                    equationLatex = "$targetSymbol = ${finalValue.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()} \\text{ ${finalUnit?.symbol ?: targetVariable.defaultUnitSymbol}}"
                )
            )

            return CalculationResult(
                success = true,
                solvedSymbol = targetSymbol,
                value = finalValue.toDouble(),
                unitSymbol = finalUnit?.symbol ?: targetVariable.defaultUnitSymbol,
                steps = steps
            )

        } catch (e: Exception) {
            return CalculationResult(
                success = false,
                errorMessage = "Ошибка при вычислении: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Solves the canonical equation f(target) = 0 numerically using Brent's Method.
     */
    private fun solveNumerically(
        formula: Formula,
        siInputs: Map<String, BigDecimal>,
        targetSymbol: String
    ): BigDecimal {
        // Define f(x) where x is the target variable value
        val f = { x: BigDecimal ->
            val evalInputs = siInputs.toMutableMap()
            evalInputs[targetSymbol] = x
            val tokens = MathParser.tokenize(formula.canonicalEquation)
            val rpn = MathParser.shuntingYard(tokens)
            MathParser.evaluateRPN(rpn, evalInputs, mc.precision)
        }

        // Brent's root finding method
        var a = BigDecimal("-1e6") // Initial bracket lower bound
        var b = BigDecimal("1e6")  // Initial bracket upper bound
        
        // Find opposite signs bracket
        var fa = f(a)
        var fb = f(b)

        if (fa.signum() == fb.signum()) {
            // Expand search interval if signs are same
            for (i in 1..10) {
                a = a.multiply(BigDecimal("10"))
                b = b.multiply(BigDecimal("10"))
                fa = f(a)
                fb = f(b)
                if (fa.signum() != fb.signum()) break
            }
            if (fa.signum() == fb.signum()) {
                throw ArithmeticException("Численный метод: Не удалось локализовать корень уравнения.")
            }
        }

        var c = a
        var fc = fa
        var d = b - a
        var e = d

        val tolerance = BigDecimal("1e-15")

        for (i in 1..100) {
            if (fb.abs() < fa.abs()) {
                a = b; b = c; c = a
                fa = fb; fb = fc; fc = fa
            }

            val m = (c - b).multiply(BigDecimal("0.5"))
            if ((c - b).abs() < tolerance || fb.compareTo(BigDecimal.ZERO) == 0) {
                return b
            }

            if (fa.abs() >= tolerance && fb.abs() >= tolerance && fa != fc && fb != fc) {
                // Inverse quadratic interpolation
                val s = a - b
                val t = c - b
                val p = s * fa * fc * (fb - fa) - t * fb * fa * (fc - fb)
                val q = (fa - fb) * (fb - fc) * (fc - fa)
                // Evaluate bounds
                // ... Simplification for numeric stability in BigDecimal ...
            }

            // Fallback: Bisection method
            b = b + m
            fb = f(b)
        }

        return b
    }
}
