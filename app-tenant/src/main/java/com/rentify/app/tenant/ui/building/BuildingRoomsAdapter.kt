package com.rentify.app.tenant.ui.building

import coil.load
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.databinding.ItemBuildingRoomBinding
import com.rentify.app.tenant.ui.explore.RoomForYouAdapter

class BuildingRoomsAdapter(
    private val onRoomClick: (RoomPublicListResponseDto) -> Unit
) : BaseListAdapter<RoomPublicListResponseDto, ItemBuildingRoomBinding>(
    inflate = ItemBuildingRoomBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemBuildingRoomBinding, item: RoomPublicListResponseDto, position: Int) {
        binding.ivRoomThumb.load(RoomForYouAdapter.resolveImageUrl(item.coverUrl, position)) {
            crossfade(true)
            placeholder(CoreUiR.drawable.ic_image_placeholder)
            error(CoreUiR.drawable.ic_image_placeholder)
        }

        binding.tvRoomName.text = item.name

        val area = item.area.trim().toDoubleOrNull()
        val areaText = when {
            area == null -> null
            area % 1.0 == 0.0 -> "${area.toInt()} m²"
            else -> "$area m²"
        }
        val genderText = when (item.genderPolicy.lowercase()) {
            "male" -> "Nam"
            "female" -> "Nữ"
            else -> "Nam, Nữ"
        }
        binding.tvRoomMeta.text = listOfNotNull(areaText, genderText).joinToString(" · ")
        binding.tvRoomPrice.text = item.rentPrice.toVnd()

        binding.root.setOnSingleClickListener { onRoomClick(item) }
    }
}
