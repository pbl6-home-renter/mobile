package com.rentify.app.tenant.ui.invoice.detail

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.invoice.InvoiceDetailResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.TenantInvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InvoiceDetailViewModel(
    private val invoiceRepository: TenantInvoiceRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<InvoiceDetailResponseDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<InvoiceDetailResponseDto>> = _uiState.asStateFlow()

    fun loadInvoiceDetail(invoiceId: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = invoiceRepository.getInvoiceDetail(invoiceId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _uiState.value = UiState.Success(detail)
                    } else {
                        _uiState.value = UiState.Error("Không tìm thấy thông tin hóa đơn")
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiState.value = UiState.Error(result.error?.message ?: "Lỗi tải chi tiết hóa đơn")
                }
                is NetworkResult.NetworkError -> {
                    _uiState.value = UiState.Error("Không có kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _uiState.value = UiState.Error(result.exception?.message ?: "Đã có lỗi xảy ra")
                }
            }
        }
    }
}

