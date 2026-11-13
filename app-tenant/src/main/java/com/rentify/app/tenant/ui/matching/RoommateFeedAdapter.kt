package com.rentify.app.tenant.ui.matching

import androidx.core.view.isVisible
import coil.load
import com.rentify.app.core.network.dto.matching.RoommateProfileListResponseDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.ItemRoommateProfileBinding
import java.time.Year

class RoommateFeedAdapter(
    private val onViewDetailClick: (RoommateProfileListResponseDto) -> Unit,
    private val onSendRequestClick: (RoommateProfileListResponseDto) -> Unit
) : BaseListAdapter<RoommateProfileListResponseDto, ItemRoommateProfileBinding>(
    inflate = ItemRoommateProfileBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemRoommateProfileBinding, item: RoommateProfileListResponseDto, position: Int) {
        if (!item.avatarUrl.isNullOrBlank()) {
            binding.ivAvatar.load(item.avatarUrl) {
                crossfade(true)
                placeholder(CoreR.drawable.ic_user_circle)
                error(CoreR.drawable.ic_user_circle)
            }
        } else {
            binding.ivAvatar.setImageResource(CoreR.drawable.ic_user_circle)
        }

        val ageStr = item.birthYear?.let { ", ${Year.now().value - it} tuổi" } ?: ""
        val genderStr = when (item.gender.lowercase()) {
            "male" -> "Nam"
            "female" -> "Nữ"
            else -> "Khác"
        }
        binding.tvNameAge.text = "${item.tenantName} ($genderStr$ageStr)"
        binding.tvOccupation.text = item.occupation ?: "Chưa cập nhật nghề nghiệp"

        if (item.matchScore != null) {
            binding.layoutAiMatchBadge.isVisible = true
            binding.tvMatchScore.text = "${item.matchScore}% Hợp"
        } else {
            binding.layoutAiMatchBadge.isVisible = false
        }

        val minBudgetStr = item.budgetMin?.toVnd() ?: "0 đ"
        val maxBudgetStr = item.budgetMax?.toVnd() ?: "Thương lượng"
        binding.tvBudget.text = "$minBudgetStr - $maxBudgetStr"

        val district = item.preferredDistrict.orEmpty()
        val area = item.preferredArea.orEmpty()
        binding.tvArea.text = when {
            district.isNotBlank() && area.isNotBlank() -> "$district · $area"
            district.isNotBlank() -> district
            area.isNotBlank() -> area
            else -> "Chưa xác định"
        }

        // Tags
        val tags = item.lifestyleTags
        binding.tvTag1.isVisible = tags.isNotEmpty()
        if (tags.isNotEmpty()) binding.tvTag1.text = tags[0]

        binding.tvTag2.isVisible = tags.size > 1
        if (tags.size > 1) binding.tvTag2.text = tags[1]

        binding.tvTag3.isVisible = tags.size > 2
        if (tags.size > 2) binding.tvTag3.text = tags[2]

        binding.layoutTags.isVisible = tags.isNotEmpty()

        // Bio
        binding.tvBio.isVisible = !item.bio.isNullOrBlank()
        binding.tvBio.text = item.bio

        // Click listeners
        binding.btnViewDetail.setOnSingleClickListener { onViewDetailClick(item) }
        binding.btnSendRequest.setOnSingleClickListener { onSendRequestClick(item) }
    }
}

