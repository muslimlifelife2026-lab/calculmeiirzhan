package com.aerocalc.smartcalculator.ads

import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

/** Load ahead; skip unavailable ads instead of delaying a completed task. */
class LearningInterstitial(
    private val activity: ComponentActivity,
    private val unitId: String,
    private val canRequestAds: () -> Boolean
) : DefaultLifecycleObserver {
    private val policy = InterstitialPolicy(SystemClock.elapsedRealtime())
    private var ad: InterstitialAd? = null
    private var activeAd: InterstitialAd? = null
    private var loadedAtMs = 0L
    private var lastLoadAttemptMs: Long? = null
    private var loading = false
    private var disposed = false
    private var consentGeneration = 0L

    fun invalidateConsent() {
        consentGeneration++
        ad?.destroy(); ad = null
        loading = false
        lastLoadAttemptMs = null
    }

    init { activity.lifecycle.addObserver(this) }

    fun preload() {
        val now = SystemClock.elapsedRealtime()
        if (disposed || loading || ad != null || activeAd != null || unitId.isBlank() || !canRequestAds()) return
        // A failed network request is retried at the next natural opportunity, at most once/minute.
        if (lastLoadAttemptMs?.let { now - it < 60_000L } == true) return
        lastLoadAttemptMs = now
        loading = true
        val generation = consentGeneration
        try {
            InterstitialAd.load(AdRequest.Builder(unitId).build(), object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    activity.runOnUiThread {
                        if (generation == consentGeneration) loading = false
                        if (disposed || generation != consentGeneration || !canRequestAds()) loaded.destroy()
                        else { ad = loaded; loadedAtMs = SystemClock.elapsedRealtime() }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    activity.runOnUiThread { if (generation == consentGeneration) loading = false }
                    Log.w("AeroCalcAds", "Interstitial unavailable: ${error.message}")
                }
            })
        } catch (error: Exception) {
            loading = false
            Log.w("AeroCalcAds", "Interstitial load failed", error)
        }
    }

    fun onActivityCompleted() {
        if (disposed || !canRequestAds()) return
        policy.activityCompleted()
        val now = SystemClock.elapsedRealtime()
        if (ad != null && now - loadedAtMs >= 3_600_000L) { ad?.destroy(); ad = null }
        val available = ad
        if (available == null || activeAd != null || !policy.canShow(now) ||
            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            preload()
            return
        }
        ad = null
        activeAd = available
        available.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                activity.runOnUiThread { policy.adShown(SystemClock.elapsedRealtime()) }
            }
            override fun onAdDismissedFullScreenContent() { finishAd() }
            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.w("AeroCalcAds", "Interstitial show failed: ${error.message}")
                finishAd()
            }
        }
        try { available.show(activity) }
        catch (error: Exception) { Log.w("AeroCalcAds", "Interstitial show failed", error); finishAd() }
    }

    private fun finishAd() = activity.runOnUiThread {
        activeAd?.destroy()
        activeAd = null
        preload()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        disposed = true
        ad?.destroy(); ad = null
        activeAd?.destroy(); activeAd = null
        activity.lifecycle.removeObserver(this)
    }
}
