package com.rentify.app.landlord.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.notification.NotificationListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.landlord.databinding.ItemLandlordNotificationBinding

class LandlordNotificationAdapter(
    private val onItemClick: (NotificationListResponseDto) -> Unit
) : ListAdapter<NotificationListResponseDto, LandlordNotificationAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLandlordNotificationBinding.inflate(
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
        private val binding: ItemLandlordNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationListResponseDto) {
            val ctx = binding.root.context
            val isUnread = item.readAt.isNullOrBlank()

            binding.tvNotificationTitle.text = translateTitle(item.title)
            binding.tvNotificationBody.text = translateBody(item.body)
            binding.tvNotificationTime.text = formatDateTime(item.createdAt)
            binding.viewUnreadDot.visibility = if (isUnread) View.VISIBLE else View.GONE

            val cardBgColor = if (isUnread) {
                ContextCompat.getColor(ctx, CoreUiR.color.rentify_soft_surface)
            } else {
                ContextCompat.getColor(ctx, CoreUiR.color.rentify_surface)
            }
            binding.cardNotification.setCardBackgroundColor(cardBgColor)

            when (item.type.lowercase()) {
                "invoice" -> {
                    binding.tvNotificationBadge.text = "Hóa đơn"
                    binding.ivNotificationType.setImageResource(CoreUiR.drawable.ic_receipt_text)
                }
                "issue" -> {
                    binding.tvNotificationBadge.text = "Sự cố"
                    binding.ivNotificationType.setImageResource(CoreUiR.drawable.ic_wrench_tool)
                }
                else -> {
                    binding.tvNotificationBadge.text = "Hệ thống"
                    binding.ivNotificationType.setImageResource(CoreUiR.drawable.ic_shield_info)
                }
            }

            binding.cardNotification.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun translateTitle(raw: String): String {
            return when (raw) {
                "USER_ACCOUNT_LOCKED_TITLE" -> "Tài khoản đã bị tạm khóa"
                "USER_ACCOUNT_UNLOCKED_TITLE" -> "Tài khoản đã được mở khóa"
                "CONTRACT_SUSPENDED_TITLE" -> "Hợp đồng bị tạm ngưng"
                "CONTRACT_LOCK_DECISION_REQUIRED_TITLE" -> "Cần xử lý hợp đồng tạm ngưng"
                "CONTRACT_RESTORED_TITLE" -> "Hợp đồng đã được khôi phục"
                "CONTRACT_TERMINATED_TITLE" -> "Hợp đồng đã chấm dứt"
                else -> raw.ifBlank { "Thông báo hệ thống" }
            }
        }

        private fun translateBody(raw: String): String {
            return when (raw) {
                "USER_ACCOUNT_LOCKED_BODY" -> "Tài khoản của bạn đã bị khóa bởi quản trị viên."
                "USER_ACCOUNT_UNLOCKED_BODY" -> "Tài khoản của bạn đã được mở khóa và hoạt động bình thường."
                "CONTRACT_SUSPENDED_BODY" -> "Một số hợp đồng đang bị tạm ngưng do trạng thái tài khoản."
                "CONTRACT_LOCK_DECISION_REQUIRED_BODY" -> "Vui lòng kiểm tra và quyết định trạng thái hợp đồng sau khi mở khóa."
                "CONTRACT_RESTORED_BODY" -> "Hợp đồng thuê phòng đã được khôi phục hiệu lực."
                "CONTRACT_TERMINATED_BODY" -> "Hợp đồng thuê phòng đã được chấm dứt."
                else -> raw
            }
        }

        private fun formatDateTime(iso: String): String {
            if (iso.length >= 16) {
                val datePart = iso.substring(0, 10)
                val timePart = iso.substring(11, 16)
                val parts = datePart.split("-")
                if (parts.size == 3) {
                    return "${parts[2]}/${parts[1]}/${parts[0]} $timePart"
                }
            }
            return iso
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NotificationListResponseDto>() {
        override fun areItemsTheSame(
            oldItem: NotificationListResponseDto,
            newItem: NotificationListResponseDto
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: NotificationListResponseDto,
            newItem: NotificationListResponseDto
        ): Boolean = oldItem == newItem
    }
}
