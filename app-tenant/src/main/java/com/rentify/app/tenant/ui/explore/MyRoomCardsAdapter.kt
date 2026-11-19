package com.rentify.app.tenant.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.network.dto.room.RoomHubInvoiceDto
import com.rentify.app.tenant.databinding.ItemMyRoomCardBinding

class MyRoomCardsAdapter(
    private val onCardClick: (MyRoomHubResponseDto) -> Unit,
    private val onPayInvoice: (MyRoomHubResponseDto, RoomHubInvoiceDto) -> Unit,
    private val onContactLandlord: (MyRoomHubResponseDto) -> Unit,
    private val onReportIssue: (MyRoomHubResponseDto) -> Unit
) : ListAdapter<MyRoomHubResponseDto, MyRoomCardsAdapter.RoomCardViewHolder>(DiffCallback) {

    inner class RoomCardViewHolder(
        private val binding: ItemMyRoomCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MyRoomHubResponseDto) {
            MyRoomCardBinder.bind(
                binding = binding,
                hub = item,
                onCardClick = onCardClick,
                onPayInvoice = onPayInvoice,
                onContactLandlord = onContactLandlord,
                onReportIssue = onReportIssue
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomCardViewHolder {
        val binding = ItemMyRoomCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RoomCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<MyRoomHubResponseDto>() {
        override fun areItemsTheSame(
            oldItem: MyRoomHubResponseDto,
            newItem: MyRoomHubResponseDto
        ): Boolean {
            return oldItem.room.id == newItem.room.id
        }

        override fun areContentsTheSame(
            oldItem: MyRoomHubResponseDto,
            newItem: MyRoomHubResponseDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}
