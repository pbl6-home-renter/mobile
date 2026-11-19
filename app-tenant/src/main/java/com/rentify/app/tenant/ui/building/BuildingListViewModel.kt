package com.rentify.app.tenant.ui.building

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.BuildingSummary
import com.rentify.app.tenant.data.repository.TenantBuildingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BuildingListViewModel(
    private val buildingRepository: TenantBuildingRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<BuildingSummary>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<BuildingSummary>>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load(isRefresh: Boolean = false) {
        if (!isRefresh) _uiState.value = UiState.Loading
        launchSafe(
            onError = { _uiState.value = UiState.Error(it.message ?: "Đã xảy ra lỗi không xác định") }
        ) {
            _uiState.value = when (val result = buildingRepository.getBuildings()) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.ApiError -> UiState.Error(result.error?.message ?: "Không tải được dữ liệu")
                is NetworkResult.NetworkError -> UiState.Error("Không có kết nối mạng. Vui lòng kiểm tra lại.")
                is NetworkResult.UnknownError -> UiState.Error(result.exception?.message ?: "Đã xảy ra lỗi không xác định")
            }
        }
    }
}
