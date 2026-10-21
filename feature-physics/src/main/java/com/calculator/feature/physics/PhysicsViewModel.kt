package com.calculator.feature.physics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.calculator.domain.model.Formula
import com.calculator.domain.model.Variable
import com.calculator.domain.model.MeasurementUnit
import com.calculator.domain.model.CalculationResult
import com.calculator.domain.utils.FormulaLoader
import com.calculator.domain.utils.FormulaInputValidator
import com.calculator.core.data.database.AppDatabase
import com.calculator.core.data.database.CalculationEntity
import com.calculator.engine.solver.FormulaSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

class PhysicsViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.calculationDao()

    private val _formulas = MutableStateFlow<List<Formula>>(emptyList())
    val formulas: StateFlow<List<Formula>> = _formulas.asStateFlow()

    private val _selectedFormula = MutableStateFlow<Formula?>(null)
    val selectedFormula: StateFlow<Formula?> = _selectedFormula.asStateFlow()

    private val _inputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val inputs: StateFlow<Map<String, String>> = _inputs.asStateFlow()

    private val _inputUnits = MutableStateFlow<Map<String, MeasurementUnit>>(emptyMap())
    val inputUnits: StateFlow<Map<String, MeasurementUnit>> = _inputUnits.asStateFlow()

    private val _targetVariableSymbol = MutableStateFlow<String>("")
    val targetVariableSymbol: StateFlow<String> = _targetVariableSymbol.asStateFlow()

    private val _calculationResult = MutableStateFlow<CalculationResult?>(null)
    val calculationResult: StateFlow<CalculationResult?> = _calculationResult.asStateFlow()
    private var solveVersion = 0L

    private fun invalidateResult() {
        solveVersion++
        _calculationResult.value = null
    }

    init {
        loadFormulas()
    }

    private fun loadFormulas() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                FormulaLoader.formulas.filter { it.category == "PHYSICS" }
            }
            _formulas.value = list
            if (list.isNotEmpty()) {
                val savedInputs = savedState.get<HashMap<String, String>>("inputs")
                val savedUnits = savedState.get<HashMap<String, String>>("units")
                val savedTarget = savedState.get<String>("target")
                selectFormula(list.firstOrNull { it.id == savedState.get<String>("formulaId") } ?: list.first())
                _inputs.value = savedInputs ?: emptyMap()
                _inputUnits.value = _selectedFormula.value!!.variables.associate { variable ->
                    variable.symbol to (variable.supportedUnits.firstOrNull { it.symbol == savedUnits?.get(variable.symbol) }
                        ?: _inputUnits.value.getValue(variable.symbol))
                }
                if (savedTarget != null && _selectedFormula.value!!.variables.any { it.symbol == savedTarget }) {
                    _targetVariableSymbol.value = savedTarget
                }
                // selectFormula initializes these keys. Persist restored values for the next recreation too.
                savedState["inputs"] = HashMap(_inputs.value)
                savedState["units"] = HashMap(_inputUnits.value.mapValues { it.value.symbol })
                savedState["target"] = _targetVariableSymbol.value
            }
        }
    }

    fun selectFormula(formula: Formula) {
        _selectedFormula.value = formula
        savedState["formulaId"] = formula.id
        _inputs.value = emptyMap()
        savedState["inputs"] = hashMapOf<String, String>()
        _inputUnits.value = formula.variables.associate { it.symbol to (it.supportedUnits.firstOrNull { u -> u.symbol == it.defaultUnitSymbol } ?: MeasurementUnit(it.defaultUnitSymbol, it.defaultUnitSymbol, 1.0)) }
        savedState["units"] = HashMap(_inputUnits.value.mapValues { it.value.symbol })
        _targetVariableSymbol.value = formula.variables.firstOrNull()?.symbol ?: ""
        savedState["target"] = _targetVariableSymbol.value
        invalidateResult()
    }

    fun onInputValueChange(symbol: String, value: String) {
        val current = _inputs.value.toMutableMap()
        current[symbol] = value
        _inputs.value = current
        savedState["inputs"] = HashMap(current)
        invalidateResult()
    }

    fun onUnitChange(symbol: String, unit: MeasurementUnit) {
        val current = _inputUnits.value.toMutableMap()
        current[symbol] = unit
        _inputUnits.value = current
        savedState["units"] = HashMap(current.mapValues { it.value.symbol })
        invalidateResult()
    }

    fun setTargetVariable(symbol: String) {
        _targetVariableSymbol.value = symbol
        savedState["target"] = symbol
        invalidateResult()
    }

    fun applyPreset(presetInputs: Map<String, String>, target: String) {
        _inputs.value = presetInputs
        savedState["inputs"] = HashMap(presetInputs)
        _targetVariableSymbol.value = target
        savedState["target"] = target
        solveFormula()
    }

    fun solveFormula() {
        val formula = _selectedFormula.value ?: return
        val target = _targetVariableSymbol.value
        if (target.isEmpty()) return
        val inputSnapshot = _inputs.value
        val unitSnapshot = _inputUnits.value
        val requestVersion = ++solveVersion
        _calculationResult.value = null
        val parsedInputs = mutableMapOf<String, BigDecimal>()
        for (variable in formula.variables) {
            if (variable.symbol == target) continue
            val checked = FormulaInputValidator.check(variable, inputSnapshot[variable.symbol].orEmpty())
            if (checked.error != null) {
                _calculationResult.value = CalculationResult(false, errorMessage = checked.error)
                return
            }
            parsedInputs[variable.symbol] = checked.value!!
        }

        viewModelScope.launch {

            val result = withContext(Dispatchers.Default) {
                FormulaSolver.solve(
                    formula = formula,
                    inputs = parsedInputs,
                    inputUnits = unitSnapshot,
                    targetSymbol = target
                )
            }
            
            if (requestVersion != solveVersion) return@launch
            _calculationResult.value = result

            if (result.success) {
                withContext(Dispatchers.IO) {
                    try {
                        val entity = CalculationEntity(
                            formulaId = formula.id,
                            formulaName = formula.name,
                            solvedVariable = result.solvedSymbol ?: "",
                            resultValue = result.value,
                            resultUnit = result.unitSymbol,
                            serializedSteps = Json.encodeToString(result.steps),
                            expression = "${formula.name}: " + inputSnapshot.entries.joinToString(", ") { (symbol, value) ->
                                "$symbol=$value ${unitSnapshot[symbol]?.symbol.orEmpty()}".trim()
                            } + " → $target",
                            result = result.exactValue.ifBlank { result.value.toString() },
                            subject = "PHYSICS"
                        )
                        dao.insertCalculation(entity)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
