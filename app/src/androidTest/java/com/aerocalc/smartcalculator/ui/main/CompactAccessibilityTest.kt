package com.aerocalc.smartcalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.calculator.core.ui.theme.AeroCalcTheme
import com.calculator.core.ui.theme.SurfaceElevated
import com.aerocalc.smartcalculator.ui.onboarding.AdAgeGroup
import com.aerocalc.smartcalculator.ui.onboarding.AgeGroupScreen
import com.calculator.core.ui.components.MathKeyboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompactAccessibilityTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

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
