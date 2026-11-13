package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceMyListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentCreateIntentRequestDto
import com.rentify.app.core.network.dto.invoice.PaymentCreateIntentResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentMyListResponseDto
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

    @GET("invoices/{invoiceId}")
    suspend fun getInvoiceDetail(
        @Path("invoiceId") invoiceId: String
    ): Response<ApiSingleResponse<InvoiceDetailResponseDto>>

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

