package com.calculator.domain.repository

import com.calculator.domain.model.CalculationHistory
import kotlinx.coroutines.flow.Flow

interface CalculationRepository {
    fun getAllHistory(): Flow<List<CalculationHistory>>
    fun getHistoryBySubject(subject: String): Flow<List<CalculationHistory>>
    suspend fun saveCalculation(history: CalculationHistory)
    suspend fun deleteCalculation(id: Int)
    suspend fun clearHistory()
}
