package com.calculator.domain.model

data class Element(
    val symbol: String,
    val name: String,
    val atomicNumber: Int,
    val atomicMass: Double // Standard atomic weight in g/mol
)
