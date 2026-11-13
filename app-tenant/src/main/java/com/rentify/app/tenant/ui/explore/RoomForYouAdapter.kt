package com.rentify.app.tenant.ui.explore

import androidx.core.content.ContextCompat
import coil.load
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.databinding.ItemRoomForYouBinding

class RoomForYouAdapter(
    private val onRoomClick: (RoomPublicListResponseDto) -> Unit,
    private val onFavoriteClick: (RoomPublicListResponseDto) -> Unit
) : BaseListAdapter<RoomPublicListResponseDto, ItemRoomForYouBinding>(
    inflate = ItemRoomForYouBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemRoomForYouBinding, item: RoomPublicListResponseDto, position: Int) {
        val context = binding.root.context

        val photoUrl = resolveImageUrl(item.thumbnailUrl, position)
        binding.ivThumbnail.load(photoUrl) {
            crossfade(true)
            placeholder(CoreUiR.drawable.ic_image_placeholder)
            error(CoreUiR.drawable.ic_image_placeholder)
        }

        binding.tvRoomType.text = item.roomType?.ifBlank { "Phòng thường" } ?: "Phòng thường"
        binding.tvRentPrice.text = item.rentPrice.toVnd()
        binding.tvRoomName.text = item.name

        val parsedArea = item.area.trim().toDoubleOrNull()
        val areaText = if (parsedArea != null) {
            if (parsedArea % 1.0 == 0.0) "${parsedArea.toInt()}" else "$parsedArea"
        } else {
            "20"
        }
        val locationText = if (item.district.isNotBlank()) "P. ${item.district}" else item.province
        binding.tvRoomDetails.text = "$areaText m² · $locationText"

        if (item.isFavorited) {
            binding.ivFavoriteIcon.setImageResource(CoreUiR.drawable.ic_heart_filled)
            binding.ivFavoriteIcon.setColorFilter(ContextCompat.getColor(context, CoreUiR.color.rentify_card_saved_icon))
        } else {
            binding.ivFavoriteIcon.setImageResource(CoreUiR.drawable.ic_heart_outline)
            binding.ivFavoriteIcon.setColorFilter(ContextCompat.getColor(context, CoreUiR.color.rentify_neutral))
        }

        binding.root.setOnSingleClickListener {
            onRoomClick(item)
        }

        binding.btnFavorite.setOnSingleClickListener {
            onFavoriteClick(item)
        }
    }

    companion object {
        // TODO: Xóa bỏ workaround resolveImageUrl khi kết nối Backend production trả về URL ảnh thật
        fun resolveImageUrl(rawUrl: String?, position: Int = 0): String {
            val fallbackPhotos = listOf(
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=600&auto=format&fit=crop"
            )
            val fallback = fallbackPhotos[Math.abs(position) % fallbackPhotos.size]
            if (rawUrl.isNullOrBlank()) return fallback

            val isMockFakerUrl = !rawUrl.contains("unsplash.com") &&
                    !rawUrl.contains("cloudinary.com") &&
                    !rawUrl.contains("firebase") &&
                    !rawUrl.contains(".jpg") &&
                    !rawUrl.contains(".jpeg") &&
                    !rawUrl.contains(".png") &&
                    !rawUrl.contains(".webp")

            return if (isMockFakerUrl) fallback else rawUrl
        }
    }
}
