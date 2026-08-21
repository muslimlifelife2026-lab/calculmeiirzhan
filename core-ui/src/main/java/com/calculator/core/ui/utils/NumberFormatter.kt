package com.calculator.core.ui.utils

import java.util.Locale

object NumberFormatter {
    fun formatResult(value: Double, maxDecimals: Int = 6): String {
        return formatInternal(value, maxDecimals)
    }

    fun formatCompact(value: Double): String {
        return formatInternal(value, 4)
    }

    private fun formatInternal(value: Double, decimals: Int): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        
        val absValue = Math.abs(value)
        if (absValue > 0 && (absValue >= 1e9 || absValue < 1e-6)) {
            val formatStr = "%." + decimals + "E"
            var result = String.format(Locale.US, formatStr, value)
            if (result.contains("E")) {
                val parts = result.split("E")
                var mantissa = parts[0]
                val exponent = parts[1]
                if (mantissa.contains(".")) {
                    mantissa = mantissa.trimEnd('0').trimEnd('.')
                }
                result = "${mantissa}E${exponent}"
            }
            return result
        }

        val formatStr = "%." + decimals + "f"
        var formatted = String.format(Locale.US, formatStr, value)
        if (formatted.contains(".")) {
            formatted = formatted.trimEnd('0').trimEnd('.')
        }
        return formatted
    }
}
