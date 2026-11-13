package com.rentify.app.tenant.ui.explore

import androidx.core.content.ContextCompat
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.tenant.databinding.ItemPopularAreaBinding

data class PopularAreaItem(
    val id: String,
    val name: String,
    val type: String = "Quận",
    val province: String = "Đà Nẵng",
    val bgColorRes: Int = CoreUiR.color.rentify_card_near_bg
)

class PopularAreaAdapter(
    private val onAreaClick: (PopularAreaItem) -> Unit
) : BaseListAdapter<PopularAreaItem, ItemPopularAreaBinding>(
    inflate = ItemPopularAreaBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemPopularAreaBinding, item: PopularAreaItem, position: Int) {
        binding.tvAreaType.text = item.type
        binding.tvAreaName.text = item.name
        binding.cardArea.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, item.bgColorRes))
        binding.root.setOnClickListener {
            onAreaClick(item)
        }
    }
}
