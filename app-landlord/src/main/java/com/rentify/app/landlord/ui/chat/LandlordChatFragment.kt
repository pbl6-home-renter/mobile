package com.rentify.app.landlord.ui.chat

import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.FragmentLandlordChatBinding

class LandlordChatFragment : BaseFragment<FragmentLandlordChatBinding>(FragmentLandlordChatBinding::inflate) {

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = false)
    }

    companion object {
        fun newInstance(): LandlordChatFragment = LandlordChatFragment()
    }
}

