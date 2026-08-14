package com.calculator.domain.usecase

import com.calculator.domain.repository.CalculationRepository

class ClearHistoryUseCase(private val repository: CalculationRepository) {
    suspend operator fun invoke() {
        repository.clearHistory()
    }
}
