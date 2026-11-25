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

    private val _completedExpression = MutableStateFlow<String?>(savedState["completedExpression"])
    val completedExpression: StateFlow<String?> = _completedExpression.asStateFlow()

    private val _completedResult = MutableStateFlow<String?>(savedState["completedResult"])
    val completedResult: StateFlow<String?> = _completedResult.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    private var previewJob: Job? = null
    private var evalJob: Job? = null

    private fun setExpression(value: String) {
        _displayExpression.value = value
        savedState["expression"] = value
    }

    private fun setCompleted(expr: String?, result: String?) {
        _completedExpression.value = expr
        _completedResult.value = result
        savedState["completedExpression"] = expr
        savedState["completedResult"] = result
    }

    init {
        val restoredCompleted = _completedExpression.value
        if (!restoredCompleted.isNullOrBlank()) {
            restoreCompletedSteps(restoredCompleted)
        } else if (_displayExpression.value.isNotBlank()) {
            evaluateExpressionQuietly()
        }
    }

    fun toggleAngleMode() {
        evalJob?.cancel()
        _errorMessage.value = ""
        setCompleted(null, null)
        _isDegrees.value = !_isDegrees.value
        savedState["degrees"] = _isDegrees.value
        evaluateExpressionQuietly()
    }

    fun clearDisplay() {
        previewJob?.cancel()
        evalJob?.cancel()
        _errorMessage.value = ""
        setCompleted(null, null)
        setExpression("")
        _calculatedResult.value = ""
        _calculationSteps.value = emptyList()
    }

    fun pasteFromHistory(value: String) {
        if (value.isBlank()) return
        evalJob?.cancel()
        _errorMessage.value = ""
        setCompleted(null, null)
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
        if (key != "=") {
            evalJob?.cancel()
            if (_completedResult.value != null) {
                if (key.firstOrNull()?.isDigit() == true || key in listOf(".", "(", "π", "e") || key.endsWith("(")) {
                    setExpression("")
                }
                setCompleted(null, null)
            }
        }
        when (key) {
            "C" -> clearDisplay()
            "⌫" -> {
                val current = _displayExpression.value
                if (current.isNotEmpty()) {
                    setExpression(current.dropLast(1))
                }
                evaluateExpressionQuietly()
            }
            "=" -> evaluateExpression()
            else -> {
                setExpression(_displayExpression.value + key)
                evaluateExpressionQuietly()
            }
        }
    }

    private fun normalizeExpression(expr: String): String {
        var parserExpr = expr
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace('–', '-')
            .replace(',', '.')
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
        return parserExpr
    }

    private fun restoreCompletedSteps(completedExpr: String) {
        previewJob?.cancel()
        if (completedExpr.length > 256) return
        val degrees = _isDegrees.value
        previewJob = viewModelScope.launch {
            try {
                val parserExpr = normalizeExpression(completedExpr)
                val (_, steps) = withContext(Dispatchers.Default) {
                    val tokens = MathParser.tokenize(parserExpr)
                    require(tokens.size <= 256) { "Слишком длинное выражение" }
                    val rpn = MathParser.shuntingYard(tokens)
                    MathParser.evaluateRPNWithSteps(rpn, isDegrees = degrees)
                }
                if (_completedExpression.value == completedExpr) {
                    _calculationSteps.value = steps
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep saved display result even if step restoration fails.
            }
        }
    }

    private fun evaluateExpressionQuietly() {
        previewJob?.cancel()
        val expr = _displayExpression.value.trim()
        if (expr.isEmpty() || expr.length > 256) {
            _calculatedResult.value = ""
            _calculationSteps.value = emptyList()
            return
        }
        _calculatedResult.value = ""
        previewJob = viewModelScope.launch {
            delay(80)
            try {
                val parserExpr = normalizeExpression(expr)
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
            } catch (_: Exception) {
                _calculatedResult.value = ""
                _calculationSteps.value = emptyList()
            }
        }
    }

    private fun evaluateExpression() {
        previewJob?.cancel()
        evalJob?.cancel()
        val expr = _displayExpression.value.trim()
        if (expr.isEmpty()) return
        if (expr.length > 256) {
            _errorMessage.value = "Слишком длинное выражение (максимум 256 символов)"
            return
        }
        val degrees = _isDegrees.value
        evalJob = viewModelScope.launch {
            try {
                val parserExpr = normalizeExpression(expr)
                val (result, steps, formattedResult) = withContext(Dispatchers.Default) {
                    val tokens = MathParser.tokenize(parserExpr)
                    require(tokens.size <= 256) { "Слишком длинное выражение" }
                    val rpn = MathParser.shuntingYard(tokens)
                    val (evaluated, stepList) = MathParser.evaluateRPNWithSteps(rpn, isDegrees = degrees)
                    Triple(evaluated, stepList, NumberFormatter.formatExact(evaluated))
                }
                if (_displayExpression.value.trim() != expr) return@launch

                val stepsStr = steps.joinToString(";") { "${it.description}|${it.expression}" }
                setExpression(formattedResult)
                setCompleted(expr, formattedResult)
                _calculatedResult.value = ""
                _calculationSteps.value = steps

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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Ошибка вычисления"
                _calculatedResult.value = ""
                _calculationSteps.value = emptyList()
            }
        }
    }
}
