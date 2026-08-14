package com.calculator.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculations")
data class CalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val formulaId: String = "general",
    val formulaName: String = "Вычисление",
    val solvedVariable: String = "x",
    val resultValue: Double = 0.0,
    val resultUnit: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val serializedSteps: String = "",
    val expression: String = "", // Математический/Алгебраический/Физический запрос
    val result: String = "",     // Результат решения
    val subject: String = "MATH" // Предмет: MATH, PHYSICS, CHEMISTRY, GEOMETRY, CAMERA
)
