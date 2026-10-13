package com.calculator.domain.utils

import com.calculator.domain.model.Variable
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FormulaInputValidatorTest {
    private fun variable(name: String) = Variable(symbol = "x", name = name, defaultUnitSymbol = "m")

    @Test fun rejectsMissingAndMalformedValues() {
        assertNotNull(FormulaInputValidator.check(variable("Радиус"), "").error)
        assertNotNull(FormulaInputValidator.check(variable("Радиус"), "abc").error)
    }

    @Test fun acceptsDecimalCommaAndSignedAcceleration() {
        assertEquals(0, FormulaInputValidator.check(variable("Радиус"), "1,5").value!!.compareTo(BigDecimal("1.5")))
        assertEquals(0, FormulaInputValidator.check(variable("Ускорение"), "-2").value!!.compareTo(BigDecimal("-2")))
    }

    @Test fun rejectsNegativeDimensions() {
        assertNotNull(FormulaInputValidator.check(variable("Радиус"), "-1").error)
    }
}
