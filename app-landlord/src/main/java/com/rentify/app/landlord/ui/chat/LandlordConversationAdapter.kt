package com.rentify.app.landlord.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.landlord.databinding.ItemLandlordConversationBinding

class LandlordConversationAdapter(
    private val onItemClick: (ConversationListResponseDto) -> Unit
) : ListAdapter<ConversationListResponseDto, LandlordConversationAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLandlordConversationBinding.inflate(
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
        private val binding: ItemLandlordConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ConversationListResponseDto) {
            val title = item.displayName ?: item.title ?: "Cuộc trò chuyện"
            binding.tvConversationTitle.text = title

            val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
            binding.tvAvatarInitial.text = initial

            val lastMsg = item.lastMessage?.contentPreview
                ?: if (item.lastMessage?.type == "image") "[Hình ảnh]" else "Chưa có tin nhắn"
            binding.tvLastMessage.text = lastMsg

            val sentAt = item.lastMessage?.sentAt
            binding.tvLastMessageTime.text = if (!sentAt.isNullOrBlank() && sentAt.length >= 16) {
                sentAt.substring(11, 16)
            } else {
                ""
            }

            if (item.unreadCount > 0) {
                binding.tvUnreadBadge.visibility = View.VISIBLE
                binding.tvUnreadBadge.text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString()
            } else {
                binding.tvUnreadBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ConversationListResponseDto>() {
        override fun areItemsTheSame(
            oldItem: ConversationListResponseDto,
            newItem: ConversationListResponseDto
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ConversationListResponseDto,
            newItem: ConversationListResponseDto
        ): Boolean = oldItem == newItem
    }
}

