package com.calculator.domain.model

data class CalculationHistory(
    val id: Int = 0,
    val expression: String,
    val result: String,
    val subject: String, // MATH, ALGEBRA, GEOMETRY, PHYSICS, CHEMISTRY, CAMERA
    val timestamp: Long = System.currentTimeMillis()
)
