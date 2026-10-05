package com.rentify.app.landlord.ui.onboarding

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.showIf
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.FragmentOnboardingBinding

class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(
    FragmentOnboardingBinding::inflate,
) {

    private val onboardingAdapter = OnboardingAdapter()

    private val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.img_onboarding_1,
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_desc_1,
        ),
        OnboardingPage(
            imageRes = R.drawable.img_onboarding_2,
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_desc_2,
        ),
        OnboardingPage(
            imageRes = R.drawable.img_onboarding_3,
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_desc_3,
        )
    )

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun initView() {
        setupViewPager()
        setupIndicator()
        setupListeners()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = onboardingAdapter
        onboardingAdapter.submitList(pages)

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val isLastPage = (position == pages.size - 1)
                binding.btnSkip.showIf(!isLastPage)
                binding.btnContinue.setText(
                    if (isLastPage) R.string.onboarding_start else R.string.onboarding_continue
                )
            }
        }
        pageChangeCallback?.let { binding.viewPager.registerOnPageChangeCallback(it) }
    }

    private fun setupIndicator() {
        TabLayoutMediator(binding.tabIndicator, binding.viewPager) { _, _ -> }.attach()
    }

    private fun setupListeners() {
        binding.btnSkip.setOnSingleClickListener {
            navigateToLogin()
        }

        binding.btnContinue.setOnSingleClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < pages.size - 1) {
                binding.viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        toast(R.string.onboarding_toast_login)
    }

    override fun onDestroyView() {
        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): OnboardingFragment = OnboardingFragment()
    }
}
