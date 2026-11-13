package com.rentify.app.tenant.ui.issue.list

import androidx.core.view.isVisible
import com.rentify.app.core.network.dto.issue.IssueReportMyListResponseDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.tenant.databinding.ItemMyIssueBinding
import com.rentify.app.tenant.ui.issue.IssueUiHelper

class MyIssuesAdapter(
    private val onIssueClick: (IssueReportMyListResponseDto) -> Unit
) : BaseListAdapter<IssueReportMyListResponseDto, ItemMyIssueBinding>(
    inflate = ItemMyIssueBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    init {
        onItemClick = onIssueClick
    }

    override fun bind(binding: ItemMyIssueBinding, item: IssueReportMyListResponseDto, position: Int) {
        IssueUiHelper.applyStatusBadge(binding.tvStatusBadge, item.status)
        IssueUiHelper.applyUrgencyBadge(binding.tvUrgencyBadge, item.urgency)
        binding.tvTitle.text = item.title
        binding.tvCategory.text = IssueUiHelper.getCategoryLabel(item.category)
        binding.ivCategoryIcon.setImageResource(IssueUiHelper.getCategoryIconResId(item.category))
        
        binding.tvRoomName.text = if (!item.roomName.isNullOrBlank()) item.roomName else "Phòng của bạn"
        binding.tvCreatedAt.text = IssueUiHelper.formatDate(item.createdAt)
        binding.tvAssignedTo.isVisible = false

        binding.root.setOnSingleClickListener {
            onIssueClick(item)
        }
    }
}
