package com.aerocalc.smartcalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.calculator.engine.solver.GraphEvaluator
import com.calculator.feature.calculator.AlgebraScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlgebraLearningWorkflowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun graphValue(index: Int, x: Double): Double {
        val expression = rule.onNodeWithTag("graph_expression_$index").fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text
        return GraphEvaluator(expression).evaluate(x)
    }

    @Test fun systemSolutionOpensItsLinesAndFreshImportsReplaceOnlyRequestedGraphDrafts() {
        rule.setContent { MainScreen() }
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithTag("tools_list").performScrollToNode(hasText("Алгебра"))
        rule.onNodeWithText("Алгебра").performClick()
        rule.onNodeWithTag("algebra_system_mode").performClick()
        rule.onNodeWithTag("algebra_calculate").performScrollTo().performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("linear_system_result").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("linear_system_result").performScrollTo()
        rule.onNodeWithText("x = 2").assertExists()
        rule.onNodeWithText("y = 1").assertExists()
        rule.onNodeWithTag("system_show_graph").performScrollTo().performClick()
        assertEquals(1.0, graphValue(0, 2.0), 1e-12)
        assertEquals(1.0, graphValue(1, 2.0), 1e-12)
        rule.onNodeWithTag("graph_expression_0").performScrollTo().performTextReplacement("x^2")
        rule.onNodeWithText("Расчёт").performClick()
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithTag("tools_list").performScrollToNode(hasText("Графики"))
        rule.onNodeWithText("Графики").performClick()
        rule.onNodeWithTag("graph_expression_0").assertTextContains("x^2")
        rule.onNodeWithContentDescription("Все инструменты").performClick()
        rule.onNodeWithTag("tools_list").performScrollToNode(hasText("Алгебра"))
        rule.onNodeWithText("Алгебра").performClick()
        rule.onNodeWithTag("system_first_equation").performScrollTo().performTextReplacement("x+y=4")
        rule.onNodeWithTag("system_second_equation").performScrollTo().performTextReplacement("x-y=0")
        rule.onNodeWithTag("algebra_calculate").performScrollTo().performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("system_show_graph").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("system_show_graph").performScrollTo().performClick()
        assertEquals(2.0, graphValue(0, 2.0), 1e-12)
        assertEquals(2.0, graphValue(1, 2.0), 1e-12)
    }

    @Test fun scannedSystemKeepsBothSidesAndManualDraftUntilAnotherImportIsRequested() {
        var source by mutableStateOf("2x+y=5\nx-y=1")
        var request by mutableLongStateOf(1L)
        val restoration = StateRestorationTester(rule)
        restoration.setContent { AlgebraScreen(initialExpression = source, initialRequest = request) }
        rule.onNodeWithTag("system_first_equation").assertTextContains("2x+y=5")
        rule.onNodeWithTag("system_second_equation").assertTextContains("x-y=1")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("linear_system_result").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("x = 2").assertExists()
        rule.onNodeWithText("y = 1").assertExists()
        rule.onNodeWithTag("system_first_equation").performTextReplacement("x+y=9")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("system_first_equation").assertTextContains("x+y=9")
        rule.runOnIdle { source = "x+y=4\nx-y=0"; request++ }
        rule.onNodeWithTag("system_first_equation").assertTextContains("x+y=4")
        rule.onNodeWithTag("system_second_equation").assertTextContains("x-y=0")
    }
}
