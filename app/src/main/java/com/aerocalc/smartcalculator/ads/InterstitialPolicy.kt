package com.aerocalc.smartcalculator.ads

/** Eligibility is checked only at a user-completed learning activity, never by a timer. */
class InterstitialPolicy(private val sessionStartedAtMs: Long) {
    private var completedActivities = 0
    private var shown = 0
    private var lastShownAtMs: Long? = null

    fun activityCompleted() { completedActivities++ }

    fun canShow(nowMs: Long): Boolean =
        nowMs - sessionStartedAtMs >= INITIAL_GRACE_MS &&
            completedActivities >= ACTIVITIES_BETWEEN_ADS && shown < MAX_PER_SESSION &&
            (lastShownAtMs == null || nowMs - lastShownAtMs!! >= INTERVAL_MS)

    fun adShown(nowMs: Long) {
        shown++
        completedActivities = 0
        lastShownAtMs = nowMs
    }

    companion object {
        const val INITIAL_GRACE_MS = 180_000L
        const val INTERVAL_MS = 300_000L
        const val ACTIVITIES_BETWEEN_ADS = 3
        const val MAX_PER_SESSION = 2
    }
}
