package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.InvoiceApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.invoice.InvoiceCancelRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceGenerateRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceGenerateResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceIssueRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceIssueResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentRecordCashRequestDto
import java.util.UUID

class LandlordInvoiceRepositoryImpl(
    private val invoiceApiService: InvoiceApiService
) : BaseRepository(), LandlordInvoiceRepository {

    override suspend fun getInvoices(
        status: String?,
        roomId: String?,
        buildingId: String?,
        period: String?,
        sort: String?,
        order: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<InvoiceListResponseDto>> {
        return safeApiCall {
            invoiceApiService.getLandlordInvoices(
                status = status,
                roomId = roomId,
                buildingId = buildingId,
                period = period,
                sort = sort,
                order = order,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun generateInvoices(
        request: InvoiceGenerateRequestDto
    ): NetworkResult<ApiSingleResponse<InvoiceGenerateResponseDto>> {
        return safeApiCall {
            invoiceApiService.generateInvoices(request)
        }
    }

    override suspend fun issueInvoices(
        request: InvoiceIssueRequestDto,
        idempotencyKey: String?
    ): NetworkResult<ApiSingleResponse<InvoiceIssueResponseDto>> {
        val key = idempotencyKey ?: UUID.randomUUID().toString()
        return safeApiCall {
            invoiceApiService.issueInvoices(key, request)
        }
    }

    override suspend fun getInvoiceDetail(
        invoiceId: String
    ): NetworkResult<ApiSingleResponse<InvoiceDetailResponseDto>> {
        return safeApiCall {
            invoiceApiService.getInvoiceDetail(invoiceId)
        }
    }

    override suspend fun cancelInvoice(
        invoiceId: String,
        reason: String
    ): NetworkResult<ApiSingleResponse<InvoiceDetailResponseDto>> {
        return safeApiCall {
            invoiceApiService.cancelInvoice(
                invoiceId = invoiceId,
                request = InvoiceCancelRequestDto(reason = reason)
            )
        }
    }

    override suspend fun recordCashPayment(
        request: PaymentRecordCashRequestDto,
        idempotencyKey: String?
    ): NetworkResult<ApiSingleResponse<Any>> {
        val key = idempotencyKey ?: UUID.randomUUID().toString()
        return safeApiCall {
            invoiceApiService.recordCashPayment(key, request)
        }
    }
}

