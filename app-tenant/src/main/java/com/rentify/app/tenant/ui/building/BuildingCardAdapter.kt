package com.rentify.app.tenant.ui.building

import android.view.ViewGroup
import coil.load
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.data.repository.BuildingSummary
import com.rentify.app.tenant.databinding.ItemBuildingCardBinding
import com.rentify.app.tenant.ui.explore.RoomForYouAdapter

class BuildingCardAdapter(
    private val fullWidth: Boolean = false,
    private val onBuildingClick: (BuildingSummary) -> Unit
) : BaseListAdapter<BuildingSummary, ItemBuildingCardBinding>(
    inflate = ItemBuildingCardBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemBuildingCardBinding, item: BuildingSummary, position: Int) {
        if (fullWidth) {
            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.marginEnd = 0
            params.bottomMargin = binding.root.resources.getDimensionPixelSize(CoreUiR.dimen.spacing_smd)
            binding.root.layoutParams = params
        }

        binding.ivBuildingCover.load(RoomForYouAdapter.resolveImageUrl(item.coverUrl, position)) {
            crossfade(true)
            placeholder(CoreUiR.drawable.ic_image_placeholder)
            error(CoreUiR.drawable.ic_image_placeholder)
        }

        binding.tvBuildingName.text = item.name
        binding.tvBuildingLocation.text = BuildingTextFormatter.location(item)
        binding.tvBuildingPrice.text = "Từ ${item.minRentPrice.toVnd()}"
        binding.tvBuildingRoomCount.text = "· ${item.roomCount} phòng"

        binding.root.setOnSingleClickListener { onBuildingClick(item) }
    }
}
