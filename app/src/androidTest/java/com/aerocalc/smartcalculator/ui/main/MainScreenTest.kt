package com.aerocalc.smartcalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
        rule.onNodeWithText("Порядок действий").performClick()
        rule.onNodeWithText("Сначала умножьте", substring = true).assertExists()
        rule.onNodeWithText("Тренажёр по физике").performScrollTo().performClick()
        rule.onAllNodesWithText("Тренажёр").onFirst().assertExists()
        rule.onNodeWithContentDescription("Вернуться к обучению").performClick()
        rule.onNodeWithText("Порядок действий").assertExists()
    }
}
