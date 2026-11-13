package com.rentify.app.tenant.ui.contract

import android.graphics.Color
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale

object ContractUiHelper {

    fun applyStatusBadge(textView: TextView, status: String?) {
        when (status?.lowercase()) {
            "active" -> {
                textView.text = "Đang hiệu lực"
                textView.setTextColor(Color.parseColor("#059669")) // Emerald 600
            }
            "signed" -> {
                textView.text = "Đã ký kết"
                textView.setTextColor(Color.parseColor("#2563EB")) // Blue 600
            }
            "expired" -> {
                textView.text = "Hết hạn"
                textView.setTextColor(Color.parseColor("#DC2626")) // Red 600
            }
            "terminated" -> {
                textView.text = "Đã chấm dứt"
                textView.setTextColor(Color.parseColor("#6B7280")) // Gray 500
            }
            "draft" -> {
                textView.text = "Bản nháp"
                textView.setTextColor(Color.parseColor("#D97706")) // Amber 600
            }
            "suspended" -> {
                textView.text = "Tạm dừng"
                textView.setTextColor(Color.parseColor("#7C3AED")) // Purple 600
            }
            else -> {
                textView.text = status ?: "Không rõ"
                textView.setTextColor(Color.parseColor("#6B7280"))
            }
        }
    }

    fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "--"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = input.parse(dateStr) ?: return dateStr
            val output = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            output.format(date)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun getRentalTypeLabel(rentalType: String?): String {
        return when (rentalType?.uppercase()) {
            "MONTHLY" -> "Theo tháng"
            "DAILY" -> "Theo ngày"
            else -> "Theo tháng"
        }
    }
}

