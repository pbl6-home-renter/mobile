package com.rentify.app.landlord.ui.property

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.room.RoomCreateRequestDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordBuildingRepository
import com.rentify.app.landlord.data.repository.LandlordRoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RoomListUiState {
    object Loading : RoomListUiState
    data class Success(val rooms: List<RoomMyListResponseDto>) : RoomListUiState
    data class Error(val message: String) : RoomListUiState
}

sealed interface RoomActionUiState {
    object Idle : RoomActionUiState
    object Loading : RoomActionUiState
    data class Success(val message: String, val data: Any? = null) : RoomActionUiState
    data class Error(val message: String) : RoomActionUiState
}

class RoomViewModel(
    private val roomRepository: LandlordRoomRepository,
    private val buildingRepository: LandlordBuildingRepository
) : BaseViewModel() {

    private val _roomListState = MutableStateFlow<RoomListUiState>(RoomListUiState.Loading)
    val roomListState: StateFlow<RoomListUiState> = _roomListState.asStateFlow()

    private val _approvedBuildings = MutableStateFlow<List<BuildingMyListResponseDto>>(emptyList())
    val approvedBuildings: StateFlow<List<BuildingMyListResponseDto>> = _approvedBuildings.asStateFlow()

    private val _actionState = MutableStateFlow<RoomActionUiState>(RoomActionUiState.Idle)
    val actionState: StateFlow<RoomActionUiState> = _actionState.asStateFlow()

    fun loadRooms(buildingId: String? = null, status: String? = null) {
        _roomListState.value = RoomListUiState.Loading
        viewModelScope.launch {
            when (val result = roomRepository.getMyRooms(buildingId = buildingId, status = status)) {
                is NetworkResult.Success -> {
                    _roomListState.value = RoomListUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _roomListState.value = RoomListUiState.Error(result.error?.message ?: "Lỗi tải danh sách phòng")
                }
                is NetworkResult.NetworkError -> {
                    _roomListState.value = RoomListUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _roomListState.value = RoomListUiState.Error("Không thể tải danh sách phòng")
                }
            }
        }
    }

    fun loadApprovedBuildings() {
        viewModelScope.launch {
            when (val result = buildingRepository.getMyBuildings(status = "approved")) {
                is NetworkResult.Success -> {
                    _approvedBuildings.value = result.data.data
                }
                else -> {
                    _approvedBuildings.value = emptyList()
                }
            }
        }
    }

    fun createRoom(request: RoomCreateRequestDto) {
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            when (val result = roomRepository.createRoom(request)) {
                is NetworkResult.Success -> {
                    _actionState.value = RoomActionUiState.Success("Tạo phòng mới thành công!", result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = RoomActionUiState.Error(result.error?.message ?: "Tạo phòng thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = RoomActionUiState.Error("Đã xảy ra lỗi khi tạo phòng")
                }
            }
        }
    }

    fun markRoomReady(roomId: String) {
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            when (val result = roomRepository.markRoomReady(roomId)) {
                is NetworkResult.Success -> {
                    _actionState.value = RoomActionUiState.Success("Đã chuyển phòng sang trạng thái sẵn sàng cho thuê!")
                    loadRooms()
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = RoomActionUiState.Error(result.error?.message ?: "Cập nhật trạng thái thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = RoomActionUiState.Error("Đã xảy ra lỗi khi cập nhật trạng thái phòng")
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = RoomActionUiState.Idle
    }
}

