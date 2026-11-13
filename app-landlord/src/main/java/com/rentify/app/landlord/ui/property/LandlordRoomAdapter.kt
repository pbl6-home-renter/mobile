package com.rentify.app.landlord.ui.property

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.landlord.R
import java.text.NumberFormat
import java.util.Locale

class LandlordRoomAdapter(
    private val onRoomClick: (RoomMyListResponseDto) -> Unit,
    private val onMarkReadyClick: (RoomMyListResponseDto) -> Unit
) : ListAdapter<RoomMyListResponseDto, LandlordRoomAdapter.RoomViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_landlord_room, parent, false)
        return RoomViewHolder(view, onRoomClick, onMarkReadyClick)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RoomViewHolder(
        itemView: View,
        private val onRoomClick: (RoomMyListResponseDto) -> Unit,
        private val onMarkReadyClick: (RoomMyListResponseDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvRoomStatusBadge)
        private val tvRoomFloorArea: TextView = itemView.findViewById(R.id.tvRoomFloorArea)
        private val tvRoomPrice: TextView = itemView.findViewById(R.id.tvRoomPrice)
        private val layoutTenantInfo: LinearLayout = itemView.findViewById(R.id.layoutTenantInfo)
        private val tvTenantName: TextView = itemView.findViewById(R.id.tvTenantName)
        private val tvTenantPhone: TextView = itemView.findViewById(R.id.tvTenantPhone)
        private val layoutActiveIssues: LinearLayout = itemView.findViewById(R.id.layoutActiveIssues)
        private val tvActiveIssues: TextView = itemView.findViewById(R.id.tvActiveIssues)
        private val btnMarkReady: Button = itemView.findViewById(R.id.btnMarkReady)

        fun bind(item: RoomMyListResponseDto) {
            tvRoomName.text = item.name

            val floorStr = item.floorNumber?.let { "Tầng $it" } ?: "Chưa rõ tầng"
            val areaStr = item.area?.let { "$it m²" } ?: "Chưa rõ DT"
            tvRoomFloorArea.text = "$floorStr • $areaStr"

            val formattedPrice = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(item.rentPrice)
            tvRoomPrice.text = "$formattedPrice đ/tháng"

            val (badgeText, textColor, bgColor) = when (item.status.lowercase()) {
                "occupied" -> Triple("Đang thuê", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
                "maintenance" -> Triple("Bảo trì", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                else -> Triple("Trống", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
            }

            tvStatusBadge.text = badgeText
            tvStatusBadge.setTextColor(textColor)
            tvStatusBadge.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 16f
            }

            if (item.status.equals("occupied", ignoreCase = true) && !item.currentTenantName.isNullOrBlank()) {
                layoutTenantInfo.visibility = View.VISIBLE
                tvTenantName.text = "Khách thuê: ${item.currentTenantName}"
                tvTenantPhone.text = "SĐT: ${item.currentTenantPhone ?: "Chưa cập nhật"}"
            } else {
                layoutTenantInfo.visibility = View.GONE
            }

            if (item.activeIssuesCount > 0) {
                layoutActiveIssues.visibility = View.VISIBLE
                tvActiveIssues.text = "⚠️ Có ${item.activeIssuesCount} sự cố chưa xử lý"
            } else {
                layoutActiveIssues.visibility = View.GONE
            }

            if (item.status.equals("maintenance", ignoreCase = true)) {
                btnMarkReady.visibility = View.VISIBLE
                btnMarkReady.setOnClickListener {
                    onMarkReadyClick(item)
                }
            } else {
                btnMarkReady.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onRoomClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RoomMyListResponseDto>() {
        override fun areItemsTheSame(
            oldItem: RoomMyListResponseDto,
            newItem: RoomMyListResponseDto
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: RoomMyListResponseDto,
            newItem: RoomMyListResponseDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}

