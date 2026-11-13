package com.rentify.app.tenant.ui.invoice.list

import com.rentify.app.core.network.dto.invoice.InvoiceMyListResponseDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toDisplayDate
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.databinding.ItemMyInvoiceBinding
import com.rentify.app.tenant.ui.invoice.InvoiceUiHelper

class MyInvoicesAdapter(
    private val onInvoiceClick: (InvoiceMyListResponseDto) -> Unit
) : BaseListAdapter<InvoiceMyListResponseDto, ItemMyInvoiceBinding>(
    inflate = ItemMyInvoiceBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    init {
        onItemClick = onInvoiceClick
    }

    override fun bind(binding: ItemMyInvoiceBinding, item: InvoiceMyListResponseDto, position: Int) {
        val context = binding.root.context

        binding.tvInvoicePeriod.text = InvoiceUiHelper.formatPeriod(item.period)
        binding.tvRoomName.text = if (item.roomName.isNotEmpty()) "Phòng: ${item.roomName}" else "Phòng trọ"
        binding.tvInvoiceCode.text = item.code

        val statusText = InvoiceUiHelper.getInvoiceStatusText(item.invoiceStatus, item.remainingAmount, item.dueDate)
        val statusColor = InvoiceUiHelper.getInvoiceStatusColor(context, item.invoiceStatus, item.remainingAmount, item.dueDate)
        binding.tvInvoiceStatusBadge.text = statusText
        binding.tvInvoiceStatusBadge.setTextColor(statusColor)

        binding.tvDueDate.text = item.dueDate.toDisplayDate()

        if (item.remainingAmount > 0) {
            binding.tvAmountLabel.text = "Còn nợ"
            binding.tvTotalAmount.text = item.remainingAmount.toVnd()
            binding.tvTotalAmount.setTextColor(statusColor)
        } else {
            binding.tvAmountLabel.text = "Tổng tiền"
            binding.tvTotalAmount.text = item.totalAmountLong.toVnd()
            binding.tvTotalAmount.setTextColor(statusColor)
        }

        binding.root.setOnSingleClickListener {
            onInvoiceClick(item)
        }
    }
}

