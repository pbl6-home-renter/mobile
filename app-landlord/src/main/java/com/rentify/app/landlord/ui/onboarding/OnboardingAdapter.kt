package com.rentify.app.landlord.ui.onboarding

import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.landlord.databinding.ItemOnboardingPageBinding

class OnboardingAdapter : BaseListAdapter<OnboardingPage, ItemOnboardingPageBinding>(
    inflate = ItemOnboardingPageBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem ->
        oldItem.titleRes == newItem.titleRes
    }
) {
    override fun bind(binding: ItemOnboardingPageBinding, item: OnboardingPage, position: Int) {
        binding.ivImage.setImageResource(item.imageRes)
        binding.tvTitle.setText(item.titleRes)
        binding.tvDescription.setText(item.descriptionRes)
    }
}
