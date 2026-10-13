package com.calculator.domain.utils

import com.calculator.domain.model.Variable
import java.math.BigDecimal

object FormulaInputValidator {
    data class CheckedValue(val value: BigDecimal? = null, val error: String? = null)

    fun check(variable: Variable, raw: String): CheckedValue {
        val normalized = raw.trim().replace(',', '.')
        val value = normalized.toBigDecimalOrNull()
            ?: return CheckedValue(error = "Введите число для «${variable.name}»")
        val name = variable.name.lowercase()
        val nonnegative = listOf("масса", "радиус", "длина", "ширина", "площадь", "объём", "объем", "плотность", "давление", "время", "период")
            .any { name.contains(it) }
        if (nonnegative && value.signum() < 0) {
            return CheckedValue(error = "«${variable.name}» не может быть отрицательным")
        }
        return CheckedValue(value = value)
    }
}
