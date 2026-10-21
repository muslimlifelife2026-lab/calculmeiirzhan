package com.aerocalc.smartcalculator.ads

/** Consent changes suspend requests even when the SDK finishes initialization mid-form. */
internal class AdConsentState {
    var sdkInitialized = false
    var allowed = false
    var changing = false
    val ready: Boolean get() = sdkInitialized && allowed && !changing
}
