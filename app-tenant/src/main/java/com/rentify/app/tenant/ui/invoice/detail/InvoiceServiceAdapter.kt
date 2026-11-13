package com.rentify.app.tenant.ui.invoice.detail

import androidx.core.view.isVisible
import com.rentify.app.core.network.dto.invoice.InvoiceServiceItemDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.databinding.ItemInvoiceServiceBinding

class InvoiceServiceAdapter : BaseListAdapter<InvoiceServiceItemDto, ItemInvoiceServiceBinding>(
    inflate = ItemInvoiceServiceBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem ->
        oldItem.name == newItem.name && oldItem.amount == newItem.amount
    }
) {

    override fun bind(binding: ItemInvoiceServiceBinding, item: InvoiceServiceItemDto, position: Int) {
        binding.tvServiceName.text = item.name
        binding.tvServiceAmount.text = item.amount.toVnd()

        val hasReadings = !item.previousReading.isNullOrBlank() && !item.currentReading.isNullOrBlank()
        val unitPriceStr = item.unitPrice?.toVnd() ?: ""

        val detailText = when {
            hasReadings -> {
                val usageStr = if (!item.usage.isNullOrBlank()) "${item.usage} ${item.unit}" else ""
                val priceStr = if (unitPriceStr.isNotEmpty()) " x $unitPriceStr" else ""
                "Chỉ số: ${item.previousReading} → ${item.currentReading} ($usageStr$priceStr)"
            }
            !item.usage.isNullOrBlank() && unitPriceStr.isNotEmpty() -> {
                "Sử dụng: ${item.usage} ${item.unit} x $unitPriceStr"
            }
            item.unit.isNotEmpty() && unitPriceStr.isNotEmpty() -> {
                "Đơn giá: $unitPriceStr / ${item.unit}"
            }
            item.unit.isNotEmpty() -> {
                "Đơn vị: ${item.unit}"
            }
            else -> ""
        }

        binding.tvServiceDetail.isVisible = detailText.isNotEmpty()
        binding.tvServiceDetail.text = detailText
    }
}

