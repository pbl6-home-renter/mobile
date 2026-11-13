package com.rentify.app.landlord.ui.billing

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.landlord.databinding.ItemLandlordMeterEntryBinding

data class RoomMeterEntry(
    val roomId: String,
    val roomName: String,
    val status: String,
    var electricityReading: String = "",
    var waterReading: String = ""
)

class BulkMeterAdapter : RecyclerView.Adapter<BulkMeterAdapter.ViewHolder>() {

    private val items = mutableListOf<RoomMeterEntry>()

    fun submitRooms(rooms: List<RoomMyListResponseDto>) {
        items.clear()
        items.addAll(rooms.map { room ->
            RoomMeterEntry(
                roomId = room.id,
                roomName = room.name,
                status = room.status
            )
        })
        notifyDataSetChanged()
    }

    fun getEntries(): List<RoomMeterEntry> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLandlordMeterEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemLandlordMeterEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.etElectricityReading.doAfterTextChanged { s ->
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    items[pos].electricityReading = s?.toString()?.trim() ?: ""
                }
            }

            binding.etWaterReading.doAfterTextChanged { s ->
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    items[pos].waterReading = s?.toString()?.trim() ?: ""
                }
            }
        }

        fun bind(entry: RoomMeterEntry) {
            binding.tvRoomName.text = entry.roomName

            val (statusText, textColor, bgColor) = when (entry.status.lowercase()) {
                "rented" -> Triple("Đang thuê", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
                "available" -> Triple("Còn trống", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
                else -> Triple(entry.status, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            }

            binding.tvRoomStatus.text = statusText
            binding.tvRoomStatus.setTextColor(textColor)
            binding.tvRoomStatus.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 16f
            }

            if (binding.etElectricityReading.text?.toString() != entry.electricityReading) {
                binding.etElectricityReading.setText(entry.electricityReading)
            }
            if (binding.etWaterReading.text?.toString() != entry.waterReading) {
                binding.etWaterReading.setText(entry.waterReading)
            }
        }
    }
}
