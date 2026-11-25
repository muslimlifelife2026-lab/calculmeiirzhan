package com.aerocalc.smartcalculator.ads

/** Keeps interstitial frequency and grace limits intact across Activity recreation (rotation/config change). */
object SessionAdPolicy {
    @Volatile
    private var instance: InterstitialPolicy? = null

    fun current(nowMs: Long): InterstitialPolicy =
        instance ?: synchronized(this) {
            instance ?: InterstitialPolicy(nowMs).also { instance = it }
        }

    internal fun resetForTest(nowMs: Long) {
        synchronized(this) {
            instance = InterstitialPolicy(nowMs)
        }
    }
}
