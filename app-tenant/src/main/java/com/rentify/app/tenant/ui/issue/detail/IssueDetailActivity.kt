package com.rentify.app.tenant.ui.issue.detail

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.databinding.ActivityIssueDetailBinding
import com.rentify.app.tenant.ui.issue.IssueUiHelper
import kotlinx.coroutines.launch

class IssueDetailActivity : BaseActivity<ActivityIssueDetailBinding>(ActivityIssueDetailBinding::inflate) {

    private val issueId: String by lazy {
        intent.getStringExtra(EXTRA_ISSUE_ID).orEmpty()
    }

    private val viewModel: IssueDetailViewModel by viewModels {
        IssueDetailViewModelFactory(this, issueId)
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnCancelIssue.setOnSingleClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun showCancelConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hủy báo cáo sự cố")
            .setMessage("Bạn có chắc chắn muốn hủy báo cáo sự cố này không?")
            .setPositiveButton("Hủy sự cố") { _, _ ->
                viewModel.cancelIssue()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                binding.progressBar.isVisible = true
                                binding.scrollViewContent.isVisible = false
                                binding.layoutBottomAction.isVisible = false
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

                launch {
                    viewModel.isCancelling.collect { isCancelling ->
                        binding.btnCancelIssue.isEnabled = !isCancelling
                        binding.btnCancelIssue.text = if (isCancelling) "Đang hủy sự cố..." else "Hủy báo cáo sự cố"
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is IssueDetailUiEvent.ShowToast -> toast(event.message)
                            is IssueDetailUiEvent.CancelSuccess -> toast(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun bindDetail(detail: IssueReportDetailResponseDto) {
        IssueUiHelper.applyStatusBadge(binding.tvStatusBadge, detail.status)
        IssueUiHelper.applyUrgencyBadge(binding.tvUrgencyBadge, detail.urgency)
        binding.tvTitle.text = detail.title
        binding.tvCreatedAt.text = "Gửi lúc: ${IssueUiHelper.formatDate(detail.createdAt)}"

        val location = buildString {
            if (!detail.roomName.isNullOrBlank()) append(detail.roomName)
            if (!detail.buildingName.isNullOrBlank()) {
                if (isNotEmpty()) append(" - ")
                append(detail.buildingName)
            }
        }
        binding.tvRoomLocation.text = if (location.isNotEmpty()) location else "Phòng của bạn"
        binding.tvCategory.text = IssueUiHelper.getCategoryLabel(detail.category)
        binding.ivCategoryIcon.setImageResource(IssueUiHelper.getCategoryIconResId(detail.category))

        if (!detail.reporterName.isNullOrBlank()) {
            binding.layoutReporter.isVisible = true
            binding.tvReporterName.text = detail.reporterName
        } else {
            binding.layoutReporter.isVisible = false
        }

        binding.tvDescription.text = if (!detail.description.isNullOrBlank()) {
            detail.description
        } else {
            "Không có mô tả chi tiết."
        }

        val hasResolutionInfo = !detail.assignedTo.isNullOrBlank() ||
                detail.repairCost != null ||
                !detail.resolutionNote.isNullOrBlank() ||
                !detail.closedAt.isNullOrBlank()

        binding.cardResolution.isVisible = hasResolutionInfo

        if (hasResolutionInfo) {
            if (!detail.assignedTo.isNullOrBlank()) {
                binding.layoutAssignedTo.isVisible = true
                binding.tvAssignedTo.text = detail.assignedTo
            } else {
                binding.layoutAssignedTo.isVisible = false
            }

            val cost = detail.repairCost
            if (cost != null) {
                binding.layoutRepairCost.isVisible = true
                binding.tvRepairCost.text = cost.toVnd()
            } else {
                binding.layoutRepairCost.isVisible = false
            }

            if (!detail.resolutionNote.isNullOrBlank()) {
                binding.layoutResolutionNote.isVisible = true
                binding.tvResolutionNote.text = detail.resolutionNote
            } else {
                binding.layoutResolutionNote.isVisible = false
            }

            if (!detail.closedAt.isNullOrBlank()) {
                binding.layoutClosedAt.isVisible = true
                binding.tvClosedAt.text = IssueUiHelper.formatDate(detail.closedAt)
            } else {
                binding.layoutClosedAt.isVisible = false
            }
        }

        val isOpen = detail.status?.equals("open", ignoreCase = true) == true
        binding.layoutBottomAction.isVisible = isOpen
    }

    companion object {
        private const val EXTRA_ISSUE_ID = "EXTRA_ISSUE_ID"

        fun start(context: Context, issueId: String) {
            val intent = Intent(context, IssueDetailActivity::class.java).apply {
                putExtra(EXTRA_ISSUE_ID, issueId)
            }
            context.startActivity(intent)
        }
    }
}
