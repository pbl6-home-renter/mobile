package com.rentify.app.tenant.ui.contract.detail

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.TenantContractRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContractDetailViewModel(
    private val contractId: String,
    private val contractRepository: TenantContractRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<ContractDetailResponseDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<ContractDetailResponseDto>> = _uiState.asStateFlow()

    init {
        loadContractDetail()
    }

    fun loadContractDetail() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.getContractDetail(contractId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _uiState.value = UiState.Success(detail)
                    } else {
                        _uiState.value = UiState.Error("Không tìm thấy thông tin hợp đồng")
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiState.value = UiState.Error(result.error?.message ?: "Lỗi khi tải chi tiết hợp đồng (${result.code})")
                }
                is NetworkResult.NetworkError -> {
                    _uiState.value = UiState.Error("Không có kết nối mạng. Vui lòng thử lại.")
                }
                is NetworkResult.UnknownError -> {
                    _uiState.value = UiState.Error(result.exception?.localizedMessage ?: "Đã xảy ra lỗi không xác định")
                }
            }
        }
    }
}

