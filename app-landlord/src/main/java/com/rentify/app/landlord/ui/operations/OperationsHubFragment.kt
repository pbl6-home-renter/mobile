package com.rentify.app.landlord.ui.operations

import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.FragmentOperationsHubBinding

class OperationsHubFragment : BaseFragment<FragmentOperationsHubBinding>(FragmentOperationsHubBinding::inflate) {

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = false)
    }

    companion object {
        fun newInstance(): OperationsHubFragment = OperationsHubFragment()
    }
}

