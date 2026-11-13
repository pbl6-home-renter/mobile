package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.invoice.InvoiceCancelRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceGenerateRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceGenerateResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceIssueRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceIssueResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceMyListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentCreateIntentRequestDto
import com.rentify.app.core.network.dto.invoice.PaymentCreateIntentResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentMyListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentRecordCashRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InvoiceApiService {

    @GET("me/invoices")
    suspend fun getMyInvoices(
        @Query("filter[period]") period: String? = null,
        @Query("filter[contractId]") contractId: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<InvoiceMyListResponseDto>>

    @GET("invoices")
    suspend fun getLandlordInvoices(
        @Query("filter[status]") status: String? = null,
        @Query("filter[roomId]") roomId: String? = null,
        @Query("filter[buildingId]") buildingId: String? = null,
        @Query("filter[period]") period: String? = null,
        @Query("sort") sort: String? = "period",
        @Query("order") order: String? = "desc",
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiListResponse<InvoiceListResponseDto>>

    @POST("invoices/generate")
    suspend fun generateInvoices(
        @Body request: InvoiceGenerateRequestDto
    ): Response<ApiSingleResponse<InvoiceGenerateResponseDto>>

    @POST("invoices/issue-invoice")
    suspend fun issueInvoices(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: InvoiceIssueRequestDto
    ): Response<ApiSingleResponse<InvoiceIssueResponseDto>>

    @GET("invoices/{invoiceId}")
    suspend fun getInvoiceDetail(
        @Path("invoiceId") invoiceId: String
    ): Response<ApiSingleResponse<InvoiceDetailResponseDto>>

    @POST("invoices/{invoiceId}/cancel-invoice")
    suspend fun cancelInvoice(
        @Path("invoiceId") invoiceId: String,
        @Body request: InvoiceCancelRequestDto
    ): Response<ApiSingleResponse<InvoiceDetailResponseDto>>

    @POST("payments/record-cash-payment")
    suspend fun recordCashPayment(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: PaymentRecordCashRequestDto
    ): Response<ApiSingleResponse<Any>>

    @GET("me/payments")
    suspend fun getMyPayments(
        @Query("filter[invoiceId]") invoiceId: String? = null,
        @Query("filter[method]") method: String? = null,
        @Query("filter[paymentStatus]") paymentStatus: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<PaymentMyListResponseDto>>

    @POST("payments/create-intent")
    suspend fun createPaymentIntent(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: PaymentCreateIntentRequestDto
    ): Response<ApiSingleResponse<PaymentCreateIntentResponseDto>>
}
