package com.rentify.app.tenant.ui.contract.list

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.TenantContractRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyContractsViewModel(
    private val contractRepository: TenantContractRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ContractMyListResponseDto>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ContractMyListResponseDto>>> = _uiState.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    init {
        loadContracts()
    }

    fun setStatusFilter(status: String?) {
        if (_selectedStatus.value == status) return
        _selectedStatus.value = status
        loadContracts()
    }

    fun loadContracts(isRefresh: Boolean = false) {
        if (!isRefresh) {
            _uiState.value = UiState.Loading
        }
        viewModelScope.launch {
            when (val result = contractRepository.getMyContracts(status = _selectedStatus.value)) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    _uiState.value = UiState.Success(list)
                }
                is NetworkResult.ApiError -> {
                    _uiState.value = UiState.Error(result.error?.message ?: "Lỗi khi tải danh sách hợp đồng (${result.code})")
                }
                is NetworkResult.NetworkError -> {
                    _uiState.value = UiState.Error("Không có kết nối mạng. Vui lòng kiểm tra lại.")
                }
                is NetworkResult.UnknownError -> {
                    _uiState.value = UiState.Error(result.exception?.localizedMessage ?: "Đã xảy ra lỗi không xác định")
                }
            }
        }
    }
}

