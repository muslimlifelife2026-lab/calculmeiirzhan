package com.calculator.core.ui.utils

import java.util.Locale

object ErrorLocalizer {
    fun localize(error: String, locale: Locale = Locale.getDefault()): String {
        return when {
            error.contains("Parentheses mismatch") -> "несовпадение скобок"
            error.contains("Division by zero") -> "деление на ноль"
            error.contains("Unknown operator") -> error.replace("Unknown operator", "неизвестный оператор")
            error.contains("Unexpected character") -> error
                .replace("Unexpected character", "неожиданный символ")
                .replace("at position", "на позиции")
            error.contains("Variable not initialized") -> "переменная не задана"
            error.contains("Факториал определен") -> "факториал определен только для целых неотрицательных чисел"
            error.contains("Значение слишком велико") -> "значение слишком велико для факториала"
            else -> error
        }
    }
}
