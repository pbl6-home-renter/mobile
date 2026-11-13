package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceGenerateRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceGenerateResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceIssueRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceIssueResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentRecordCashRequestDto

interface LandlordInvoiceRepository {
    suspend fun getInvoices(
        status: String? = null,
        roomId: String? = null,
        buildingId: String? = null,
        period: String? = null,
        sort: String? = "period",
        order: String? = "desc",
        pageNo: Int = 1,
        pageSize: Int = 50
    ): NetworkResult<ApiListResponse<InvoiceListResponseDto>>

    suspend fun generateInvoices(
        request: InvoiceGenerateRequestDto
    ): NetworkResult<ApiSingleResponse<InvoiceGenerateResponseDto>>

    suspend fun issueInvoices(
        request: InvoiceIssueRequestDto,
        idempotencyKey: String? = null
    ): NetworkResult<ApiSingleResponse<InvoiceIssueResponseDto>>

    suspend fun getInvoiceDetail(
        invoiceId: String
    ): NetworkResult<ApiSingleResponse<InvoiceDetailResponseDto>>

    suspend fun cancelInvoice(
        invoiceId: String,
        reason: String
    ): NetworkResult<ApiSingleResponse<InvoiceDetailResponseDto>>

    suspend fun recordCashPayment(
        request: PaymentRecordCashRequestDto,
        idempotencyKey: String? = null
    ): NetworkResult<ApiSingleResponse<Any>>
}

