package com.rentify.app.landlord

import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.landlord.databinding.ActivityMainBinding
import com.rentify.app.landlord.ui.onboarding.OnboardingFragment

class MainActivity : BaseActivity<ActivityMainBinding>(
    ActivityMainBinding::inflate,
) {
    override fun initView() {
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, OnboardingFragment.newInstance())
                .commit()
        }
    }
}