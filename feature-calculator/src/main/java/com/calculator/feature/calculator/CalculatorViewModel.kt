package com.calculator.feature.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.data.database.AppDatabase
import com.calculator.core.data.database.CalculationEntity
import com.calculator.engine.parser.MathParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.calculationDao()

    val recentHistory: StateFlow<List<CalculationEntity>> = dao.getAllCalculations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDegrees = MutableStateFlow(true)
    val isDegrees: StateFlow<Boolean> = _isDegrees.asStateFlow()

    private val _displayExpression = MutableStateFlow("")
    val displayExpression: StateFlow<String> = _displayExpression.asStateFlow()

    private val _calculatedResult = MutableStateFlow("")
    val calculatedResult: StateFlow<String> = _calculatedResult.asStateFlow()

    private val _calculationSteps = MutableStateFlow<List<MathParser.MathStep>>(emptyList())
    val calculationSteps: StateFlow<List<MathParser.MathStep>> = _calculationSteps.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    fun toggleAngleMode() {
        _isDegrees.value = !_isDegrees.value
        evaluateExpressionQuietly()
    }

    fun clearDisplay() {
        _displayExpression.value = ""
        _calculatedResult.value = ""
        _calculationSteps.value = emptyList()
    }

    fun pasteFromHistory(value: String) {
        if (value.isBlank()) return
        if (_displayExpression.value.isEmpty() || _displayExpression.value == "0") {
            _displayExpression.value = value
        } else {
            _displayExpression.value += value
        }
        evaluateExpressionQuietly()
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCalculation(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearHistory()
        }
    }

    fun onKeyPressed(key: String) {
        _errorMessage.value = ""
        when (key) {
            "C" -> {
                _displayExpression.value = ""
                _calculatedResult.value = ""
                _calculationSteps.value = emptyList()
            }
            "⌫" -> {
                val current = _displayExpression.value
                if (current.isNotEmpty()) {
                    _displayExpression.value = current.dropLast(1)
                }
                evaluateExpressionQuietly()
            }
            "=" -> {
                evaluateExpression()
            }
            else -> {
                _displayExpression.value += key
                evaluateExpressionQuietly()
            }
        }
    }

    private fun evaluateExpressionQuietly() {
        val expr = _displayExpression.value.trim()
        if (expr.isEmpty()) {
            _calculatedResult.value = ""
            _calculationSteps.value = emptyList()
            return
        }
        try {
            var parserExpr = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "pi")
                .replace("%", "/100")

            var openCount = 0
            var closeCount = 0
            for (char in parserExpr) {
                if (char == '(') openCount++
                else if (char == ')') closeCount++
            }
            if (openCount > closeCount) {
                parserExpr += ")".repeat(openCount - closeCount)
            }

            val tokens = MathParser.tokenize(parserExpr)
            val rpn = MathParser.shuntingYard(tokens)
            val (result, steps) = MathParser.evaluateRPNWithSteps(rpn, isDegrees = _isDegrees.value)
            
            _calculatedResult.value = result.setScale(10, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
            _calculationSteps.value = steps
        } catch (e: Exception) {
            _calculatedResult.value = ""
            _calculationSteps.value = emptyList()
        }
    }

    private fun evaluateExpression() {
        val expr = _displayExpression.value.trim()
        if (expr.isEmpty()) return
        try {
            var parserExpr = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "pi")
                .replace("%", "/100")

            var openCount = 0
            var closeCount = 0
            for (char in parserExpr) {
                if (char == '(') openCount++
                else if (char == ')') closeCount++
            }
            if (openCount > closeCount) {
                parserExpr += ")".repeat(openCount - closeCount)
            }

            val tokens = MathParser.tokenize(parserExpr)
            val rpn = MathParser.shuntingYard(tokens)
            val (result, steps) = MathParser.evaluateRPNWithSteps(rpn, isDegrees = _isDegrees.value)
            
            val formattedResult = result.setScale(10, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
                
            val stepsStr = steps.joinToString(";") { "${it.description}|${it.expression}" }

            // Update UI State
            _displayExpression.value = formattedResult
            _calculatedResult.value = ""
            _calculationSteps.value = emptyList()

            // Save to Room DB with full expression, result and subject
            viewModelScope.launch(Dispatchers.IO) {
                dao.insertCalculation(
                    CalculationEntity(
                        formulaId = "calc",
                        formulaName = "Математика",
                        solvedVariable = "x",
                        resultValue = result.toDouble(),
                        resultUnit = "",
                        serializedSteps = stepsStr,
                        expression = expr,
                        result = formattedResult,
                        subject = "MATH"
                    )
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = e.localizedMessage ?: "Ошибка вычисления"
            _calculatedResult.value = ""
            _calculationSteps.value = emptyList()
        }
    }
}
