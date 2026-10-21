package com.aerocalc.smartcalculator.learning

import android.content.Context

/** Progress stays on this device; no account, network or analytics is involved. */
class LearningProgressRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)

    fun load(): LearningProgress {
        val knownIds = LearningPractice.questions.map { it.id }.toSet()
        val completed = preferences.getStringSet("completed", emptySet()).orEmpty().filterTo(mutableSetOf()) { it in knownIds }
        val question = LearningPractice.find(preferences.getString("active", null))
        fun readAttempt(question: PracticeQuestion, prefix: String): PracticeAttempt {
            val savedIndex = preferences.getInt("${prefix}answer", -1)
            val selected = savedIndex.takeIf { index -> index in question.answers.indices }
            return PracticeAttempt(question.id, selected, selected != null && preferences.getBoolean("${prefix}checked", false))
        }
        val attempt = question?.let { readAttempt(it, "") }
        val drafts = LearningPractice.questions.filter { preferences.contains("draft.${it.id}.answer") }
            .associate { it.id to readAttempt(it, "draft.${it.id}.") }
        val review = preferences.getStringSet("review", emptySet()).orEmpty().filterTo(mutableSetOf()) { it in knownIds }
        val lastCompleted = preferences.getString("lastCompleted", null)?.takeIf { it in knownIds }
        return LearningProgress(completed, attempt, review, drafts, lastCompleted)
    }

    fun save(progress: LearningProgress) {
        val edit = preferences.edit()
            .putStringSet("completed", progress.completedIds.toSet())
            .putString("active", progress.activeAttempt?.questionId)
            .putInt("answer", progress.activeAttempt?.selectedAnswerIndex ?: -1)
            .putBoolean("checked", progress.activeAttempt?.checked ?: false)
            .putStringSet("review", progress.reviewIds.toSet())
            .putString("lastCompleted", progress.lastCompletedId)
        LearningPractice.questions.forEach { question ->
            val prefix = "draft.${question.id}."
            val draft = progress.drafts[question.id]
            if (draft == null) {
                edit.remove("${prefix}answer").remove("${prefix}checked")
            } else {
                edit.putInt("${prefix}answer", draft.selectedAnswerIndex ?: -1)
                    .putBoolean("${prefix}checked", draft.checked)
            }
        }
        edit.apply()
    }
}
