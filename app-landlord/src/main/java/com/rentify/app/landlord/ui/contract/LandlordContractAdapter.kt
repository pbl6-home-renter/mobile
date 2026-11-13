package com.rentify.app.landlord.ui.contract

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.contract.ContractListResponseDto
import com.rentify.app.landlord.R
import java.text.NumberFormat
import java.util.Locale

class LandlordContractAdapter(
    private val onContractClick: (ContractListResponseDto) -> Unit
) : ListAdapter<ContractListResponseDto, LandlordContractAdapter.ContractViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_landlord_contract, parent, false)
        return ContractViewHolder(view, onContractClick)
    }

    override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContractViewHolder(
        itemView: View,
        private val onContractClick: (ContractListResponseDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvCode: TextView = itemView.findViewById(R.id.tvContractCode)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvContractStatusBadge)
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvContractRoomName)
        private val tvTenantInfo: TextView = itemView.findViewById(R.id.tvContractTenantInfo)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvContractPrice)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvContractDuration)

        fun bind(item: ContractListResponseDto) {
            tvCode.text = item.contractCode.ifEmpty { "HĐ-${item.id.take(6)}" }
            tvRoomName.text = "Phòng ${item.roomName}"

            val tenantName = item.tenantName ?: "Chưa rõ tên"
            val tenantPhone = item.tenantPhone ?: "Chưa rõ SĐT"
            tvTenantInfo.text = "Khách thuê: $tenantName • $tenantPhone"

            val priceFormatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(item.rentPrice.toLong())
            val depositFormatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(item.depositAmount.toLong())
            tvPrice.text = "$priceFormatted đ/tháng • Cọc: $depositFormatted đ"

            tvDuration.text = "${item.startDate} → ${item.endDate}"

            val (badgeText, textColor, bgColor) = when (item.status.lowercase()) {
                "active" -> Triple("Đang hiệu lực", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
                "signed" -> Triple("Đã ký", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
                "draft" -> Triple("Nháp", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
                "suspended" -> Triple("Tạm dừng", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                "expired" -> Triple("Hết hạn", Color.parseColor("#7C3AED"), Color.parseColor("#F3E8FF"))
                "terminated" -> Triple("Đã thanh lý", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
                else -> Triple(item.status, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            }

            tvStatusBadge.text = badgeText
            tvStatusBadge.setTextColor(textColor)
            tvStatusBadge.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 16f
            }

            itemView.setOnClickListener {
                onContractClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ContractListResponseDto>() {
        override fun areItemsTheSame(
            oldItem: ContractListResponseDto,
            newItem: ContractListResponseDto
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ContractListResponseDto,
            newItem: ContractListResponseDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}

