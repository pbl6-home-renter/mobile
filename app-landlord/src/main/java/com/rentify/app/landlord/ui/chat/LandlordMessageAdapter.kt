package com.rentify.app.landlord.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rentify.app.core.network.client.NetworkConfig
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.landlord.databinding.ItemLandlordMessageBotBinding
import com.rentify.app.landlord.databinding.ItemLandlordMessageReceivedBinding
import com.rentify.app.landlord.databinding.ItemLandlordMessageSentBinding

class LandlordMessageAdapter(
    private var currentUserId: String? = null,
    private val onMessageLongClick: ((MessageListResponseDto) -> Unit)? = null
) : ListAdapter<MessageListResponseDto, RecyclerView.ViewHolder>(DiffCallback) {

    fun setCurrentUserId(userId: String?) {
        if (currentUserId != userId) {
            currentUserId = userId
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.senderType == "bot" || message.type == "bot" -> VIEW_TYPE_BOT
            !currentUserId.isNullOrBlank() && message.senderUserId == currentUserId -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemLandlordMessageSentBinding.inflate(inflater, parent, false)
                SentViewHolder(binding)
            }
            VIEW_TYPE_BOT -> {
                val binding = ItemLandlordMessageBotBinding.inflate(inflater, parent, false)
                BotViewHolder(binding)
            }
            else -> {
                val binding = ItemLandlordMessageReceivedBinding.inflate(inflater, parent, false)
                ReceivedViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(item)
            is ReceivedViewHolder -> holder.bind(item)
            is BotViewHolder -> holder.bind(item)
        }
    }

    inner class SentViewHolder(
        private val binding: ItemLandlordMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto) {
            val reply = item.replyTo
            if (reply != null) {
                binding.layoutReplyQuote.visibility = View.VISIBLE
                binding.tvReplySender.text = reply.senderName?.ifBlank { "Tin nhắn" } ?: "Tin nhắn"
                binding.tvReplyContent.text = when {
                    reply.isUnavailable -> "Tin nhắn đã được thu hồi"
                    reply.type == "image" -> "[Hình ảnh]"
                    else -> reply.contentPreview ?: ""
                }
            } else {
                binding.layoutReplyQuote.visibility = View.GONE
            }

            if (item.type == "image" && !item.mediaId.isNullOrBlank()) {
                binding.cardMessageImage.visibility = View.VISIBLE
                val mediaUrl = "${NetworkConfig.DEFAULT_BASE_URL}media/${item.mediaId}"
                binding.ivMessageImage.load(mediaUrl) {
                    crossfade(true)
                    placeholder(CoreUiR.drawable.ic_nav_properties)
                    error(CoreUiR.drawable.ic_nav_properties)
                }
            } else {
                binding.cardMessageImage.visibility = View.GONE
            }

            val textContent = item.content?.trim().orEmpty()
            if (textContent.isEmpty() && item.type == "image") {
                binding.tvMessageContent.visibility = View.GONE
            } else {
                binding.tvMessageContent.visibility = View.VISIBLE
                binding.tvMessageContent.text = textContent
            }

            binding.tvMessageTime.text = formatTime(item.createdAt)

            binding.root.setOnLongClickListener {
                onMessageLongClick?.invoke(item)
                true
            }
        }
    }

    inner class ReceivedViewHolder(
        private val binding: ItemLandlordMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto) {
            val senderName = item.senderName.ifBlank { "Khách thuê" }
            binding.tvSenderName.text = senderName
            binding.senderAvatarView.bindPersonal(
                userId = item.senderUserId ?: senderName,
                displayName = senderName,
                avatarUrl = item.senderAvatarUrl
            )

            val reply = item.replyTo
            if (reply != null) {
                binding.layoutReplyQuote.visibility = View.VISIBLE
                binding.tvReplySender.text = reply.senderName?.ifBlank { "Tin nhắn" } ?: "Tin nhắn"
                binding.tvReplyContent.text = when {
                    reply.isUnavailable -> "Tin nhắn đã được thu hồi"
                    reply.type == "image" -> "[Hình ảnh]"
                    else -> reply.contentPreview ?: ""
                }
            } else {
                binding.layoutReplyQuote.visibility = View.GONE
            }

            if (item.type == "image" && !item.mediaId.isNullOrBlank()) {
                binding.cardMessageImage.visibility = View.VISIBLE
                val mediaUrl = "${NetworkConfig.DEFAULT_BASE_URL}media/${item.mediaId}"
                binding.ivMessageImage.load(mediaUrl) {
                    crossfade(true)
                    placeholder(CoreUiR.drawable.ic_nav_properties)
                    error(CoreUiR.drawable.ic_nav_properties)
                }
            } else {
                binding.cardMessageImage.visibility = View.GONE
            }

            val textContent = item.content?.trim().orEmpty()
            if (textContent.isEmpty() && item.type == "image") {
                binding.tvMessageContent.visibility = View.GONE
            } else {
                binding.tvMessageContent.visibility = View.VISIBLE
                binding.tvMessageContent.text = textContent
            }

            binding.tvMessageTime.text = formatTime(item.createdAt)

            binding.root.setOnLongClickListener {
                onMessageLongClick?.invoke(item)
                true
            }
        }
    }

    inner class BotViewHolder(
        private val binding: ItemLandlordMessageBotBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto) {
            binding.tvBotMessageContent.text = item.content ?: ""
            binding.tvBotMessageTime.text = formatTime(item.createdAt)
        }
    }

    private fun formatTime(createdAt: String?): String {
        return if (!createdAt.isNullOrBlank() && createdAt.length >= 16) {
            createdAt.substring(11, 16)
        } else {
            ""
        }
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_BOT = 3

        val DiffCallback = object : DiffUtil.ItemCallback<MessageListResponseDto>() {
            override fun areItemsTheSame(
                oldItem: MessageListResponseDto,
                newItem: MessageListResponseDto
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: MessageListResponseDto,
                newItem: MessageListResponseDto
            ): Boolean = oldItem == newItem
        }
    }
}
