package com.rentify.app.tenant.ui.matching

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toDisplayDateTime
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.data.repository.MatchRequestItem
import com.rentify.app.tenant.databinding.ItemMatchRequestBinding

class MatchRequestAdapter(
    private var isIncoming: Boolean = true,
    private val onAcceptClick: (MatchRequestItem) -> Unit,
    private val onRejectClick: (MatchRequestItem) -> Unit,
    private val onWithdrawClick: (MatchRequestItem) -> Unit,
    private val onChatClick: (MatchRequestItem) -> Unit
) : BaseListAdapter<MatchRequestItem, ItemMatchRequestBinding>(
    inflate = ItemMatchRequestBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.request.id == newItem.request.id && oldItem.request.status == newItem.request.status }
) {

    fun setDirection(isIncoming: Boolean) {
        this.isIncoming = isIncoming
        notifyDataSetChanged()
    }

    override fun bind(binding: ItemMatchRequestBinding, item: MatchRequestItem, position: Int) {
        val user = item.counterpart
        val request = item.request
        val context = binding.root.context

        binding.ivUserAvatar.bindPersonal(user.id, user.name, user.avatarUrl)

        val prefix = if (isIncoming) "Từ: " else "Gửi tới: "
        binding.tvUserName.text = "$prefix${user.name}"
        binding.tvCreatedAt.text = request.createdAt.toDisplayDateTime()

        // Status Tag
        when (request.status.lowercase()) {
            "pending" -> {
                binding.tvRequestStatusBadge.text = if (isIncoming) "Chờ bạn phản hồi" else "Đang chờ đối phương"
                binding.tvRequestStatusBadge.setTextColor(ContextCompat.getColor(context, CoreR.color.rentify_warning))
            }
            "accepted" -> {
                binding.tvRequestStatusBadge.text = "Đã ghép đôi thành công"
                binding.tvRequestStatusBadge.setTextColor(ContextCompat.getColor(context, CoreR.color.rentify_success))
            }
            "rejected" -> {
                binding.tvRequestStatusBadge.text = "Đã từ chối"
                binding.tvRequestStatusBadge.setTextColor(ContextCompat.getColor(context, CoreR.color.rentify_error))
            }
            "withdrawn" -> {
                binding.tvRequestStatusBadge.text = "Đã rút lời mời"
                binding.tvRequestStatusBadge.setTextColor(ContextCompat.getColor(context, CoreR.color.rentify_neutral))
            }
            else -> {
                binding.tvRequestStatusBadge.text = request.status
                binding.tvRequestStatusBadge.setTextColor(ContextCompat.getColor(context, CoreR.color.rentify_neutral))
            }
        }

        // Actions visibility
        if (isIncoming) {
            binding.btnAccept.isVisible = request.status.equals("pending", ignoreCase = true)
            binding.btnReject.isVisible = request.status.equals("pending", ignoreCase = true)
            binding.btnWithdraw.isVisible = false
            binding.btnChat.isVisible = request.status.equals("accepted", ignoreCase = true)
        } else {
            binding.btnAccept.isVisible = false
            binding.btnReject.isVisible = false
            binding.btnWithdraw.isVisible = request.status.equals("pending", ignoreCase = true)
            binding.btnChat.isVisible = request.status.equals("accepted", ignoreCase = true)
        }

        binding.layoutActions.isVisible = binding.btnAccept.isVisible ||
                binding.btnReject.isVisible ||
                binding.btnWithdraw.isVisible ||
                binding.btnChat.isVisible

        // Click listeners
        binding.btnAccept.setOnSingleClickListener { onAcceptClick(item) }
        binding.btnReject.setOnSingleClickListener { onRejectClick(item) }
        binding.btnWithdraw.setOnSingleClickListener { onWithdrawClick(item) }
        binding.btnChat.setOnSingleClickListener { onChatClick(item) }
    }
}

