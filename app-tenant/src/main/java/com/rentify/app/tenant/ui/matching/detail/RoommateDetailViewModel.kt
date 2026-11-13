package com.rentify.app.tenant.ui.matching.detail

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.matching.RoommateProfileDetailResponseDto
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

sealed interface RoommateDetailUiEvent {
    data class ShowToast(val message: String) : RoommateDetailUiEvent
    data object RequestSentSuccess : RoommateDetailUiEvent
    data class RequireProfileVisibility(val reason: String) : RoommateDetailUiEvent
    data object RequireCreateProfile : RoommateDetailUiEvent
}

class RoommateDetailViewModel(
    private val profileId: String,
    private val matchingRepository: TenantMatchingRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<RoommateProfileDetailResponseDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<RoommateProfileDetailResponseDto>> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RoommateDetailUiEvent>()
    val uiEvent: SharedFlow<RoommateDetailUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = matchingRepository.getRoommateProfileDetail(profileId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _uiState.value = UiState.Success(detail)
                    } else {
                        _uiState.value = UiState.Error("Không tìm thấy thông tin hồ sơ")
                    }
                }
                is NetworkResult.ApiError -> {
                    val msg = if (result.error?.code == "PROFILE_NOT_VISIBLE") {
                        "Hồ sơ này hiện đang tắt chế độ hiển thị công khai"
                    } else {
                        result.error?.message ?: "Lỗi khi tải chi tiết hồ sơ"
                    }
                    _uiState.value = UiState.Error(msg)
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

    fun sendMatchRequest() {
        val currentDetail = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            when (val myProfileResult = matchingRepository.getMyRoommateProfile()) {
                is NetworkResult.Success -> {
                    val myProfile = myProfileResult.data.data
                    if (myProfile == null) {
                        _uiEvent.emit(RoommateDetailUiEvent.RequireCreateProfile)
                        return@launch
                    }
                    if (!myProfile.isVisible) {
                        _uiEvent.emit(
                            RoommateDetailUiEvent.RequireProfileVisibility(
                                "Hồ sơ của bạn đang ở chế độ ẩn. Vui lòng bật hiển thị hồ sơ để đối phương có thể xem thông tin của bạn trước khi kết nối."
                            )
                        )
                        return@launch
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiEvent.emit(RoommateDetailUiEvent.RequireCreateProfile)
                    return@launch
                }
                is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    // Tiếp tục thử gửi
                }
            }

            when (val result = matchingRepository.createMatchRequest(currentDetail.tenantId)) {
                is NetworkResult.Success -> {
                    _uiEvent.emit(RoommateDetailUiEvent.ShowToast("Đã gửi lời mời ghép đôi tới ${currentDetail.tenantName}!"))
                    _uiEvent.emit(RoommateDetailUiEvent.RequestSentSuccess)
                }
                is NetworkResult.ApiError -> {
                    val msg = when (result.error?.code) {
                        "DUPLICATE_MATCH_REQUEST" -> "Bạn đã gửi lời mời tới người này rồi"
                        "SELF_MATCHING_NOT_ALLOWED" -> "Không thể gửi lời mời cho chính mình"
                        else -> result.error?.message ?: "Không thể gửi lời mời"
                    }
                    _uiEvent.emit(RoommateDetailUiEvent.ShowToast(msg))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(RoommateDetailUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(RoommateDetailUiEvent.ShowToast(result.exception?.message ?: "Lỗi không xác định"))
                }
            }
        }
    }
}

