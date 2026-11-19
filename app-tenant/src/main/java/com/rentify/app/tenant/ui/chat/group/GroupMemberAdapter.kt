package com.rentify.app.tenant.ui.chat.group

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.tenant.databinding.ItemGroupMemberBinding

class GroupMemberAdapter(
    var currentUserId: String?,
    var isCurrentUserAdmin: Boolean,
    private val onMessageMemberClick: (ConversationMemberDto) -> Unit,
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
            val context = binding.root.context
            val isMe = !item.userId.isNullOrBlank() && item.userId == currentUserId
            val name = item.displayName.ifBlank { item.userId ?: "Thành viên" }

            binding.memberAvatarView.bindPersonal(
                userId = item.userId ?: name,
                displayName = name,
                avatarUrl = item.avatarUrl
            )

            if (isMe) {
                val suffix = " (Bạn)"
                val spannable = SpannableStringBuilder(name).apply {
                    val start = length
                    append(suffix)
                    setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(context, CoreUiR.color.rentify_text_faint)),
                        start,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                binding.txtMemberName.text = spannable
                binding.btnMessageMember.isVisible = false
                binding.btnMessageMember.setOnClickListener(null)
            } else {
                binding.txtMemberName.text = name
                binding.btnMessageMember.isVisible = true
                binding.btnMessageMember.contentDescription = "Nhắn tin với $name"
                binding.btnMessageMember.setOnClickListener {
                    onMessageMemberClick(item)
                }
            }

            // Nhãn vai trò chỉ hiện cho vai trò đặc biệt ("Trưởng nhóm"), bỏ dòng "Thành viên"
            val isAdmin = item.role.equals("admin", ignoreCase = true)
            binding.txtRoleBadge.isVisible = isAdmin
            if (isAdmin) {
                binding.txtRoleBadge.text = "Trưởng nhóm"
            }

            val canManage = isCurrentUserAdmin && !isMe
            binding.root.setOnLongClickListener {
                if (canManage) {
                    onMemberActionClick(item)
                    true
                } else {
                    false
                }
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
