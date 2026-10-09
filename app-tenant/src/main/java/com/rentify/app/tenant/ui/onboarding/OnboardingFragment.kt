package com.rentify.app.tenant.ui.onboarding

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.showIf
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.transformer.ParallaxDepthPageTransformer
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.FragmentOnboardingBinding

class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(
    FragmentOnboardingBinding::inflate,
) {

    private val onboardingAdapter = OnboardingAdapter()

    private val pages = listOf(
        OnboardingPage(
            animationAsset = "onboarding_1.json",
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_desc_1,
        ),
        OnboardingPage(
            animationAsset = "onboarding_2.json",
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_desc_2,
        ),
        OnboardingPage(
            animationAsset = "onboarding_3.json",
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_desc_3,
        ),
    )

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun initView() {
        setupViewPager()
        setupIndicator()
        setupListeners()
    }

    private fun setupViewPager() {
        binding.vpOnboarding.adapter = onboardingAdapter
        binding.vpOnboarding.setPageTransformer(ParallaxDepthPageTransformer())
        onboardingAdapter.submitList(pages)

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val isLastPage = position == (pages.size - 1)
                binding.btnSkip.showIf(!isLastPage)
                binding.btnNext.setText(
                    if (isLastPage) R.string.onboarding_start else R.string.onboarding_continue,
                )
            }
        }
        pageChangeCallback?.let { binding.vpOnboarding.registerOnPageChangeCallback(it) }
    }

    private fun setupIndicator() {
        TabLayoutMediator(binding.tabIndicator, binding.vpOnboarding) { _, _ -> }.attach()
    }

    private fun setupListeners() {
        binding.btnSkip.setOnSingleClickListener {
            navigateToLogin()
        }

        binding.btnNext.setOnSingleClickListener {
            val currentItem = binding.vpOnboarding.currentItem
            if (currentItem < (pages.size - 1)) {
                binding.vpOnboarding.setCurrentItem(currentItem + 1, true)
            } else {
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        toast(R.string.onboarding_toast_login)
    }

    override fun onDestroyView() {
        pageChangeCallback?.let { binding.vpOnboarding.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): OnboardingFragment = OnboardingFragment()
    }
}
