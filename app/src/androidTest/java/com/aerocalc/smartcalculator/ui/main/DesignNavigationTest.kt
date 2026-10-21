package com.aerocalc.smartcalculator.ui.main

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Rule
import org.junit.Test

class DesignNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun save(name: String) {
        rule.waitForIdle()
        // Compose semantics settle before SurfaceFlinger presents the new screen.
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(600)
        val folder = File(rule.activity.getExternalFilesDir(null), "design-previews").apply { mkdirs() }
        val bitmap = requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun redesignedCatalogsKeepEachScienceWorkspaceReachable() {
        rule.setContent { MainScreen() }
        rule.onNodeWithContentDescription("Равно").assertIsDisplayed()
        save("calculator")
        rule.onNodeWithText("Инструменты").performClick()
        save("tools")
        for (title in listOf("Физика", "Химия", "Геометрия")) {
            rule.onNodeWithTag("tools_list").performScrollToNode(hasText(title))
            rule.onNodeWithText(title).performClick()
            rule.onNodeWithContentDescription("Все инструменты").assertIsDisplayed()
            save(when (title) { "Физика" -> "physics"; "Химия" -> "chemistry"; else -> "geometry" })
            rule.onNodeWithContentDescription("Все инструменты").performClick()
        }
        rule.onNodeWithText("Обучение").performClick()
        rule.onNodeWithText("Задание дня").assertExists()
        save("learning")
    }
}
