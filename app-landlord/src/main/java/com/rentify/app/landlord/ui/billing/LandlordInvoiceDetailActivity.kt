package com.rentify.app.landlord.ui.billing

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentRecordCashRequestDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.ActivityLandlordInvoiceDetailBinding
import com.rentify.app.landlord.databinding.BottomSheetRecordCashBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LandlordInvoiceDetailActivity : BaseActivity<ActivityLandlordInvoiceDetailBinding>(
    ActivityLandlordInvoiceDetailBinding::inflate
) {
    private val billingViewModel: BillingViewModel by viewModels {
        BillingViewModelFactory(this)
    }

    private var invoiceId: String? = null
    private var currentInvoiceDetail: InvoiceDetailResponseDto? = null

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        invoiceId = intent.getStringExtra(EXTRA_INVOICE_ID)
        if (invoiceId.isNullOrBlank()) {
            Toast.makeText(this, "Không tìm thấy mã hóa đơn", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        billingViewModel.loadInvoiceDetail(invoiceId!!)
    }

    private fun renderDetail(detail: InvoiceDetailResponseDto) {
        currentInvoiceDetail = detail
        val context = this
        val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

        binding.tvInvoiceCode.text = detail.code.ifBlank { "HĐ-${detail.id.take(8)}" }
        binding.tvPeriod.text = "Kỳ thanh toán: ${detail.period}"
        binding.tvRoomName.text = detail.roomName.ifBlank { "Phòng" }
        binding.tvTenantName.text = detail.tenantName ?: "Chưa có tên"
        binding.tvDueDate.text = detail.dueDate.ifBlank { "Chưa cập nhật" }

        // Status badge
        val (statusText, textColor, bgColor) = when (detail.invoiceStatus.lowercase()) {
            "pending" -> Triple("Chờ phát hành", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "issued" -> Triple("Đã phát hành", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
            "paid" -> Triple("Đã thanh toán", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
            "partially_paid" -> Triple("Một phần", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "overdue" -> Triple("Quá hạn", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
            "void" -> Triple("Đã hủy", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            else -> Triple(detail.invoiceStatus, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
        }

        binding.tvStatusBadge.text = statusText
        binding.tvStatusBadge.setTextColor(textColor)
        binding.tvStatusBadge.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 16f
        }

        // Financials
        binding.tvRentAmount.text = "${numberFormat.format(detail.rentAmountLong)} ₫"
        binding.tvTotalAmount.text = "${numberFormat.format(detail.totalAmountLong)} ₫"
        binding.tvPaidAmount.text = "${numberFormat.format(detail.paidAmount)} ₫"
        binding.tvRemainingAmount.text = "${numberFormat.format(detail.remainingAmount)} ₫"

        // Render Services
        binding.layoutServicesContainer.removeAllViews()
        detail.effectiveServices.forEach { service ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            }

            val tvName = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = if (!service.usage.isNullOrBlank() && service.unit.isNotBlank()) {
                    "${service.name} (${service.usage} ${service.unit})"
                } else {
                    service.name
                }
                setTextColor(getColor(com.rentify.app.core.ui.R.color.rentify_text_secondary))
                textSize = 14f
            }

            val tvAmount = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "${numberFormat.format(service.amount)} ₫"
                setTextColor(getColor(com.rentify.app.core.ui.R.color.rentify_text_primary))
                textSize = 14f
            }

            row.addView(tvName)
            row.addView(tvAmount)
            binding.layoutServicesContainer.addView(row)
        }

        // Render Other Fees
        binding.layoutOtherFeesContainer.removeAllViews()
        detail.otherFees.forEach { fee ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            }

            val tvName = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = fee.name
                setTextColor(getColor(com.rentify.app.core.ui.R.color.rentify_text_secondary))
                textSize = 14f
            }

            val tvAmount = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "${numberFormat.format(fee.amountLong)} ₫"
                setTextColor(getColor(com.rentify.app.core.ui.R.color.rentify_text_primary))
                textSize = 14f
            }

            row.addView(tvName)
            row.addView(tvAmount)
            binding.layoutOtherFeesContainer.addView(row)
        }

        // Payment History
        binding.layoutPaymentsContainer.removeAllViews()
        if (detail.payments.isNotEmpty()) {
            binding.cardPaymentHistory.visibility = View.VISIBLE
            detail.payments.forEach { payment ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 16
                    }
                }

                val headerRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val tvMethod = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = "${payment.paymentMethod.uppercase()} - ${payment.paidAt.take(10)}"
                    setTextColor(getColor(com.rentify.app.core.ui.R.color.rentify_text_primary))
                    textSize = 13f
                }

                val tvAmount = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = "+${numberFormat.format(payment.amount)} ₫"
                    setTextColor(getColor(com.rentify.app.core.ui.R.color.rentify_success))
                    textSize = 14f
                }

                headerRow.addView(tvMethod)
                headerRow.addView(tvAmount)
                row.addView(headerRow)
                binding.layoutPaymentsContainer.addView(row)
            }
        } else {
            binding.cardPaymentHistory.visibility = View.GONE
        }

        // Void Reason
        if (detail.invoiceStatus == "void") {
            binding.cardVoidReason.visibility = View.VISIBLE
            binding.tvVoidReason.text = detail.voidReason ?: "Không có lý do chi tiết"
        } else {
            binding.cardVoidReason.visibility = View.GONE
        }

        // Bottom Actions Setup
        when (detail.invoiceStatus) {
            "pending" -> {
                binding.cardBottomActions.visibility = View.VISIBLE
                binding.btnIssueInvoice.visibility = View.VISIBLE
                binding.btnRecordCash.visibility = View.GONE
                binding.btnCancelInvoice.visibility = View.GONE

                binding.btnIssueInvoice.setOnClickListener {
                    billingViewModel.issueInvoices(listOf(detail.id))
                }
            }
            "issued", "partially_paid", "overdue" -> {
                binding.cardBottomActions.visibility = View.VISIBLE
                binding.btnIssueInvoice.visibility = View.GONE
                binding.btnRecordCash.visibility = View.VISIBLE
                binding.btnCancelInvoice.visibility = View.VISIBLE

                binding.btnRecordCash.setOnClickListener {
                    showRecordCashBottomSheet(detail)
                }
                binding.btnCancelInvoice.setOnClickListener {
                    showCancelInvoiceDialog(detail.id)
                }
            }
            else -> {
                binding.cardBottomActions.visibility = View.GONE
            }
        }
    }

    private fun showRecordCashBottomSheet(detail: InvoiceDetailResponseDto) {
        val bottomSheet = BottomSheetDialog(this)
        val sheetBinding = BottomSheetRecordCashBinding.inflate(LayoutInflater.from(this))
        bottomSheet.setContentView(sheetBinding.root)

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        sheetBinding.etCashAmount.setText(detail.remainingAmount.toString())
        sheetBinding.etCashPayerName.setText(detail.tenantName ?: "")
        sheetBinding.etCashPaymentDate.setText(currentDate)

        sheetBinding.btnConfirmCash.setOnClickListener {
            val amountStr = sheetBinding.etCashAmount.text?.toString()?.trim() ?: ""
            val amount = amountStr.toLongOrNull() ?: 0L
            val payerName = sheetBinding.etCashPayerName.text?.toString()?.trim() ?: ""
            val paymentDate = sheetBinding.etCashPaymentDate.text?.toString()?.trim() ?: ""
            val note = sheetBinding.etCashNote.text?.toString()?.trim()

            if (amount <= 0) {
                Toast.makeText(this, "Số tiền thanh toán phải lớn hơn 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (payerName.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập tên người nộp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (paymentDate.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập ngày thu tiền", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = PaymentRecordCashRequestDto(
                invoiceId = detail.id,
                amount = amount,
                paymentDate = paymentDate,
                payerName = payerName,
                transactionId = "CASH-${UUID.randomUUID().toString().take(8)}",
                note = note
            )

            bottomSheet.dismiss()
            billingViewModel.recordCashPayment(request)
        }

        bottomSheet.show()
    }

    private fun showCancelInvoiceDialog(invoiceId: String) {
        val input = EditText(this).apply {
            hint = "Nhập lý do hủy hóa đơn..."
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Hủy hóa đơn")
            .setMessage("Hóa đơn sau khi hủy sẽ không thể hoàn tác. Bạn có chắc chắn muốn hủy?")
            .setView(input)
            .setPositiveButton("Hủy hóa đơn") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isBlank()) {
                    Toast.makeText(this, "Vui lòng nhập lý do hủy", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                billingViewModel.cancelInvoice(invoiceId, reason)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    billingViewModel.invoiceDetailState.collect { state ->
                        binding.progressBar.visibility = View.GONE
                        when (state) {
                            is InvoiceDetailUiState.Success -> {
                                renderDetail(state.detail)
                            }
                            is InvoiceDetailUiState.Error -> {
                                Toast.makeText(this@LandlordInvoiceDetailActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                            InvoiceDetailUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    billingViewModel.actionState.collect { state ->
                        when (state) {
                            is BillingActionUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is BillingActionUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@LandlordInvoiceDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                                billingViewModel.resetActionState()
                                invoiceId?.let { billingViewModel.loadInvoiceDetail(it) }
                            }
                            is BillingActionUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@LandlordInvoiceDetailActivity, state.message, Toast.LENGTH_LONG).show()
                                billingViewModel.resetActionState()
                            }
                            BillingActionUiState.Idle -> {}
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_INVOICE_ID = "extra_invoice_id"
    }
}

