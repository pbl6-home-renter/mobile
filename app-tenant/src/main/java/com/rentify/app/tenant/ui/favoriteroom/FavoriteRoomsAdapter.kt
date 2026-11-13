package com.rentify.app.tenant.ui.favoriteroom

import coil.load
import com.rentify.app.core.network.dto.room.FavoriteRoomListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.databinding.ItemFavoriteRoomBinding
import com.rentify.app.tenant.ui.explore.RoomForYouAdapter

class FavoriteRoomsAdapter(
    private val onRoomClick: (FavoriteRoomListResponseDto) -> Unit,
    private val onRemoveFavoriteClick: (FavoriteRoomListResponseDto) -> Unit
) : BaseListAdapter<FavoriteRoomListResponseDto, ItemFavoriteRoomBinding>(
    inflate = ItemFavoriteRoomBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.roomId == newItem.roomId }
) {

    init {
        onItemClick = onRoomClick
    }

    override fun bind(binding: ItemFavoriteRoomBinding, item: FavoriteRoomListResponseDto, position: Int) {
        val photoUrl = RoomForYouAdapter.resolveImageUrl(null, position)
        binding.ivRoomThumb.load(photoUrl) {
            crossfade(true)
            placeholder(CoreUiR.drawable.ic_image_placeholder)
            error(CoreUiR.drawable.ic_image_placeholder)
        }

        binding.tvRoomName.text = item.name
        binding.tvRoomPrice.text = item.rentPrice.toVnd()

        binding.root.setOnSingleClickListener {
            onRoomClick(item)
        }

        binding.btnRemoveFavorite.setOnSingleClickListener {
            onRemoveFavoriteClick(item)
        }
    }
}
