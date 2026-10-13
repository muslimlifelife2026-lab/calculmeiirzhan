package com.calculator.feature.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.calculator.core.data.database.AppDatabase
import com.calculator.core.data.database.CalculationEntity
import com.calculator.engine.parser.MathParser
import com.calculator.core.ui.utils.NumberFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalculatorViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.calculationDao()

    val recentHistory: StateFlow<List<CalculationEntity>> = dao.getAllCalculations()
        .map { entries -> entries.filter { it.subject == "MATH" && it.result.toBigDecimalOrNull() != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDegrees = MutableStateFlow(savedState["degrees"] ?: true)
    val isDegrees: StateFlow<Boolean> = _isDegrees.asStateFlow()

    private val _displayExpression = MutableStateFlow(savedState["expression"] ?: "")
    val displayExpression: StateFlow<String> = _displayExpression.asStateFlow()

    private val _calculatedResult = MutableStateFlow("")
    val calculatedResult: StateFlow<String> = _calculatedResult.asStateFlow()

    private val _calculationSteps = MutableStateFlow<List<MathParser.MathStep>>(emptyList())
    val calculationSteps: StateFlow<List<MathParser.MathStep>> = _calculationSteps.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    private var previewJob: Job? = null

    private fun setExpression(value: String) {
        _displayExpression.value = value
        savedState["expression"] = value
    }

    init { if (_displayExpression.value.isNotBlank()) evaluateExpressionQuietly() }

    fun toggleAngleMode() {
        _isDegrees.value = !_isDegrees.value
        savedState["degrees"] = _isDegrees.value
        evaluateExpressionQuietly()
    }

    fun clearDisplay() {
        previewJob?.cancel()
        setExpression("")
        _calculatedResult.value = ""
        _calculationSteps.value = emptyList()
    }

    fun pasteFromHistory(value: String) {
        if (value.isBlank()) return
        setExpression(value)
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
                previewJob?.cancel()
                setExpression("")
                _calculatedResult.value = ""
                _calculationSteps.value = emptyList()
            }
            "⌫" -> {
                val current = _displayExpression.value
                if (current.isNotEmpty()) {
                    setExpression(current.dropLast(1))
                }
                evaluateExpressionQuietly()
            }
            "=" -> {
                evaluateExpression()
            }
            else -> {
                setExpression(_displayExpression.value + key)
                evaluateExpressionQuietly()
            }
        }
    }

    private fun evaluateExpressionQuietly() {
        previewJob?.cancel()
        val expr = _displayExpression.value.trim()
        if (expr.isEmpty()) {
            _calculatedResult.value = ""
            _calculationSteps.value = emptyList()
            return
        }
        if (expr.length > 256) {
            _calculatedResult.value = ""
            _calculationSteps.value = emptyList()
            return
        }
        _calculatedResult.value = ""
        previewJob = viewModelScope.launch {
          delay(80)
          try {
            var parserExpr = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "pi")

            var openCount = 0
            var closeCount = 0
            for (char in parserExpr) {
                if (char == '(') openCount++
                else if (char == ')') closeCount++
            }
            if (openCount > closeCount) {
                parserExpr += ")".repeat(openCount - closeCount)
            }

            val degrees = _isDegrees.value
            val (result, steps) = withContext(Dispatchers.Default) {
                val tokens = MathParser.tokenize(parserExpr)
                require(tokens.size <= 256) { "Слишком длинное выражение" }
                val rpn = MathParser.shuntingYard(tokens)
                MathParser.evaluateRPNWithSteps(rpn, isDegrees = degrees)
            }
            if (_displayExpression.value.trim() != expr) return@launch
            
            _calculatedResult.value = NumberFormatter.formatExact(result)
            _calculationSteps.value = steps
          } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
          } catch (e: Exception) {
            _calculatedResult.value = ""
            _calculationSteps.value = emptyList()
          }
        }
    }

    private fun evaluateExpression() {
        previewJob?.cancel()
        val expr = _displayExpression.value.trim()
        if (expr.isEmpty()) return
        if (expr.length > 256) {
            _errorMessage.value = "Слишком длинное выражение (максимум 256 символов)"
            return
        }
        try {
            var parserExpr = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "pi")

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
            require(tokens.size <= 256) { "Слишком длинное выражение" }
            val rpn = MathParser.shuntingYard(tokens)
            val (result, steps) = MathParser.evaluateRPNWithSteps(rpn, isDegrees = _isDegrees.value)
            
            val formattedResult = NumberFormatter.formatExact(result)
                
            val stepsStr = steps.joinToString(";") { "${it.description}|${it.expression}" }

            // Update UI State
            setExpression(formattedResult)
            _calculatedResult.value = ""
            _calculationSteps.value = steps

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
