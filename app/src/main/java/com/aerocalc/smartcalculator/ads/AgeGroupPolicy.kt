package com.aerocalc.smartcalculator.ads

import com.aerocalc.smartcalculator.ui.onboarding.AdAgeGroup

data class AgeGroupPolicy(
    val adsAllowed: Boolean,
    val tagForChildDirected: Boolean,
    val tagForUnderAgeOfConsent: Boolean,
    val isTeen: Boolean,
    val maxAdContentRating: ContentRating
) {
    enum class ContentRating { G }

    companion object {
        fun forGroup(group: AdAgeGroup): AgeGroupPolicy = when (group) {
            AdAgeGroup.UNDER_13 -> AgeGroupPolicy(
                adsAllowed = false,
                tagForChildDirected = true,
                tagForUnderAgeOfConsent = true,
                isTeen = false,
                maxAdContentRating = ContentRating.G
            )
            AdAgeGroup.TEEN -> AgeGroupPolicy(
                adsAllowed = true,
                tagForChildDirected = false,
                tagForUnderAgeOfConsent = true,
                isTeen = true,
                maxAdContentRating = ContentRating.G
            )
            AdAgeGroup.ADULT -> AgeGroupPolicy(
                adsAllowed = true,
                tagForChildDirected = false,
                tagForUnderAgeOfConsent = false,
                isTeen = false,
                maxAdContentRating = ContentRating.G
            )
        }
    }
}
