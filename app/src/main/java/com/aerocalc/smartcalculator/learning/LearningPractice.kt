package com.aerocalc.smartcalculator.learning

import java.util.Calendar

data class PracticeQuestion(
    val id: String,
    val topic: String,
    val prompt: String,
    val answers: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class PracticeAttempt(
    val questionId: String,
    val selectedAnswerIndex: Int? = null,
    val checked: Boolean = false
) {
    fun select(index: Int, question: PracticeQuestion): PracticeAttempt {
        require(question.id == questionId && index in question.answers.indices)
        return copy(selectedAnswerIndex = index, checked = false)
    }

    fun check(question: PracticeQuestion): PracticeAttempt {
        require(question.id == questionId)
        return if (selectedAnswerIndex?.let { it in question.answers.indices } == true) copy(checked = true) else this
    }

    fun canComplete(question: PracticeQuestion): Boolean =
        question.id == questionId && checked && selectedAnswerIndex == question.correctAnswerIndex
}

data class LearningProgress(
    val completedIds: Set<String> = emptySet(),
    val activeAttempt: PracticeAttempt? = null
) {
    fun complete(question: PracticeQuestion): LearningProgress =
        if (activeAttempt?.canComplete(question) == true) {
            copy(completedIds = completedIds + question.id, activeAttempt = null)
        } else this
}

object LearningPractice {
    val questions = listOf(
        PracticeQuestion("operations", "Математика", "Чему равно 2 + 3 × 4?", listOf("20", "14", "24"), 1,
            "Сначала умножение: 3 × 4 = 12. Затем сложение: 2 + 12 = 14. Для ответа 20 нужны скобки: (2 + 3) × 4."),
        PracticeQuestion("percent", "Математика", "Сколько составляет 15% от 200?", listOf("15", "30", "45"), 1,
            "15% = 15 / 100 = 0,15. Умножаем: 200 × 0,15 = 30."),
        PracticeQuestion("length", "Единицы измерения", "Переведите 1,5 км в метры.", listOf("150 м", "15 000 м", "1500 м"), 2,
            "1 км = 1000 м. Поэтому 1,5 × 1000 = 1500 м."),
        PracticeQuestion("force", "Физика", "Масса тела 2 кг, ускорение 3 м/с². Какова равнодействующая сила в инерциальной системе отсчёта?", listOf("6 Н", "1,5 Н", "5 Н"), 0,
            "Второй закон Ньютона: F = ma. При постоянной массе F = 2 × 3 = 6 Н. Это равнодействующая всех сил, а не обязательно одна отдельная сила."),
        PracticeQuestion("water", "Химия", "Какова молярная масса H₂O, если принять M(H) = 1 г/моль и M(O) = 16 г/моль?", listOf("17 г/моль", "18 г/моль", "32 г/моль"), 1,
            "В формульной единице H₂O два атома водорода и один кислорода: M(H₂O) = 2 × 1 + 16 = 18 г/моль. Здесь используются округлённые значения из условия."),
        PracticeQuestion("triangle", "Геометрия", "Основание треугольника 6 см, высота к этому основанию 4 см. Чему равна площадь?", listOf("24 см²", "10 см²", "12 см²"), 2,
            "Площадь треугольника S = bh / 2. Подставляем: 6 × 4 / 2 = 12 см². Высота должна быть перпендикулярна выбранному основанию."),
        PracticeQuestion("graph-root", "Графики", "В какой точке по оси x график y = 2x − 4 пересекает ось x?", listOf("x = 2", "x = −2", "x = 4"), 0,
            "На оси x значение y равно нулю. Решаем 2x − 4 = 0: 2x = 4, значит x = 2. Точка пересечения — (2; 0)."),
        PracticeQuestion("speed", "Единицы измерения", "Переведите скорость 36 км/ч в м/с.", listOf("36 м/с", "10 м/с", "100 м/с"), 1,
            "1 км = 1000 м, 1 ч = 3600 с. Поэтому 36 × 1000 / 3600 = 10 м/с. Чтобы перейти из км/ч в м/с, делите на 3,6.")
    )

    fun find(id: String?): PracticeQuestion? = questions.firstOrNull { it.id == id }

    /** Local calendar day independent of the user's UTC offset; works on Android API 24. */
    fun localEpochDay(calendar: Calendar): Long = Math.floorDiv(
        calendar.timeInMillis + calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET),
        86_400_000L
    )

    fun dailyQuestion(epochDay: Long): PracticeQuestion =
        questions[Math.floorMod(epochDay, questions.size.toLong()).toInt()]

    fun nextQuestion(progress: LearningProgress): PracticeQuestion? =
        find(progress.activeAttempt?.questionId)
            ?: questions.firstOrNull { it.id !in progress.completedIds }
}
