package com.rentify.app.landlord.ui.billing

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.landlord.databinding.ItemLandlordInvoiceBinding
import java.text.NumberFormat
import java.util.Locale

class LandlordInvoiceAdapter(
    private val onItemClick: (InvoiceListResponseDto) -> Unit
) : ListAdapter<InvoiceListResponseDto, LandlordInvoiceAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLandlordInvoiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLandlordInvoiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InvoiceListResponseDto) {
            binding.tvInvoiceCode.text = item.code.ifBlank { "HĐ-${item.id.take(8)}" }
            binding.tvRoomName.text = item.roomName.ifBlank { "Phòng" }
            binding.tvTenantName.text = item.tenantName ?: "Chưa có tên"
            binding.tvPeriod.text = "Kỳ: ${item.period}"

            val formattedAmount = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(item.totalAmountLong)
            binding.tvTotalAmount.text = "$formattedAmount ₫"

            val (statusText, textColor, bgColor) = when (item.invoiceStatus.lowercase()) {
                "pending" -> Triple("Chờ phát hành", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                "issued" -> Triple("Đã phát hành", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
                "paid" -> Triple("Đã thanh toán", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
                "partially_paid" -> Triple("Một phần", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                "overdue" -> Triple("Quá hạn", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
                "void" -> Triple("Đã hủy", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
                else -> Triple(item.invoiceStatus, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            }

            binding.tvInvoiceStatusBadge.text = statusText
            binding.tvInvoiceStatusBadge.setTextColor(textColor)
            binding.tvInvoiceStatusBadge.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 16f
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<InvoiceListResponseDto>() {
        override fun areItemsTheSame(
            oldItem: InvoiceListResponseDto,
            newItem: InvoiceListResponseDto
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: InvoiceListResponseDto,
            newItem: InvoiceListResponseDto
        ): Boolean = oldItem == newItem
    }
}
