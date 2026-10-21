package com.aerocalc.smartcalculator.ads

import org.junit.Assert.*
import org.junit.Test

class AdConsentStateTest {
    @Test fun initializationDuringPrivacyFormCannotEnableAds() {
        val state = AdConsentState()
        state.allowed = true
        state.changing = true
        state.sdkInitialized = true
        assertFalse(state.ready)
        state.allowed = false
        state.changing = false
        assertFalse(state.ready)
    }

    @Test fun restoredConsentCanResumeWithoutInitializingSdkAgain() {
        val state = AdConsentState()
        state.sdkInitialized = true
        state.allowed = true
        assertTrue(state.ready)
        state.changing = true
        assertFalse(state.ready)
        state.changing = false
        assertTrue(state.ready)
        state.allowed = false
        assertFalse(state.ready)
    }
}
