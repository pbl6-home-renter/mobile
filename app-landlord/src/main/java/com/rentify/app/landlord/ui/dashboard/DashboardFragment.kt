package com.rentify.app.landlord.ui.dashboard

import android.content.Context
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.landlord.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    interface DashboardListener {
        fun onNavigateToProperties()
        fun onNavigateToOperations(tabIndex: Int = 0)
    }

    private var listener: DashboardListener? = null

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireContext())
    }

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
            viewModel.loadDashboard()
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

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DashboardUiState.Loading -> {
                            binding.swipeRefresh.isRefreshing = true
                        }
                        is DashboardUiState.Success -> {
                            binding.swipeRefresh.isRefreshing = false
                            val metrics = state.metrics
                            binding.tvOccupancyRate.text = metrics.occupancyRate.ifBlank { "0%" }
                            binding.tvTotalBuildings.text = metrics.totalBuildings.toString()
                            binding.tvTotalRooms.text = metrics.totalRooms.toString()
                            binding.tvTotalTenants.text = metrics.totalTenants.toString()

                            if (metrics.pendingApprovalBuildings > 0) {
                                binding.tvPendingBuildingsSubtitle.text = "${metrics.pendingApprovalBuildings} tòa chờ duyệt"
                            } else {
                                binding.tvPendingBuildingsSubtitle.text = "Tất cả đã duyệt"
                            }
                        }
                        is DashboardUiState.Error -> {
                            binding.swipeRefresh.isRefreshing = false
                            toast(state.message)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(): DashboardFragment = DashboardFragment()
    }
}
