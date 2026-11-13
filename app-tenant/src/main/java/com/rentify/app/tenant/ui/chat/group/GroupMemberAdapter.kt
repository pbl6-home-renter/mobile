package com.rentify.app.tenant.ui.chat.group

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.tenant.databinding.ItemGroupMemberBinding

class GroupMemberAdapter(
    var currentUserId: String?,
    var isCurrentUserAdmin: Boolean,
    private val onMemberActionClick: (ConversationMemberDto) -> Unit
) : ListAdapter<ConversationMemberDto, GroupMemberAdapter.MemberViewHolder>(MemberDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(private val binding: ItemGroupMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ConversationMemberDto) {
            val isMe = item.userId != null && item.userId == currentUserId
            val name = item.displayName.ifBlank { item.userId ?: "Thành viên" }
            binding.txtMemberName.text = name

            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            binding.txtMemberAvatar.text = initial

            binding.txtYouBadge.isVisible = isMe

            val isAdmin = item.role.equals("admin", ignoreCase = true)
            binding.txtMemberRole.text = if (isAdmin) "Trưởng nhóm" else "Thành viên"

            // Only admin can manage other members
            val canManage = isCurrentUserAdmin && !isMe
            binding.btnMemberMenu.isVisible = canManage

            if (canManage) {
                binding.btnMemberMenu.setOnClickListener {
                    onMemberActionClick(item)
                }
            } else {
                binding.btnMemberMenu.setOnClickListener(null)
            }
        }
    }

    object MemberDiffCallback : DiffUtil.ItemCallback<ConversationMemberDto>() {
        override fun areItemsTheSame(oldItem: ConversationMemberDto, newItem: ConversationMemberDto): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: ConversationMemberDto, newItem: ConversationMemberDto): Boolean {
            return oldItem == newItem
        }
    }
}

