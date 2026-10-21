package com.calculator.feature.physics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.core.ui.theme.Background
import com.calculator.feature.physics.simulators.PendulumSimulator
import com.calculator.feature.physics.simulators.ProjectileSimulator
import com.calculator.feature.physics.simulators.NewtonSimulator
import com.calculator.feature.physics.sensors.SensorLabScreen

@Composable
fun PhysicsScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = -1,
    viewModel: PhysicsViewModel = viewModel()
) {
    val formulas by viewModel.formulas.collectAsState()
    val selectedFormula by viewModel.selectedFormula.collectAsState()
    val inputs by viewModel.inputs.collectAsState()
    val inputUnits by viewModel.inputUnits.collectAsState()
    val targetSymbol by viewModel.targetVariableSymbol.collectAsState()
    val calculationResult by viewModel.calculationResult.collectAsState()

    var selectedPhysicsTab by rememberSaveable { mutableIntStateOf(initialTab) } // Keep practice at index 3 for learning navigation.
    val listState = rememberLazyListState()
    BackHandler(selectedPhysicsTab >= 0 && initialTab == -1) { selectedPhysicsTab = -1 }

    LaunchedEffect(calculationResult, selectedPhysicsTab) {
        if (selectedPhysicsTab == 0 && calculationResult?.success == true) {
            kotlinx.coroutines.delay(100)
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()

            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (selectedPhysicsTab >= 0 && initialTab == -1) {
            TextButton(onClick = { selectedPhysicsTab = -1 }) { Text("‹  Разделы физики") }
        }

        when (selectedPhysicsTab) {
            -1 -> PhysicsLibrary { selectedPhysicsTab = it }
            1 -> {
                PendulumSimulator()
            }
            2 -> {
                ProjectileSimulator()
            }
            3 -> {
                PhysicsQuizSection(formulas = formulas)
            }
            4 -> NewtonSimulator()
            5 -> SensorLabScreen()
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

private data class PhysicsDestination(val index: Int, val title: String, val detail: String, val mark: String)

@Composable
private fun PhysicsLibrary(onSelect: (Int) -> Unit) {
    val sections = listOf(
        PhysicsDestination(0, "Формулы", "Найдите неизвестную величину с объяснением каждого шага", "ƒ"),
        PhysicsDestination(3, "Тренажёр", "Решайте задачи и проверяйте свои знания", "?"),
        PhysicsDestination(4, "Сила и движение", "Посмотрите, как масса и сила меняют движение", "F"),
        PhysicsDestination(1, "Маятник", "Исследуйте колебания, длину нити и затухание", "T"),
        PhysicsDestination(2, "Баллистика", "Изменяйте скорость и угол, наблюдайте траекторию", "↗"),
        PhysicsDestination(5, "Датчики", "Запишите ускорение телефона и сохраните CSV", "xyz")
    )
    LazyColumn(Modifier.fillMaxSize().testTag("physics_library"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Понять движение", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text("От формулы — к эксперименту. Выберите, что хотите исследовать.", color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(sections, key = { it.index }) { section ->
            GlassCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button) { onSelect(section.index) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(section.mark, style = MaterialTheme.typography.headlineSmall, color = TextSecondary,
                        modifier = Modifier.width(44.dp).padding(top = 3.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleLarge)
                        Text(section.detail, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
