package com.rentify.app.tenant.ui.chat.util

import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import coil.load
import coil.transform.CircleCropTransformation
import com.rentify.app.core.network.client.NetworkConfig

object ChatAvatarHelper {

    /**
     * MOCK AVATARS: Tạm thời sử dụng trong giai đoạn Mock Server chưa có avatar media thật.
     * Cần xóa bỏ và dọn dẹp khi kết nối với Backend Production. (Ghi nhận Mục 5 PROJECT_LOG.md)
     */
    private val MOCK_USER_AVATARS = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=160&auto=format&fit=crop&q=80"
    )

    private val MOCK_GROUP_AVATARS = listOf(
        "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=160&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1543269865-cbf427effbad?w=160&auto=format&fit=crop&q=80"
    )

    private val GRADIENTS = listOf(
        intArrayOf(0xFF2563EB.toInt(), 0xFF7C3AED.toInt()), // Blue -> Purple
        intArrayOf(0xFF0D9488.toInt(), 0xFF059669.toInt()), // Teal -> Emerald
        intArrayOf(0xFFEA580C.toInt(), 0xFFD97706.toInt()), // Orange -> Amber
        intArrayOf(0xFFDB2777.toInt(), 0xFF9333EA.toInt()), // Pink -> Purple
        intArrayOf(0xFF4F46E5.toInt(), 0xFF06B6D4.toInt()), // Indigo -> Cyan
        intArrayOf(0xFF0284C7.toInt(), 0xFF2563EB.toInt())  // Sky -> Blue
    )

    fun getGradientDrawable(seed: String): GradientDrawable {
        val index = kotlin.math.abs(seed.hashCode()) % GRADIENTS.size
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, GRADIENTS[index]).apply {
            shape = GradientDrawable.OVAL
        }
    }

    fun resolveAvatarUrl(conversationId: String, avatarMediaId: String?, isGroup: Boolean): String {
        if (!avatarMediaId.isNullOrBlank() && (avatarMediaId.startsWith("http://") || avatarMediaId.startsWith("https://"))) {
            return avatarMediaId
        }
        val list = if (isGroup) MOCK_GROUP_AVATARS else MOCK_USER_AVATARS
        val seed = if (!avatarMediaId.isNullOrBlank()) avatarMediaId else conversationId
        val index = kotlin.math.abs(seed.hashCode()) % list.size
        return list[index]
    }

    fun bindAvatar(
        imgAvatar: ImageView,
        txtInitial: TextView,
        imgGroupIcon: ImageView? = null,
        conversationId: String,
        displayName: String?,
        avatarMediaId: String? = null,
        isGroup: Boolean = false
    ) {
        val seed = conversationId.ifBlank { displayName ?: "Default" }
        val backgroundGradient = getGradientDrawable(seed)

        if (isGroup && imgGroupIcon != null) {
            imgGroupIcon.background = backgroundGradient
            imgGroupIcon.isVisible = true
            txtInitial.isVisible = false
        } else {
            txtInitial.background = backgroundGradient
            txtInitial.isVisible = true
            txtInitial.text = displayName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            imgGroupIcon?.isVisible = false
        }

        val url = resolveAvatarUrl(conversationId, avatarMediaId, isGroup)
        imgAvatar.load(url) {
            crossfade(true)
            transformations(CircleCropTransformation())
            listener(
                onSuccess = { _, _ ->
                    imgAvatar.isVisible = true
                    txtInitial.isVisible = false
                    imgGroupIcon?.isVisible = false
                },
                onError = { _, _ ->
                    imgAvatar.isVisible = false
                    if (isGroup && imgGroupIcon != null) {
                        imgGroupIcon.isVisible = true
                        txtInitial.isVisible = false
                    } else {
                        txtInitial.isVisible = true
                        imgGroupIcon?.isVisible = false
                    }
                }
            )
        }
    }
}

