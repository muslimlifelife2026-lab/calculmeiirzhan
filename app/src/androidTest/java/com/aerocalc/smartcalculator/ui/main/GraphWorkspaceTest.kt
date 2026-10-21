package com.aerocalc.smartcalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.calculator.feature.graphing.GraphingScreen
import org.junit.Rule
import org.junit.Test

class GraphWorkspaceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun changingModesAndRestorationKeepSeparateFunctionDrafts() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { GraphingScreen() }
        rule.onNodeWithTag("graph_expression_0").performScrollTo().performTextReplacement("x^2+7")
        rule.onNodeWithText("y = f(x)").performScrollTo().performClick()
        rule.onNodeWithTag("graph_expression_0").assertTextContains("x^2+7")
        rule.onNodeWithText("r = f(θ)").performScrollTo().performClick()
        rule.onNodeWithTag("graph_expression_0").performScrollTo().performTextReplacement("2+cos(theta)")
        rule.onNodeWithText("y = f(x)").performScrollTo().performClick()
        rule.onNodeWithTag("graph_expression_0").assertTextContains("x^2+7")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("graph_expression_0").performScrollTo().assertTextContains("x^2+7")
        rule.onNodeWithText("r = f(θ)").performScrollTo().performClick()
        rule.onNodeWithTag("graph_expression_0").assertTextContains("2+cos(theta)")
    }
}
