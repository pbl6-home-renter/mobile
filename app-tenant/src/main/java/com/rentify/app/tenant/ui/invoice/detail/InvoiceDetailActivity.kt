package com.rentify.app.tenant.ui.invoice.detail

import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toDisplayDate
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.ActivityInvoiceDetailBinding
import com.rentify.app.tenant.ui.invoice.InvoiceUiHelper
import com.rentify.app.tenant.ui.myroom.VietQrBottomSheetDialog
import kotlinx.coroutines.launch

class InvoiceDetailActivity : BaseActivity<ActivityInvoiceDetailBinding>(ActivityInvoiceDetailBinding::inflate) {

    private val viewModel: InvoiceDetailViewModel by viewModels {
        InvoiceDetailViewModelFactory(this)
    }

    private lateinit var serviceAdapter: InvoiceServiceAdapter
    private var currentInvoiceId: String = ""

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        currentInvoiceId = intent.getStringExtra(EXTRA_INVOICE_ID).orEmpty()
        if (currentInvoiceId.isBlank()) {
            toast("Không tìm thấy mã hóa đơn")
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadInvoiceDetail(currentInvoiceId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        serviceAdapter = InvoiceServiceAdapter()
        binding.rvServices.apply {
            layoutManager = LinearLayoutManager(this@InvoiceDetailActivity)
            adapter = serviceAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.pbLoading.isVisible = true
                            binding.layoutContent.isVisible = false
                            binding.layoutBottomPayBar.isVisible = false
                        }
                        is UiState.Error -> {
                            binding.pbLoading.isVisible = false
                            toast(state.message)
                        }
                        is UiState.Success -> {
                            binding.pbLoading.isVisible = false
                            binding.layoutContent.isVisible = true
                            bindData(state.data)
                        }
                        is UiState.Idle -> Unit
                    }
                }
            }
        }
    }

    private fun bindData(detail: InvoiceDetailResponseDto) {
        binding.tvPeriod.text = InvoiceUiHelper.formatPeriod(detail.period)
        binding.tvInvoiceCode.text = "Mã hóa đơn: ${detail.code}"

        val statusText = InvoiceUiHelper.getInvoiceStatusText(detail.invoiceStatus, detail.remainingAmount, detail.dueDate)
        val statusColor = InvoiceUiHelper.getInvoiceStatusColor(this, detail.invoiceStatus, detail.remainingAmount, detail.dueDate)
        binding.tvStatusBadge.text = statusText
        binding.tvStatusBadge.setTextColor(statusColor)

        binding.tvRoomName.text = if (detail.roomName.isNotEmpty()) detail.roomName else "Phòng trọ"
        binding.tvTenantName.text = if (!detail.tenantName.isNullOrBlank()) detail.tenantName else "--"
        binding.tvDueDate.text = detail.dueDate.toDisplayDate()

        // 1. Rent amount
        binding.tvRentAmount.text = detail.rentAmountLong.toVnd()

        // 2. Services list
        serviceAdapter.submitList(detail.effectiveServices)

        // 3. Other fees
        if (detail.otherFees.isNotEmpty()) {
            binding.layoutOtherFees.isVisible = true
            binding.layoutOtherFeesContainer.removeAllViews()
            detail.otherFees.forEach { fee ->
                val feeTextView = TextView(this).apply {
                    val desc = if (!fee.description.isNullOrBlank()) " (${fee.description})" else ""
                    text = "${fee.name}$desc: ${fee.amountLong.toVnd()}"
                    setTextColor(ContextCompat.getColor(this@InvoiceDetailActivity, CoreR.color.rentify_text_primary))
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                }
                binding.layoutOtherFeesContainer.addView(feeTextView)
            }
        } else {
            binding.layoutOtherFees.isVisible = false
        }

        // 4. Financial summary
        binding.tvTotalAmount.text = detail.totalAmountLong.toVnd()
        binding.tvPaidAmount.text = detail.paidAmount.toVnd()
        binding.tvRemainingAmount.text = detail.remainingAmount.toVnd()

        // 5. Payment history of this invoice
        if (detail.payments.isNotEmpty()) {
            binding.layoutInvoicePayments.isVisible = true
            binding.layoutInvoicePaymentsList.removeAllViews()
            detail.payments.forEach { payment ->
                val methodStr = InvoiceUiHelper.getPaymentMethodText(payment.paymentMethod)
                val statusStr = InvoiceUiHelper.getPaymentStatusText(payment.paymentStatus)
                val payTextView = TextView(this).apply {
                    text = "• $methodStr: ${payment.amount.toVnd()} - $statusStr (${payment.paidAt.toDisplayDate()})"
                    setTextColor(ContextCompat.getColor(this@InvoiceDetailActivity, CoreR.color.rentify_text_secondary))
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                }
                binding.layoutInvoicePaymentsList.addView(payTextView)
            }
        } else {
            binding.layoutInvoicePayments.isVisible = false
        }

        // 6. Bottom Pay Bar
        val needPayment = detail.remainingAmount > 0 && !detail.invoiceStatus.equals("void", ignoreCase = true)
        binding.layoutBottomPayBar.isVisible = needPayment

        if (needPayment) {
            binding.btnPayVietQr.setOnSingleClickListener {
                val dialog = VietQrBottomSheetDialog.newInstance(
                    invoiceCode = detail.code,
                    period = detail.period,
                    amount = detail.remainingAmount,
                    qrUrl = detail.vietQrPaymentUrl
                )
                dialog.show(supportFragmentManager, "VietQrBottomSheet")
            }
        }
    }

    companion object {
        private const val EXTRA_INVOICE_ID = "extra_invoice_id"

        fun start(context: Context, invoiceId: String) {
            val intent = Intent(context, InvoiceDetailActivity::class.java).apply {
                putExtra(EXTRA_INVOICE_ID, invoiceId)
            }
            context.startActivity(intent)
        }
    }
}
