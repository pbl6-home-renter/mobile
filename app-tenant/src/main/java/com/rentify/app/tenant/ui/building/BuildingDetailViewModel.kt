package com.rentify.app.tenant.ui.building

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.BuildingSummary
import com.rentify.app.tenant.data.repository.TenantBuildingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BuildingDetailViewModel(
    private val buildingId: String,
    private val buildingRepository: TenantBuildingRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<BuildingSummary>>(UiState.Loading)
    val uiState: StateFlow<UiState<BuildingSummary>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        launchSafe(
            onError = { _uiState.value = UiState.Error(it.message ?: "Đã xảy ra lỗi không xác định") }
        ) {
            _uiState.value = when (val result = buildingRepository.getBuilding(buildingId)) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.ApiError -> UiState.Error(
                    if (result.code == 404) "Không tìm thấy tòa nhà này" else (result.error?.message ?: "Không tải được dữ liệu")
                )
                is NetworkResult.NetworkError -> UiState.Error("Không có kết nối mạng. Vui lòng kiểm tra lại.")
                is NetworkResult.UnknownError -> UiState.Error(result.exception?.message ?: "Đã xảy ra lỗi không xác định")
            }
        }
    }
}
