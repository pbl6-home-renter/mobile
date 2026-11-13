package com.rentify.app.landlord.ui.billing

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceGenerateRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceIssueRequestDto
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentRecordCashRequestDto
import com.rentify.app.core.network.dto.meter.BulkMeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.BulkMeterReadingResponseDto
import com.rentify.app.core.network.dto.meter.MeterReadingListResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordInvoiceRepository
import com.rentify.app.landlord.data.repository.LandlordMeterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MeterReadingsUiState {
    object Loading : MeterReadingsUiState
    data class Success(val readings: List<MeterReadingListResponseDto>) : MeterReadingsUiState
    data class Error(val message: String) : MeterReadingsUiState
}

sealed interface BulkMeterUiState {
    object Idle : BulkMeterUiState
    object Loading : BulkMeterUiState
    data class Success(val response: BulkMeterReadingResponseDto) : BulkMeterUiState
    data class Error(val message: String) : BulkMeterUiState
}

sealed interface InvoicesUiState {
    object Loading : InvoicesUiState
    data class Success(val invoices: List<InvoiceListResponseDto>) : InvoicesUiState
    data class Error(val message: String) : InvoicesUiState
}

sealed interface InvoiceDetailUiState {
    object Loading : InvoiceDetailUiState
    data class Success(val detail: InvoiceDetailResponseDto) : InvoiceDetailUiState
    data class Error(val message: String) : InvoiceDetailUiState
}

sealed interface BillingActionUiState {
    object Idle : BillingActionUiState
    object Loading : BillingActionUiState
    data class Success(val message: String) : BillingActionUiState
    data class Error(val message: String) : BillingActionUiState
}

