package com.rentify.app.landlord.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.landlord.databinding.ItemLandlordMessageBotBinding
import com.rentify.app.landlord.databinding.ItemLandlordMessageReceivedBinding
import com.rentify.app.landlord.databinding.ItemLandlordMessageSentBinding

class LandlordMessageAdapter(
    private val currentUserId: String?
) : ListAdapter<MessageListResponseDto, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.senderType == "bot" || message.type == "bot" -> VIEW_TYPE_BOT
            message.senderUserId != null && message.senderUserId == currentUserId -> VIEW_TYPE_SENT
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
            binding.tvMessageContent.text = item.content ?: ""
            binding.tvMessageTime.text = formatTime(item.createdAt)
        }
    }

    inner class ReceivedViewHolder(
        private val binding: ItemLandlordMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto) {
            binding.tvSenderName.text = item.senderName.ifBlank { "Khách thuê" }
            binding.tvMessageContent.text = item.content ?: ""
            binding.tvMessageTime.text = formatTime(item.createdAt)
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

