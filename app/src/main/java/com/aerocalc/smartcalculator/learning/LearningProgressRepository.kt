package com.aerocalc.smartcalculator.learning

import android.content.Context

/** Progress stays on this device; no account, network or analytics is involved. */
class LearningProgressRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)

    fun load(): LearningProgress {
        val knownIds = LearningPractice.questions.map { it.id }.toSet()
        val completed = preferences.getStringSet("completed", emptySet()).orEmpty().filterTo(mutableSetOf()) { it in knownIds }
        val question = LearningPractice.find(preferences.getString("active", null))
        val attempt = question?.let {
            val savedIndex = preferences.getInt("answer", -1)
            val selected = savedIndex.takeIf { index -> index in it.answers.indices }
            PracticeAttempt(it.id, selected, selected != null && preferences.getBoolean("checked", false))
        }
        return LearningProgress(completed, attempt)
    }

    fun save(progress: LearningProgress) {
        preferences.edit()
            .putStringSet("completed", progress.completedIds.toSet())
            .putString("active", progress.activeAttempt?.questionId)
            .putInt("answer", progress.activeAttempt?.selectedAnswerIndex ?: -1)
            .putBoolean("checked", progress.activeAttempt?.checked ?: false)
            .apply()
    }
}
