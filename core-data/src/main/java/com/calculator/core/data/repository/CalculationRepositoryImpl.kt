package com.calculator.core.data.repository

import com.calculator.core.data.database.CalculationDao
import com.calculator.core.data.database.CalculationEntity
import com.calculator.domain.model.CalculationHistory
import com.calculator.domain.repository.CalculationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CalculationRepositoryImpl(
    private val dao: CalculationDao
) : CalculationRepository {

    override fun getAllHistory(): Flow<List<CalculationHistory>> {
        return dao.getAllCalculations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getHistoryBySubject(subject: String): Flow<List<CalculationHistory>> {
        return dao.getCalculationsBySubject(subject).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveCalculation(history: CalculationHistory) {
        withContext(Dispatchers.IO) {
            dao.insertCalculation(history.toEntity())
        }
    }

    override suspend fun deleteCalculation(id: Int) {
        withContext(Dispatchers.IO) {
            dao.deleteCalculation(id)
        }
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            dao.clearHistory()
        }
    }

    private fun CalculationEntity.toDomain(): CalculationHistory {
        return CalculationHistory(
            id = id,
            expression = if (expression.isNotBlank()) expression else formulaName,
            result = if (result.isNotBlank()) result else "$resultValue $resultUnit".trim(),
            subject = subject,
            timestamp = timestamp
        )
    }

    private fun CalculationHistory.toEntity(): CalculationEntity {
        return CalculationEntity(
            id = id,
            expression = expression,
            result = result,
            subject = subject,
            timestamp = timestamp
        )
    }
}
