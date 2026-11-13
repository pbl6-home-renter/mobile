package com.rentify.app.tenant.ui.invoice.list

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.invoice.InvoiceMyListResponseDto
import com.rentify.app.core.network.dto.invoice.PaymentMyListResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.tenant.data.repository.TenantInvoiceRepository
import com.rentify.app.tenant.ui.invoice.InvoiceUiHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InvoiceFilterStatus {
    ALL, UNPAID, PAID, OVERDUE
}

data class MyInvoicesUiState(
    val isLoading: Boolean = false,
    val selectedTab: Int = 0,
    val selectedStatusFilter: InvoiceFilterStatus = InvoiceFilterStatus.ALL,
    val allInvoices: List<InvoiceMyListResponseDto> = emptyList(),
    val displayedInvoices: List<InvoiceMyListResponseDto> = emptyList(),
    val payments: List<PaymentMyListResponseDto> = emptyList(),
    val errorMessage: String? = null
)

class MyInvoicesViewModel(
    private val invoiceRepository: TenantInvoiceRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyInvoicesUiState())
    val uiState: StateFlow<MyInvoicesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(isRefresh: Boolean = false) {
        if (!isRefresh) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        viewModelScope.launch {
            val invoicesResult = invoiceRepository.getMyInvoices(pageSize = 50)
            val paymentsResult = invoiceRepository.getMyPayments(pageSize = 50)

            var invoices: List<InvoiceMyListResponseDto> = _uiState.value.allInvoices
            var payments: List<PaymentMyListResponseDto> = _uiState.value.payments
            var error: String? = null

            when (invoicesResult) {
                is NetworkResult.Success -> {
                    invoices = invoicesResult.data.data
                }
                is NetworkResult.ApiError -> {
                    error = invoicesResult.error?.message ?: "Lỗi tải danh sách hóa đơn"
                }
                is NetworkResult.NetworkError -> {
                    error = "Không có kết nối mạng"
                }
                is NetworkResult.UnknownError -> {
                    error = invoicesResult.exception?.message ?: "Đã có lỗi xảy ra"
                }
            }

            if (paymentsResult is NetworkResult.Success) {
                payments = paymentsResult.data.data
            }

            val currentFilter = _uiState.value.selectedStatusFilter
            val filtered = applyFilter(invoices, currentFilter)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    allInvoices = invoices,
                    displayedInvoices = filtered,
                    payments = payments,
                    errorMessage = error
                )
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setFilter(status: InvoiceFilterStatus) {
        _uiState.update {
            it.copy(
                selectedStatusFilter = status,
                displayedInvoices = applyFilter(it.allInvoices, status)
            )
        }
    }

    private fun applyFilter(
        invoices: List<InvoiceMyListResponseDto>,
        filter: InvoiceFilterStatus
    ): List<InvoiceMyListResponseDto> {
        return when (filter) {
            InvoiceFilterStatus.ALL -> invoices
            InvoiceFilterStatus.UNPAID -> invoices.filter {
                it.invoiceStatus.equals("issued", ignoreCase = true) && it.remainingAmount > 0
            }
            InvoiceFilterStatus.PAID -> invoices.filter {
                it.remainingAmount <= 0
            }
            InvoiceFilterStatus.OVERDUE -> invoices.filter {
                val statusText = InvoiceUiHelper.getInvoiceStatusText(it.invoiceStatus, it.remainingAmount, it.dueDate)
                statusText == "Quá hạn"
            }
        }
    }
}

