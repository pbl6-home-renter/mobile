package com.rentify.app.tenant.ui.contract.list

import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.databinding.ItemMyContractBinding
import com.rentify.app.tenant.ui.contract.ContractUiHelper

class MyContractsAdapter(
    private val onContractClick: (ContractMyListResponseDto) -> Unit
) : BaseListAdapter<ContractMyListResponseDto, ItemMyContractBinding>(
    inflate = ItemMyContractBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    init {
        onItemClick = onContractClick
    }

    override fun bind(binding: ItemMyContractBinding, item: ContractMyListResponseDto, position: Int) {
        val code = item.contractCode.trim()
        binding.tvContractCode.text = when {
            code.startsWith("HD", ignoreCase = true) -> code
            code.isNotEmpty() -> "HĐ #$code"
            else -> "HĐ thuê phòng"
        }
        ContractUiHelper.applyStatusBadge(binding.tvStatusBadge, item.status)

        val location = buildString {
            if (item.roomName.isNotEmpty()) append(item.roomName)
            if (item.buildingName.isNotEmpty()) {
                if (isNotEmpty()) append(" - ")
                append(item.buildingName)
            }
        }
        binding.tvRoomLocation.text = if (location.isNotEmpty()) location else "Phòng trọ"

        val startDate = ContractUiHelper.formatDate(item.startDate)
        val endDate = ContractUiHelper.formatDate(item.endDate)
        binding.tvDuration.text = "$startDate → $endDate"

        val rentPriceText = item.rentPrice.toLong().toVnd()
        val rentalTypeLabel = ContractUiHelper.getRentalTypeLabel(item.rentalType)
        binding.tvRentPrice.text = "$rentPriceText / $rentalTypeLabel"

        val depositText = item.depositAmount.toLong().toVnd()
        binding.tvDepositAmount.text = "Tiền cọc: $depositText"

        binding.root.setOnSingleClickListener {
            onContractClick(item)
        }
    }
}

