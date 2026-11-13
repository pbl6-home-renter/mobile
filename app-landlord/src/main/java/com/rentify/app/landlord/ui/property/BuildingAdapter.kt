package com.rentify.app.landlord.ui.property

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.landlord.R

class BuildingAdapter(
    private val onItemClick: (BuildingMyListResponseDto) -> Unit
) : ListAdapter<BuildingMyListResponseDto, BuildingAdapter.BuildingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_landlord_building, parent, false)
        return BuildingViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BuildingViewHolder(
        itemView: View,
        private val onItemClick: (BuildingMyListResponseDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvBuildingName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvBuildingAddress)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val tvOccupiedRooms: TextView = itemView.findViewById(R.id.tvOccupiedRooms)
        private val tvOccupancyRate: TextView = itemView.findViewById(R.id.tvOccupancyRate)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        private val layoutRejection: LinearLayout = itemView.findViewById(R.id.layoutRejection)
        private val tvRejectionReason: TextView = itemView.findViewById(R.id.tvRejectionReason)

        fun bind(item: BuildingMyListResponseDto) {
            tvName.text = item.name
            tvAddress.text = item.address
            tvOccupiedRooms.text = "${item.occupiedRooms} phòng đang thuê"
            tvOccupancyRate.text = "Tỷ lệ lấp đầy: ${item.occupancyRate}"

            if (item.createdAt.length >= 10) {
                tvCreatedAt.text = item.createdAt.substring(0, 10)
            } else {
                tvCreatedAt.text = item.createdAt
            }

            // Status Badge configuration
            val (badgeText, textColor, bgColor) = when (item.status.lowercase()) {
                "approved" -> Triple("Đã duyệt", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
                "rejected" -> Triple("Bị từ chối", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
                else -> Triple("Chờ duyệt", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            }

            tvStatusBadge.text = badgeText
            tvStatusBadge.setTextColor(textColor)
            val badgeBg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 16f
            }
            tvStatusBadge.background = badgeBg

            if (item.status.equals("rejected", ignoreCase = true) && !item.rejectionReason.isNullOrBlank()) {
                layoutRejection.visibility = View.VISIBLE
                tvRejectionReason.text = item.rejectionReason
                val rejectionBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#FEE2E2"))
                    cornerRadius = 12f
                }
                layoutRejection.background = rejectionBg
            } else {
                layoutRejection.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<BuildingMyListResponseDto>() {
        override fun areItemsTheSame(
            oldItem: BuildingMyListResponseDto,
            newItem: BuildingMyListResponseDto
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: BuildingMyListResponseDto,
            newItem: BuildingMyListResponseDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}
