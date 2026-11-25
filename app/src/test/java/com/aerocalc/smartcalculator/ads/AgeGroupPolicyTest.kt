package com.aerocalc.smartcalculator.ads

import com.aerocalc.smartcalculator.ui.onboarding.AdAgeGroup
import org.junit.Assert.*
import org.junit.Test

class AgeGroupPolicyTest {
    @Test fun under13DisablesAdsAndForcesChildDirectedRestrictions() {
        val policy = AgeGroupPolicy.forGroup(AdAgeGroup.UNDER_13)
        assertFalse(policy.adsAllowed)
        assertTrue(policy.tagForChildDirected)
        assertTrue(policy.tagForUnderAgeOfConsent)
        assertFalse(policy.isTeen)
        assertEquals(AgeGroupPolicy.ContentRating.G, policy.maxAdContentRating)
    }

    @Test fun teenEnablesRestrictedAdsWithoutChildDirectedTag() {
        val policy = AgeGroupPolicy.forGroup(AdAgeGroup.TEEN)
        assertTrue(policy.adsAllowed)
        assertFalse(policy.tagForChildDirected)
        assertTrue(policy.tagForUnderAgeOfConsent)
        assertTrue(policy.isTeen)
        assertEquals(AgeGroupPolicy.ContentRating.G, policy.maxAdContentRating)
    }

    @Test fun adultEnablesAdsWithoutUnderAgeRestrictionsAndKeepsStrictRating() {
        val policy = AgeGroupPolicy.forGroup(AdAgeGroup.ADULT)
        assertTrue(policy.adsAllowed)
        assertFalse(policy.tagForChildDirected)
        assertFalse(policy.tagForUnderAgeOfConsent)
        assertFalse(policy.isTeen)
        assertEquals(AgeGroupPolicy.ContentRating.G, policy.maxAdContentRating)
    }

    @Test fun sessionInterstitialPolicySurvivesRecreateCalls() {
        SessionAdPolicy.resetForTest(0L)
        val first = SessionAdPolicy.current(0L)
        repeat(3) { first.activityCompleted() }
        val afterRecreate = SessionAdPolicy.current(180_000L)
        assertTrue(afterRecreate.canShow(180_000L))
        afterRecreate.adShown(180_000L)
        assertFalse(SessionAdPolicy.current(180_000L).canShow(200_000L))
    }
}
