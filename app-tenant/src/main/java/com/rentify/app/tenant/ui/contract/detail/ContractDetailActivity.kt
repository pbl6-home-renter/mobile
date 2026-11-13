package com.rentify.app.tenant.ui.contract.detail

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.databinding.ActivityContractDetailBinding
import com.rentify.app.tenant.ui.contract.ContractUiHelper
import kotlinx.coroutines.launch

class ContractDetailActivity : BaseActivity<ActivityContractDetailBinding>(ActivityContractDetailBinding::inflate) {

    private val contractId: String by lazy {
        intent.getStringExtra(EXTRA_CONTRACT_ID).orEmpty()
    }

    private val viewModel: ContractDetailViewModel by viewModels {
        ContractDetailViewModelFactory(this, contractId)
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.scrollViewContent.isVisible = false
                        }
                        is UiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.scrollViewContent.isVisible = true
                            bindDetail(state.data)
                        }
                        is UiState.Error -> {
                            binding.progressBar.isVisible = false
                            toast(state.message)
                        }
                        is UiState.Idle -> {
                            binding.progressBar.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun bindDetail(detail: ContractDetailResponseDto) {
        ContractUiHelper.applyStatusBadge(binding.tvStatusBadge, detail.status)

        val startDate = ContractUiHelper.formatDate(detail.startDate)
        val endDate = ContractUiHelper.formatDate(detail.endDate)
        binding.tvDuration.text = "Thời hạn: $startDate → $endDate"

        binding.tvTenantName.text = if (!detail.tenantName.isNullOrBlank()) detail.tenantName else "Khách thuê"
        binding.tvTenantPhone.text = if (!detail.tenantPhone.isNullOrBlank()) detail.tenantPhone else "--"

        binding.tvRentPrice.text = "${detail.rentPrice.toLong().toVnd()} / tháng"
        binding.tvDepositAmount.text = detail.depositAmount.toLong().toVnd()

        val vehicleCount = detail.vehicleCount
        binding.tvVehicleCount.text = if (vehicleCount != null && vehicleCount > 0) {
            "$vehicleCount xe máy/phương tiện"
        } else {
            "Chưa đăng ký xe"
        }

        binding.tvDescription.text = if (!detail.description.isNullOrBlank()) {
            detail.description
        } else {
            "Không có thỏa thuận hoặc ghi chú bổ sung."
        }
    }

    companion object {
        private const val EXTRA_CONTRACT_ID = "EXTRA_CONTRACT_ID"

        fun start(context: Context, contractId: String) {
            val intent = Intent(context, ContractDetailActivity::class.java).apply {
                putExtra(EXTRA_CONTRACT_ID, contractId)
            }
            context.startActivity(intent)
        }
    }
}

