package com.rentify.app.landlord.ui.contract

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.contract.ContractMemberListResponseDto
import com.rentify.app.landlord.R

class ContractMemberAdapter(
    private val onMemberLeaveClick: (ContractMemberListResponseDto) -> Unit
) : ListAdapter<ContractMemberListResponseDto, ContractMemberAdapter.MemberViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contract_member, parent, false)
        return MemberViewHolder(view, onMemberLeaveClick)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemberViewHolder(
        itemView: View,
        private val onMemberLeaveClick: (ContractMemberListResponseDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvMemberName)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvMemberStatusBadge)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvMemberPhone)
        private val tvJoinedAt: TextView = itemView.findViewById(R.id.tvMemberJoinedAt)
        private val btnLeave: Button = itemView.findViewById(R.id.btnMemberLeave)

        fun bind(item: ContractMemberListResponseDto) {
            tvName.text = item.tenantName ?: "Thành viên"
            tvPhone.text = "SĐT: ${item.tenantPhone ?: "Chưa có"}"
            tvJoinedAt.text = "Vào ở: ${item.joinedAt}"

            val isActive = item.status.equals("active", ignoreCase = true)
            val (badgeText, textColor, bgColor) = if (isActive) {
                Triple("Đang ở", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
            } else {
                Triple("Đã rời", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            }

            tvStatusBadge.text = badgeText
            tvStatusBadge.setTextColor(textColor)
            tvStatusBadge.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 10f
            }

            if (isActive && !item.tenantId.isNullOrEmpty()) {
                btnLeave.visibility = View.VISIBLE
                btnLeave.setOnClickListener {
                    onMemberLeaveClick(item)
                }
            } else {
                btnLeave.visibility = View.GONE
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ContractMemberListResponseDto>() {
        override fun areItemsTheSame(
            oldItem: ContractMemberListResponseDto,
            newItem: ContractMemberListResponseDto
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ContractMemberListResponseDto,
            newItem: ContractMemberListResponseDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}

