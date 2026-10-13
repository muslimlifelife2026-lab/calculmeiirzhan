package com.calculator.feature.converter

import com.calculator.domain.conversion.DecimalUnitConverter
import java.math.BigDecimal

typealias ConversionCategory = com.calculator.domain.conversion.ConversionCategory
typealias UnitItem = com.calculator.domain.conversion.UnitItem

object UnitConverter {
    fun getUnitsForCategory(category: ConversionCategory): List<UnitItem> = DecimalUnitConverter.units.getValue(category)
    fun convert(value: BigDecimal, from: UnitItem, to: UnitItem, category: ConversionCategory,
                temperatureDifference: Boolean = false): BigDecimal =
        DecimalUnitConverter.convert(value, from, to, category, temperatureDifference)
}
