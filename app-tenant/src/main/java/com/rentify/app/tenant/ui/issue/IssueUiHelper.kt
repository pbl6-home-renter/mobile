package com.rentify.app.tenant.ui.issue

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import com.rentify.app.core.ui.R as CoreR
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object IssueUiHelper {

    fun getCategoryLabel(category: String?): String {
        return when (category?.uppercase()) {
            "PLUMBING" -> "Đường nước"
            "ELECTRICAL" -> "Hệ thống điện"
            "APPLIANCE" -> "Thiết bị gia dụng"
            "STRUCTURAL" -> "Cửa / Kết cấu"
            "SECURITY" -> "An ninh / Khóa"
            else -> "Khác"
        }
    }

    fun getCategoryIconResId(category: String?): Int {
        return when (category?.uppercase()) {
            "PLUMBING" -> CoreR.drawable.ic_issue_plumbing
            "ELECTRICAL" -> CoreR.drawable.ic_issue_electrical
            "APPLIANCE" -> CoreR.drawable.ic_issue_appliance
            "STRUCTURAL" -> CoreR.drawable.ic_issue_structural
            "SECURITY" -> CoreR.drawable.ic_issue_security
            else -> CoreR.drawable.ic_issue_other
        }
    }

    fun getUrgencyLabel(urgency: String?): String {
        return when (urgency?.uppercase()) {
            "LOW" -> "Thấp"
            "MEDIUM" -> "Trung bình"
            "HIGH" -> "Cao"
            "EMERGENCY" -> "Khẩn cấp"
            else -> "Trung bình"
        }
    }

    fun applyUrgencyBadge(textView: TextView, urgency: String?) {
        val label = getUrgencyLabel(urgency)
        textView.text = "● $label"
        when (urgency?.uppercase()) {
            "LOW" -> {
                textView.setBackgroundResource(CoreR.drawable.bg_badge_urgency_low)
                textView.setTextColor(Color.parseColor("#059669"))
            }
            "MEDIUM" -> {
                textView.setBackgroundResource(CoreR.drawable.bg_badge_urgency_medium)
                textView.setTextColor(Color.parseColor("#2563EB"))
            }
            "HIGH" -> {
                textView.setBackgroundResource(CoreR.drawable.bg_badge_urgency_high)
                textView.setTextColor(Color.parseColor("#D97706"))
            }
            "EMERGENCY" -> {
                textView.setBackgroundResource(CoreR.drawable.bg_badge_urgency_emergency)
                textView.setTextColor(Color.parseColor("#DC2626"))
            }
            else -> {
                textView.setBackgroundResource(CoreR.drawable.bg_badge_urgency_medium)
                textView.setTextColor(Color.parseColor("#2563EB"))
            }
        }
        val horizontalPadding = dpToPx(textView.context, 8)
        val verticalPadding = dpToPx(textView.context, 3)
        textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
    }

    fun applyStatusBadge(textView: TextView, status: String?) {
        when (status?.lowercase()) {
            "open" -> {
                textView.text = "Chờ tiếp nhận"
                textView.setTextColor(Color.parseColor("#D97706")) // Amber 600
            }
            "in_progress" -> {
                textView.text = "Đang xử lý"
                textView.setTextColor(Color.parseColor("#2563EB")) // Blue 600
            }
            "resolved" -> {
                textView.text = "Đã giải quyết"
                textView.setTextColor(Color.parseColor("#059669")) // Emerald 600
            }
            "cancelled" -> {
                textView.text = "Đã hủy"
                textView.setTextColor(Color.parseColor("#6B7280")) // Gray 500
            }
            else -> {
                textView.text = status ?: "Không rõ"
                textView.setTextColor(Color.parseColor("#6B7280"))
            }
        }
    }

    fun formatDate(isoDateString: String?): String {
        if (isoDateString.isNullOrBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            // Parse prefix without milliseconds/nanoseconds or time zone suffix
            val cleanDate = isoDateString.split(".")[0].replace("Z", "")
            val date = inputFormat.parse(cleanDate) ?: return isoDateString
            val outputFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
            outputFormat.format(date)
        } catch (_: Exception) {
            isoDateString
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
