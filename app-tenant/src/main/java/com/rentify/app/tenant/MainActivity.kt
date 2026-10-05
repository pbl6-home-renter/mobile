package com.rentify.app.tenant

import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.tenant.databinding.ActivityMainBinding
import com.rentify.app.tenant.ui.onboarding.OnboardingFragment

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