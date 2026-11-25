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
import com.aerocalc.smartcalculator.ui.onboarding.AdAgeGroup

class AdsController(private val activity: ComponentActivity, private val appId: String) {
    var ready by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private var initializationStarted = false
    private val consentState = AdConsentState()
    private var currentPolicy: AgeGroupPolicy = AgeGroupPolicy.forGroup(AdAgeGroup.UNDER_13)
    private val interstitial = LearningInterstitial(activity, BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID) {
        ready && currentPolicy.adsAllowed && consent.canRequestAds()
    }

    fun onLearningActivityCompleted() = interstitial.onActivityCompleted()

    private fun buildRequestConfiguration(policy: AgeGroupPolicy): RequestConfiguration {
        val ageTreatment = when {
            policy.tagForChildDirected -> AgeRestrictedTreatment.CHILD
            policy.isTeen -> AgeRestrictedTreatment.TEEN
            else -> AgeRestrictedTreatment.UNSPECIFIED
        }
        val childDirected = if (policy.tagForChildDirected) {
            RequestConfiguration.TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        } else {
            RequestConfiguration.TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        }
        val underAge = if (policy.tagForUnderAgeOfConsent) {
            RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        } else {
            RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
        }
        return RequestConfiguration.Builder()
            .setAgeRestrictedTreatment(ageTreatment)
            .setTagForChildDirectedTreatment(childDirected)
            .setTagForUnderAgeOfConsent(underAge)
            .setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G)
            .build()
    }

    private fun updateInitializedRequestConfig(policy: AgeGroupPolicy): Boolean =
        if (runCatching { MobileAds.isInitialized }.getOrDefault(false)) {
            runCatching { MobileAds.setRequestConfiguration(buildRequestConfiguration(policy)) }
            true
        } else {
            false
        }

    fun resetForAudienceChange() {
        currentPolicy = AgeGroupPolicy.forGroup(AdAgeGroup.UNDER_13)
        ready = false
        privacyOptionsRequired = false
        consentState.allowed = false
        consentState.changing = false
        interstitial.invalidateConsent()
        consent.reset()
        updateInitializedRequestConfig(currentPolicy)
    }

    fun applyAgeGroup(group: AdAgeGroup) {
        val policy = AgeGroupPolicy.forGroup(group)
        currentPolicy = policy
        if (!policy.adsAllowed) {
            ready = false
            privacyOptionsRequired = false
            consentState.allowed = false
            consentState.changing = false
            interstitial.invalidateConsent()
            updateInitializedRequestConfig(policy)
            return
        }
        if (updateInitializedRequestConfig(policy)) {
            consentState.sdkInitialized = true
            initializationStarted = true
        }
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(policy.tagForUnderAgeOfConsent).build(),
            {
                if (currentPolicy != policy) return@requestConsentInfoUpdate
                privacyOptionsRequired = consent.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w("AeroCalcAds", "Consent form: ${error.message}")
                    initializeIfAllowed()
                }
            },
            { error ->
                Log.w("AeroCalcAds", "Consent update: ${error.message}")
                initializeIfAllowed()
            }
        )
    }

    fun showPrivacyOptions() {
        if (currentPolicy.adsAllowed && privacyOptionsRequired && !consentState.changing && !activity.isDestroyed) {
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
        if (activity.isDestroyed || !currentPolicy.adsAllowed) {
            ready = false
            interstitial.invalidateConsent()
            return
        }
        consentState.allowed = consent.canRequestAds()
        privacyOptionsRequired = consent.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        ready = consentState.ready
        if (!ready) interstitial.invalidateConsent()
        if (ready) interstitial.preload()
        if (!consentState.allowed || consentState.changing || initializationStarted) return
        initializationStarted = true
        val policy = currentPolicy
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestConfig = buildRequestConfiguration(policy)
                val appContext = activity.applicationContext
                MobileAds.initialize(
                    appContext,
                    InitializationConfig.Builder(appId).setRequestConfiguration(requestConfig).build()
                ) {
                    activity.runOnUiThread {
                        if (!activity.isDestroyed) {
                            if (currentPolicy == policy && currentPolicy.adsAllowed) {
                                // An old activity must not overwrite the age settings after recreation.
                                MobileAds.setRequestConfiguration(requestConfig)
                                consentState.sdkInitialized = true
                                initializeIfAllowed()
                            } else if (!currentPolicy.adsAllowed) {
                                runCatching { MobileAds.setRequestConfiguration(buildRequestConfiguration(currentPolicy)) }
                            }
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
