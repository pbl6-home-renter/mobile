package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceMyListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentCreateIntentResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentMyListResponseDto

interface TenantInvoiceRepository {

    suspend fun getMyInvoices(
        status: String? = null,
        period: String? = null,
        contractId: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 20
    ): NetworkResult<ApiListResponse<InvoiceMyListResponseDto>>

    suspend fun getInvoiceDetail(
        invoiceId: String
    ): NetworkResult<ApiSingleResponse<InvoiceDetailResponseDto>>

    suspend fun getMyPayments(
        invoiceId: String? = null,
        method: String? = null,
        paymentStatus: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 20
    ): NetworkResult<ApiListResponse<PaymentMyListResponseDto>>

    suspend fun createPaymentIntent(
        invoiceId: String,
        method: String = "vietqr"
    ): NetworkResult<ApiSingleResponse<PaymentCreateIntentResponseDto>>
}

