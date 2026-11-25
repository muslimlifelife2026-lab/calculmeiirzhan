package com.calculator.core.ui.utils

object MathFormatter {

    private val fractionRegex = Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")
    private val superscriptRegex = Regex("""\^([0-9]+)""")
    private val subscriptRegex = Regex("""_([0-9]+)""")
    private val multiSpaceRegex = Regex("""\s+""")
    private val superDigits = charArrayOf('⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹')
    private val subDigits = charArrayOf('₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉')

    private fun wrapIfCompound(part: String): String {
        val trimmed = part.trim()
        return if (trimmed.any { it == '+' || it == '-' || it == '·' || it == '/' || it.isWhitespace() }) {
            "($trimmed)"
        } else {
            trimmed
        }
    }

    fun format(template: String): String {
        var result = template
            .replace("\\cdot", " · ")
            .replace("\\times", " × ")
            .replace("\\pi", "π")
            .replace("\\rho", "ρ")
            .replace("\\mu", "μ")
            .replace("\\tau", "τ")
            .replace("\\lambda", "λ")
            .replace("\\theta", "θ")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\Delta", "Δ")
            .replace("\\frac{1}{2}", "½")
            .replace("\\frac{1}{3}", "⅓")
            .replace("\\frac{4}{3}", "⁴⁄₃")

        repeat(5) {
            val next = fractionRegex.replace(result) { match ->
                val num = wrapIfCompound(match.groupValues[1])
                val den = wrapIfCompound(match.groupValues[2])
                "$num / $den"
            }
            if (next == result) return@repeat
            result = next
        }

        result = superscriptRegex.replace(result) { match ->
            match.groupValues[1].map { superDigits[it - '0'] }.joinToString("")
        }
        result = subscriptRegex.replace(result) { match ->
            match.groupValues[1].map { subDigits[it - '0'] }.joinToString("")
        }

        return result
            .replace("\\", "")
            .let { multiSpaceRegex.replace(it, " ") }
            .trim()
    }
}
