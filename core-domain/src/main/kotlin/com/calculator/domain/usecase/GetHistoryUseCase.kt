package com.calculator.domain.usecase

import com.calculator.domain.model.CalculationHistory
import com.calculator.domain.repository.CalculationRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase(private val repository: CalculationRepository) {
    operator fun invoke(subjectFilter: String? = null): Flow<List<CalculationHistory>> {
        return if (subjectFilter.isNullOrBlank() || subjectFilter == "ALL") {
            repository.getAllHistory()
        } else {
            repository.getHistoryBySubject(subjectFilter)
        }
    }
}
