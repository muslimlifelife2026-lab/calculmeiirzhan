package com.calculator.core.ui.utils

import java.util.Locale

object ErrorLocalizer {
    fun localize(error: String, locale: Locale = Locale.getDefault()): String {
        return when {
            error.contains("Parentheses mismatch", ignoreCase = true) ||
                error.contains("Mismatched parentheses", ignoreCase = true) -> "Несовпадение скобок"
            error.contains("Division by zero", ignoreCase = true) ||
                error.contains("Divide by zero", ignoreCase = true) -> "Деление на ноль"
            error.contains("Illegal sqrt", ignoreCase = true) -> "Квадратный корень из отрицательного числа не определён"
            error.contains("Illegal log", ignoreCase = true) -> "Логарифм определён только для положительных чисел"
            error.contains("Illegal asin", ignoreCase = true) ||
                error.contains("Illegal acos", ignoreCase = true) -> "Аргумент должен быть в диапазоне от −1 до 1"
            error.contains("Illegal ", ignoreCase = true) -> "Аргумент вне области определения"
            error.contains("Not enough operands", ignoreCase = true) ||
                error.contains("Invalid expression", ignoreCase = true) -> "Незавершённое или некорректное выражение"
            error.contains("Unknown function", ignoreCase = true) ||
                error.contains("Unsupported function", ignoreCase = true) -> "Неизвестная функция"
            error.contains("Unknown operator", ignoreCase = true) ->
                error.replace("Unknown operator", "Неизвестный оператор", ignoreCase = true)
            error.contains("Unexpected character", ignoreCase = true) -> error
                .replace("Unexpected character", "Неожиданный символ", ignoreCase = true)
                .replace("at position", "на позиции", ignoreCase = true)
            error.contains("Variable not initialized", ignoreCase = true) -> "Переменная не задана"
            error.contains("Exponent overflow", ignoreCase = true) ||
                error.contains("Overflow", ignoreCase = true) -> "Результат слишком велик"
            error.contains("For input string", ignoreCase = true) ||
                error.contains("NumberFormatException", ignoreCase = true) -> "Некорректный формат числа"
            error.contains("Факториал определен") -> "Факториал определен только для целых неотрицательных чисел"
            error.contains("Значение слишком велико") -> "Значение слишком велико для факториала"
            else -> error
        }
    }
}
