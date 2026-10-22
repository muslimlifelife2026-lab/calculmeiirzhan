package com.calculator.engine.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MathScanNormalizerTest {
    @Test fun preservesBothSidesOfEquationAndPowers() {
        assertEquals("x^2 - 5x + 6 = 0", MathScanNormalizer.normalize(" x² − 5x + 6 = 0 "))
        assertEquals("2x + 3 = 7", MathScanNormalizer.normalize("2x + 3 = 7"))
    }

    @Test fun preservesTwoEquationsWithoutMergingThem() {
        assertEquals("2x + y = 7\nx - y = 2", MathScanNormalizer.normalize("\n2X + Y = 7\n\nx − y = 2\n"))
        assertNull(MathScanNormalizer.normalize("2x + y = 7\nx - y = 2\nx=3"))
    }

    @Test fun normalizesArithmeticWithoutChangingVariableXToMultiplication() {
        assertEquals("1.5 * 4 / 2", MathScanNormalizer.normalize("1,5 × 4 ÷ 2"))
        assertEquals("sin(pi/2) + sqrt(9) + exp(1)", MathScanNormalizer.normalize("SIN(pi/2) + sqrt(9) + exp(1)"))
        assertEquals("x*y=2", MathScanNormalizer.normalize("x·y=2"))
        assertEquals("e", MathScanNormalizer.normalize("e"))
    }

    @Test fun rejectsAmbiguousLayoutsAndUnrelatedTextInsteadOfInventingAProblem() {
        listOf("", "Пример 2x=4", "Solve x=3", "2+2\n3+3", "2+2=4\n3+3=6", "1\n2", "2x=4\n+3", "x=2=3",
            "2+2=", "=4", "hello", "123 ?", "x + z = 2").forEach {
            assertNull(it, MathScanNormalizer.normalize(it))
        }
    }

    @Test fun rejectsOversizedInputBeforeAndAfterCleanup() {
        assertNull(MathScanNormalizer.normalize("1".repeat(257)))
        assertNull(MathScanNormalizer.normalize(" ".repeat(1025)))
        assertNull(MathScanNormalizer.normalize("x²".repeat(100)))
    }
}
