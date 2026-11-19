package com.rentify.app.tenant.ui.chat

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.tenant.databinding.ItemConversationBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ConversationAdapter(
    private val onItemClickListener: (ConversationListResponseDto) -> Unit
) : BaseListAdapter<ConversationListResponseDto, ItemConversationBinding>(
    inflate = ItemConversationBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    var currentUserId: String? = null

    init {
        onItemClick = onItemClickListener
    }

    override fun bind(binding: ItemConversationBinding, item: ConversationListResponseDto, position: Int) {
        val context = binding.root.context
        val isGroup = item.kind.equals("group", ignoreCase = true)

        val displayTitle = when {
            !item.displayName.isNullOrBlank() -> item.displayName!!
            !item.title.isNullOrBlank() -> item.title!!
            isGroup -> "Nhóm trao đổi"
            else -> "Trò chuyện trực tiếp"
        }

        binding.txtTitle.text = displayTitle

        // Nhãn "Chủ trọ" (ẩn vì API GET /conversations chưa trả về vai trò người đối diện)
        binding.tvLandlordBadge.isVisible = false

        // Chấm trạng thái trực tuyến (ẩn vì API chưa hỗ trợ online presence)
        binding.viewOnlineDot.isVisible = false

        val resolvedAvatarUrl = item.avatarUrl
            ?: item.avatarMediaId?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

        if (isGroup) {
            binding.avatarView.bindGroup(
                groupId = item.id,
                groupName = displayTitle,
                avatarUrl = resolvedAvatarUrl,
                otherMembers = emptyList()
            )
        } else {
            binding.avatarView.bindPersonal(
                userId = item.id,
                displayName = displayTitle,
                avatarUrl = resolvedAvatarUrl
            )
        }

        val isUnread = item.unreadCount > 0
        val colorText = ContextCompat.getColor(context, CoreUiR.color.rentify_text)
        val colorTextSecondary = ContextCompat.getColor(context, CoreUiR.color.rentify_text_secondary)
        val colorTextMuted = ContextCompat.getColor(context, CoreUiR.color.rentify_text_muted)
        val colorPrimary = ContextCompat.getColor(context, CoreUiR.color.rentify_primary)

        val fontRegular = ResourcesCompat.getFont(context, CoreUiR.font.plus_jakarta_sans)
        val fontMedium = ResourcesCompat.getFont(context, CoreUiR.font.plus_jakarta_sans_medium)
        val fontSemibold = ResourcesCompat.getFont(context, CoreUiR.font.plus_jakarta_sans_semibold)

        val lastMsg = item.lastMessage
        if (lastMsg != null) {
            val isFromSelf = (!currentUserId.isNullOrBlank() && lastMsg.senderUserId == currentUserId) ||
                (!isGroup &&
                    !lastMsg.senderName.isNullOrBlank() &&
                    !item.displayName.isNullOrBlank() &&
                    !lastMsg.senderName.equals(item.displayName, ignoreCase = true) &&
                    lastMsg.type != "system" &&
                    lastMsg.type != "bot")

            val previewBody = lastMsg.contentPreview?.takeIf { it.isNotBlank() }
                ?: when (lastMsg.type?.lowercase(Locale.ROOT)) {
                    "image" -> "Đã gửi ảnh"
                    else -> "Đã gửi tin nhắn"
                }

            val prefix = when {
                isFromSelf -> "Bạn: "
                isGroup && !lastMsg.senderName.isNullOrBlank() && lastMsg.type != "system" -> "${lastMsg.senderName}: "
                else -> ""
            }

            if (isGroup && prefix.isNotEmpty()) {
                val spannable = SpannableStringBuilder(prefix).apply {
                    setSpan(
                        ForegroundColorSpan(colorTextSecondary),
                        0,
                        prefix.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(previewBody)
                }
                binding.txtLastMessage.text = spannable
            } else {
                binding.txtLastMessage.text = "$prefix$previewBody"
            }

            binding.txtTime.text = formatDisplayTime(lastMsg.sentAt)
        } else {
            binding.txtLastMessage.text = "Chưa có tin nhắn nào"
            binding.txtTime.text = ""
        }

        if (isUnread) {
            binding.txtTitle.typeface = fontSemibold
            binding.txtTitle.setTextColor(colorText)

            binding.txtLastMessage.typeface = fontMedium
            binding.txtLastMessage.setTextColor(colorText)

            binding.txtTime.typeface = fontMedium
            binding.txtTime.setTextColor(colorPrimary)

            binding.txtUnreadBadge.isVisible = true
            binding.txtUnreadBadge.text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString()
        } else {
            binding.txtTitle.typeface = fontMedium
            binding.txtTitle.setTextColor(colorText)

            binding.txtLastMessage.typeface = fontRegular
            binding.txtLastMessage.setTextColor(colorTextMuted)

            binding.txtTime.typeface = fontRegular
            binding.txtTime.setTextColor(colorTextMuted)

            binding.txtUnreadBadge.isVisible = false
        }

        binding.root.setOnSingleClickListener {
            onItemClickListener(item)
        }
    }

    private fun formatDisplayTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        val date = parseIsoUtc(isoString) ?: return ""

        val nowCal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }

        if (nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }

        val yesterdayCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        if (yesterdayCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Hôm qua"
        }

        val sixDaysAgoCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!msgCal.before(sixDaysAgoCal) && !msgCal.after(nowCal)) {
            return when (msgCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                Calendar.SUNDAY -> "CN"
                else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
            }
        }

        return SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }

    private fun parseIsoUtc(isoString: String): Date? {
        return try {
            val cleanIso = isoString.substringBefore("Z").substringBefore("+")
            val millisPart = cleanIso.substringAfter(".", "")
            val normalized = if (millisPart.isNotEmpty()) {
                val base = cleanIso.substringBefore(".")
                "$base.${millisPart.take(3).padEnd(3, '0')}"
            } else {
                cleanIso
            }
            val pattern = if (normalized.contains(".")) "yyyy-MM-dd'T'HH:mm:ss.SSS" else "yyyy-MM-dd'T'HH:mm:ss"
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalized)
        } catch (_: Exception) {
            null
        }
    }
}
