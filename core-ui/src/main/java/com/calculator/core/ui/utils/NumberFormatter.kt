package com.calculator.core.ui.utils

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import com.calculator.domain.model.CalculationResult

object NumberFormatter {
    fun formatResult(result: CalculationResult): String =
        result.exactValue.takeIf { it.isNotBlank() }?.let { formatExact(BigDecimal(it)) }
            ?: formatResult(result.value)

    fun formatExact(value: BigDecimal): String {
        val compact = value.stripTrailingZeros()
        if (compact.signum() == 0) return "0"
        val exponent = compact.precision() - compact.scale() - 1
        return if (exponent < -6 || exponent > 12) compact.toString() else compact.toPlainString()
    }

    fun formatResult(value: Double, significantDigits: Int = 8): String =
        formatDouble(value, significantDigits)

    fun formatCompact(value: Double): String = formatDouble(value, 5)

    private fun formatDouble(value: Double, significantDigits: Int): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        val rounded = BigDecimal.valueOf(value).round(MathContext(significantDigits, RoundingMode.HALF_UP))
        return formatExact(rounded)
    }
}
