package com.rentify.app.core.ui.home

import android.content.Context
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.databinding.FragmentHomePlaceholderBinding

class HomePlaceholderFragment : BaseFragment<FragmentHomePlaceholderBinding>(FragmentHomePlaceholderBinding::inflate) {

    interface HomePlaceholderListener {
        fun onLogoutClicked()
    }

    private var listener: HomePlaceholderListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? HomePlaceholderListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.btnLogout.setOnClickListener {
            listener?.onLogoutClicked()
        }
    }

    companion object {
        fun newInstance(): HomePlaceholderFragment {
            return HomePlaceholderFragment()
        }
    }
}
