package com.rentify.app.tenant.ui.search

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import coil.load
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.R
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
        val photoUrl = RoomForYouAdapter.resolveImageUrl(item.coverUrl, position)
        binding.ivThumbnail.load(photoUrl) {
            crossfade(true)
            placeholder(CoreUiR.drawable.ic_image_placeholder)
            error(CoreUiR.drawable.ic_image_placeholder)
        }

        binding.tvRoomName.text = item.name

        val locationText = listOfNotNull(
            item.ward.takeIf { it.isNotBlank() },
            item.province.takeIf { it.isNotBlank() }
        ).joinToString(", ").ifBlank { "Đang cập nhật địa chỉ" }
        binding.tvLocation.text = locationText

        val parsedArea = item.area.trim().toDoubleOrNull()
        val areaText = if (parsedArea != null) {
            if (parsedArea % 1.0 == 0.0) "${parsedArea.toInt()}" else "$parsedArea"
        } else {
            "25"
        }
        binding.tvAreaTag.text = "$areaText m²"

        binding.tvRentPrice.text = item.rentPrice.toVnd()

        if (item.isFavorited) {
            binding.btnFavorite.setBackgroundResource(R.drawable.bg_heart_btn_default)
            binding.ivFavoriteIcon.setImageResource(CoreUiR.drawable.ic_heart_filled)
            binding.ivFavoriteIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
            binding.btnFavorite.contentDescription = "Bỏ lưu phòng"
        } else {
            binding.btnFavorite.setBackgroundResource(R.drawable.bg_heart_btn_default)
            binding.ivFavoriteIcon.setImageResource(CoreUiR.drawable.ic_heart_outline)
            binding.ivFavoriteIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#6B7280"))
            binding.btnFavorite.contentDescription = "Lưu phòng"
        }

        binding.root.setOnSingleClickListener {
            onRoomClick(item)
        }

        binding.btnFavorite.setOnSingleClickListener {
            onFavoriteClick(item)
        }
    }
}
