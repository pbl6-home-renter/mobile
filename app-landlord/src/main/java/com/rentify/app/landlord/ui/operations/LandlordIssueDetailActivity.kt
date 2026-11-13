package com.rentify.app.landlord.ui.operations

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.ActivityLandlordIssueDetailBinding
import com.rentify.app.landlord.databinding.DialogUpdateIssueBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class LandlordIssueDetailActivity : BaseActivity<ActivityLandlordIssueDetailBinding>(
    ActivityLandlordIssueDetailBinding::inflate
) {
    private val issueViewModel: IssueViewModel by viewModels {
        IssueViewModelFactory(this)
    }

    private var issueId: String? = null
    private var currentDetail: IssueReportDetailResponseDto? = null

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        issueId = intent.getStringExtra(EXTRA_ISSUE_ID)
        if (issueId.isNullOrBlank()) {
            Toast.makeText(this, "Không tìm thấy mã sự cố", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        issueViewModel.loadIssueDetail(issueId!!)
    }

    private fun renderDetail(detail: IssueReportDetailResponseDto) {
        currentDetail = detail
        val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

        binding.tvIssueTitle.text = detail.title.ifBlank { "Sự cố không có tiêu đề" }

        // Category icon & label
        val (categoryLabel, categoryIconRes) = when (detail.category.uppercase()) {
            "PLUMBING" -> Pair("Đường nước", CoreR.drawable.ic_issue_plumbing)
            "ELECTRICAL" -> Pair("Hệ thống điện", CoreR.drawable.ic_issue_electrical)
            "APPLIANCE" -> Pair("Thiết bị gia dụng", CoreR.drawable.ic_issue_appliance)
            "STRUCTURAL" -> Pair("Cửa / Kết cấu", CoreR.drawable.ic_issue_structural)
            "SECURITY" -> Pair("An ninh / Khóa", CoreR.drawable.ic_issue_security)
            else -> Pair("Khác", CoreR.drawable.ic_issue_other)
        }
        binding.tvCategoryLabel.text = categoryLabel
        binding.ivCategoryIcon.setImageResource(categoryIconRes)

        // Urgency badge
        val (urgencyText, uTextColor, uBgColor) = when (detail.urgency.uppercase()) {
            "EMERGENCY" -> Triple("● Khẩn cấp", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
            "HIGH" -> Triple("● Cao", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "MEDIUM" -> Triple("● Trung bình", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
            "LOW" -> Triple("● Thấp", Color.parseColor("#059669"), Color.parseColor("#D1FAE5"))
            else -> Triple("● ${detail.urgency}", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
        }
        binding.tvUrgencyBadge.text = urgencyText
        binding.tvUrgencyBadge.setTextColor(uTextColor)
        binding.tvUrgencyBadge.background = GradientDrawable().apply {
            setColor(uBgColor)
            cornerRadius = 12f
        }

        // Status badge
        val (statusText, sTextColor, sBgColor) = when (detail.status.lowercase()) {
            "open" -> Triple("Mới gửi", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "in_progress" -> Triple("Đang xử lý", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
            "resolved" -> Triple("Đã giải quyết", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
            "cancelled" -> Triple("Đã hủy", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            else -> Triple(detail.status, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
        }
        binding.tvStatusBadge.text = statusText
        binding.tvStatusBadge.setTextColor(sTextColor)
        binding.tvStatusBadge.background = GradientDrawable().apply {
            setColor(sBgColor)
            cornerRadius = 16f
        }

        binding.tvCreatedAt.text = if (detail.createdAt.length >= 16) detail.createdAt.take(16).replace("T", " ") else detail.createdAt

        // Location & Reporter
        val locationText = when {
            detail.roomName.isNotBlank() && detail.buildingName.isNotBlank() -> "${detail.roomName} • ${detail.buildingName}"
            detail.roomName.isNotBlank() -> detail.roomName
            detail.buildingName.isNotBlank() -> detail.buildingName
            else -> "Khu trọ"
        }
        binding.tvLocationDetail.text = locationText

        val reporterPhone = detail.reporterPhone.ifBlank { "Chưa cập nhật SĐT" }
        val reporterName = detail.reporterName.ifBlank { "Khách thuê" }
        binding.tvReporterDetail.text = "$reporterName • $reporterPhone"

        if (detail.reporterPhone.isNotBlank()) {
            binding.btnCallReporter.visibility = View.VISIBLE
            binding.btnCallReporter.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${detail.reporterPhone}"))
                startActivity(intent)
            }
        } else {
            binding.btnCallReporter.visibility = View.GONE
        }

        // Description
        binding.tvDescription.text = detail.description ?: "Khách thuê không để lại mô tả chi tiết"

        // Progress
        binding.tvAssignedTo.text = detail.assignedTo ?: "Chưa phân công thợ"
        val cost = detail.repairCost
        binding.tvRepairCost.text = if (cost != null && cost > 0) {
            "${numberFormat.format(cost)} ₫"
        } else {
            "Chưa cập nhật chi phí"
        }
        binding.tvResolutionNote.text = detail.resolutionNote ?: "Chưa có ghi chú nghiệm thu"

        // Bottom Actions Setup
        val primaryColor = getColor(com.rentify.app.core.ui.R.color.rentify_primary)
        val errorColor = getColor(com.rentify.app.core.ui.R.color.rentify_error)

        when (detail.status.lowercase()) {
            "open" -> {
                binding.cardBottomActions.visibility = View.VISIBLE
                binding.btnSecondaryAction.text = "Hủy sự cố"
                binding.btnSecondaryAction.setTextColor(errorColor)
                binding.btnSecondaryAction.strokeColor = ColorStateList.valueOf(errorColor)
                binding.btnSecondaryAction.setOnClickListener {
                    showCancelIssueDialog(detail.id)
                }

                binding.btnPrimaryAction.text = "Tiếp nhận xử lý"
                binding.btnPrimaryAction.setOnClickListener {
                    showAcceptIssueDialog(detail)
                }
            }
            "in_progress" -> {
                binding.cardBottomActions.visibility = View.VISIBLE
                binding.btnSecondaryAction.text = "Cập nhật tiến độ"
                binding.btnSecondaryAction.setTextColor(primaryColor)
                binding.btnSecondaryAction.strokeColor = ColorStateList.valueOf(primaryColor)
                binding.btnSecondaryAction.setOnClickListener {
                    showUpdateProgressDialog(detail)
                }

                binding.btnPrimaryAction.text = "Nghiệm thu hoàn tất"
                binding.btnPrimaryAction.setOnClickListener {
                    showResolveIssueDialog(detail)
                }
            }
            else -> {
                binding.cardBottomActions.visibility = View.GONE
            }
        }
    }

    private fun showAcceptIssueDialog(detail: IssueReportDetailResponseDto) {
        val dialogBinding = DialogUpdateIssueBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = "Tiếp nhận xử lý sự cố"
        dialogBinding.tvDialogSubtitle.text = "Phân công thợ sửa chữa hoặc ghi chú kế hoạch kiểm tra sự cố."
        dialogBinding.tilRepairCost.visibility = View.GONE

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Tiếp nhận") { _, _ ->
                val assignedTo = dialogBinding.etAssignedTo.text?.toString()?.trim()
                val note = dialogBinding.etResolutionNote.text?.toString()?.trim()

                issueViewModel.updateIssueStatus(
                    issueId = detail.id,
                    status = "in_progress",
                    assignedTo = if (!assignedTo.isNullOrBlank()) assignedTo else null,
                    resolutionNote = if (!note.isNullOrBlank()) note else null
                )
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showUpdateProgressDialog(detail: IssueReportDetailResponseDto) {
        val dialogBinding = DialogUpdateIssueBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = "Cập nhật tiến độ xử lý"
        dialogBinding.tvDialogSubtitle.text = "Cập nhật thông tin thợ phụ trách và ghi chú tiến độ mới nhất."
        dialogBinding.tilRepairCost.visibility = View.GONE
        dialogBinding.etAssignedTo.setText(detail.assignedTo ?: "")
        dialogBinding.etResolutionNote.setText(detail.resolutionNote ?: "")

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Lưu cập nhật") { _, _ ->
                val assignedTo = dialogBinding.etAssignedTo.text?.toString()?.trim()
                val note = dialogBinding.etResolutionNote.text?.toString()?.trim()

                issueViewModel.updateIssueStatus(
                    issueId = detail.id,
                    status = "in_progress",
                    assignedTo = if (!assignedTo.isNullOrBlank()) assignedTo else null,
                    resolutionNote = if (!note.isNullOrBlank()) note else null
                )
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showResolveIssueDialog(detail: IssueReportDetailResponseDto) {
        val dialogBinding = DialogUpdateIssueBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = "Nghiệm thu & Hoàn tất sự cố"
        dialogBinding.tvDialogSubtitle.text = "Xác nhận sự cố đã được sửa chữa xong, nhập chi phí và ghi chú xử lý."
        dialogBinding.etAssignedTo.setText(detail.assignedTo ?: "")
        dialogBinding.etResolutionNote.setText(detail.resolutionNote ?: "")

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Hoàn tất") { _, _ ->
                val assignedTo = dialogBinding.etAssignedTo.text?.toString()?.trim()
                val costStr = dialogBinding.etRepairCost.text?.toString()?.trim() ?: ""
                val repairCost = costStr.toLongOrNull()
                val note = dialogBinding.etResolutionNote.text?.toString()?.trim()

                issueViewModel.updateIssueStatus(
                    issueId = detail.id,
                    status = "resolved",
                    assignedTo = if (!assignedTo.isNullOrBlank()) assignedTo else null,
                    repairCost = repairCost,
                    resolutionNote = if (!note.isNullOrBlank()) note else null
                )
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showCancelIssueDialog(issueId: String) {
        AlertDialog.Builder(this)
            .setTitle("Hủy sự cố")
            .setMessage("Bạn có chắc chắn muốn hủy báo cáo sự cố này không?")
            .setPositiveButton("Hủy sự cố") { _, _ ->
                issueViewModel.cancelIssue(issueId)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    issueViewModel.issueDetailState.collect { state ->
                        binding.progressBar.visibility = View.GONE
                        when (state) {
                            is IssueDetailUiState.Success -> {
                                renderDetail(state.detail)
                            }
                            is IssueDetailUiState.Error -> {
                                Toast.makeText(this@LandlordIssueDetailActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                            IssueDetailUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    issueViewModel.actionState.collect { state ->
                        when (state) {
                            is IssueActionUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is IssueActionUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@LandlordIssueDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                                issueViewModel.resetActionState()
                            }
                            is IssueActionUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@LandlordIssueDetailActivity, state.message, Toast.LENGTH_LONG).show()
                                issueViewModel.resetActionState()
                            }
                            IssueActionUiState.Idle -> {}
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ISSUE_ID = "extra_issue_id"
    }
}

