package com.calculator.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the physical dimensions of a unit using 7 base SI dimensions:
 * 1. Length (L)
 * 2. Mass (M)
 * 3. Time (T)
 * 4. Electric Current (I)
 * 5. Thermodynamic Temperature (Theta)
 * 6. Amount of Substance (N)
 * 7. Luminous Intensity (J)
 *
 * Example: Speed (L * T^-1) is [1, 0, -1, 0, 0, 0, 0]
 * Example: Force (M * L * T^-2) is [1, 1, -2, 0, 0, 0, 0]
 */
@Serializable
data class DimensionVector(
    val length: Int = 0,
    val mass: Int = 0,
    val time: Int = 0,
    val current: Int = 0,
    val temperature: Int = 0,
    val amount: Int = 0,
    val luminous: Int = 0
) {
    companion object {
        val DIMENSIONLESS = DimensionVector()
        val LENGTH = DimensionVector(length = 1)
        val MASS = DimensionVector(mass = 1)
        val TIME = DimensionVector(time = 1)
        val CURRENT = DimensionVector(current = 1)
        val TEMPERATURE = DimensionVector(temperature = 1)
        val AMOUNT = DimensionVector(amount = 1)
        val LUMINOUS = DimensionVector(luminous = 1)
        
        val VELOCITY = DimensionVector(length = 1, time = -1)
        val ACCELERATION = DimensionVector(length = 1, time = -2)
        val FORCE = DimensionVector(length = 1, mass = 1, time = -2)
        val ENERGY = DimensionVector(length = 2, mass = 1, time = -2)
        val POWER = DimensionVector(length = 2, mass = 1, time = -3)
        val PRESSURE = DimensionVector(length = -1, mass = 1, time = -2)
    }

    operator fun plus(other: DimensionVector) = DimensionVector(
        length + other.length,
        mass + other.mass,
        time + other.time,
        current + other.current,
        temperature + other.temperature,
        amount + other.amount,
        luminous + other.luminous
    )

    operator fun minus(other: DimensionVector) = DimensionVector(
        length - other.length,
        mass - other.mass,
        time - other.time,
        current - other.current,
        temperature - other.temperature,
        amount - other.amount,
        luminous - other.luminous
    )

    operator fun times(scalar: Int) = DimensionVector(
        length * scalar,
        mass * scalar,
        time * scalar,
        current * scalar,
        temperature * scalar,
        amount * scalar,
        luminous * scalar
    )
}

@Serializable
data class MeasurementUnit(
    val symbol: String,
    val name: String,
    val scaleFactor: Double, // Factor to multiply by to convert to SI base
    val offset: Double = 0.0  // Offset to add to convert to SI base (e.g. for Celsius to Kelvin)
)

@Serializable
data class Variable(
    val symbol: String,
    val name: String,
    val description: String = "",
    val dimensionVector: DimensionVector = DimensionVector.DIMENSIONLESS,
    val defaultUnitSymbol: String,
    val supportedUnits: List<MeasurementUnit> = emptyList()
)

@Serializable
data class Formula(
    val id: String,
    val name: String,
    val category: String,
    val subcategory: String = "",
    val description: String = "",
    val canonicalEquation: String, // E.g., "v_f - v_i - a * t = 0"
    val latexTemplate: String,     // E.g., "v_f = v_i + a \\cdot t"
    val variables: List<Variable>,
    val solvedEquations: Map<String, String> = emptyMap() // Map of variable symbol to its isolated equation
)

@Serializable
data class SolutionStep(
    val order: Int,
    val description: String,
    val equationLatex: String,
    val substitutionDetails: String? = null
)

@Serializable
data class CalculationResult(
    val success: Boolean,
    val solvedSymbol: String? = null,
    val value: Double = 0.0,
    val unitSymbol: String = "",
    val steps: List<SolutionStep> = emptyList(),
    val errorMessage: String? = null
)
