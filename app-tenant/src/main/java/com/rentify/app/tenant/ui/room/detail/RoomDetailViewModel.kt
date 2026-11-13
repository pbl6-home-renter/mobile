package com.rentify.app.tenant.ui.room.detail

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.session.SessionManager
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.TenantChatRepository
import com.rentify.app.tenant.data.repository.TenantRoomRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface RoomDetailUiEvent {
    data class NavigateToChat(val conversationId: String, val recipientName: String) : RoomDetailUiEvent
    data class CallPhone(val phone: String) : RoomDetailUiEvent
    data object ShowLoginRequired : RoomDetailUiEvent
    data class ShowToast(val message: String) : RoomDetailUiEvent
}

class RoomDetailViewModel(
    private val roomId: String,
    initialIsFavorited: Boolean,
    private val roomRepository: TenantRoomRepository,
    private val chatRepository: TenantChatRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<RoomDetailResponseDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<RoomDetailResponseDto>> = _uiState.asStateFlow()

    private val _isFavorited = MutableStateFlow(initialIsFavorited)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isContacting = MutableStateFlow(false)
    val isContacting: StateFlow<Boolean> = _isContacting.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RoomDetailUiEvent>()
    val uiEvent: SharedFlow<RoomDetailUiEvent> = _uiEvent.asSharedFlow()

    init {
        checkLoginStatus()
        loadDetail()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = sessionManager.isLoggedIn.first()
        }
    }

    fun loadDetail() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = roomRepository.getRoomDetail(roomId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _uiState.value = UiState.Success(detail)
                    } else {
                        _uiState.value = UiState.Error("Không tìm thấy thông tin phòng trọ")
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiState.value = UiState.Error(result.error?.message ?: "Lỗi khi tải chi tiết phòng")
                }
                is NetworkResult.NetworkError -> {
                    _uiState.value = UiState.Error("Không có kết nối mạng. Vui lòng kiểm tra lại.")
                }
                is NetworkResult.UnknownError -> {
                    _uiState.value = UiState.Error("Đã xảy ra lỗi không xác định")
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            if (!sessionManager.isLoggedIn.first()) {
                _uiEvent.emit(RoomDetailUiEvent.ShowLoginRequired)
                return@launch
            }

            val currentFav = _isFavorited.value
            if (currentFav) {
                when (val result = roomRepository.removeFavoriteRoom(roomId)) {
                    is NetworkResult.Success -> {
                        _isFavorited.value = false
                        _uiEvent.emit(RoomDetailUiEvent.ShowToast("Đã bỏ lưu phòng"))
                    }
                    is NetworkResult.ApiError -> {
                        _uiEvent.emit(RoomDetailUiEvent.ShowToast(result.error?.message ?: "Không thể bỏ lưu phòng"))
                    }
                    else -> {
                        _uiEvent.emit(RoomDetailUiEvent.ShowToast("Lỗi kết nối khi cập nhật lưu phòng"))
                    }
                }
            } else {
                when (val result = roomRepository.addFavoriteRoom(roomId)) {
                    is NetworkResult.Success -> {
                        _isFavorited.value = true
                        _uiEvent.emit(RoomDetailUiEvent.ShowToast("Đã lưu phòng vào danh sách yêu thích"))
                    }
                    is NetworkResult.ApiError -> {
                        _uiEvent.emit(RoomDetailUiEvent.ShowToast(result.error?.message ?: "Không thể lưu phòng"))
                    }
                    else -> {
                        _uiEvent.emit(RoomDetailUiEvent.ShowToast("Lỗi kết nối khi cập nhật lưu phòng"))
                    }
                }
            }
        }
    }

    fun contactLandlord() {
        val currentData = (_uiState.value as? UiState.Success)?.data ?: return
        val landlord = currentData.landlord
        if (landlord.id.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(RoomDetailUiEvent.ShowToast("Không tìm thấy thông tin liên hệ của chủ trọ"))
            }
            return
        }

        viewModelScope.launch {
            if (!sessionManager.isLoggedIn.first()) {
                _uiEvent.emit(RoomDetailUiEvent.ShowLoginRequired)
                return@launch
            }

            if (_isContacting.value) return@launch
            _isContacting.value = true

            when (val result = chatRepository.getOrCreateDirectConversation(landlord.id)) {
                is NetworkResult.Success -> {
                    _isContacting.value = false
                    val conversation = result.data.data
                    if (conversation != null) {
                        _uiEvent.emit(
                            RoomDetailUiEvent.NavigateToChat(
                                conversationId = conversation.id,
                                recipientName = landlord.name
                            )
                        )
                    } else {
                        _uiEvent.emit(RoomDetailUiEvent.ShowToast("Không thể tạo cuộc trò chuyện"))
                    }
                }
                is NetworkResult.ApiError -> {
                    _isContacting.value = false
                    _uiEvent.emit(RoomDetailUiEvent.ShowToast(result.error?.message ?: "Lỗi khi mở cuộc trò chuyện"))
                }
                else -> {
                    _isContacting.value = false
                    _uiEvent.emit(RoomDetailUiEvent.ShowToast("Lỗi mạng khi kết nối tới chủ trọ"))
                }
            }
        }
    }

    fun callLandlord() {
        val currentData = (_uiState.value as? UiState.Success)?.data ?: return
        val phone = currentData.landlord.phone
        if (phone.isNotBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(RoomDetailUiEvent.CallPhone(phone))
            }
        } else {
            viewModelScope.launch {
                _uiEvent.emit(RoomDetailUiEvent.ShowToast("Chủ trọ chưa cập nhật số điện thoại"))
            }
        }
    }
}

