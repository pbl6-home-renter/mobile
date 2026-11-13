package com.rentify.app.tenant.ui.invoice.list

import com.rentify.app.core.network.dto.invoice.PaymentMyListResponseDto
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.databinding.ItemPaymentHistoryBinding
import com.rentify.app.tenant.ui.invoice.InvoiceUiHelper

class PaymentHistoryAdapter : BaseListAdapter<PaymentMyListResponseDto, ItemPaymentHistoryBinding>(
    inflate = ItemPaymentHistoryBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.id == newItem.id }
) {

    override fun bind(binding: ItemPaymentHistoryBinding, item: PaymentMyListResponseDto, position: Int) {
        val context = binding.root.context

        binding.tvPaymentMethod.text = InvoiceUiHelper.getPaymentMethodText(item.method)
        binding.tvTransactionId.text = if (!item.transactionId.isNullOrBlank()) {
            "Mã GD: ${item.transactionId}"
        } else {
            "Giao dịch #${item.id.takeLast(8)}"
        }

        binding.tvPaymentAmount.text = "+${item.amount.toVnd()}"

        val statusText = InvoiceUiHelper.getPaymentStatusText(item.paymentStatus)
        val statusColor = InvoiceUiHelper.getPaymentStatusColor(context, item.paymentStatus)
        binding.tvPaymentStatus.text = statusText
        binding.tvPaymentStatus.setTextColor(statusColor)
    }
}

