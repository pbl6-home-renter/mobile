package com.rentify.app.tenant.ui.invoice

import android.content.Context
import androidx.core.content.ContextCompat
import com.rentify.app.core.ui.R as CoreR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceUiHelper {

    fun formatPeriod(period: String?): String {
        if (period.isNullOrBlank()) return "--"
        // Expected format: YYYY-MM
        val parts = period.split("-")
        return if (parts.size == 2) {
            "Kỳ tháng ${parts[1]}/${parts[0]}"
        } else {
            period
        }
    }

    fun getInvoiceStatusText(status: String?, remainingAmount: Long, dueDateStr: String?): String {
        return when (status?.lowercase()) {
            "issued" -> {
                if (remainingAmount <= 0) {
                    "Đã thanh toán"
                } else if (isOverdue(dueDateStr)) {
                    "Quá hạn"
                } else {
                    "Chờ thanh toán"
                }
            }
            "pending" -> "Chờ xuất"
            "void" -> "Đã hủy"
            else -> status ?: "Chưa xác định"
        }
    }

    fun getInvoiceStatusColor(context: Context, status: String?, remainingAmount: Long, dueDateStr: String?): Int {
        return when (status?.lowercase()) {
            "issued" -> {
                if (remainingAmount <= 0) {
                    ContextCompat.getColor(context, CoreR.color.rentify_success)
                } else if (isOverdue(dueDateStr)) {
                    ContextCompat.getColor(context, CoreR.color.rentify_error)
                } else {
                    ContextCompat.getColor(context, CoreR.color.rentify_warning)
                }
            }
            "void" -> ContextCompat.getColor(context, CoreR.color.rentify_neutral)
            else -> ContextCompat.getColor(context, CoreR.color.rentify_text_secondary)
        }
    }

    fun getPaymentMethodText(method: String?): String {
        return when (method?.lowercase()) {
            "vietqr" -> "Chuyển khoản VietQR"
            "vnpay" -> "Cổng VNPAY"
            "momo" -> "Ví điện tử MoMo"
            "cash" -> "Tiền mặt"
            "mock" -> "Thanh toán giả lập"
            else -> method ?: "Khác"
        }
    }

    fun getPaymentStatusText(status: String?): String {
        return when (status?.lowercase()) {
            "success" -> "Thành công"
            "pending" -> "Đang xử lý"
            "failed" -> "Thất bại"
            else -> status ?: "Chưa rõ"
        }
    }

    fun getPaymentStatusColor(context: Context, status: String?): Int {
        return when (status?.lowercase()) {
            "success" -> ContextCompat.getColor(context, CoreR.color.rentify_success)
            "failed" -> ContextCompat.getColor(context, CoreR.color.rentify_error)
            else -> ContextCompat.getColor(context, CoreR.color.rentify_warning)
        }
    }

    private fun isOverdue(dueDateStr: String?): Boolean {
        if (dueDateStr.isNullOrBlank()) return false
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = isoFormat.parse(dueDateStr) ?: return false
            date.before(Date())
        } catch (_: Exception) {
            try {
                val simpleFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = simpleFormat.parse(dueDateStr) ?: return false
                date.before(Date())
            } catch (_: Exception) {
                false
            }
        }
    }
}

