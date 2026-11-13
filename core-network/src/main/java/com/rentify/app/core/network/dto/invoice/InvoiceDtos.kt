package com.rentify.app.core.network.dto.invoice

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InvoiceMyListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "code") val code: String = "",
    @field:Json(name = "contractId") val contractId: String = "",
    @field:Json(name = "roomName") val roomName: String = "",
    @field:Json(name = "period") val period: String = "",
    @field:Json(name = "totalAmount") val totalAmount: String = "0",
    @field:Json(name = "paidAmount") val paidAmount: Long = 0L,
    @field:Json(name = "remainingAmount") val remainingAmount: Long = 0L,
    @field:Json(name = "dueDate") val dueDate: String = "",
    @field:Json(name = "invoiceStatus") val invoiceStatus: String = "issued",
    @field:Json(name = "issuedAt") val issuedAt: String? = null
) {
    val totalAmountLong: Long
        get() = totalAmount.toDoubleOrNull()?.toLong() ?: 0L
}

@JsonClass(generateAdapter = true)
data class InvoiceTierDetailDto(
    @field:Json(name = "step") val step: Int = 1,
    @field:Json(name = "from") val from: Int = 0,
    @field:Json(name = "to") val to: Int? = null,
    @field:Json(name = "price") val price: String = "0",
    @field:Json(name = "quantity") val quantity: String = "0",
    @field:Json(name = "amount") val amount: String = "0"
)

@JsonClass(generateAdapter = true)
data class InvoiceBreakdownItemDto(
    @field:Json(name = "type") val type: String = "",
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "rateKind") val rateKind: String = "",
    @field:Json(name = "unitPrice") val unitPrice: String? = null,
    @field:Json(name = "quantity") val quantity: String = "1",
    @field:Json(name = "unit") val unit: String = "",
    @field:Json(name = "serviceStart") val serviceStart: String? = null,
    @field:Json(name = "serviceEnd") val serviceEnd: String? = null,
    @field:Json(name = "prorateRatio") val prorateRatio: String? = null,
    @field:Json(name = "amount") val amount: String = "0",
    @field:Json(name = "tierDetail") val tierDetail: List<InvoiceTierDetailDto> = emptyList()
) {
    val amountLong: Long
        get() = amount.toDoubleOrNull()?.toLong() ?: 0L
    val unitPriceLong: Long?
        get() = unitPrice?.toDoubleOrNull()?.toLong()
}

@JsonClass(generateAdapter = true)
data class InvoiceServiceItemDto(
    @field:Json(name = "category") val category: String = "OTHER",
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "rateKind") val rateKind: String? = null,
    @field:Json(name = "previousReading") val previousReading: String? = null,
    @field:Json(name = "currentReading") val currentReading: String? = null,
    @field:Json(name = "usage") val usage: String? = null,
    @field:Json(name = "unit") val unit: String = "",
    @field:Json(name = "unitPrice") val unitPrice: Long? = null,
    @field:Json(name = "amount") val amount: Long = 0L
)

@JsonClass(generateAdapter = true)
data class InvoiceOtherFeeItemDto(
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "amount") val amount: String = "0",
    @field:Json(name = "description") val description: String? = null
) {
    val amountLong: Long
        get() = amount.toDoubleOrNull()?.toLong() ?: 0L
}

@JsonClass(generateAdapter = true)
data class InvoicePaymentItemDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "paymentMethod") val paymentMethod: String = "",
    @field:Json(name = "amount") val amount: Long = 0L,
    @field:Json(name = "paymentStatus") val paymentStatus: String = "",
    @field:Json(name = "paidAt") val paidAt: String = "",
    @field:Json(name = "payerName") val payerName: String? = null,
    @field:Json(name = "transactionId") val transactionId: String? = null
)

@JsonClass(generateAdapter = true)
data class InvoiceDetailResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "code") val code: String = "",
    @field:Json(name = "contractId") val contractId: String = "",
    @field:Json(name = "roomId") val roomId: String = "",
    @field:Json(name = "roomName") val roomName: String = "",
    @field:Json(name = "tenantName") val tenantName: String? = null,
    @field:Json(name = "tenantPhone") val tenantPhone: String? = null,
    @field:Json(name = "period") val period: String = "",
    @field:Json(name = "rentAmount") val rentAmount: String = "0",
    @field:Json(name = "breakdown") val breakdown: List<InvoiceBreakdownItemDto> = emptyList(),
    @field:Json(name = "services") val services: List<InvoiceServiceItemDto> = emptyList(),
    @field:Json(name = "otherFees") val otherFees: List<InvoiceOtherFeeItemDto> = emptyList(),
    @field:Json(name = "totalAmount") val totalAmount: String = "0",
    @field:Json(name = "paidAmount") val paidAmount: Long = 0L,
    @field:Json(name = "remainingAmount") val remainingAmount: Long = 0L,
    @field:Json(name = "dueDate") val dueDate: String = "",
    @field:Json(name = "invoiceStatus") val invoiceStatus: String = "issued",
    @field:Json(name = "issuedAt") val issuedAt: String? = null,
    @field:Json(name = "voidedInvoiceId") val voidedInvoiceId: String? = null,
    @field:Json(name = "voidReason") val voidReason: String? = null,
    @field:Json(name = "note") val note: String? = null,
    @field:Json(name = "vietQrPaymentUrl") val vietQrPaymentUrl: String? = null,
    @field:Json(name = "payments") val payments: List<InvoicePaymentItemDto> = emptyList()
) {
    val totalAmountLong: Long
        get() = totalAmount.toDoubleOrNull()?.toLong() ?: 0L
    val rentAmountLong: Long
        get() = rentAmount.toDoubleOrNull()?.toLong() ?: 0L

    val effectiveServices: List<InvoiceServiceItemDto>
        get() = if (breakdown.isNotEmpty()) {
            breakdown.map { item ->
                InvoiceServiceItemDto(
                    category = item.type.uppercase(),
                    name = item.name,
                    rateKind = item.rateKind,
                    usage = item.quantity,
                    unit = item.unit,
                    unitPrice = item.unitPriceLong,
                    amount = item.amountLong
                )
            }
        } else {
            services
        }
}

@JsonClass(generateAdapter = true)
data class PaymentMyListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "invoiceId") val invoiceId: String = "",
    @field:Json(name = "method") val method: String = "",
    @field:Json(name = "paymentStatus") val paymentStatus: String = "",
    @field:Json(name = "amount") val amount: Long = 0L,
    @field:Json(name = "transactionId") val transactionId: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentCreateIntentRequestDto(
    @field:Json(name = "invoiceId") val invoiceId: String,
    @field:Json(name = "method") val method: String = "vietqr"
)

@JsonClass(generateAdapter = true)
data class PaymentCreateIntentResponseDto(
    @field:Json(name = "paymentId") val paymentId: String = "",
    @field:Json(name = "invoiceCode") val invoiceCode: String = "",
    @field:Json(name = "method") val method: String = "",
    @field:Json(name = "amount") val amount: Long = 0L,
    @field:Json(name = "qrContent") val qrContent: String? = null,
    @field:Json(name = "bankCode") val bankCode: String? = null,
    @field:Json(name = "bankName") val bankName: String? = null,
    @field:Json(name = "accountNumber") val accountNumber: String? = null,
    @field:Json(name = "accountHolderName") val accountHolderName: String? = null,
    @field:Json(name = "paymentMemo") val paymentMemo: String? = null,
    @field:Json(name = "qrCodeUrl") val qrCodeUrl: String? = null
)

