package com.aerocalc.smartcalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class MainScreenTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun setup() { rule.setContent { MainScreen() } }

    @Test fun toolsExposeConverterAndBackNavigation() {
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithText("Конвертер").performClick()
        rule.onNodeWithText("Температура").assertExists()
        rule.onNodeWithContentDescription("Все инструменты").performClick()
        rule.onNodeWithText("Физика").assertExists()
        rule.onNodeWithText("Калькулятор").performClick()
        rule.onNodeWithText("AeroCalc").assertExists()
    }

    @Test fun completedCalculationKeepsExplanationAcrossTabs() {
        rule.onNodeWithContentDescription("Очистить").performClick()
        rule.onNodeWithText("8").performClick()
        rule.onNodeWithText("+").performClick()
        rule.onNodeWithText("7").performClick()
        rule.onNodeWithContentDescription("Равно").performClick()
        rule.onNodeWithText("15").assertExists()
        rule.onNodeWithText("Как получен ответ").assertExists()
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithText("Калькулятор").performClick()
        rule.onNodeWithText("15").assertExists()
        rule.onNodeWithText("Как получен ответ").performClick()
        rule.onNodeWithText("Решение по шагам").assertExists()
        rule.onNodeWithText("Сохранить PDF").assertExists()
    }

    @Test fun tinyNonzeroResultRemainsVisible() {
        rule.onNodeWithContentDescription("Очистить").performClick()
        rule.onNodeWithText("1").performClick()
        rule.onNodeWithText("÷").performClick()
        rule.onNodeWithText("1").performClick()
        repeat(11) { rule.onNodeWithText("0").performClick() }
        rule.onNodeWithContentDescription("Равно").performClick()
        rule.onNodeWithText("1E-11").assertExists()
    }

    @Test fun scientificFunctionsAreDiscoverableButInitiallyCollapsed() {
        rule.onNodeWithText("sin").assertDoesNotExist()
        rule.onNodeWithText("Научный").performClick()
        rule.onNodeWithText("sin").assertExists()
        rule.onNodeWithContentDescription("Квадратный корень").assertExists()
        rule.onNodeWithText("Научный").performClick()
        rule.onNodeWithText("sin").assertDoesNotExist()
    }

    @Test fun converterShowsResultAndBottomKeyboardRow() {
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithText("Конвертер").performClick()
        rule.onNodeWithText("0.001").assertIsDisplayed()
        rule.onNodeWithContentDescription("Равно").assertIsDisplayed()
    }

    @Test fun everyToolOpensAndReturnsToCatalog() {
        rule.onNodeWithText("Инструменты").performClick()
        for (title in listOf("Конвертер", "Физика", "Геометрия", "Графики", "Химия")) {
            rule.onNodeWithText(title).performClick()
            rule.onNodeWithContentDescription("Все инструменты").assertExists().performClick()
            rule.onNodeWithText(title).assertExists()
        }
    }

    @Test fun learningLessonsAndPhysicsPracticeAreReachable() {
        rule.onNodeWithText("Обучение").performClick()
        rule.onNodeWithTag("learning_list").performScrollToNode(hasText("Порядок действий"))
        rule.onNodeWithText("Порядок действий").performClick()
        rule.onNodeWithText("Сначала умножьте", substring = true).assertExists()
        rule.onNodeWithTag("learning_list").performScrollToNode(hasText("Тренажёр по физике"))
        rule.onNodeWithText("Тренажёр по физике").performClick()
        rule.onAllNodesWithText("Тренажёр").onFirst().assertExists()
        rule.onNodeWithContentDescription("Вернуться к обучению").performClick()
        rule.onNodeWithTag("learning_list").performScrollToNode(hasText("Порядок действий"))
        rule.onNodeWithText("Порядок действий").assertExists()
    }

    @Test fun graphParametersRemainEditableAfterReturningFromAnotherTab() {
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithText("Графики").performClick()
        rule.onNodeWithText("Библиотека").performClick()
        rule.onNodeWithText("Параметры a и b").performClick()
        rule.onNodeWithTag("graph_param_a").performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
        rule.onNodeWithTag("graph_value_a").assertTextContains("2", substring = true)
        rule.onNodeWithText("Калькулятор").performClick()
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithText("Графики").performClick()
        rule.onNodeWithTag("graph_value_a").assertTextContains("2", substring = true)
        rule.onNodeWithTag("graph_param_b").assertExists()
    }

    @Test fun newtonExperimentIsReachableAndUsesTheSelectedForce() {
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithText("Физика").performClick()
        rule.onNodeWithText("Сила и движение").performScrollTo().performClick()
        rule.onNodeWithText("a = F / m").assertExists()
        rule.onNodeWithContentDescription("Равнодействующая сила в ньютонах").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        rule.onNodeWithText("a = ${String.format(Locale.getDefault(), "%.2f", 0.0)} Н /", substring = true).assertExists()
    }

    @Test fun absoluteTemperatureRejectsBelowZeroInsteadOfShowingAFalseNumber() {
        rule.onNodeWithText("Инструменты").performClick()
        rule.onNodeWithText("Конвертер").performClick()
        rule.onNodeWithText("Температура").performClick()
        rule.onNodeWithContentDescription("Очистить").performClick()
        rule.onNodeWithText("±").performClick()
        for (key in listOf("2", "7", "3", ".", "1", "6")) rule.onNodeWithText(key).performClick()
        rule.onNodeWithText("Температура ниже абсолютного нуля").assertExists()
        rule.onNodeWithText("—").assertExists()
    }
}
