package com.aerocalc.smartcalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.calculator.core.ui.theme.AeroCalcTheme
import com.calculator.core.ui.theme.SurfaceElevated
import com.aerocalc.smartcalculator.ui.onboarding.AdAgeGroup
import com.aerocalc.smartcalculator.ui.onboarding.AgeGroupScreen
import com.calculator.core.ui.components.MathKeyboard
import com.calculator.feature.calculator.CalculatorDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompactAccessibilityTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun typingPreviewDoesNotResizeOrMoveTheEnteredNumber() {
        var preview by androidx.compose.runtime.mutableStateOf("")
        rule.setContent {
            AeroCalcTheme {
                CalculatorDisplay("7", preview, "", true, false, {}, {}, {}, {})
            }
        }
        fun expressionSize(): androidx.compose.ui.unit.TextUnit {
            var value = 0.sp
            rule.onNodeWithTag("calculator_expression")
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val results = mutableListOf<TextLayoutResult>()
                    assertTrue(action(results))
                    value = results.single().layoutInput.style.fontSize
                }
            return value
        }
        val originalSize = expressionSize()
        val originalTop = rule.onNodeWithTag("calculator_expression").fetchSemanticsNode().boundsInRoot.top
        val copy = rule.onNodeWithContentDescription("Скопировать выражение")
        copy.assertIsDisplayed()
        val copyBounds = copy.fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { preview = "7" }
        assertEquals(originalSize, expressionSize())
        copy.assertIsDisplayed()
        assertEquals(copyBounds, copy.fetchSemanticsNode().boundsInRoot)
        assertEquals(originalTop, rule.onNodeWithTag("calculator_expression").fetchSemanticsNode().boundsInRoot.top, 0.5f)
        rule.runOnIdle { preview = "" }
        assertEquals(originalSize, expressionSize())
        copy.assertIsDisplayed()
        assertEquals(copyBounds, copy.fetchSemanticsNode().boundsInRoot)
    }

    @Test fun selectedControlsAndContainerRolesHaveReadableContrast() {
        var selected by androidx.compose.runtime.mutableStateOf(true)
        var chipBackground = Color.Unspecified
        var tonalBackground = Color.Unspecified
        rule.setContent {
            AeroCalcTheme {
                val scheme = MaterialTheme.colorScheme
                chipBackground = if (selected) scheme.secondaryContainer else SurfaceElevated
                tonalBackground = scheme.secondaryContainer
                listOf(
                    scheme.onPrimaryContainer to scheme.primaryContainer,
                    scheme.onSecondaryContainer to scheme.secondaryContainer,
                    scheme.onTertiaryContainer to scheme.tertiaryContainer
                ).forEach { (foreground, background) ->
                    assertReadableContrast(foreground, background)
                }
                Column {
                    FilterChip(selected = selected, onClick = { selected = !selected },
                        label = { Text("v · Скорость", Modifier.testTag("choice_text")) })
                    FilledTonalButton(onClick = {}) { Text("Найти", Modifier.testTag("tonal_text")) }
                }
            }
        }
        fun checkText(tag: String, background: Color) {
            rule.onNodeWithTag(tag, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val results = mutableListOf<TextLayoutResult>()
                    assertTrue(action(results))
                    assertReadableContrast(results.single().layoutInput.style.color, background)
                }
        }
        checkText("choice_text", chipBackground)
        checkText("tonal_text", tonalBackground)
        rule.onNodeWithText("v · Скорость").performClick()
        rule.waitForIdle()
        checkText("choice_text", chipBackground)
    }

    private fun assertReadableContrast(foreground: Color, background: Color) {
        val a = foreground.luminance()
        val b = background.luminance()
        val contrast = (maxOf(a, b) + .05) / (minOf(a, b) + .05)
        assertTrue("Control contrast was $contrast", contrast >= 4.5)
    }

    @Test fun plainTextOnTransparentDarkThemeHasReadableContrast() {
        rule.setContent { AeroCalcTheme { Text("Заголовок", modifier = Modifier.testTag("theme_text")) } }
        rule.onNodeWithTag("theme_text").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val results = mutableListOf<TextLayoutResult>()
            assertTrue(action(results))
            val text = results.single().layoutInput.style.color.luminance()
            val background = SurfaceElevated.luminance()
            val contrast = (maxOf(text, background) + .05) / (minOf(text, background) + .05)
            assertTrue("Text contrast was $contrast", contrast >= 4.5)
        }
    }

    @Test fun oldestAgeChoiceRemainsReachableWithLargeTextOnShortScreen() {
        var selected: AdAgeGroup? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.7f)) {
                Box(Modifier.size(320.dp, 280.dp)) { AgeGroupScreen { selected = it } }
            }
        }
        rule.onNodeWithText("18 лет и старше").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(AdAgeGroup.ADULT, selected) }
    }

    @Test fun scientificKeyboardHasReachableKeysWithLargeTextOnShortScreen() {
        var lastKey: String? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.7f)) {
                Box(Modifier.size(320.dp, 280.dp)) {
                    MathKeyboard(onKeyPressed = { lastKey = it }, showVariables = true)
                }
            }
        }
        rule.onNodeWithText("Научный").performClick()
        rule.onNodeWithContentDescription("Квадратный корень").performScrollTo()
            .assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals("sqrt(", lastKey) }
        rule.onNodeWithContentDescription("Равно").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals("=", lastKey) }
    }
}
