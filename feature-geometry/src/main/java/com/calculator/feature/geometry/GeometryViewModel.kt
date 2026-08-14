package com.calculator.feature.geometry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.domain.model.Formula
import com.calculator.domain.model.Variable
import com.calculator.domain.model.MeasurementUnit
import com.calculator.domain.model.CalculationResult
import com.calculator.domain.utils.FormulaLoader
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

class GeometryViewModel(application: Application) : AndroidViewModel(application) {

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

    init {
        loadFormulas()
    }

    private fun loadFormulas() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                FormulaLoader.formulas.filter { it.category == "GEOMETRY" }
            }
            _formulas.value = list
            if (list.isNotEmpty()) {
                selectFormula(list.first())
            }
        }
    }

    fun selectFormula(formula: Formula) {
        _selectedFormula.value = formula
        _inputs.value = emptyMap()
        _inputUnits.value = formula.variables.associate { it.symbol to (it.supportedUnits.firstOrNull { u -> u.symbol == it.defaultUnitSymbol } ?: MeasurementUnit(it.defaultUnitSymbol, it.defaultUnitSymbol, 1.0)) }
        _targetVariableSymbol.value = formula.variables.firstOrNull()?.symbol ?: ""
        _calculationResult.value = null
    }

    fun onInputValueChange(symbol: String, value: String) {
        val current = _inputs.value.toMutableMap()
        current[symbol] = value
        _inputs.value = current
    }

    fun onUnitChange(symbol: String, unit: MeasurementUnit) {
        val current = _inputUnits.value.toMutableMap()
        current[symbol] = unit
        _inputUnits.value = current
    }

    fun setTargetVariable(symbol: String) {
        _targetVariableSymbol.value = symbol
        _calculationResult.value = null
    }

    fun solveFormula() {
        val formula = _selectedFormula.value ?: return
        val target = _targetVariableSymbol.value
        if (target.isEmpty()) return

        viewModelScope.launch {
            val parsedInputs = mutableMapOf<String, BigDecimal>()
            for (variable in formula.variables) {
                if (variable.symbol == target) continue
                val rawVal = _inputs.value[variable.symbol] ?: ""
                val bigDecimalVal = rawVal.toBigDecimalOrNull() ?: BigDecimal.ZERO
                parsedInputs[variable.symbol] = bigDecimalVal
            }

            val result = withContext(Dispatchers.Default) {
                FormulaSolver.solve(
                    formula = formula,
                    inputs = parsedInputs,
                    inputUnits = _inputUnits.value,
                    targetSymbol = target
                )
            }
            
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
                            serializedSteps = Json.encodeToString(result.steps)
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
