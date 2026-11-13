package com.rentify.app.tenant.ui.chat.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rentify.app.core.network.client.NetworkConfig
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.tenant.databinding.ItemMessageImageMeBinding
import com.rentify.app.tenant.databinding.ItemMessageImageOtherBinding
import com.rentify.app.tenant.databinding.ItemMessageMeBinding
import com.rentify.app.tenant.databinding.ItemMessageOtherBinding
import com.rentify.app.tenant.databinding.ItemMessageSystemBinding

class MessageAdapter(
    var currentUserId: String?,
    private val isGroupChat: Boolean,
    private val onImageClick: ((String) -> Unit)? = null,
    private val onMessageLongClick: ((MessageListResponseDto) -> Unit)? = null
) : ListAdapter<MessageListResponseDto, RecyclerView.ViewHolder>(MessageDiffCallback) {

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        val isMe = item.senderUserId != null && item.senderUserId == currentUserId

        return when {
            item.senderType.equals("system", ignoreCase = true) || item.type.equals("system", ignoreCase = true) -> VIEW_TYPE_SYSTEM
            item.type.equals("image", ignoreCase = true) -> if (isMe) VIEW_TYPE_IMAGE_ME else VIEW_TYPE_IMAGE_OTHER
            isMe -> VIEW_TYPE_ME
            else -> VIEW_TYPE_OTHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ME -> {
                val binding = ItemMessageMeBinding.inflate(inflater, parent, false)
                MeMessageViewHolder(binding, onMessageLongClick)
            }
            VIEW_TYPE_OTHER -> {
                val binding = ItemMessageOtherBinding.inflate(inflater, parent, false)
                OtherMessageViewHolder(binding, onMessageLongClick)
            }
            VIEW_TYPE_IMAGE_ME -> {
                val binding = ItemMessageImageMeBinding.inflate(inflater, parent, false)
                ImageMeViewHolder(binding, onImageClick, onMessageLongClick)
            }
            VIEW_TYPE_IMAGE_OTHER -> {
                val binding = ItemMessageImageOtherBinding.inflate(inflater, parent, false)
                ImageOtherViewHolder(binding, onImageClick, onMessageLongClick)
            }
            else -> {
                val binding = ItemMessageSystemBinding.inflate(inflater, parent, false)
                SystemMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is MeMessageViewHolder -> holder.bind(item)
            is OtherMessageViewHolder -> holder.bind(item, isGroupChat)
            is ImageMeViewHolder -> holder.bind(item)
            is ImageOtherViewHolder -> holder.bind(item, isGroupChat)
            is SystemMessageViewHolder -> holder.bind(item)
        }
    }

    class MeMessageViewHolder(
        private val binding: ItemMessageMeBinding,
        private val onMessageLongClick: ((MessageListResponseDto) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto) {
            binding.txtContent.text = item.content ?: ""
            binding.txtTime.text = formatTime(item.createdAt)

            val reply = item.replyTo
            if (reply != null) {
                binding.layoutReplyQuote.isVisible = true
                binding.txtReplySender.text = if (reply.isUnavailable) "Tin nhắn đã xóa" else (reply.senderName ?: "Người gửi")
                binding.txtReplyContent.text = if (reply.isUnavailable) "Tin nhắn không còn khả dụng" else (reply.contentPreview ?: "")
            } else {
                binding.layoutReplyQuote.isVisible = false
            }

            binding.bubbleMe.setOnLongClickListener {
                onMessageLongClick?.invoke(item)
                true
            }
        }
    }

    class OtherMessageViewHolder(
        private val binding: ItemMessageOtherBinding,
        private val onMessageLongClick: ((MessageListResponseDto) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto, isGroup: Boolean) {
            binding.txtContent.text = item.content ?: ""
            binding.txtTime.text = formatTime(item.createdAt)

            if (isGroup && !item.senderName.isNullOrBlank()) {
                binding.txtSenderName.isVisible = true
                binding.txtSenderName.text = item.senderName
            } else {
                binding.txtSenderName.isVisible = false
            }

            val initial = item.senderName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            binding.txtAvatarSmall.text = initial

            val reply = item.replyTo
            if (reply != null) {
                binding.layoutReplyQuote.isVisible = true
                binding.txtReplySender.text = if (reply.isUnavailable) "Tin nhắn đã xóa" else (reply.senderName ?: "Người gửi")
                binding.txtReplyContent.text = if (reply.isUnavailable) "Tin nhắn không còn khả dụng" else (reply.contentPreview ?: "")
            } else {
                binding.layoutReplyQuote.isVisible = false
            }

            binding.bubbleOther.setOnLongClickListener {
                onMessageLongClick?.invoke(item)
                true
            }
        }
    }

    class ImageMeViewHolder(
        private val binding: ItemMessageImageMeBinding,
        private val onImageClick: ((String) -> Unit)?,
        private val onMessageLongClick: ((MessageListResponseDto) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto) {
            val imageUrl = resolveImageUrl(item)
            binding.txtImageTime.text = formatTime(item.createdAt)
            binding.pbUploadProgress.isVisible = item.mediaId.isNullOrBlank()

            binding.ivMessageImage.load(imageUrl) {
                crossfade(true)
                placeholder(com.rentify.app.core.ui.R.drawable.ic_image_outline)
                error(com.rentify.app.core.ui.R.drawable.ic_image_outline)
            }

            binding.cardImage.setOnClickListener {
                if (!imageUrl.isNullOrBlank()) {
                    onImageClick?.invoke(imageUrl)
                }
            }

            binding.cardImage.setOnLongClickListener {
                onMessageLongClick?.invoke(item)
                true
            }
        }
    }

    class ImageOtherViewHolder(
        private val binding: ItemMessageImageOtherBinding,
        private val onImageClick: ((String) -> Unit)?,
        private val onMessageLongClick: ((MessageListResponseDto) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto, isGroup: Boolean) {
            val imageUrl = resolveImageUrl(item)
            binding.txtImageTime.text = formatTime(item.createdAt)

            if (isGroup && !item.senderName.isNullOrBlank()) {
                binding.txtSenderName.isVisible = true
                binding.txtSenderName.text = item.senderName
            } else {
                binding.txtSenderName.isVisible = false
            }

            val initial = item.senderName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            binding.txtAvatarSmall.text = initial

            binding.ivMessageImage.load(imageUrl) {
                crossfade(true)
                placeholder(com.rentify.app.core.ui.R.drawable.ic_image_outline)
                error(com.rentify.app.core.ui.R.drawable.ic_image_outline)
            }

            binding.cardImage.setOnClickListener {
                if (!imageUrl.isNullOrBlank()) {
                    onImageClick?.invoke(imageUrl)
                }
            }

            binding.cardImage.setOnLongClickListener {
                onMessageLongClick?.invoke(item)
                true
            }
        }
    }

    class SystemMessageViewHolder(private val binding: ItemMessageSystemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListResponseDto) {
            binding.txtSystemMessage.text = item.content ?: "Thông báo từ hệ thống"
        }
    }

    companion object {
        private const val VIEW_TYPE_ME = 1
        private const val VIEW_TYPE_OTHER = 2
        private const val VIEW_TYPE_SYSTEM = 3
        private const val VIEW_TYPE_IMAGE_ME = 4
        private const val VIEW_TYPE_IMAGE_OTHER = 5

        /**
         * MOCK CHAT PHOTOS: Danh sách ảnh chụp thực tế chất lượng cao (căn hộ, phòng ngủ, phòng khách, ban công)
         * hiển thị thay thế khi chạy trên Mock Server chưa có kho media R2 thật. (Ghi nhận Mục 5 PROJECT_LOG.md)
         */
        private val MOCK_CHAT_PHOTOS = listOf(
            "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1484154218962-a197022b5858?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1554995207-c18c203602cb?w=800&auto=format&fit=crop&q=80"
        )

        fun resolveImageUrl(item: MessageListResponseDto): String? {
            val content = item.content
            if (!content.isNullOrBlank() && (content.startsWith("content://") || content.startsWith("file://") || content.startsWith("http://") || content.startsWith("https://"))) {
                return content
            }
            val key = item.mediaId?.takeIf { it.isNotBlank() } ?: item.id
            val index = kotlin.math.abs(key.hashCode()) % MOCK_CHAT_PHOTOS.size
            return MOCK_CHAT_PHOTOS[index]
        }

        private fun formatTime(isoString: String?): String {
            if (isoString.isNullOrBlank()) return ""
            return try {
                if (isoString.length >= 16) {
                    isoString.substring(11, 16)
                } else {
                    isoString
                }
            } catch (e: Exception) {
                isoString
            }
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