class BillingViewModel(
    private val meterRepository: LandlordMeterRepository,
    private val invoiceRepository: LandlordInvoiceRepository
) : BaseViewModel() {

    private val _meterReadingsState = MutableStateFlow<MeterReadingsUiState>(MeterReadingsUiState.Loading)
    val meterReadingsState: StateFlow<MeterReadingsUiState> = _meterReadingsState.asStateFlow()

    private val _bulkMeterState = MutableStateFlow<BulkMeterUiState>(BulkMeterUiState.Idle)
    val bulkMeterState: StateFlow<BulkMeterUiState> = _bulkMeterState.asStateFlow()

    private val _invoicesState = MutableStateFlow<InvoicesUiState>(InvoicesUiState.Loading)
    val invoicesState: StateFlow<InvoicesUiState> = _invoicesState.asStateFlow()

    private val _invoiceDetailState = MutableStateFlow<InvoiceDetailUiState>(InvoiceDetailUiState.Loading)
    val invoiceDetailState: StateFlow<InvoiceDetailUiState> = _invoiceDetailState.asStateFlow()

    private val _actionState = MutableStateFlow<BillingActionUiState>(BillingActionUiState.Idle)
    val actionState: StateFlow<BillingActionUiState> = _actionState.asStateFlow()

    fun loadMeterReadings(
        roomId: String? = null,
        buildingId: String? = null,
        period: String? = null,
        type: String? = null
    ) {
        _meterReadingsState.value = MeterReadingsUiState.Loading
        viewModelScope.launch {
            when (val result = meterRepository.getMeterReadings(
                roomId = roomId,
                buildingId = buildingId,
                period = period,
                type = type
            )) {
                is NetworkResult.Success -> {
                    _meterReadingsState.value = MeterReadingsUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _meterReadingsState.value = MeterReadingsUiState.Error(
                        result.error?.message ?: "Không thể tải danh sách chỉ số điện nước"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _meterReadingsState.value = MeterReadingsUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _meterReadingsState.value = MeterReadingsUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun bulkRecordMeters(request: BulkMeterReadingCreateRequestDto) {
        _bulkMeterState.value = BulkMeterUiState.Loading
        viewModelScope.launch {
            when (val result = meterRepository.bulkCreateMeterReadings(request)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    if (data != null) {
                        _bulkMeterState.value = BulkMeterUiState.Success(data)
                    } else {
                        _bulkMeterState.value = BulkMeterUiState.Error("Không có dữ liệu phản hồi từ hệ thống")
                    }
                }
                is NetworkResult.ApiError -> {
                    _bulkMeterState.value = BulkMeterUiState.Error(
                        result.error?.message ?: "Lỗi ghi nhận chỉ số hàng loạt"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _bulkMeterState.value = BulkMeterUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _bulkMeterState.value = BulkMeterUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun loadInvoices(
        status: String? = null,
        roomId: String? = null,
        buildingId: String? = null,
        period: String? = null
    ) {
        _invoicesState.value = InvoicesUiState.Loading
        viewModelScope.launch {
            when (val result = invoiceRepository.getInvoices(
                status = status,
                roomId = roomId,
                buildingId = buildingId,
                period = period
            )) {
                is NetworkResult.Success -> {
                    _invoicesState.value = InvoicesUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _invoicesState.value = InvoicesUiState.Error(
                        result.error?.message ?: "Không thể tải danh sách hóa đơn"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _invoicesState.value = InvoicesUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _invoicesState.value = InvoicesUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun generateInvoices(buildingId: String, period: String) {
        _actionState.value = BillingActionUiState.Loading
        viewModelScope.launch {
            when (val result = invoiceRepository.generateInvoices(
                InvoiceGenerateRequestDto(buildingId = buildingId, period = period)
            )) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    val count = if (data != null) (data.generated.size + data.recalculated.size) else 0
                    _actionState.value = BillingActionUiState.Success(
                        "Đã lập/tính toán thành công $count hóa đơn kỳ $period"
                    )
                    loadInvoices(buildingId = buildingId, period = period)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BillingActionUiState.Error(
                        result.error?.message ?: "Lỗi lập hóa đơn tự động"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BillingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BillingActionUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun issueInvoices(invoiceIds: List<String>) {
        _actionState.value = BillingActionUiState.Loading
        viewModelScope.launch {
            when (val result = invoiceRepository.issueInvoices(
                InvoiceIssueRequestDto(invoiceIds = invoiceIds)
            )) {
                is NetworkResult.Success -> {
                    val count = result.data.data?.succeeded?.size ?: invoiceIds.size
                    _actionState.value = BillingActionUiState.Success(
                        "Đã phát hành thành công $count hóa đơn"
                    )
                    loadInvoices()
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BillingActionUiState.Error(
                        result.error?.message ?: "Lỗi phát hành hóa đơn"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BillingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BillingActionUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun loadInvoiceDetail(invoiceId: String) {
        _invoiceDetailState.value = InvoiceDetailUiState.Loading
        viewModelScope.launch {
            when (val result = invoiceRepository.getInvoiceDetail(invoiceId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _invoiceDetailState.value = InvoiceDetailUiState.Success(detail)
                    } else {
                        _invoiceDetailState.value = InvoiceDetailUiState.Error("Không tìm thấy thông tin hóa đơn")
                    }
                }
                is NetworkResult.ApiError -> {
                    _invoiceDetailState.value = InvoiceDetailUiState.Error(
                        result.error?.message ?: "Không thể tải chi tiết hóa đơn"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _invoiceDetailState.value = InvoiceDetailUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _invoiceDetailState.value = InvoiceDetailUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun cancelInvoice(invoiceId: String, reason: String) {
        _actionState.value = BillingActionUiState.Loading
        viewModelScope.launch {
            when (val result = invoiceRepository.cancelInvoice(invoiceId, reason)) {
                is NetworkResult.Success -> {
                    _actionState.value = BillingActionUiState.Success("Hóa đơn đã được hủy thành công")
                    loadInvoiceDetail(invoiceId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BillingActionUiState.Error(
                        result.error?.message ?: "Lỗi hủy hóa đơn"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BillingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BillingActionUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun recordCashPayment(request: PaymentRecordCashRequestDto) {
        _actionState.value = BillingActionUiState.Loading
        viewModelScope.launch {
            when (val result = invoiceRepository.recordCashPayment(request)) {
                is NetworkResult.Success -> {
                    _actionState.value = BillingActionUiState.Success("Đã ghi nhận thanh toán tiền mặt thành công")
                    loadInvoiceDetail(request.invoiceId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BillingActionUiState.Error(
                        result.error?.message ?: "Lỗi ghi nhận thanh toán tiền mặt"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BillingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BillingActionUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = BillingActionUiState.Idle
    }

    fun resetBulkMeterState() {
        _bulkMeterState.value = BulkMeterUiState.Idle
    }
}
