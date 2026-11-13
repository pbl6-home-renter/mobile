package com.rentify.app.tenant.ui.matching.profile

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.matching.RoommateProfileMyResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileSaveRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.TenantMatchingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MyRoommateProfileUiEvent {
    data class ShowToast(val message: String) : MyRoommateProfileUiEvent
    data object SaveSuccess : MyRoommateProfileUiEvent
}

class MyRoommateProfileViewModel(
    private val matchingRepository: TenantMatchingRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<RoommateProfileMyResponseDto?>>(UiState.Loading)
    val uiState: StateFlow<UiState<RoommateProfileMyResponseDto?>> = _uiState.asStateFlow()

    private val _saveLoading = MutableStateFlow(false)
    val saveLoading: StateFlow<Boolean> = _saveLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<MyRoommateProfileUiEvent>()
    val uiEvent: SharedFlow<MyRoommateProfileUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadMyProfile()
    }

    fun loadMyProfile() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = matchingRepository.getMyRoommateProfile()) {
                is NetworkResult.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    if (result.code == 404 || result.error?.code == "NOT_FOUND") {
                        // User hasn't created a roommate profile yet
                        _uiState.value = UiState.Success(null)
                    } else {
                        _uiState.value = UiState.Error(result.error?.message ?: "Không thể tải hồ sơ của bạn")
                    }
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

    fun saveProfile(request: RoommateProfileSaveRequestDto) {
        _saveLoading.value = true
        viewModelScope.launch {
            when (val result = matchingRepository.saveMyRoommateProfile(request)) {
                is NetworkResult.Success -> {
                    _saveLoading.value = false
                    _uiEvent.emit(MyRoommateProfileUiEvent.ShowToast("Lưu hồ sơ tìm bạn ở thành công!"))
                    _uiEvent.emit(MyRoommateProfileUiEvent.SaveSuccess)
                }
                is NetworkResult.ApiError -> {
                    _saveLoading.value = false
                    val msg = if (result.error?.code == "BUDGET_RANGE_INVALID") {
                        "Khoảng ngân sách không hợp lệ (tối thiểu không được lớn hơn tối đa)"
                    } else {
                        result.error?.message ?: "Lỗi khi lưu hồ sơ"
                    }
                    _uiEvent.emit(MyRoommateProfileUiEvent.ShowToast(msg))
                }
                is NetworkResult.NetworkError -> {
                    _saveLoading.value = false
                    _uiEvent.emit(MyRoommateProfileUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _saveLoading.value = false
                    _uiEvent.emit(MyRoommateProfileUiEvent.ShowToast(result.exception?.message ?: "Đã có lỗi xảy ra"))
                }
            }
        }
    }
}

