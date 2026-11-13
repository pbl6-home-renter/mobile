package com.rentify.app.landlord.ui.operations

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.issue.IssueReportListResponseDto
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.landlord.databinding.ItemLandlordIssueBinding

class LandlordIssueAdapter(
    private val onItemClick: (IssueReportListResponseDto) -> Unit
) : ListAdapter<IssueReportListResponseDto, LandlordIssueAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLandlordIssueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLandlordIssueBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IssueReportListResponseDto) {
            binding.tvIssueTitle.text = item.title.ifBlank { "Sự cố không có tiêu đề" }

            val locationText = when {
                item.roomName.isNotBlank() && item.buildingName.isNotBlank() -> "${item.roomName} • ${item.buildingName}"
                item.roomName.isNotBlank() -> item.roomName
                item.buildingName.isNotBlank() -> item.buildingName
                else -> "Khu trọ"
            }
            binding.tvLocation.text = locationText

            binding.tvReporter.text = if (item.reporterName.isNotBlank()) "Người gửi: ${item.reporterName}" else "Người gửi: Khách thuê"
            binding.tvCreatedAt.text = if (item.createdAt.length >= 10) item.createdAt.take(10) else item.createdAt

            // Category Icon
            val categoryIconRes = when (item.category.uppercase()) {
                "PLUMBING" -> CoreR.drawable.ic_issue_plumbing
                "ELECTRICAL" -> CoreR.drawable.ic_issue_electrical
                "APPLIANCE" -> CoreR.drawable.ic_issue_appliance
                "STRUCTURAL" -> CoreR.drawable.ic_issue_structural
                "SECURITY" -> CoreR.drawable.ic_issue_security
                else -> CoreR.drawable.ic_issue_other
            }
            binding.ivCategoryIcon.setImageResource(categoryIconRes)

            // Urgency badge
            val (urgencyText, uTextColor, uBgColor) = when (item.urgency.uppercase()) {
                "EMERGENCY" -> Triple("● Khẩn cấp", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
                "HIGH" -> Triple("● Cao", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                "MEDIUM" -> Triple("● Trung bình", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
                "LOW" -> Triple("● Thấp", Color.parseColor("#059669"), Color.parseColor("#D1FAE5"))
                else -> Triple("● ${item.urgency}", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            }
            binding.tvUrgencyBadge.text = urgencyText
            binding.tvUrgencyBadge.setTextColor(uTextColor)
            binding.tvUrgencyBadge.background = GradientDrawable().apply {
                setColor(uBgColor)
                cornerRadius = 12f
            }

            // Status badge
            val (statusText, sTextColor, sBgColor) = when (item.status.lowercase()) {
                "open" -> Triple("Mới gửi", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                "in_progress" -> Triple("Đang xử lý", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
                "resolved" -> Triple("Đã giải quyết", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
                "cancelled" -> Triple("Đã hủy", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
                else -> Triple(item.status, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            }
            binding.tvStatusBadge.text = statusText
            binding.tvStatusBadge.setTextColor(sTextColor)
            binding.tvStatusBadge.background = GradientDrawable().apply {
                setColor(sBgColor)
                cornerRadius = 16f
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<IssueReportListResponseDto>() {
        override fun areItemsTheSame(
            oldItem: IssueReportListResponseDto,
            newItem: IssueReportListResponseDto
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: IssueReportListResponseDto,
            newItem: IssueReportListResponseDto
        ): Boolean = oldItem == newItem
    }
}

