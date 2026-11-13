package com.rentify.app.landlord.ui.dashboard

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.dashboard.DashboardResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordDashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val metrics: DashboardResponseDto) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    private val dashboardRepository: LandlordDashboardRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        _uiState.value = DashboardUiState.Loading
        viewModelScope.launch {
            when (val result = dashboardRepository.getDashboard()) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    if (data != null) {
                        _uiState.value = DashboardUiState.Success(data)
                    } else {
                        _uiState.value = DashboardUiState.Error("Không có dữ liệu thống kê")
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiState.value = DashboardUiState.Error(result.error?.message ?: "Lỗi tải số liệu tổng quan")
                }
                is NetworkResult.NetworkError -> {
                    _uiState.value = DashboardUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _uiState.value = DashboardUiState.Error("Không thể tải thông tin bảng điều khiển")
                }
            }
        }
    }
}

