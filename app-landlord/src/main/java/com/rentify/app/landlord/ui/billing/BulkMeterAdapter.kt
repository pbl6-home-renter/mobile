package com.rentify.app.landlord.ui.billing

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.meter.MeterReadingListResponseDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.landlord.databinding.ItemLandlordMeterEntryBinding

data class RoomMeterEntry(
    val roomId: String,
    val roomName: String,
    val status: String,
    var prevElectricityReading: String = "0",
    var prevWaterReading: String = "0",
    var electricityReading: String = "",
    var waterReading: String = ""
)

class BulkMeterAdapter(
    private val onDetailedEntryClick: (RoomMeterEntry) -> Unit
) : RecyclerView.Adapter<BulkMeterAdapter.ViewHolder>() {

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

    fun applyExistingReadings(readings: List<MeterReadingListResponseDto>) {
        val byRoom = readings.groupBy { it.roomId }
        items.forEach { entry ->
            val roomReadings = byRoom[entry.roomId].orEmpty()
            val elec = roomReadings.firstOrNull { it.type.equals("electricity", ignoreCase = true) }
            val water = roomReadings.firstOrNull { it.type.equals("water", ignoreCase = true) }
            if (elec != null) {
                entry.prevElectricityReading = elec.previousReading.ifBlank { "0" }
                entry.electricityReading = elec.currentReading
            } else {
                entry.prevElectricityReading = "0"
            }
            if (water != null) {
                entry.prevWaterReading = water.previousReading.ifBlank { "0" }
                entry.waterReading = water.currentReading
            } else {
                entry.prevWaterReading = "0"
            }
        }
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
                "occupied", "rented" -> Triple("Đang thuê", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
                "available" -> Triple("Phòng trống", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
                "maintenance" -> Triple("Đang bảo trì", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                else -> Triple(entry.status, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            }

            binding.tvRoomStatus.text = statusText
            binding.tvRoomStatus.setTextColor(textColor)
            binding.tvRoomStatus.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 16f
            }

            binding.tvPrevElectricity.text = "Chỉ số cũ: ${entry.prevElectricityReading} kWh"
            binding.tvPrevWater.text = "Chỉ số cũ: ${entry.prevWaterReading} m³"

            if (binding.etElectricityReading.text?.toString() != entry.electricityReading) {
                binding.etElectricityReading.setText(entry.electricityReading)
            }
            if (binding.etWaterReading.text?.toString() != entry.waterReading) {
                binding.etWaterReading.setText(entry.waterReading)
            }

            binding.btnDetailedEntry.setOnClickListener {
                onDetailedEntryClick(entry)
            }
        }
    }
}
