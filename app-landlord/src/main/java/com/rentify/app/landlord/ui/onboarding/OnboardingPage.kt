package com.rentify.app.landlord.ui.onboarding

import androidx.annotation.StringRes

data class OnboardingPage(
    val animationAsset: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int
)
