package com.calculator.core.ui.utils

object MathFormatter {

    fun format(template: String): String {
        return template
            .replace("\\cdot", " · ")
            .replace("\\frac{1}{2}", "½")
            .replace("\\frac{4}{3}", "⁴/₃")
            .replace("\\frac{U}{R}", "U / R")
            .replace("\\frac{F}{S}", "F / S")
            .replace("\\frac{m_1 \\cdot m_2}{r^2}", "(m₁ · m₂) / r²")
            .replace("\\frac{R}{100}", "R / 100")
            .replace("\\pi", "π")
            .replace("\\rho", "ρ")
            .replace("r^2", "r²")
            .replace("v^2", "v²")
            .replace("c^2", "c²")
            .replace("a^2", "a²")
            .replace("b^2", "b²")
            .replace("r^3", "r³")
            .replace("\\", "") // Remove any remaining backslashes
            .replace("  ", " ") // Clean double spaces
            .trim()
    }
}
