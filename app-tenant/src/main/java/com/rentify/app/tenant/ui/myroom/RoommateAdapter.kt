package com.rentify.app.tenant.ui.myroom

import androidx.core.view.isVisible
import com.rentify.app.core.network.dto.room.RoomHubRoommateDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.tenant.databinding.ItemRoommateBinding

class RoommateAdapter(
    private val onChatClick: (RoomHubRoommateDto) -> Unit
) : BaseListAdapter<RoomHubRoommateDto, ItemRoommateBinding>(
    inflate = ItemRoommateBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemRoommateBinding, item: RoomHubRoommateDto, position: Int) {
        binding.tvRoommateName.text = item.name
        binding.tvRepresentativeBadge.isVisible = item.isRepresentative
        binding.avatarRoommate.bindPersonal(item.id, item.name, item.avatarUrl)
        binding.btnChatRoommate.setOnSingleClickListener {
            onChatClick(item)
        }
    }
}
