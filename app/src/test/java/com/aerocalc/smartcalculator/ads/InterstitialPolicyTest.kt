package com.aerocalc.smartcalculator.ads

import org.junit.Assert.*
import org.junit.Test

class InterstitialPolicyTest {
    @Test fun timeAloneAndEarlyCompletionsNeverMakeAnAdEligible() {
        val policy = InterstitialPolicy(1_000)
        assertFalse(policy.canShow(900_000))
        repeat(3) { policy.activityCompleted() }
        assertFalse(policy.canShow(180_999))
        assertTrue(policy.canShow(181_000))
    }

    @Test fun showingRequiresNewCompletionsAndCooldownAndStopsAtSessionCap() {
        val policy = InterstitialPolicy(0)
        repeat(3) { policy.activityCompleted() }
        policy.adShown(180_000)
        assertFalse(policy.canShow(900_000))
        repeat(3) { policy.activityCompleted() }
        assertFalse(policy.canShow(479_999))
        assertTrue(policy.canShow(480_000))
        policy.adShown(480_000)
        repeat(20) { policy.activityCompleted() }
        assertFalse(policy.canShow(9_000_000))
    }

    @Test fun unavailableAdDoesNotConsumeCompletedWorkOrQuota() {
        val policy = InterstitialPolicy(0)
        repeat(3) { policy.activityCompleted() }
        assertTrue(policy.canShow(180_000))
        assertTrue(policy.canShow(600_000))
    }
}
