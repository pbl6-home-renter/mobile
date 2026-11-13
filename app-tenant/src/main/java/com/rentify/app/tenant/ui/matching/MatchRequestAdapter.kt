package com.rentify.app.tenant.ui.matching

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import coil.load
import com.rentify.app.core.network.dto.matching.MatchRequestListResponseDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toDisplayDateTime
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.ItemMatchRequestBinding

class MatchRequestAdapter(
    private var isIncoming: Boolean = true,
    private val onAcceptClick: (MatchRequestListResponseDto) -> Unit,
    private val onRejectClick: (MatchRequestListResponseDto) -> Unit,
    private val onWithdrawClick: (MatchRequestListResponseDto) -> Unit,
    private val onChatClick: (MatchRequestListResponseDto) -> Unit
) : BaseListAdapter<MatchRequestListResponseDto, ItemMatchRequestBinding>(
    inflate = ItemMatchRequestBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id && oldItem.status == newItem.status }
) {

    fun setDirection(isIncoming: Boolean) {
        this.isIncoming = isIncoming
        notifyDataSetChanged()
    }

    override fun bind(binding: ItemMatchRequestBinding, item: MatchRequestListResponseDto, position: Int) {
        val user = if (isIncoming) item.requester else item.target
        val context = binding.root.context

        if (!user.avatarUrl.isNullOrBlank()) {
            binding.ivUserAvatar.load(user.avatarUrl) {
                crossfade(true)
                placeholder(CoreR.drawable.ic_user_circle)
                error(CoreR.drawable.ic_user_circle)
            }
        } else {
            binding.ivUserAvatar.setImageResource(CoreR.drawable.ic_user_circle)
        }

        val prefix = if (isIncoming) "Từ: " else "Gửi tới: "
        binding.tvUserName.text = "$prefix${user.name}"
        binding.tvCreatedAt.text = item.createdAt.toDisplayDateTime()

        // Message
        binding.tvMessage.isVisible = !item.message.isNullOrBlank()
        binding.tvMessage.text = item.message

        // Status Tag
        when (item.status.lowercase()) {
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
                binding.tvRequestStatusBadge.text = item.status
                binding.tvRequestStatusBadge.setTextColor(ContextCompat.getColor(context, CoreR.color.rentify_neutral))
            }
        }

        // Actions visibility
        if (isIncoming) {
            binding.btnAccept.isVisible = item.status.equals("pending", ignoreCase = true)
            binding.btnReject.isVisible = item.status.equals("pending", ignoreCase = true)
            binding.btnWithdraw.isVisible = false
            binding.btnChat.isVisible = item.status.equals("accepted", ignoreCase = true)
        } else {
            binding.btnAccept.isVisible = false
            binding.btnReject.isVisible = false
            binding.btnWithdraw.isVisible = item.status.equals("pending", ignoreCase = true)
            binding.btnChat.isVisible = item.status.equals("accepted", ignoreCase = true)
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

