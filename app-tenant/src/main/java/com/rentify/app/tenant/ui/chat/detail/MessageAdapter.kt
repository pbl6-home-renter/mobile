package com.rentify.app.tenant.ui.chat.detail

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ItemMessageImageMeBinding
import com.rentify.app.tenant.databinding.ItemMessageImageOtherBinding
import com.rentify.app.tenant.databinding.ItemMessageMeBinding
import com.rentify.app.tenant.databinding.ItemMessageOtherBinding
import com.rentify.app.tenant.databinding.ItemMessageSystemBinding
import com.rentify.app.tenant.databinding.LayoutMessageDateSeparatorBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

class MessageAdapter(
    var currentUserId: String?,
    private val isGroupChat: Boolean,
    private val onImageClick: ((String) -> Unit)? = null,
    private val onMessageLongClick: ((MessageListResponseDto) -> Unit)? = null,
    private val mediaUrlResolver: (suspend (String) -> String?)? = null,
    private val scope: CoroutineScope? = null
) : ListAdapter<MessageListResponseDto, RecyclerView.ViewHolder>(MessageDiffCallback) {

    private var members: List<ConversationMemberDto> = emptyList()
    private val expandedTimeMessageIds = HashSet<String>()
    private val resolvedMediaUrls = HashMap<String, Pair<String, Long>>()

    private fun bindChatImage(imageView: ImageView, item: MessageListResponseDto, onUrl: (String?) -> Unit) {
        val localUrl = localImageUrl(item)
        if (localUrl != null) {
            showChatImage(imageView, localUrl)
            onUrl(localUrl)
            return
        }

        val mediaId = item.mediaId?.takeIf { it.isNotBlank() }
        imageView.tag = mediaId
        val cached = mediaId?.let { resolvedMediaUrls[it] }
        if (cached != null && System.currentTimeMillis() - cached.second < MEDIA_URL_TTL_MS) {
            showChatImage(imageView, cached.first)
            onUrl(cached.first)
            return
        }

        showChatImage(imageView, null)
        onUrl(null)
        val resolver = mediaUrlResolver ?: return
        if (mediaId == null || scope == null) return
        scope.launch {
            val url = resolver(mediaId) ?: return@launch
            resolvedMediaUrls[mediaId] = url to System.currentTimeMillis()
            if (imageView.tag == mediaId) {
                showChatImage(imageView, url)
                onUrl(url)
            }
        }
    }

    private fun showChatImage(imageView: ImageView, url: String?) {
        imageView.load(url) {
            crossfade(true)
            placeholder(CoreUiR.drawable.ic_image_outline)
            error(CoreUiR.drawable.ic_image_outline)
        }
    }

    fun updateMembers(newMembers: List<ConversationMemberDto>) {
        members = newMembers
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun submitList(list: List<MessageListResponseDto>?) {
        super.submitList(list) {
            if (itemCount > 0) {
                notifyItemRangeChanged(0, minOf(itemCount, 4))
            }
        }
    }

    override fun submitList(list: List<MessageListResponseDto>?, commitCallback: Runnable?) {
        super.submitList(list) {
            if (itemCount > 0) {
                notifyItemRangeChanged(0, minOf(itemCount, 4))
            }
            commitCallback?.run()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        val isMe = !item.senderUserId.isNullOrBlank() && item.senderUserId == currentUserId

        return when {
            isSystemMessage(item) -> VIEW_TYPE_SYSTEM
            item.type.equals("image", ignoreCase = true) && !isRecalledMessage(item) -> {
                if (isMe) VIEW_TYPE_IMAGE_ME else VIEW_TYPE_IMAGE_OTHER
            }
            isMe -> VIEW_TYPE_ME
            else -> VIEW_TYPE_OTHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ME -> {
                val binding = ItemMessageMeBinding.inflate(inflater, parent, false)
                MeMessageViewHolder(binding)
            }
            VIEW_TYPE_OTHER -> {
                val binding = ItemMessageOtherBinding.inflate(inflater, parent, false)
                OtherMessageViewHolder(binding)
            }
            VIEW_TYPE_IMAGE_ME -> {
                val binding = ItemMessageImageMeBinding.inflate(inflater, parent, false)
                ImageMeViewHolder(binding)
            }
            VIEW_TYPE_IMAGE_OTHER -> {
                val binding = ItemMessageImageOtherBinding.inflate(inflater, parent, false)
                ImageOtherViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageSystemBinding.inflate(inflater, parent, false)
                SystemMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val groupInfo = computeGroupPositionInfo(position)
        when (holder) {
            is MeMessageViewHolder -> holder.bind(item, groupInfo)
            is OtherMessageViewHolder -> holder.bind(item, groupInfo)
            is ImageMeViewHolder -> holder.bind(item, groupInfo)
            is ImageOtherViewHolder -> holder.bind(item, groupInfo)
            is SystemMessageViewHolder -> holder.bind(item, groupInfo)
        }
    }

    private data class GroupPositionInfo(
        val groupedWithAbove: Boolean,
        val groupedWithBelow: Boolean,
        val showDateSeparator: Boolean,
        val dateSeparatorText: String
    ) {
        val isFirstInGroup: Boolean get() = !groupedWithAbove
        val isLastInGroup: Boolean get() = !groupedWithBelow
    }

    private fun computeGroupPositionInfo(position: Int): GroupPositionInfo {
        val item = getItem(position)
        // Vì RecyclerView dùng reverseLayout = true:
        // position + 1 là tin nhắn CŨ HƠN (nằm ngay PHÍA TRÊN trên màn hình)
        // position - 1 là tin nhắn MỚI HƠN (nằm ngay PHÍA DƯỚI trên màn hình)
        val olderMsg = if (position + 1 < itemCount) getItem(position + 1) else null
        val newerMsg = if (position - 1 >= 0) getItem(position - 1) else null

        val showDateSep = olderMsg == null || !isSameCalendarDay(item.createdAt, olderMsg.createdAt)
        val dateSepText = if (showDateSep) formatDateSeparator(item.createdAt) else ""

        val groupedWithAbove = olderMsg != null && isGroupedTogether(olderMsg, item)
        val groupedWithBelow = newerMsg != null && isGroupedTogether(item, newerMsg)

        return GroupPositionInfo(
            groupedWithAbove = groupedWithAbove,
            groupedWithBelow = groupedWithBelow,
            showDateSeparator = showDateSep && dateSepText.isNotEmpty(),
            dateSeparatorText = dateSepText
        )
    }

    private fun isGroupedTogether(first: MessageListResponseDto, second: MessageListResponseDto): Boolean {
        if (isSystemMessage(first) || isSystemMessage(second)) return false
        val senderA = first.senderUserId
        val senderB = second.senderUserId
        if (senderA.isNullOrBlank() || senderA != senderB) return false
        if (!isSameCalendarDay(first.createdAt, second.createdAt)) return false

        val timeA = parseIsoUtc(first.createdAt)?.time ?: return false
        val timeB = parseIsoUtc(second.createdAt)?.time ?: return false
        return abs(timeA - timeB) <= GROUP_TIME_THRESHOLD_MS
    }

    private fun toggleTimeVisibility(messageId: String, position: Int) {
        if (!expandedTimeMessageIds.add(messageId)) {
            expandedTimeMessageIds.remove(messageId)
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position)
        }
    }

    private fun buildSentTimeAndStatusText(item: MessageListResponseDto): String {
        val timeText = formatTime(item.createdAt)
        val seq = item.sequence.toLongOrNull() ?: 0L
        if (seq <= 0L) return timeText

        val seenCount = members.count { m ->
            m.participantType.equals("user", ignoreCase = true) &&
                m.status.equals("active", ignoreCase = true) &&
                !m.userId.isNullOrBlank() &&
                m.userId != currentUserId &&
                (m.lastReadSequence.toLongOrNull() ?: 0L) >= seq
        }

        if (seenCount <= 0) return timeText
        return if (isGroupChat) {
            if (timeText.isNotEmpty()) "$timeText, đã xem bởi $seenCount người" else "Đã xem bởi $seenCount người"
        } else {
            if (timeText.isNotEmpty()) "$timeText, đã xem" else "Đã xem"
        }
    }

    private fun applyVerticalGroupSpacing(root: View, info: GroupPositionInfo) {
        val density = root.resources.displayMetrics.density
        val horizontalPx = (14f * density).roundToInt()
        val topPx = ((if (info.isFirstInGroup) 4f else 1.5f) * density).roundToInt()
        val bottomPx = ((if (info.isLastInGroup) 4f else 1.5f) * density).roundToInt()
        root.setPadding(horizontalPx, topPx, horizontalPx, bottomPx)
    }

    private fun bindDateSeparator(sepBinding: LayoutMessageDateSeparatorBinding, info: GroupPositionInfo) {
        sepBinding.root.isVisible = info.showDateSeparator
        if (info.showDateSeparator) {
            sepBinding.tvDateSeparator.text = info.dateSeparatorText
        }
    }

    private fun createSentBubbleDrawable(context: Context, info: GroupPositionInfo): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val largeRadius = 20f * density
        val smallRadius = 6f * density
        val topRight = if (info.groupedWithAbove) smallRadius else largeRadius
        val bottomRight = if (info.groupedWithBelow) smallRadius else largeRadius

        val baseDrawable = ContextCompat.getDrawable(context, CoreUiR.drawable.bg_pill_gradient)?.mutate() as? GradientDrawable
            ?: GradientDrawable()
        return baseDrawable.apply {
            cornerRadii = floatArrayOf(
                largeRadius, largeRadius, // top-left
                topRight, topRight,       // top-right (phía người gửi)
                bottomRight, bottomRight, // bottom-right (phía người gửi)
                largeRadius, largeRadius  // bottom-left
            )
        }
    }

    private fun createReceivedBubbleDrawable(context: Context, info: GroupPositionInfo): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val largeRadius = 20f * density
        val smallRadius = 6f * density
        val topLeft = if (info.groupedWithAbove) smallRadius else largeRadius
        val bottomLeft = if (info.groupedWithBelow) smallRadius else largeRadius

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(context, CoreUiR.color.rentify_field_bg))
            cornerRadii = floatArrayOf(
                topLeft, topLeft,         // top-left (phía người gửi)
                largeRadius, largeRadius, // top-right
                largeRadius, largeRadius, // bottom-right
                bottomLeft, bottomLeft    // bottom-left (phía người gửi)
            )
        }
    }

    private fun resolveMemberAvatarUrl(senderUserId: String?, senderAvatarUrl: String?): String? {
        if (!senderAvatarUrl.isNullOrBlank()) return senderAvatarUrl
        if (senderUserId.isNullOrBlank()) return null
        return members.firstOrNull { it.userId == senderUserId }?.avatarUrl
    }

    private inner class MeMessageViewHolder(
        private val binding: ItemMessageMeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageListResponseDto, info: GroupPositionInfo) {
            val context = binding.root.context
            val density = context.resources.displayMetrics.density
            val maxBubbleWidth = (context.resources.displayMetrics.widthPixels * 0.76f).roundToInt()
            val innerMaxWidth = (maxBubbleWidth - (26f * density).roundToInt()).coerceAtLeast(100)

            applyVerticalGroupSpacing(binding.root, info)
            bindDateSeparator(binding.layoutDateSeparator, info)

            binding.txtContent.maxWidth = innerMaxWidth
            binding.txtReplySender.maxWidth = innerMaxWidth
            binding.txtReplyContent.maxWidth = innerMaxWidth

            val fontRegular = ResourcesCompat.getFont(context, CoreUiR.font.plus_jakarta_sans)

            if (isRecalledMessage(item)) {
                binding.bubbleMe.setBackgroundResource(R.drawable.bg_message_recalled)
                binding.layoutReplyQuote.isVisible = false
                binding.txtContent.text = "Tin nhắn đã được thu hồi"
                binding.txtContent.setTextColor(ContextCompat.getColor(context, CoreUiR.color.rentify_text_faint))
                binding.txtContent.setTypeface(fontRegular, Typeface.ITALIC)
            } else {
                binding.bubbleMe.background = createSentBubbleDrawable(context, info)
                binding.txtContent.text = item.content ?: ""
                binding.txtContent.setTextColor(ContextCompat.getColor(context, CoreUiR.color.rentify_on_primary))
                binding.txtContent.setTypeface(fontRegular, Typeface.NORMAL)

                val reply = item.replyTo
                if (reply != null) {
                    binding.layoutReplyQuote.isVisible = true
                    binding.txtReplySender.text = reply.senderName?.takeIf { it.isNotBlank() } ?: "Người gửi"
                    binding.txtReplyContent.text = if (reply.isUnavailable) {
                        "Tin nhắn đã được thu hồi"
                    } else {
                        reply.contentPreview ?: ""
                    }
                } else {
                    binding.layoutReplyQuote.isVisible = false
                }
            }

            val showTime = info.isLastInGroup || expandedTimeMessageIds.contains(item.id)
            binding.txtTimeStatus.isVisible = showTime
            if (showTime) {
                binding.txtTimeStatus.text = buildSentTimeAndStatusText(item)
            }

            binding.bubbleMe.setOnClickListener {
                toggleTimeVisibility(item.id, bindingAdapterPosition)
            }

            binding.bubbleMe.setOnLongClickListener {
                if (!isRecalledMessage(item)) {
                    onMessageLongClick?.invoke(item)
                }
                true
            }
        }
    }

    private inner class OtherMessageViewHolder(
        private val binding: ItemMessageOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageListResponseDto, info: GroupPositionInfo) {
            val context = binding.root.context
            val density = context.resources.displayMetrics.density
            val maxBubbleWidth = (context.resources.displayMetrics.widthPixels * 0.76f).roundToInt()
            val innerMaxWidth = (maxBubbleWidth - (26f * density).roundToInt()).coerceAtLeast(100)

            applyVerticalGroupSpacing(binding.root, info)
            bindDateSeparator(binding.layoutDateSeparator, info)

            binding.txtContent.maxWidth = innerMaxWidth
            binding.txtReplySender.maxWidth = innerMaxWidth
            binding.txtReplyContent.maxWidth = innerMaxWidth

            // 3.3 Avatar & Tên người gửi
            if (isGroupChat) {
                binding.layoutAvatarSlot.isVisible = true
                if (info.isLastInGroup) {
                    binding.avatarSmall.visibility = View.VISIBLE
                    binding.avatarSmall.bindPersonal(
                        userId = item.senderUserId ?: item.senderName,
                        displayName = item.senderName,
                        avatarUrl = resolveMemberAvatarUrl(item.senderUserId, item.senderAvatarUrl)
                    )
                } else {
                    binding.avatarSmall.visibility = View.INVISIBLE
                }

                binding.txtSenderName.isVisible = info.isFirstInGroup && item.senderName.isNotBlank()
                binding.txtSenderName.text = item.senderName
            } else {
                // Chat CÁ NHÂN: KHÔNG hiện avatar trong khung chat
                binding.layoutAvatarSlot.isVisible = false
                binding.txtSenderName.isVisible = false
            }

            val fontRegular = ResourcesCompat.getFont(context, CoreUiR.font.plus_jakarta_sans)

            if (isRecalledMessage(item)) {
                binding.bubbleOther.setBackgroundResource(R.drawable.bg_message_recalled)
                binding.layoutReplyQuote.isVisible = false
                binding.txtContent.text = "Tin nhắn đã được thu hồi"
                binding.txtContent.setTextColor(ContextCompat.getColor(context, CoreUiR.color.rentify_text_faint))
                binding.txtContent.setTypeface(fontRegular, Typeface.ITALIC)
            } else {
                binding.bubbleOther.background = createReceivedBubbleDrawable(context, info)
                binding.txtContent.text = item.content ?: ""
                binding.txtContent.setTextColor(ContextCompat.getColor(context, CoreUiR.color.rentify_text))
                binding.txtContent.setTypeface(fontRegular, Typeface.NORMAL)

                val reply = item.replyTo
                if (reply != null) {
                    binding.layoutReplyQuote.isVisible = true
                    binding.txtReplySender.text = reply.senderName?.takeIf { it.isNotBlank() } ?: "Người gửi"
                    binding.txtReplyContent.text = if (reply.isUnavailable) {
                        "Tin nhắn đã được thu hồi"
                    } else {
                        reply.contentPreview ?: ""
                    }
                } else {
                    binding.layoutReplyQuote.isVisible = false
                }
            }

            val showTime = info.isLastInGroup || expandedTimeMessageIds.contains(item.id)
            binding.txtTimeStatus.isVisible = showTime
            if (showTime) {
                binding.txtTimeStatus.text = formatTime(item.createdAt)
                val timeStartMargin = if (isGroupChat) (36f * density).roundToInt() else 0
                (binding.txtTimeStatus.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                    lp.marginStart = timeStartMargin
                    binding.txtTimeStatus.layoutParams = lp
                }
            }

            binding.bubbleOther.setOnClickListener {
                toggleTimeVisibility(item.id, bindingAdapterPosition)
            }

            binding.bubbleOther.setOnLongClickListener {
                if (!isRecalledMessage(item)) {
                    onMessageLongClick?.invoke(item)
                }
                true
            }
        }
    }

    private inner class ImageMeViewHolder(
        private val binding: ItemMessageImageMeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageListResponseDto, info: GroupPositionInfo) {
            val context = binding.root.context
            val density = context.resources.displayMetrics.density
            val largeRadius = 20f * density
            val smallRadius = 6f * density

            applyVerticalGroupSpacing(binding.root, info)
            bindDateSeparator(binding.layoutDateSeparator, info)

            binding.cardImage.shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, largeRadius)
                .setBottomLeftCorner(CornerFamily.ROUNDED, largeRadius)
                .setTopRightCorner(CornerFamily.ROUNDED, if (info.groupedWithAbove) smallRadius else largeRadius)
                .setBottomRightCorner(CornerFamily.ROUNDED, if (info.groupedWithBelow) smallRadius else largeRadius)
                .build()

            var imageUrl: String? = null
            binding.pbUploadProgress.isVisible = item.mediaId.isNullOrBlank()
            bindChatImage(binding.ivMessageImage, item) { imageUrl = it }

            val showTime = info.isLastInGroup || expandedTimeMessageIds.contains(item.id)
            binding.txtTimeStatus.isVisible = showTime
            if (showTime) {
                binding.txtTimeStatus.text = buildSentTimeAndStatusText(item)
            }

            binding.cardImage.setOnClickListener {
                val url = imageUrl
                if (!url.isNullOrBlank()) {
                    onImageClick?.invoke(url)
                } else {
                    toggleTimeVisibility(item.id, bindingAdapterPosition)
                }
            }

            binding.cardImage.setOnLongClickListener {
                onMessageLongClick?.invoke(item)
                true
            }
        }
    }

    private inner class ImageOtherViewHolder(
        private val binding: ItemMessageImageOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageListResponseDto, info: GroupPositionInfo) {
            val context = binding.root.context
            val density = context.resources.displayMetrics.density
            val largeRadius = 20f * density
            val smallRadius = 6f * density

            applyVerticalGroupSpacing(binding.root, info)
            bindDateSeparator(binding.layoutDateSeparator, info)

            if (isGroupChat) {
                binding.layoutAvatarSlot.isVisible = true
                if (info.isLastInGroup) {
                    binding.avatarSmall.visibility = View.VISIBLE
                    binding.avatarSmall.bindPersonal(
                        userId = item.senderUserId ?: item.senderName,
                        displayName = item.senderName,
                        avatarUrl = resolveMemberAvatarUrl(item.senderUserId, item.senderAvatarUrl)
                    )
                } else {
                    binding.avatarSmall.visibility = View.INVISIBLE
                }

                binding.txtSenderName.isVisible = info.isFirstInGroup && item.senderName.isNotBlank()
                binding.txtSenderName.text = item.senderName
            } else {
                binding.layoutAvatarSlot.isVisible = false
                binding.txtSenderName.isVisible = false
            }

            binding.cardImage.shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, if (info.groupedWithAbove) smallRadius else largeRadius)
                .setBottomLeftCorner(CornerFamily.ROUNDED, if (info.groupedWithBelow) smallRadius else largeRadius)
                .setTopRightCorner(CornerFamily.ROUNDED, largeRadius)
                .setBottomRightCorner(CornerFamily.ROUNDED, largeRadius)
                .build()

            var imageUrl: String? = null
            bindChatImage(binding.ivMessageImage, item) { imageUrl = it }

            val showTime = info.isLastInGroup || expandedTimeMessageIds.contains(item.id)
            binding.txtTimeStatus.isVisible = showTime
            if (showTime) {
                binding.txtTimeStatus.text = formatTime(item.createdAt)
                val timeStartMargin = if (isGroupChat) (36f * density).roundToInt() else 0
                (binding.txtTimeStatus.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                    lp.marginStart = timeStartMargin
                    binding.txtTimeStatus.layoutParams = lp
                }
            }

            binding.cardImage.setOnClickListener {
                val url = imageUrl
                if (!url.isNullOrBlank()) {
                    onImageClick?.invoke(url)
                } else {
                    toggleTimeVisibility(item.id, bindingAdapterPosition)
                }
            }

            binding.cardImage.setOnLongClickListener {
                onMessageLongClick?.invoke(item)
                true
            }
        }
    }

    private inner class SystemMessageViewHolder(
        private val binding: ItemMessageSystemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageListResponseDto, info: GroupPositionInfo) {
            val context = binding.root.context
            bindDateSeparator(binding.layoutDateSeparator, info)

            val maxSystemWidth = (context.resources.displayMetrics.widthPixels * 0.82f).roundToInt()
            binding.txtSystemMessage.maxWidth = maxSystemWidth

            val body = item.content?.takeIf { it.isNotBlank() } ?: "Thông báo từ hệ thống"
            val timeText = formatTime(item.createdAt)
            binding.txtSystemMessage.text = if (timeText.isNotEmpty()) "$body, $timeText" else body
        }
    }

    companion object {
        private const val VIEW_TYPE_ME = 1
        private const val VIEW_TYPE_OTHER = 2
        private const val VIEW_TYPE_SYSTEM = 3
        private const val VIEW_TYPE_IMAGE_ME = 4
        private const val VIEW_TYPE_IMAGE_OTHER = 5

        // Hằng số 5 phút cho gom nhóm tin nhắn liên tiếp của cùng 1 người
        const val GROUP_TIME_THRESHOLD_MS = 5 * 60 * 1000L

        private const val MEDIA_URL_TTL_MS = 240_000L

        private fun isSystemMessage(item: MessageListResponseDto): Boolean {
            return item.senderType.equals("system", ignoreCase = true) ||
                item.type.equals("system", ignoreCase = true)
        }

        private fun isRecalledMessage(item: MessageListResponseDto): Boolean {
            return item.type.equals("recalled", ignoreCase = true)
        }

        private fun localImageUrl(item: MessageListResponseDto): String? {
            val content = item.content
            if (!content.isNullOrBlank() &&
                (content.startsWith("content://") ||
                    content.startsWith("file://") ||
                    content.startsWith("http://") ||
                    content.startsWith("https://"))
            ) {
                return content
            }
            return null
        }

        private fun parseIsoUtc(isoString: String?): Date? {
            if (isoString.isNullOrBlank()) return null
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

        private fun isSameCalendarDay(isoA: String?, isoB: String?): Boolean {
            val dateA = parseIsoUtc(isoA) ?: return true
            val dateB = parseIsoUtc(isoB) ?: return true
            val calA = Calendar.getInstance().apply { time = dateA }
            val calB = Calendar.getInstance().apply { time = dateB }
            return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
                calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
        }

        private fun formatTime(isoString: String?): String {
            val date = parseIsoUtc(isoString) ?: return ""
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }

        /**
         * 3.5 Phân cách ngày: "Hôm nay", "Hôm qua", thứ trong tuần nếu trong 7 ngày, còn lại "dd/MM/yyyy"
         */
        private fun formatDateSeparator(isoString: String?): String {
            val date = parseIsoUtc(isoString) ?: return ""
            val nowCal = Calendar.getInstance()
            val msgCal = Calendar.getInstance().apply { time = date }

            if (nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
            ) {
                return "Hôm nay"
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
                    Calendar.MONDAY -> "Thứ Hai"
                    Calendar.TUESDAY -> "Thứ Ba"
                    Calendar.WEDNESDAY -> "Thứ Tư"
                    Calendar.THURSDAY -> "Thứ Năm"
                    Calendar.FRIDAY -> "Thứ Sáu"
                    Calendar.SATURDAY -> "Thứ Bảy"
                    Calendar.SUNDAY -> "Chủ Nhật"
                    else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                }
            }

            return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        }
    }

    object MessageDiffCallback : DiffUtil.ItemCallback<MessageListResponseDto>() {
        override fun areItemsTheSame(oldItem: MessageListResponseDto, newItem: MessageListResponseDto): Boolean {
            return if (!oldItem.clientMessageId.isNullOrBlank() && !newItem.clientMessageId.isNullOrBlank()) {
                oldItem.clientMessageId == newItem.clientMessageId
            } else {
                oldItem.id == newItem.id
            }
        }

        override fun areContentsTheSame(oldItem: MessageListResponseDto, newItem: MessageListResponseDto): Boolean {
            return oldItem == newItem
        }
    }
}
