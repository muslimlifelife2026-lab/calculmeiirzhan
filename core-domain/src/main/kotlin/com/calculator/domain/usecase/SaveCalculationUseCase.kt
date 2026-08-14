package com.calculator.domain.usecase

import com.calculator.domain.model.CalculationHistory
import com.calculator.domain.repository.CalculationRepository

class SaveCalculationUseCase(private val repository: CalculationRepository) {
    suspend operator fun invoke(expression: String, result: String, subject: String = "MATH") {
        if (expression.isBlank() || result.isBlank()) return
        val history = CalculationHistory(
            expression = expression,
            result = result,
            subject = subject,
            timestamp = System.currentTimeMillis()
        )
        repository.saveCalculation(history)
    }
}
