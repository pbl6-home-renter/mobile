package com.rentify.app.tenant.ui.myroom

import androidx.core.view.isVisible
import coil.load
import com.rentify.app.core.network.dto.room.RoomHubRoommateDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.ItemRoommateBinding

class RoommateAdapter(
    private val onChatClick: (RoomHubRoommateDto) -> Unit
) : BaseListAdapter<RoomHubRoommateDto, ItemRoommateBinding>(
    inflate = ItemRoommateBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemRoommateBinding, item: RoomHubRoommateDto, position: Int) {
        binding.tvRoommateName.text = item.name
        binding.tvRoommatePhone.text = item.phone ?: "Chưa có SĐT"
        binding.tvRepresentativeBadge.isVisible = item.isRepresentative

        val resolvedAvatar = resolveRoommateAvatar(item.avatarUrl, item.id.ifBlank { item.name })
        binding.ivRoommateAvatar.load(resolvedAvatar) {
            crossfade(true)
            placeholder(CoreR.drawable.ic_user_circle)
            error(CoreR.drawable.ic_user_circle)
        }

        binding.btnChatRoommate.setOnSingleClickListener {
            onChatClick(item)
        }
    }

    companion object {
        private val MOCK_ROOMMATE_AVATARS = listOf(
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=160&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=160&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=160&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=160&auto=format&fit=crop&q=80"
        )

        fun resolveRoommateAvatar(url: String?, seed: String): String {
            if (!url.isNullOrBlank() && (url.contains("unsplash.com") || url.contains("cloudinary.com") || url.contains("res.cloudinary.com"))) {
                return url
            }
            val index = kotlin.math.abs(seed.hashCode()) % MOCK_ROOMMATE_AVATARS.size
            return MOCK_ROOMMATE_AVATARS[index]
        }
    }
}
