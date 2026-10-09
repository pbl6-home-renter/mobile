package com.rentify.app.tenant.ui.onboarding

import androidx.annotation.StringRes

data class OnboardingPage(
    val animationAsset: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)
