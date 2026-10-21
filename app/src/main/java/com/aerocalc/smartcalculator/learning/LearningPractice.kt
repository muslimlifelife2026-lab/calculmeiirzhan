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
    val activeAttempt: PracticeAttempt? = null,
    val reviewIds: Set<String> = emptySet(),
    val drafts: Map<String, PracticeAttempt> = emptyMap(),
    val lastCompletedId: String? = null
) {
    /** Changing topic must not discard an answer that the learner has already entered. */
    fun start(question: PracticeQuestion): LearningProgress {
        if (activeAttempt?.questionId == question.id) return this
        val saved = activeAttempt?.let { drafts + (it.questionId to it) } ?: drafts
        return copy(activeAttempt = saved[question.id] ?: PracticeAttempt(question.id), drafts = saved)
    }

    fun updateAttempt(attempt: PracticeAttempt, question: PracticeQuestion): LearningProgress {
        require(attempt.questionId == question.id)
        val needsReview = attempt.checked && !attempt.canComplete(question)
        return copy(activeAttempt = attempt, reviewIds = if (needsReview) reviewIds + question.id else reviewIds)
    }

    fun complete(question: PracticeQuestion): LearningProgress =
        if (activeAttempt?.canComplete(question) == true) {
            copy(
                completedIds = completedIds + question.id,
                activeAttempt = null,
                // A corrected first attempt stays in review until a later successful repetition.
                reviewIds = if (question.id in completedIds) reviewIds - question.id else reviewIds,
                drafts = drafts - question.id,
                lastCompletedId = question.id
            )
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
            "1 км = 1000 м, 1 ч = 3600 с. Поэтому 36 × 1000 / 3600 = 10 м/с. Чтобы перейти из км/ч в м/с, делите на 3,6."),
        PracticeQuestion("fraction", "Математика", "Чему равно 1/2 + 1/3?", listOf("2/5", "5/6", "1/6"), 1,
            "Приведём к общему знаменателю 6: 1/2 = 3/6, 1/3 = 2/6. Сумма равна 5/6."),
        PracticeQuestion("negative-square", "Математика", "Чему равно (−3)²?", listOf("−9", "6", "9"), 2,
            "Скобки включают минус в основание степени: (−3) × (−3) = 9. Выражение −3² без скобок обычно означает −(3²) = −9."),
        PracticeQuestion("work", "Физика", "Постоянная сила 10 Н направлена вдоль перемещения 3 м. Какова её работа?", listOf("30 Дж", "13 Дж", "3,3 Дж"), 0,
            "Работа A = Fs cos α. Здесь α = 0°, cos α = 1, поэтому A = 10 × 3 = 30 Дж."),
        PracticeQuestion("ohm", "Физика", "На резисторе сопротивлением 4 Ом напряжение 12 В. Какова сила постоянного тока?", listOf("48 А", "3 А", "8 А"), 1,
            "Для омического резистора I = U/R = 12/4 = 3 А. Сопротивление считаем постоянным."),
        PracticeQuestion("kinetic", "Физика", "Тело массой 2 кг движется со скоростью 3 м/с. Какова его кинетическая энергия в классической механике?", listOf("6 Дж", "18 Дж", "9 Дж"), 2,
            "E = mv²/2 = 2 × 3² / 2 = 9 Дж. Скорость берётся относительно выбранной системы отсчёта."),
        PracticeQuestion("carbon-dioxide", "Химия", "Какова молярная масса CO₂ при M(C) = 12 г/моль и M(O) = 16 г/моль?", listOf("28 г/моль", "44 г/моль", "32 г/моль"), 1,
            "Один атом углерода и два кислорода: 12 + 2 × 16 = 44 г/моль. Использованы округлённые значения условия."),
        PracticeQuestion("amount", "Химия", "Масса воды 36 г, молярная масса 18 г/моль. Каково количество вещества?", listOf("2 моль", "0,5 моль", "648 моль"), 0,
            "Количество вещества n = m/M = 36/18 = 2 моль. Масса и молярная масса должны использовать согласованные единицы."),
        PracticeQuestion("balance", "Химия", "Какие наименьшие целые коэффициенты уравнивают H₂ + O₂ → H₂O?", listOf("1, 1, 1", "1, 2, 1", "2, 1, 2"), 2,
            "2H₂ + O₂ → 2H₂O. С обеих сторон четыре атома H и два атома O. При уравнивании меняют коэффициенты, а не индексы в формулах."),
        PracticeQuestion("pythagoras", "Геометрия", "Катеты прямоугольного треугольника равны 3 см и 4 см. Найдите гипотенузу.", listOf("7 см", "5 см", "12 см"), 1,
            "По теореме Пифагора c² = 3² + 4² = 25. Длина положительна, поэтому c = 5 см."),
        PracticeQuestion("circle-area", "Геометрия", "Радиус круга 3 см. Чему равна его площадь?", listOf("6π см²", "3π см²", "9π см²"), 2,
            "Площадь круга S = πr² = π × 3² = 9π см². Величина 2πr = 6π см — длина окружности."),
        PracticeQuestion("triangle-angles", "Геометрия", "В евклидовой плоскости два угла треугольника равны 50° и 60°. Найдите третий.", listOf("70°", "110°", "90°"), 0,
            "Сумма внутренних углов плоского треугольника равна 180°. Третий угол: 180° − 50° − 60° = 70°."),
        PracticeQuestion("line-slope", "Графики", "Для y = −3x + 2 увеличили x на 1. Как изменится y?", listOf("Увеличится на 2", "Уменьшится на 3", "Увеличится на 3"), 1,
            "Коэффициент при x равен −3. Изменение Δy = −3 × Δx = −3, поэтому y уменьшится на 3."),
        PracticeQuestion("parabola", "Графики", "В какой точке находится вершина параболы y = (x − 2)² + 1?", listOf("(−2; 1)", "(2; −1)", "(2; 1)"), 2,
            "Квадрат неотрицателен и равен нулю при x = 2. Минимальное значение y равно 1, вершина — (2; 1)."),
        PracticeQuestion("graph-value", "Графики", "Чему равно y = x² при x = −2?", listOf("4", "−4", "2"), 0,
            "Подставляем значение целиком: y = (−2)² = (−2) × (−2) = 4. Точка (−2; 4) лежит на графике."),
        PracticeQuestion("area-units", "Единицы измерения", "Сколько квадратных сантиметров в 1 м²?", listOf("100 см²", "10 000 см²", "1000 см²"), 1,
            "1 м = 100 см. Для площади коэффициент возводится в квадрат: 1 м² = 100 × 100 = 10 000 см²."),
        PracticeQuestion("temperature-delta", "Единицы измерения", "Температура повысилась на 10 °C. На сколько кельвинов она повысилась?", listOf("283,15 К", "−263,15 К", "10 К"), 2,
            "Размер одного градуса Цельсия равен одному кельвину. Для разности температур ΔT = 10 К. Смещение 273,15 применяется к самой температуре, а не к её изменению.")
    )

    fun find(id: String?): PracticeQuestion? = questions.firstOrNull { it.id == id }

    /** Local calendar day independent of the user's UTC offset; works on Android API 24. */
    fun localEpochDay(calendar: Calendar): Long = Math.floorDiv(
        calendar.timeInMillis + calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET),
        86_400_000L
    )

    fun dailyQuestion(epochDay: Long): PracticeQuestion =
        questions[Math.floorMod(epochDay, questions.size.toLong()).toInt()]

    val topics: List<String> = questions.map { it.topic }.distinct()

    fun nextQuestion(progress: LearningProgress, topic: String? = null): PracticeQuestion? {
        val candidates = questions.filter { topic == null || it.topic == topic }
        if (candidates.isEmpty()) return null
        candidates.firstOrNull { it.id == progress.activeAttempt?.questionId }?.let { return it }
        candidates.firstOrNull { it.id in progress.reviewIds }?.let { return it }
        candidates.firstOrNull { it.id !in progress.completedIds }?.let { return it }
        val previous = candidates.indexOfFirst { it.id == progress.lastCompletedId }
        return candidates[(previous + 1) % candidates.size]
    }
}
