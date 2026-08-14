package com.calculator.core.ui.premium

import android.content.Context
import android.content.SharedPreferences

class PremiumManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("aerocalc_premium_prefs", Context.MODE_PRIVATE)

    fun isPremiumActive(): Boolean {
        return System.currentTimeMillis() < getUnlockUntilTimestamp()
    }

    fun getUnlockUntilTimestamp(): Long {
        return prefs.getLong(KEY_UNLOCK_UNTIL, 0L)
    }

    fun unlockFor24Hours() {
        val unlockTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24 hours in milliseconds
        prefs.edit().putLong(KEY_UNLOCK_UNTIL, unlockTime).apply()
    }

    companion object {
        private const val KEY_UNLOCK_UNTIL = "unlock_until_timestamp"
    }
}
