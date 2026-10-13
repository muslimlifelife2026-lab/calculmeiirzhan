package com.calculator.feature.physics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.core.ui.components.GlassTabs
import com.calculator.core.ui.theme.Background
import com.calculator.feature.physics.simulators.PendulumSimulator
import com.calculator.feature.physics.simulators.ProjectileSimulator

@Composable
fun PhysicsScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    viewModel: PhysicsViewModel = viewModel()
) {
    val formulas by viewModel.formulas.collectAsState()
    val selectedFormula by viewModel.selectedFormula.collectAsState()
    val inputs by viewModel.inputs.collectAsState()
    val inputUnits by viewModel.inputUnits.collectAsState()
    val targetSymbol by viewModel.targetVariableSymbol.collectAsState()
    val calculationResult by viewModel.calculationResult.collectAsState()

    var selectedPhysicsTab by rememberSaveable { mutableIntStateOf(initialTab) } // 0: Formulas, 1: Pendulum, 2: Projectile, 3: Quiz
    val listState = rememberLazyListState()

    LaunchedEffect(calculationResult) {
        if (calculationResult?.success == true) {
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        GlassTabs(
            tabs = listOf("Формулы", "Маятник", "Баллистика", "Тренажёр"),
            selectedIndex = selectedPhysicsTab,
            onSelect = { selectedPhysicsTab = it },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (selectedPhysicsTab) {
            1 -> {
                PendulumSimulator()
            }
            2 -> {
                ProjectileSimulator()
            }
            3 -> {
                PhysicsQuizSection(formulas = formulas)
            }
            else -> {
                PhysicsFormulasSection(
                    formulas = formulas,
                    selectedFormula = selectedFormula,
                    inputs = inputs,
                    inputUnits = inputUnits,
                    targetSymbol = targetSymbol,
                    calculationResult = calculationResult,
                    onSelectFormula = { viewModel.selectFormula(it) },
                    onSetTargetVariable = { viewModel.setTargetVariable(it) },
                    onInputValueChange = { symbol, value -> viewModel.onInputValueChange(symbol, value) },
                    onUnitChange = { symbol, unit -> viewModel.onUnitChange(symbol, unit) },
                    onSolveFormula = { viewModel.solveFormula() },
                    onPresetApply = { formula, presetInputs, presetTarget ->
                        viewModel.selectFormula(formula)
                        viewModel.applyPreset(presetInputs, presetTarget)
                    },
                    listState = listState
                )
            }
        }
    }
}
