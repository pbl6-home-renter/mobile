package com.rentify.app.landlord.ui.property

import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.FragmentPropertiesBinding

class PropertiesFragment : BaseFragment<FragmentPropertiesBinding>(FragmentPropertiesBinding::inflate) {

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = false)

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    companion object {
        fun newInstance(): PropertiesFragment = PropertiesFragment()
    }
}

