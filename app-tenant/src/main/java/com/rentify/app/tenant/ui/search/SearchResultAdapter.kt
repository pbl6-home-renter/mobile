package com.rentify.app.tenant.ui.search

import androidx.core.content.ContextCompat
import coil.load
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.databinding.ItemRoomSearchResultBinding
import com.rentify.app.tenant.ui.explore.RoomForYouAdapter

class SearchResultAdapter(
    private val onRoomClick: (RoomPublicListResponseDto) -> Unit,
    private val onFavoriteClick: (RoomPublicListResponseDto) -> Unit
) : BaseListAdapter<RoomPublicListResponseDto, ItemRoomSearchResultBinding>(
    inflate = ItemRoomSearchResultBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemRoomSearchResultBinding, item: RoomPublicListResponseDto, position: Int) {
        val context = binding.root.context

        val photoUrl = RoomForYouAdapter.resolveImageUrl(item.thumbnailUrl, position)
        binding.ivThumbnail.load(photoUrl) {
            crossfade(true)
            placeholder(CoreUiR.drawable.ic_image_placeholder)
            error(CoreUiR.drawable.ic_image_placeholder)
        }

        binding.tvRoomName.text = item.name

        val locationText = listOfNotNull(
            item.district.takeIf { it.isNotBlank() },
            item.province.takeIf { it.isNotBlank() }
        ).joinToString(", ").ifBlank { "Đang cập nhật địa chỉ" }
        binding.tvLocation.text = locationText

        binding.tvRentPrice.text = item.rentPrice.toVnd()
        val parsedArea = item.area.trim().toDoubleOrNull()
        val areaText = if (parsedArea != null) {
            if (parsedArea % 1.0 == 0.0) "${parsedArea.toInt()}" else "$parsedArea"
        } else {
            "25"
        }
        binding.tvArea.text = "$areaText m²"

        if (item.isFavorited) {
            binding.btnFavorite.setImageResource(CoreUiR.drawable.ic_heart_filled)
            binding.btnFavorite.setColorFilter(ContextCompat.getColor(context, CoreUiR.color.rentify_card_saved_icon))
        } else {
            binding.btnFavorite.setImageResource(CoreUiR.drawable.ic_heart_outline)
            binding.btnFavorite.setColorFilter(ContextCompat.getColor(context, CoreUiR.color.rentify_text_secondary))
        }

        binding.root.setOnSingleClickListener {
            onRoomClick(item)
        }

        binding.btnFavorite.setOnSingleClickListener {
            onFavoriteClick(item)
        }
    }
}
