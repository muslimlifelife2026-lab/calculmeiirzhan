package com.calculator.domain.utils

import com.calculator.domain.model.MeasurementUnit
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object UnitConverter {

    fun convertToSi(value: BigDecimal, unit: MeasurementUnit, mc: MathContext = MathContext.DECIMAL128): BigDecimal {
        val offsetBigDecimal = BigDecimal.valueOf(unit.offset)
        val scaleBigDecimal = BigDecimal.valueOf(unit.scaleFactor)
        
        // SI = (V + Offset) * Scale
        return value.add(offsetBigDecimal, mc).multiply(scaleBigDecimal, mc)
    }

    fun convertFromSi(siValue: BigDecimal, targetUnit: MeasurementUnit, mc: MathContext = MathContext.DECIMAL128): BigDecimal {
        val offsetBigDecimal = BigDecimal.valueOf(targetUnit.offset)
        val scaleBigDecimal = BigDecimal.valueOf(targetUnit.scaleFactor)
        
        if (scaleBigDecimal.compareTo(BigDecimal.ZERO) == 0) {
            throw ArithmeticException("Division by zero in scale factor for unit ${targetUnit.symbol}")
        }
        
        // Target = (SI / Scale) - Offset
        return siValue.divide(scaleBigDecimal, mc).subtract(offsetBigDecimal, mc)
    }
}
