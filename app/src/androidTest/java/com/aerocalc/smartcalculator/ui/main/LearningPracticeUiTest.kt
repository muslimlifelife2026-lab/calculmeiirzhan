package com.aerocalc.smartcalculator.ui.main

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.aerocalc.smartcalculator.learning.LearningPractice
import com.aerocalc.smartcalculator.learning.LearningProgressRepository
import com.aerocalc.smartcalculator.learning.LearningProgress
import com.aerocalc.smartcalculator.learning.PracticeAttempt
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class LearningPracticeUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun repositoryRestoresActiveAndPausedAnswersAndReviewAfterRecreation() {
        rule.activity.getSharedPreferences("learning_progress", Context.MODE_PRIVATE).edit().clear().commit()
        val first = LearningPractice.questions.first()
        val second = LearningPractice.questions[1]
        val firstAnswer = PracticeAttempt(first.id).select(0, first).check(first)
        val progress = LearningProgress().updateAttempt(firstAnswer, first).start(second)
            .updateAttempt(PracticeAttempt(second.id).select(1, second), second)
        LearningProgressRepository(rule.activity).save(progress)
        val restored = LearningProgressRepository(rule.activity).load()
        assertEquals(progress, restored)
        assertEquals(firstAnswer, restored.start(first).activeAttempt)
        assertTrue(first.id in restored.reviewIds)
    }

    @Test fun wrongAnswerCannotCompleteAndCorrectCompletionPersistsWithoutRepeatingTheAdEvent() {
        rule.activity.getSharedPreferences("learning_progress", Context.MODE_PRIVATE).edit().clear().commit()
        var completions = 0
        val question = LearningPractice.dailyQuestion(LearningPractice.localEpochDay(Calendar.getInstance()))
        rule.setContent { LearningScreen({}, {}, onLearningActivityCompleted = { completions++ }) }
        rule.onNodeWithText("Решить задание").performClick()
        val list = rule.onNodeWithTag("learning_list")
        val wrong = question.answers[(question.correctAnswerIndex + 1) % question.answers.size]
        list.performScrollToNode(hasText(wrong))
        rule.onNodeWithText(wrong).performClick()
        list.performScrollToNode(hasText("Проверить ответ"))
        rule.onNodeWithText("Проверить ответ").performClick()
        rule.onNodeWithText("Завершить").assertDoesNotExist()
        assertEquals(0, completions)
        val correct = question.answers[question.correctAnswerIndex]
        list.performScrollToNode(hasText(correct))
        rule.onNodeWithText(correct).performClick()
        list.performScrollToNode(hasText("Проверить ответ"))
        rule.onNodeWithText("Проверить ответ").performClick()
        list.performScrollToNode(hasText("Завершить"))
        rule.onNodeWithText("Завершить").performClick()
        rule.runOnIdle {
            assertEquals(1, completions)
            assertTrue(question.id in LearningProgressRepository(rule.activity).load().completedIds)
        }
        list.performScrollToNode(hasText("Повторить задание"))
        rule.onNodeWithText("Повторить задание").performClick()
        list.performScrollToNode(hasText(correct))
        rule.onNodeWithText(correct).performClick()
        list.performScrollToNode(hasText("Проверить ответ"))
        rule.onNodeWithText("Проверить ответ").performClick()
        list.performScrollToNode(hasText("Завершить"))
        rule.onNodeWithText("Завершить").performClick()
        rule.runOnIdle { assertEquals(1, completions) }
    }
}
