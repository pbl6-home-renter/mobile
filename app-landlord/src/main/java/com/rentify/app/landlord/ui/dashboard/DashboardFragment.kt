package com.rentify.app.landlord.ui.dashboard

import android.content.Context
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.landlord.databinding.FragmentDashboardBinding

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    interface DashboardListener {
        fun onNavigateToProperties()
        fun onNavigateToOperations(tabIndex: Int = 0)
    }

    private var listener: DashboardListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? DashboardListener ?: context as? DashboardListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = false)

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnQuickAddBuilding.setOnSingleClickListener {
            listener?.onNavigateToProperties()
        }

        binding.btnQuickAddRoom.setOnSingleClickListener {
            listener?.onNavigateToProperties()
        }

        binding.btnQuickMeters.setOnSingleClickListener {
            listener?.onNavigateToOperations(tabIndex = 1)
        }

        binding.btnQuickIssues.setOnSingleClickListener {
            listener?.onNavigateToOperations(tabIndex = 3)
        }
    }

    companion object {
        fun newInstance(): DashboardFragment = DashboardFragment()
    }
}

