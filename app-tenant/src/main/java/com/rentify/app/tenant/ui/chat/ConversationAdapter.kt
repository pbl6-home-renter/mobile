package com.rentify.app.tenant.ui.chat

import androidx.core.view.isVisible
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.tenant.databinding.ItemConversationBinding
import com.rentify.app.tenant.ui.chat.util.ChatAvatarHelper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ConversationAdapter(
    private val onItemClickListener: (ConversationListResponseDto) -> Unit
) : BaseListAdapter<ConversationListResponseDto, ItemConversationBinding>(
    inflate = ItemConversationBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    init {
        onItemClick = onItemClickListener
    }

    override fun bind(binding: ItemConversationBinding, item: ConversationListResponseDto, position: Int) {
        val displayTitle = if (!item.displayName.isNullOrBlank()) {
            item.displayName
        } else if (!item.title.isNullOrBlank()) {
            item.title
        } else if (item.kind == "direct") {
            "Trò chuyện trực tiếp"
        } else {
            "Nhóm trao đổi"
        }

        binding.txtTitle.text = displayTitle

        val isGroup = item.kind.equals("group", ignoreCase = true)
        binding.ivGroupIndicator.isVisible = isGroup

        ChatAvatarHelper.bindAvatar(
            imgAvatar = binding.imgAvatar,
            txtInitial = binding.txtAvatarInitial,
            imgGroupIcon = binding.imgGroupIcon,
            conversationId = item.id,
            displayName = displayTitle,
            avatarMediaId = item.avatarMediaId,
            isGroup = isGroup
        )

        val lastMsg = item.lastMessage
        if (lastMsg != null) {
            val senderPrefix = if (!lastMsg.senderName.isNullOrBlank() && isGroup) {
                "${lastMsg.senderName}: "
            } else {
                ""
            }
            val previewText = lastMsg.contentPreview ?: "Đã gửi tin nhắn"
            binding.txtLastMessage.text = "$senderPrefix$previewText"
            val timeText = formatDisplayTime(lastMsg.sentAt)
            binding.txtTime.text = if (timeText.isNotBlank()) timeText else "Vừa xong"
        } else {
            binding.txtLastMessage.text = "Chưa có tin nhắn nào"
            binding.txtTime.text = "Mới"
        }

        if (item.unreadCount > 0) {
            binding.txtUnreadBadge.isVisible = true
            binding.txtUnreadBadge.text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString()
            binding.txtTitle.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.txtLastMessage.setTextColor(
                binding.root.context.getColor(com.rentify.app.core.ui.R.color.rentify_text_primary)
            )
        } else {
            binding.txtUnreadBadge.isVisible = false
            binding.txtTitle.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.txtLastMessage.setTextColor(
                binding.root.context.getColor(com.rentify.app.core.ui.R.color.rentify_text_secondary)
            )
        }

        binding.root.setOnSingleClickListener {
            onItemClickListener(item)
        }
    }

    private fun formatDisplayTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val cleanIso = isoString.substringBefore("Z").substringBefore("+")
            val pattern = if (cleanIso.contains(".")) "yyyy-MM-dd'T'HH:mm:ss.SSS" else "yyyy-MM-dd'T'HH:mm:ss"
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(cleanIso.substringBefore("."))
            if (date != null) {
                val diffMillis = System.currentTimeMillis() - date.time
                val diffHours = diffMillis / (1000 * 60 * 60)
                val diffDays = diffHours / 24

                when {
                    diffHours < 1 -> {
                        val diffMinutes = (diffMillis / (1000 * 60)).coerceAtLeast(1)
                        "$diffMinutes phút"
                    }
                    diffHours < 24 -> "${diffHours} giờ"
                    diffDays < 7 -> "${diffDays} ngày"
                    else -> {
                        if (isoString.length >= 10) isoString.substring(5, 10) else isoString
                    }
                }
            } else {
                if (isoString.length >= 16) isoString.substring(11, 16) else isoString
            }
        } catch (_: Exception) {
            if (isoString.length >= 16) isoString.substring(11, 16) else isoString
        }
    }
}
