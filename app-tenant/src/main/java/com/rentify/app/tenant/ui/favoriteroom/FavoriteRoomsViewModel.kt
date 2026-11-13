package com.rentify.app.tenant.ui.favoriteroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentify.app.tenant.data.repository.TenantRoomRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.room.FavoriteRoomListResponseDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoriteRoomsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<FavoriteRoomListResponseDto> = emptyList(),
    val pageNo: Int = 1,
    val hasNextPage: Boolean = false,
    val totalItems: Int = 0,
    val errorMessage: String? = null
)

sealed interface FavoriteRoomsEvent {
    data class ShowToast(val message: String) : FavoriteRoomsEvent
    data class ShowError(val message: String) : FavoriteRoomsEvent
    data class RoomRemoved(val roomId: String, val roomName: String) : FavoriteRoomsEvent
}

class FavoriteRoomsViewModel(
    private val roomRepository: TenantRoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteRoomsUiState())
    val uiState: StateFlow<FavoriteRoomsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FavoriteRoomsEvent>()
    val events: SharedFlow<FavoriteRoomsEvent> = _events.asSharedFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = roomRepository.getFavoriteRooms(pageNo = 1, pageSize = 20)) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    val pagination = result.data.pagination
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = list,
                            pageNo = 1,
                            hasNextPage = pagination?.hasNextPage == true,
                            totalItems = pagination?.totalItems ?: list.size,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.ApiError -> {
                    val msg = result.error?.message ?: "Không thể tải danh sách phòng đã lưu"
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                    _events.emit(FavoriteRoomsEvent.ShowError(msg))
                }
                is NetworkResult.NetworkError -> {
                    val msg = "Lỗi kết nối mạng, vui lòng kiểm tra lại"
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                    _events.emit(FavoriteRoomsEvent.ShowError(msg))
                }
                is NetworkResult.UnknownError -> {
                    val msg = result.exception?.message ?: "Đã có lỗi xảy ra"
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                    _events.emit(FavoriteRoomsEvent.ShowError(msg))
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

            when (val result = roomRepository.getFavoriteRooms(pageNo = 1, pageSize = 20)) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    val pagination = result.data.pagination
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            items = list,
                            pageNo = 1,
                            hasNextPage = pagination?.hasNextPage == true,
                            totalItems = pagination?.totalItems ?: list.size,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    _events.emit(FavoriteRoomsEvent.ShowError(result.error?.message ?: "Làm mới thất bại"))
                }
                is NetworkResult.NetworkError -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    _events.emit(FavoriteRoomsEvent.ShowError("Lỗi kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    _events.emit(FavoriteRoomsEvent.ShowError(result.exception?.message ?: "Có lỗi xảy ra"))
                }
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (!currentState.hasNextPage || currentState.isLoading || currentState.isLoadingMore || currentState.isRefreshing) {
            return
        }

        viewModelScope.launch {
            val nextPage = currentState.pageNo + 1
            _uiState.update { it.copy(isLoadingMore = true) }

            when (val result = roomRepository.getFavoriteRooms(pageNo = nextPage, pageSize = 20)) {
                is NetworkResult.Success -> {
                    val newItems = result.data.data ?: emptyList()
                    val pagination = result.data.pagination
                    _uiState.update { current ->
                        current.copy(
                            isLoadingMore = false,
                            items = current.items + newItems,
                            pageNo = nextPage,
                            hasNextPage = pagination?.hasNextPage == true,
                            totalItems = pagination?.totalItems ?: (current.items.size + newItems.size)
                        )
                    }
                }
                is NetworkResult.ApiError, is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _events.emit(FavoriteRoomsEvent.ShowToast("Không thể tải thêm phòng"))
                }
            }
        }
    }

    fun removeFavorite(room: FavoriteRoomListResponseDto) {
        val originalList = _uiState.value.items
        val originalTotal = _uiState.value.totalItems

        // Optimistic UI update
        _uiState.update { current ->
            current.copy(
                items = current.items.filterNot { it.roomId == room.roomId },
                totalItems = (current.totalItems - 1).coerceAtLeast(0)
            )
        }

        viewModelScope.launch {
            when (val result = roomRepository.removeFavoriteRoom(room.roomId)) {
                is NetworkResult.Success -> {
                    _events.emit(FavoriteRoomsEvent.RoomRemoved(room.roomId, room.name))
                    _events.emit(FavoriteRoomsEvent.ShowToast("Đã bỏ lưu phòng ${room.name}"))
                }
                is NetworkResult.ApiError, is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    // Rollback
                    _uiState.update { current ->
                        current.copy(
                            items = originalList,
                            totalItems = originalTotal
                        )
                    }
                    _events.emit(FavoriteRoomsEvent.ShowError("Không thể bỏ lưu phòng, vui lòng thử lại"))
                }
            }
        }
    }
}
