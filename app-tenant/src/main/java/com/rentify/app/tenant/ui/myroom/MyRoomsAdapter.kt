package com.rentify.app.tenant.ui.myroom

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import android.content.res.ColorStateList
import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.core.ui.base.BaseListAdapter
import com.rentify.app.core.ui.base.simpleDiff
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ItemMyRoomBinding

sealed interface MyRoomFooter {
    data class NoInvoice(val month: Int) : MyRoomFooter
    data class Pending(val month: Int) : MyRoomFooter
    data class Overdue(val month: Int, val days: Int) : MyRoomFooter
    data object AwaitingConfirm : MyRoomFooter
    data class Paid(val month: Int) : MyRoomFooter
    data class Ended(val monthYear: String) : MyRoomFooter
}

data class MyRoomCardItem(
    val contract: ContractMyListResponseDto,
    val footer: MyRoomFooter? = null
)

class MyRoomsAdapter(
    onRoomClick: (ContractMyListResponseDto) -> Unit
) : BaseListAdapter<MyRoomCardItem, ItemMyRoomBinding>(
    inflate = ItemMyRoomBinding::inflate,
    diffCallback = simpleDiff { oldItem, newItem -> oldItem.contract.id == newItem.contract.id }
) {

    init {
        onItemClick = { onRoomClick(it.contract) }
    }

    override fun bind(binding: ItemMyRoomBinding, item: MyRoomCardItem, position: Int) {
        val contract = item.contract
        binding.tvRoomName.text = contract.roomName
        binding.tvBuildingName.text = contract.buildingName
        binding.tvArea.isVisible = false
        bindFooter(binding, item.footer)
    }

    private fun bindFooter(binding: ItemMyRoomBinding, footer: MyRoomFooter?) {
        binding.layoutFooter.isVisible = footer != null
        if (footer == null) return

        val context = binding.root.context
        val resources = context.resources
        val (iconRes, colorRes, text) = when (footer) {
            is MyRoomFooter.NoInvoice -> Triple(
                CoreR.drawable.ic_receipt,
                CoreR.color.rentify_text_muted,
                context.getString(R.string.my_rooms_footer_no_invoice, footer.month)
            )
            is MyRoomFooter.Pending -> Triple(
                CoreR.drawable.ic_clock,
                CoreR.color.rentify_warning_text,
                context.getString(R.string.my_rooms_footer_pending, footer.month)
            )
            is MyRoomFooter.Overdue -> Triple(
                CoreR.drawable.ic_alert_circle,
                CoreR.color.rentify_error,
                resources.getQuantityString(
                    R.plurals.my_rooms_footer_overdue,
                    footer.days,
                    footer.month,
                    footer.days
                )
            )
            is MyRoomFooter.AwaitingConfirm -> Triple(
                CoreR.drawable.ic_hourglass,
                CoreR.color.rentify_icon_slate,
                context.getString(R.string.my_rooms_footer_awaiting_confirm)
            )
            is MyRoomFooter.Paid -> Triple(
                CoreR.drawable.ic_check_circle,
                CoreR.color.rentify_teal,
                context.getString(R.string.my_rooms_footer_paid, footer.month)
            )
            is MyRoomFooter.Ended -> Triple(
                CoreR.drawable.ic_calendar,
                CoreR.color.rentify_text_muted,
                context.getString(R.string.my_rooms_footer_ended, footer.monthYear)
            )
        }

        val color = ContextCompat.getColor(context, colorRes)
        binding.ivFooterIcon.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(binding.ivFooterIcon, ColorStateList.valueOf(color))
        binding.tvFooterText.text = text
        binding.tvFooterText.setTextColor(color)
    }
}
