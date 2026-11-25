package com.aerocalc.smartcalculator.ui

import com.calculator.core.ui.utils.ErrorLocalizer
import com.calculator.core.ui.utils.MathFormatter
import com.calculator.domain.utils.FormulaLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatterAndLocalizerTest {
    @Test fun allIndexedFormulasFormatWithoutRawBackslashesOrFracTokens() {
        FormulaLoader.formulas.forEach { formula ->
            val formatted = MathFormatter.format(formula.latexTemplate.ifBlank { formula.canonicalEquation })
            assertFalse("${formula.id} had backslash: $formatted", formatted.contains('\\'))
            assertFalse("${formula.id} had frac{: $formatted", formatted.contains("frac{"))
        }
    }

    @Test fun mathFormatterFormatsFractionsGreekLettersSubscriptsAndSuperscripts() {
        assertEquals("P = (v₂ - v₁) / v₁ · 100", MathFormatter.format("P = \\frac{v_2 - v_1}{v_1} \\cdot 100"))
        assertEquals("f = v / λ", MathFormatter.format("f = \\frac{v}{\\lambda}"))
        assertEquals("V = ⅓ π · r² · h", MathFormatter.format("V = \\frac{1}{3} \\pi \\cdot r^2 \\cdot h"))
    }

    @Test fun errorLocalizerTranslatesEngineErrorsIntoRussian() {
        val cases = listOf(
            "Parentheses mismatch",
            "Illegal sqrt(x) for x < 0",
            "Illegal log(x) for x <= 0",
            "Illegal asin(x) for x > 1",
            "Unknown function: foo",
            "Not enough operands",
            "Overflow"
        )
        cases.forEach { raw ->
            val localized = ErrorLocalizer.localize(raw)
            assertFalse("Was not localized: $localized", localized == raw)
            assertFalse("Contains English keyword: $localized", Regex("\\b(Illegal|Unknown|Not enough|Overflow|Parentheses)\\b").containsMatchIn(localized))
            assertTrue("Should contain Cyrillic: $localized", localized.any { it in '\u0400'..'\u04FF' })
        }
    }
}
