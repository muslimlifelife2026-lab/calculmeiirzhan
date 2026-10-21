package com.aerocalc.smartcalculator.ads

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.aerocalc.smartcalculator.BuildConfig

class AdsController(private val activity: ComponentActivity, private val appId: String) {
    var ready by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private var initializationStarted = false
    private val consentState = AdConsentState()
    private var isTeen = false
    private val interstitial = LearningInterstitial(activity, BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID) {
        ready && consent.canRequestAds()
    }

    fun onLearningActivityCompleted() = interstitial.onActivityCompleted()

    fun start(isTeen: Boolean) {
        this.isTeen = isTeen
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(isTeen).build(),
            {
                privacyOptionsRequired = consent.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w("AeroCalcAds", "Consent form: ${error.message}")
                    initializeIfAllowed()
                }
                initializeIfAllowed()
            },
            { error ->
                Log.w("AeroCalcAds", "Consent update: ${error.message}")
                initializeIfAllowed()
            }
        )
    }

    fun showPrivacyOptions() {
        if (privacyOptionsRequired && !consentState.changing && !activity.isDestroyed) {
            consentState.changing = true
            ready = false
            interstitial.invalidateConsent()
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) Log.w("AeroCalcAds", "Privacy options: ${error.message}")
                consentState.changing = false
                initializeIfAllowed()
            }
        }
    }

    private fun initializeIfAllowed() {
        if (activity.isDestroyed) return
        consentState.allowed = consent.canRequestAds()
        privacyOptionsRequired = consent.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        ready = consentState.ready
        if (!ready) interstitial.invalidateConsent()
        if (ready) interstitial.preload()
        if (!consentState.allowed || consentState.changing || initializationStarted) return
        initializationStarted = true
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestConfig = RequestConfiguration.Builder()
                    .setAgeRestrictedTreatment(if (isTeen) AgeRestrictedTreatment.TEEN else AgeRestrictedTreatment.UNSPECIFIED)
                    .setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G)
                    .build()
                MobileAds.initialize(
                    activity,
                    InitializationConfig.Builder(appId).setRequestConfiguration(requestConfig).build()
                ) {
                    activity.runOnUiThread {
                        if (!activity.isDestroyed) {
                            // An old activity must not overwrite the age settings after recreation.
                            MobileAds.setRequestConfiguration(requestConfig)
                            consentState.sdkInitialized = true
                            initializeIfAllowed()
                        }
                    }
                }
            } catch (error: Exception) {
                Log.e("AeroCalcAds", "SDK initialization failed", error)
                activity.runOnUiThread { initializationStarted = false; ready = false }
            }
        }
    }
}
