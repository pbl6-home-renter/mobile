package com.rentify.app.tenant.ui.explore

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import coil.load
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.network.dto.room.RoomHubInvoiceDto
import com.rentify.app.core.ui.extension.toDisplayDate
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ItemMyRoomCardBinding
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object MyRoomCardBinder {

    fun bind(
        binding: ItemMyRoomCardBinding,
        hub: MyRoomHubResponseDto,
        onCardClick: (MyRoomHubResponseDto) -> Unit,
        onPayInvoice: (MyRoomHubResponseDto, RoomHubInvoiceDto) -> Unit,
        onContactLandlord: (MyRoomHubResponseDto) -> Unit,
        onReportIssue: (MyRoomHubResponseDto) -> Unit
    ) {
        // 1. Header gradient
        binding.tvRoomName.text = "${hub.room.name} · ${hub.room.buildingName}"
        binding.tvRoomAddress.text = hub.room.address
        binding.tvRoomLandlord.text = hub.landlord?.let { "Chủ trọ: ${it.name}" }.orEmpty()
        binding.tvRoomLandlord.visibility = if (hub.landlord != null) View.VISIBLE else View.GONE

        val photo = RoomForYouAdapter.resolveImageUrl(hub.room.photoUrls.firstOrNull(), 0)
        binding.ivRoomThumbnail.load(photo) {
            crossfade(true)
            placeholder(com.rentify.app.core.ui.R.drawable.ic_image_placeholder)
            error(com.rentify.app.core.ui.R.drawable.ic_image_placeholder)
        }

        // 2. Dòng hợp đồng
        binding.tvContractEndDate.text = "Hợp đồng đến ${hub.contract.endDate.toDisplayDate()}"
        val progressPercent = calculateContractProgress(hub.contract.startDate, hub.contract.endDate)
        binding.progressContract.progress = progressPercent

        // 3. Hàng hóa đơn (6 trạng thái)
        val invoice = hub.currentInvoice
        if (invoice == null) {
            // Trạng thái 6: Chưa có hóa đơn tháng này
            binding.layoutInvoiceInfo.visibility = View.GONE
            binding.containerInvoiceAction.visibility = View.GONE
            binding.layoutNoInvoice.visibility = View.VISIBLE
        } else {
            binding.layoutNoInvoice.visibility = View.GONE
            binding.layoutInvoiceInfo.visibility = View.VISIBLE
            binding.containerInvoiceAction.visibility = View.VISIBLE

            binding.tvInvoiceLabel.text = "Hóa đơn ${invoice.period}"
            binding.tvInvoiceAmount.text = invoice.totalAmount.toVnd()

            val isPaid = invoice.invoiceStatus.equals("paid", ignoreCase = true) || invoice.remainingAmount <= 0
            val isPendingConfirm = invoice.invoiceStatus.equals("pending_confirmation", ignoreCase = true) ||
                    invoice.invoiceStatus.equals("pending", ignoreCase = true)

            when {
                // Trạng thái 5: Đã thanh toán
                isPaid -> {
                    binding.tvInvoiceAmount.setTextColor(Color.parseColor("#9CA3AF"))
                    binding.ivInvoiceStatusIcon.visibility = View.VISIBLE
                    binding.ivInvoiceStatusIcon.setImageResource(com.rentify.app.core.ui.R.drawable.ic_check_circle)
                    binding.ivInvoiceStatusIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#15803D"))

                    binding.tvInvoiceSub.text = "Đã thanh toán ${invoice.dueDate}"
                    binding.tvInvoiceSub.setTextColor(Color.parseColor("#15803D"))
                    binding.tvInvoiceSub.typeface = Typeface.DEFAULT

                    binding.btnPayInvoice.visibility = View.GONE
                    binding.btnInvoiceActionText.visibility = View.VISIBLE
                    binding.btnInvoiceActionText.text = "Xem biên lai"
                    binding.btnInvoiceActionText.setOnClickListener {
                        onPayInvoice(hub, invoice)
                    }
                }
                // Trạng thái 4: Đang chờ chủ trọ xác nhận
                isPendingConfirm -> {
                    binding.tvInvoiceAmount.setTextColor(Color.parseColor("#6B7280"))
                    binding.ivInvoiceStatusIcon.visibility = View.VISIBLE
                    binding.ivInvoiceStatusIcon.setImageResource(R.drawable.ic_clock)
                    binding.ivInvoiceStatusIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#6B7280"))

                    binding.tvInvoiceSub.text = "Đang chờ chủ trọ xác nhận"
                    binding.tvInvoiceSub.setTextColor(Color.parseColor("#6B7280"))
                    binding.tvInvoiceSub.typeface = Typeface.DEFAULT

                    binding.btnPayInvoice.visibility = View.GONE
                    binding.btnInvoiceActionText.visibility = View.VISIBLE
                    binding.btnInvoiceActionText.text = "Xem giao dịch"
                    binding.btnInvoiceActionText.setOnClickListener {
                        onPayInvoice(hub, invoice)
                    }
                }
                // Còn lại: Hóa đơn chưa đóng (Trạng thái 1, 2, 3)
                else -> {
                    binding.ivInvoiceStatusIcon.visibility = View.GONE
                    binding.btnPayInvoice.visibility = View.VISIBLE
                    binding.btnInvoiceActionText.visibility = View.GONE

                    binding.btnPayInvoice.setOnClickListener {
                        onPayInvoice(hub, invoice)
                    }

                    val daysRemaining = calculateDaysRemaining(invoice.dueDate)
                    when {
                        // Trạng thái 3: Quá hạn
                        daysRemaining != null && daysRemaining < 0 -> {
                            val daysOverdue = kotlin.math.abs(daysRemaining)
                            binding.tvInvoiceAmount.setTextColor(Color.parseColor("#DC2626"))
                            binding.tvInvoiceSub.text = "Quá hạn $daysOverdue ngày"
                            binding.tvInvoiceSub.setTextColor(Color.parseColor("#DC2626"))
                            binding.tvInvoiceSub.typeface = Typeface.DEFAULT_BOLD
                        }
                        // Trạng thái 2: Sắp đến hạn (còn <= 3 ngày)
                        daysRemaining != null && daysRemaining in 0..3 -> {
                            binding.tvInvoiceAmount.setTextColor(Color.parseColor("#161B2C"))
                            binding.tvInvoiceSub.text = if (daysRemaining == 0L) "Hôm nay là hạn đóng" else "Còn $daysRemaining ngày"
                            binding.tvInvoiceSub.setTextColor(Color.parseColor("#C2410C"))
                            binding.tvInvoiceSub.typeface = Typeface.DEFAULT
                        }
                        // Trạng thái 1: Chưa đóng, còn hạn
                        else -> {
                            binding.tvInvoiceAmount.setTextColor(Color.parseColor("#161B2C"))
                            binding.tvInvoiceSub.text = "Hạn đóng ${invoice.dueDate}"
                            binding.tvInvoiceSub.setTextColor(Color.parseColor("#595E72"))
                            binding.tvInvoiceSub.typeface = Typeface.DEFAULT
                        }
                    }
                }
            }
        }

        // 4. Hai nút dưới
        binding.btnContactLandlord.setOnClickListener {
            onContactLandlord(hub)
        }

        binding.btnReportIssue.setOnClickListener {
            onReportIssue(hub)
        }

        // 5. Chạm toàn bộ Card mở chi tiết phòng
        binding.cardMyRoomRoot.setOnClickListener {
            onCardClick(hub)
        }
    }

    private fun calculateContractProgress(startDateStr: String, endDateStr: String): Int {
        try {
            val start = parseDate(startDateStr) ?: return 65
            val end = parseDate(endDateStr) ?: return 65
            val today = LocalDate.now()

            val totalDays = ChronoUnit.DAYS.between(start, end)
            if (totalDays <= 0) return 100

            val elapsedDays = ChronoUnit.DAYS.between(start, today)
            val percent = (elapsedDays.toFloat() / totalDays * 100).toInt()
            return percent.coerceIn(5, 100)
        } catch (_: Exception) {
            return 65
        }
    }

    private fun calculateDaysRemaining(dueDateStr: String): Long? {
        try {
            val today = LocalDate.now()
            val due = when {
                dueDateStr.contains("-") -> LocalDate.parse(dueDateStr)
                dueDateStr.contains("/") -> {
                    val parts = dueDateStr.split("/")
                    if (parts.size == 2) {
                        val day = parts[0].toInt()
                        val month = parts[1].toInt()
                        LocalDate.of(today.year, month, day)
                    } else if (parts.size == 3) {
                        val day = parts[0].toInt()
                        val month = parts[1].toInt()
                        val year = parts[2].toInt()
                        LocalDate.of(year, month, day)
                    } else null
                }
                else -> null
            } ?: return null
            return ChronoUnit.DAYS.between(today, due)
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseDate(dateStr: String): LocalDate? {
        return try {
            if (dateStr.contains("-")) {
                LocalDate.parse(dateStr)
            } else if (dateStr.contains("/")) {
                val parts = dateStr.split("/")
                if (parts.size == 3) {
                    LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
