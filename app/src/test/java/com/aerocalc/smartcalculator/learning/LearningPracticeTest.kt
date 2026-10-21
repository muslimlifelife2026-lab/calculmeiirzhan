package com.aerocalc.smartcalculator.learning

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class LearningPracticeTest {
    @Test fun `daily schedule is deterministic and covers the curriculum`() {
        val start = 20_739L
        val ids = (0 until LearningPractice.questions.size).map {
            val date = start + it
            val first = LearningPractice.dailyQuestion(date)
            assertEquals(first, LearningPractice.dailyQuestion(date))
            first.id
        }
        assertEquals(LearningPractice.questions.map { it.id }.toSet(), ids.toSet())
        assertEquals(LearningPractice.dailyQuestion(start), LearningPractice.dailyQuestion(start + LearningPractice.questions.size))
        assertNotNull(LearningPractice.dailyQuestion(-25_567L))
    }

    @Test fun `local dates do not change with timezone or daylight saving offsets`() {
        fun day(zone: String, year: Int, month: Int, date: Int): Long {
            val calendar = GregorianCalendar(TimeZone.getTimeZone(zone)).apply {
                clear()
                set(year, month, date, 0, 15, 0)
            }
            return LearningPractice.localEpochDay(calendar)
        }
        assertEquals(0L, day("GMT+05:00", 1970, Calendar.JANUARY, 1))
        assertEquals(0L, day("GMT-08:00", 1970, Calendar.JANUARY, 1))
        assertEquals(day("UTC", 2026, Calendar.MARCH, 29), day("Europe/Berlin", 2026, Calendar.MARCH, 29))
        assertEquals(day("Europe/Berlin", 2026, Calendar.MARCH, 29) + 1,
            day("Europe/Berlin", 2026, Calendar.MARCH, 30))
    }

    @Test fun `a wrong or unchecked answer cannot complete a task`() {
        val question = LearningPractice.questions.first()
        val wrong = (question.correctAnswerIndex + 1) % question.answers.size
        assertFalse(PracticeAttempt(question.id).check(question).checked)
        val unchecked = PracticeAttempt(question.id).select(question.correctAnswerIndex, question)
        assertEquals(emptySet<String>(), LearningProgress(activeAttempt = unchecked).complete(question).completedIds)
        val incorrect = PracticeAttempt(question.id).select(wrong, question).check(question)
        assertEquals(emptySet<String>(), LearningProgress(activeAttempt = incorrect).complete(question).completedIds)
        val correct = unchecked.check(question)
        val completed = LearningProgress(activeAttempt = correct).complete(question)
        assertEquals(setOf(question.id), completed.completedIds)
        assertNull(completed.activeAttempt)
    }

    @Test fun `changing an answer clears verification and completion is idempotent`() {
        val question = LearningPractice.questions.first()
        val checked = PracticeAttempt(question.id).select(question.correctAnswerIndex, question).check(question)
        assertFalse(checked.select(0, question).checked)
        val progress = LearningProgress(activeAttempt = checked).complete(question)
        assertEquals(progress, progress.complete(question))
        assertEquals(1, progress.copy(activeAttempt = checked).complete(question).completedIds.size)
    }

    @Test fun `resume keeps the current task and completed tasks are skipped`() {
        val first = LearningPractice.questions.first()
        val second = LearningPractice.questions[1]
        assertEquals(second, LearningPractice.nextQuestion(LearningProgress(setOf(first.id))))
        assertEquals(first, LearningPractice.nextQuestion(LearningProgress(setOf(first.id), PracticeAttempt(first.id))))
        assertEquals(first, LearningPractice.nextQuestion(LearningProgress(LearningPractice.questions.map { it.id }.toSet())))
    }

    @Test fun `switching tasks preserves checked draft and reopening restores it`() {
        val first = LearningPractice.questions.first()
        val second = LearningPractice.questions[1]
        val answer = PracticeAttempt(first.id).select(0, first).check(first)
        val progress = LearningProgress(activeAttempt = answer).start(second).start(first)
        assertEquals(answer, progress.activeAttempt)
        assertEquals(PracticeAttempt(second.id), progress.drafts[second.id])
    }

    @Test fun `mistakes remain until successful later repetition and cannot duplicate completion`() {
        val question = LearningPractice.questions.first()
        val wrong = PracticeAttempt(question.id).select(0, question).check(question)
        var progress = LearningProgress().updateAttempt(wrong, question)
        assertEquals(setOf(question.id), progress.reviewIds)
        val correct = wrong.select(question.correctAnswerIndex, question).check(question)
        progress = progress.updateAttempt(correct, question).complete(question)
        assertEquals(setOf(question.id), progress.completedIds)
        assertEquals(setOf(question.id), progress.reviewIds)
        progress = progress.start(question).updateAttempt(correct, question).complete(question)
        assertTrue(progress.reviewIds.isEmpty())
        assertEquals(1, progress.completedIds.size)
        assertFalse(question.id in progress.drafts)
    }

    @Test fun `completed curriculum offers sequential repetition within the selected topic`() {
        val allCompleted = LearningPractice.questions.map { it.id }.toSet()
        val topic = "Графики"
        val questions = LearningPractice.questions.filter { it.topic == topic }
        questions.forEachIndexed { index, question ->
            val progress = LearningProgress(completedIds = allCompleted, lastCompletedId = question.id)
            assertEquals(questions[(index + 1) % questions.size], LearningPractice.nextQuestion(progress, topic))
        }
        assertNull(LearningPractice.nextQuestion(LearningProgress(), "Несуществующая тема"))
    }

    @Test fun `checked curriculum values follow their formulas`() {
        val q = LearningPractice.questions.associateBy { it.id }
        fun answer(id: String) = q.getValue(id).let { it.answers[it.correctAnswerIndex] }
        assertEquals((2 + 3 * 4).toString(), answer("operations"))
        assertEquals((200 * 15 / 100).toString(), answer("percent"))
        assertEquals("${2 * 3} Н", answer("force"))
        assertEquals("${2 * 1 + 16} г/моль", answer("water"))
        assertEquals("${6 * 4 / 2} см²", answer("triangle"))
        assertEquals("x = ${4 / 2}", answer("graph-root"))
        assertEquals("${36 * 1000 / 3600} м/с", answer("speed"))
        assertEquals(((-3) * (-3)).toString(), answer("negative-square"))
        assertEquals("${10 * 3} Дж", answer("work"))
        assertEquals("${12 / 4} А", answer("ohm"))
        assertEquals("${2 * 3 * 3 / 2} Дж", answer("kinetic"))
        assertEquals("${12 + 2 * 16} г/моль", answer("carbon-dioxide"))
        assertEquals("${36 / 18} моль", answer("amount"))
        assertEquals("${kotlin.math.sqrt(3.0 * 3 + 4.0 * 4).toInt()} см", answer("pythagoras"))
        assertEquals("${3 * 3}π см²", answer("circle-area"))
        assertEquals("${180 - 50 - 60}°", answer("triangle-angles"))
        assertEquals(((-2) * (-2)).toString(), answer("graph-value"))
        assertEquals(6, LearningPractice.topics.size)
        assertTrue(LearningPractice.topics.all { topic -> LearningPractice.questions.count { it.topic == topic } == 4 })
        assertEquals(LearningPractice.questions.size, q.size)
        assertTrue(q.values.all { it.correctAnswerIndex in it.answers.indices && it.answers.distinct().size == it.answers.size })
    }
}
