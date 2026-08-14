package com.calculator.domain.model

data class Element(
    val symbol: String,
    val name: String,
    val atomicNumber: Int,
    val atomicMass: Double,
    val nameRu: String = name,
    val category: String = "Неметалл",
    val group: Int = 1,
    val period: Int = 1,
    val electronConfig: String = "",
    val electronegativity: Double = 0.0,
    val oxidationStates: String = ""
)
