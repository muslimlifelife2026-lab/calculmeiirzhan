package com.aerocalc.smartcalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import com.calculator.feature.calculator.AlgebraScreen
import com.calculator.feature.physics.PhysicsViewModel
import com.calculator.feature.physics.sensors.SensorLabScreen
import com.calculator.feature.physics.sensors.SensorLabViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ScienceWorkspaceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun geometryConstructsMeasuredSegmentFromExactCoordinates() {
        rule.activity.getSharedPreferences("geometry_construction", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        rule.setContent { MainScreen() }
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithTag("tools_list").performScrollToNode(hasText("Геометрия"))
        rule.onNodeWithText("Геометрия").performClick()
        rule.onNodeWithText("Построения").performClick()
        rule.onNodeWithTag("construction_add").performScrollTo().performClick()
        rule.onNodeWithTag("construction_x").performScrollTo().performTextReplacement("3")
        rule.onNodeWithTag("construction_y").performTextReplacement("4")
        rule.onNodeWithTag("construction_add").performScrollTo().performClick()
        rule.onNodeWithText("B", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Построить отрезок").performScrollTo().performClick()
        rule.onNodeWithText("AB: длина 5.0").performScrollTo().assertIsDisplayed()
    }

    @Test fun moleculePresetProducesFormulaAndSurvivesLeavingWorkspace() {
        rule.setContent { MainScreen() }
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithTag("tools_list").performScrollToNode(hasText("Химия"))
        rule.onNodeWithText("Химия").performClick()
        rule.onNodeWithTag("chemistry_library").performScrollToNode(hasText("Структуры"))
        rule.onNodeWithText("Структуры").performClick()
        rule.onNodeWithText("Этанол").performClick()
        rule.onNodeWithTag("structure_formula").performScrollTo().assertTextEquals("C2H6O")
        rule.onNodeWithContentDescription("Все инструменты").performClick()
        rule.onNodeWithTag("tools_list").performScrollToNode(hasText("Химия"))
        rule.onNodeWithText("Химия").performClick()
        rule.onNodeWithTag("structure_formula").performScrollTo().assertTextEquals("C2H6O")
    }

    @Test fun algebraSolvesQuadraticAndRejectsVariableDivision() {
        rule.setContent { AlgebraScreen() }
        rule.onNodeWithTag("algebra_input").performTextReplacement("x^2 - 5x + 6 = 0")
        rule.onNodeWithText("Решить", substring = false).performClick()
        rule.onNodeWithTag("algebra_calculate").performScrollTo().performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTag("algebra_result").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("algebra_result").performScrollTo()
        rule.onNodeWithText("x1 = 2").assertExists()
        rule.onNodeWithText("x2 = 3").assertExists()
        rule.onNodeWithTag("algebra_input").performScrollTo().performTextReplacement("x/x")
        rule.onNodeWithText("Упростить").performClick()
        rule.onNodeWithTag("algebra_calculate").performScrollTo().performClick()
        rule.onNodeWithTag("algebra_result").assertDoesNotExist()
    }

    @Test fun restoredPhysicsInputsSurviveTheNextRestoration() {
        val saved = SavedStateHandle(mapOf("formulaId" to "force_newton", "inputs" to hashMapOf("m" to "2", "a" to "3"), "target" to "F"))
        lateinit var first: PhysicsViewModel
        lateinit var second: PhysicsViewModel
        rule.runOnIdle { first = PhysicsViewModel(rule.activity.application, saved) }
        rule.waitUntil(5000) { first.selectedFormula.value != null }
        rule.runOnIdle {
            assertEquals("2", saved.get<HashMap<String, String>>("inputs")?.get("m"))
            second = PhysicsViewModel(rule.activity.application, saved)
        }
        rule.waitUntil(5000) { second.selectedFormula.value != null }
        rule.runOnIdle {
            assertEquals("2", second.inputs.value["m"])
            assertEquals("3", second.inputs.value["a"])
            assertEquals("F", second.targetVariableSymbol.value)
        }
    }

    @Test fun sensorRecordingStopsWhenActivityLeavesForeground() {
        lateinit var model: SensorLabViewModel
        rule.runOnIdle { model = SensorLabViewModel(rule.activity.application) }
        rule.setContent { SensorLabScreen(model = model) }
        if (!model.accelerometerAvailable) {
            rule.onNodeWithText("Этот датчик недоступен на устройстве.").assertExists()
            return
        }
        rule.onNodeWithText("Начать новую запись").performScrollTo().performClick()
        rule.waitUntil(5000) { model.state.value.samples.isNotEmpty() }
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertFalse(model.state.value.running)
        assertTrue(model.state.value.samples.isNotEmpty())
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.runOnIdle { assertFalse(model.state.value.running) }
    }
}
