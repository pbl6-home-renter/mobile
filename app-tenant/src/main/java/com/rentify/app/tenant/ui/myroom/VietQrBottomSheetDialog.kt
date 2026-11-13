package com.rentify.app.tenant.ui.myroom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.BottomSheetVietQrBinding

class VietQrBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetVietQrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetVietQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val invoiceCode = arguments?.getString(ARG_INVOICE_CODE).orEmpty()
        val period = arguments?.getString(ARG_PERIOD).orEmpty()
        val amount = arguments?.getLong(ARG_AMOUNT) ?: 0L
        val qrUrl = arguments?.getString(ARG_QR_URL)

        binding.tvQrInvoiceCode.text = "Hóa đơn: $invoiceCode"
        binding.tvQrInvoicePeriod.text = "Kỳ: $period"
        binding.tvQrInvoiceAmount.text = amount.toVnd()

        if (!qrUrl.isNullOrBlank()) {
            binding.pbQrLoading.isVisible = true
            binding.ivQrCodeImage.load(qrUrl) {
                crossfade(true)
                listener(
                    onSuccess = { _, _ -> binding.pbQrLoading.isVisible = false },
                    onError = { _, _ -> binding.pbQrLoading.isVisible = false }
                )
            }
        } else {
            binding.ivQrCodeImage.setImageResource(CoreR.drawable.ic_qr_code)
        }

        binding.btnOpenQrUrl.isVisible = !qrUrl.isNullOrBlank()
        binding.btnOpenQrUrl.setOnSingleClickListener {
            if (!qrUrl.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrUrl))
                startActivity(intent)
            }
        }

        binding.btnCloseQr.setOnSingleClickListener { dismiss() }
        binding.btnDonePayment.setOnSingleClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INVOICE_CODE = "arg_invoice_code"
        private const val ARG_PERIOD = "arg_period"
        private const val ARG_AMOUNT = "arg_amount"
        private const val ARG_QR_URL = "arg_qr_url"

        fun newInstance(
            invoiceCode: String,
            period: String,
            amount: Long,
            qrUrl: String?
        ): VietQrBottomSheetDialog {
            return VietQrBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_INVOICE_CODE, invoiceCode)
                    putString(ARG_PERIOD, period)
                    putLong(ARG_AMOUNT, amount)
                    putString(ARG_QR_URL, qrUrl)
                }
            }
        }
    }
}
