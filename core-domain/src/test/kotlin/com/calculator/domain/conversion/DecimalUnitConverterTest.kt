package com.calculator.domain.conversion

import java.math.BigDecimal
import kotlin.test.*

class DecimalUnitConverterTest {
    private fun convert(value: String, category: ConversionCategory, from: String, to: String, delta: Boolean = false): BigDecimal {
        val units = DecimalUnitConverter.units.getValue(category)
        return DecimalUnitConverter.convert(BigDecimal(value), units.first { it.symbol == from },
            units.first { it.symbol == to }, category, delta)
    }
    private fun exact(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test fun exactDefinedFactorsAndLargeNumbersPreserveDecimalDigits() {
        exact("3.785411784", convert("1", ConversionCategory.VOLUME, "гал", "л"))
        exact("0.028349523125", convert("1", ConversionCategory.WEIGHT, "унц", "кг"))
        exact("4046.8564224", convert("1", ConversionCategory.AREA, "акр", "м²"))
        exact("1852", convert("3600", ConversionCategory.SPEED, "узел", "м/с"))
        exact("9007199254740993", convert("900719925474099300", ConversionCategory.LENGTH, "см", "м"))
    }
    @Test fun temperatureAndItsDifferenceHaveDifferentOffsets() {
        exact("273.15", convert("0", ConversionCategory.TEMPERATURE, "°C", "K"))
        exact("0", convert("-459.67", ConversionCategory.TEMPERATURE, "°F", "K"))
        exact("212", convert("100", ConversionCategory.TEMPERATURE, "°C", "°F"))
        exact("18", convert("10", ConversionCategory.TEMPERATURE, "°C", "°F", true))
        exact("-18", convert("-10", ConversionCategory.TEMPERATURE, "°C", "°F", true))
        assertFailsWith<IllegalArgumentException> { convert("-273.15001", ConversionCategory.TEMPERATURE, "°C", "K") }
        assertFailsWith<IllegalArgumentException> { convert("-1", ConversionCategory.TEMPERATURE, "K", "K") }
    }
    @Test fun newScientificCategoriesUseIndependentReferences() {
        exact("101325", convert("1", ConversionCategory.PRESSURE, "атм", "Па"))
        exact("3600000", convert("1", ConversionCategory.ENERGY, "кВт·ч", "Дж"))
        exact("86400", convert("1", ConversionCategory.TIME, "сут", "с"))
        exact("1000", convert("1", ConversionCategory.FORCE, "кН", "Н"))
    }
    @Test fun unitsFromDifferentCategoriesCannotBeMixed() {
        assertFailsWith<IllegalArgumentException> {
            DecimalUnitConverter.convert(BigDecimal.ONE, DecimalUnitConverter.units.getValue(ConversionCategory.LENGTH)[0],
                DecimalUnitConverter.units.getValue(ConversionCategory.WEIGHT)[0], ConversionCategory.LENGTH)
        }
    }
}
