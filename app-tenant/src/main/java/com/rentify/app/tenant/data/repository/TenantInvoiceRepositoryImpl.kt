package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.InvoiceApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceMyListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentCreateIntentRequestDto
import com.rentify.app.core.network.dto.invoice.PaymentCreateIntentResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentMyListResponseDto
import java.util.UUID

class TenantInvoiceRepositoryImpl(
    private val invoiceApiService: InvoiceApiService
) : BaseRepository(), TenantInvoiceRepository {

    override suspend fun getMyInvoices(
        status: String?,
        period: String?,
        contractId: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<InvoiceMyListResponseDto>> {
        return safeApiCall {
            invoiceApiService.getMyInvoices(
                period = period,
                contractId = contractId,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun getInvoiceDetail(
        invoiceId: String
    ): NetworkResult<ApiSingleResponse<InvoiceDetailResponseDto>> {
        return safeApiCall {
            invoiceApiService.getInvoiceDetail(invoiceId)
        }
    }

    override suspend fun getMyPayments(
        invoiceId: String?,
        method: String?,
        paymentStatus: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<PaymentMyListResponseDto>> {
        return safeApiCall {
            invoiceApiService.getMyPayments(
                invoiceId = invoiceId,
                method = method,
                paymentStatus = paymentStatus,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun createPaymentIntent(
        invoiceId: String,
        method: String
    ): NetworkResult<ApiSingleResponse<PaymentCreateIntentResponseDto>> {
        val idempotencyKey = UUID.randomUUID().toString()
        val request = PaymentCreateIntentRequestDto(invoiceId = invoiceId, method = method)
        return safeApiCall {
            invoiceApiService.createPaymentIntent(idempotencyKey, request)
        }
    }
}

