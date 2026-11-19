package com.rentify.app.landlord.ui.property

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.billing.BillingSettingCreateRequestDto
import com.rentify.app.core.network.dto.billing.BillingSettingResponseDto
import com.rentify.app.core.network.dto.billing.BillingSettingUpdateRequestDto
import com.rentify.app.core.network.dto.billing.RateItemDto
import com.rentify.app.core.network.dto.billing.RatePolicyCreateRequestDto
import com.rentify.app.core.network.dto.billing.RatePolicyResponseDto
import com.rentify.app.core.network.dto.billing.RatePolicyUpdateRequestDto
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.room.RoomCreateRequestDto
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.network.dto.room.RoomUpdateRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordBuildingRepository
import com.rentify.app.landlord.data.repository.LandlordMediaRepository
import com.rentify.app.landlord.data.repository.LandlordPolicySettingRepository
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

sealed interface RoomDetailUiState {
    object Idle : RoomDetailUiState
    object Loading : RoomDetailUiState
    data class Success(val room: RoomDetailResponseDto) : RoomDetailUiState
    data class Error(val message: String) : RoomDetailUiState
}

sealed interface RoomActionUiState {
    object Idle : RoomActionUiState
    object Loading : RoomActionUiState
    data class Success(val message: String, val data: Any? = null) : RoomActionUiState
    data class Error(val message: String) : RoomActionUiState
}

class RoomViewModel(
    private val roomRepository: LandlordRoomRepository,
    private val buildingRepository: LandlordBuildingRepository,
    private val mediaRepository: LandlordMediaRepository? = null,
    private val policySettingRepository: LandlordPolicySettingRepository? = null
) : BaseViewModel() {

    private val _roomListState = MutableStateFlow<RoomListUiState>(RoomListUiState.Loading)
    val roomListState: StateFlow<RoomListUiState> = _roomListState.asStateFlow()

    private val _roomDetailState = MutableStateFlow<RoomDetailUiState>(RoomDetailUiState.Idle)
    val roomDetailState: StateFlow<RoomDetailUiState> = _roomDetailState.asStateFlow()

    private val _roomRatePolicyState = MutableStateFlow<RatePolicyResponseDto?>(null)
    val roomRatePolicyState: StateFlow<RatePolicyResponseDto?> = _roomRatePolicyState.asStateFlow()

    private val _buildingRatePolicyState = MutableStateFlow<RatePolicyResponseDto?>(null)
    val buildingRatePolicyState: StateFlow<RatePolicyResponseDto?> = _buildingRatePolicyState.asStateFlow()

    private val _roomBillingSettingState = MutableStateFlow<BillingSettingResponseDto?>(null)
    val roomBillingSettingState: StateFlow<BillingSettingResponseDto?> = _roomBillingSettingState.asStateFlow()

    private val _buildingBillingSettingState = MutableStateFlow<BillingSettingResponseDto?>(null)
    val buildingBillingSettingState: StateFlow<BillingSettingResponseDto?> = _buildingBillingSettingState.asStateFlow()

    private val _approvedBuildings = MutableStateFlow<List<BuildingMyListResponseDto>>(emptyList())
    val approvedBuildings: StateFlow<List<BuildingMyListResponseDto>> = _approvedBuildings.asStateFlow()

    private val _actionState = MutableStateFlow<RoomActionUiState>(RoomActionUiState.Idle)
    val actionState: StateFlow<RoomActionUiState> = _actionState.asStateFlow()

    private var lastLoadedBuildingId: String? = null
    private var lastLoadedStatus: String? = null

    fun loadRooms(buildingId: String? = lastLoadedBuildingId, status: String? = lastLoadedStatus) {
        lastLoadedBuildingId = buildingId
        lastLoadedStatus = status
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

    fun loadRoomDetail(roomId: String) {
        _roomDetailState.value = RoomDetailUiState.Loading
        viewModelScope.launch {
            when (val result = roomRepository.getRoomDetail(roomId)) {
                is NetworkResult.Success -> {
                    val room = result.data.data
                    if (room != null) {
                        _roomDetailState.value = RoomDetailUiState.Success(room)
                    } else {
                        _roomDetailState.value = RoomDetailUiState.Error("Không tìm thấy thông tin phòng")
                    }
                }
                is NetworkResult.ApiError -> {
                    _roomDetailState.value = RoomDetailUiState.Error(result.error?.message ?: "Lỗi tải chi tiết phòng")
                }
                is NetworkResult.NetworkError -> {
                    _roomDetailState.value = RoomDetailUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _roomDetailState.value = RoomDetailUiState.Error("Không thể tải chi tiết phòng")
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

    fun updateRoom(roomId: String, request: RoomUpdateRequestDto) {
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            when (val result = roomRepository.updateRoom(roomId, request)) {
                is NetworkResult.Success -> {
                    _actionState.value = RoomActionUiState.Success("Cập nhật thông tin phòng thành công!", result.data.data)
                    loadRoomDetail(roomId)
                }
                is NetworkResult.ApiError -> {
                    val errorMsg = when (result.error?.code) {
                        "ROOM_HAS_ACTIVE_CONTRACT" -> "Phòng đang có hợp đồng hiệu lực, một số thông tin không thể chỉnh sửa."
                        else -> result.error?.message ?: "Cập nhật phòng thất bại"
                    }
                    _actionState.value = RoomActionUiState.Error(errorMsg)
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = RoomActionUiState.Error("Đã xảy ra lỗi khi cập nhật phòng")
                }
            }
        }
    }

    fun deleteRoom(roomId: String) {
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            when (val result = roomRepository.deleteRoom(roomId)) {
                is NetworkResult.Success -> {
                    _actionState.value = RoomActionUiState.Success("Xóa phòng thành công!")
                }
                is NetworkResult.ApiError -> {
                    val errorMsg = when (result.error?.code) {
                        "ROOM_HAS_ACTIVE_CONTRACT" -> "Không thể xóa phòng đang có hợp đồng hiệu lực hoặc chờ bàn giao."
                        else -> result.error?.message ?: "Xóa phòng thất bại"
                    }
                    _actionState.value = RoomActionUiState.Error(errorMsg)
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = RoomActionUiState.Error("Đã xảy ra lỗi khi xóa phòng")
                }
            }
        }
    }

    fun markRoomReady(roomId: String, reloadDetail: Boolean = false) {
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            when (val result = roomRepository.markRoomReady(roomId)) {
                is NetworkResult.Success -> {
                    _actionState.value = RoomActionUiState.Success("Đã chuyển phòng sang trạng thái sẵn sàng cho thuê!")
                    if (reloadDetail) {
                        loadRoomDetail(roomId)
                    } else {
                        loadRooms()
                    }
                }
                is NetworkResult.ApiError -> {
                    val errorMsg = when (result.error?.code) {
                        "ROOM_HAS_ACTIVE_CONTRACT" -> "Phòng đang có hợp đồng hiệu lực hoặc chờ bàn giao, không thể chuyển trạng thái."
                        "INVALID_ROOM_TRANSITION" -> "Chỉ có thể đánh dấu sẵn sàng đối với phòng đang bảo trì."
                        else -> result.error?.message ?: "Cập nhật trạng thái thất bại"
                    }
                    _actionState.value = RoomActionUiState.Error(errorMsg)
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

    fun uploadRoomPhoto(roomId: String, fileBytes: ByteArray, fileName: String, contentType: String) {
        val repo = mediaRepository ?: return
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            val result = repo.uploadMedia(
                ownerType = "room",
                ownerId = roomId,
                purpose = "gallery_photo",
                fileBytes = fileBytes,
                fileName = fileName,
                contentType = contentType
            )
            when (result) {
                is NetworkResult.Success -> {
                    _actionState.value = RoomActionUiState.Success("Tải ảnh phòng thành công!", result.data)
                    loadRoomDetail(roomId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = RoomActionUiState.Error(result.error?.message ?: "Tải ảnh phòng thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi kết nối mạng khi tải ảnh")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = RoomActionUiState.Error("Đã xảy ra lỗi khi tải ảnh phòng")
                }
            }
        }
    }

    fun loadRoomPoliciesAndSettings(roomId: String, buildingId: String) {
        val repo = policySettingRepository ?: return
        viewModelScope.launch {
            when (val roomRate = repo.getRatePolicies(scope = "room", roomId = roomId)) {
                is NetworkResult.Success -> {
                    _roomRatePolicyState.value = roomRate.data.data.firstOrNull { it.isActive }
                        ?: roomRate.data.data.firstOrNull()
                }
                else -> _roomRatePolicyState.value = null
            }
            if (buildingId.isNotBlank()) {
                when (val bldRate = repo.getRatePolicies(scope = "building", buildingId = buildingId)) {
                    is NetworkResult.Success -> {
                        _buildingRatePolicyState.value = bldRate.data.data.firstOrNull { it.isActive }
                    }
                    else -> _buildingRatePolicyState.value = null
                }
            }

            when (val roomBill = repo.getBillingSettings(scope = "room", roomId = roomId)) {
                is NetworkResult.Success -> {
                    _roomBillingSettingState.value = roomBill.data.data.firstOrNull { it.isActive }
                        ?: roomBill.data.data.firstOrNull()
                }
                else -> _roomBillingSettingState.value = null
            }
            if (buildingId.isNotBlank()) {
                when (val bldBill = repo.getBillingSettings(scope = "building", buildingId = buildingId)) {
                    is NetworkResult.Success -> {
                        _buildingBillingSettingState.value = bldBill.data.data.firstOrNull { it.isActive }
                    }
                    else -> _buildingBillingSettingState.value = null
                }
            }
        }
    }

    fun saveRoomRatePolicies(roomId: String, buildingId: String, rates: List<RateItemDto>) {
        val repo = policySettingRepository ?: return
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            val existing = _roomRatePolicyState.value
            val result = if (existing != null) {
                repo.updateRatePolicy(existing.id, RatePolicyUpdateRequestDto(rates = rates, isActive = true))
            } else {
                repo.createRatePolicy(
                    RatePolicyCreateRequestDto(
                        scope = "room",
                        roomId = roomId,
                        rates = rates
                    )
                )
            }
            when (result) {
                is NetworkResult.Success -> {
                    _roomRatePolicyState.value = result.data.data
                    _actionState.value = RoomActionUiState.Success("Thiết lập biểu giá riêng cho phòng thành công!")
                    loadRoomDetail(roomId)
                    loadRoomPoliciesAndSettings(roomId, buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = RoomActionUiState.Error(result.error?.message ?: "Lưu biểu giá phòng thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = RoomActionUiState.Error("Đã xảy ra lỗi khi lưu biểu giá phòng")
                }
            }
        }
    }

    fun inheritBuildingRatePolicies(roomId: String, buildingId: String) {
        val repo = policySettingRepository ?: return
        val existing = _roomRatePolicyState.value ?: return
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            when (val result = repo.updateRatePolicy(existing.id, RatePolicyUpdateRequestDto(isActive = false))) {
                is NetworkResult.Success -> {
                    _roomRatePolicyState.value = result.data.data
                    _actionState.value = RoomActionUiState.Success("Đã chuyển về kế thừa biểu giá của tòa nhà!")
                    loadRoomDetail(roomId)
                    loadRoomPoliciesAndSettings(roomId, buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = RoomActionUiState.Error(result.error?.message ?: "Cập nhật thất bại")
                }
                else -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi khi chuyển về kế thừa tòa nhà")
                }
            }
        }
    }

    fun saveRoomBillingSetting(
        roomId: String,
        buildingId: String,
        paymentDueDay: Int,
        remindDay: Int?,
        botEnabled: Boolean
    ) {
        val repo = policySettingRepository ?: return
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            val existing = _roomBillingSettingState.value
            val result = if (existing != null) {
                repo.updateBillingSetting(
                    billingSettingId = existing.id,
                    request = BillingSettingUpdateRequestDto(
                        paymentDueDay = paymentDueDay,
                        remindDay = remindDay,
                        botEnabled = botEnabled,
                        isActive = true
                    )
                )
            } else {
                repo.createBillingSetting(
                    request = BillingSettingCreateRequestDto(
                        scope = "room",
                        roomId = roomId,
                        paymentDueDay = paymentDueDay,
                        remindDay = remindDay,
                        botEnabled = botEnabled
                    )
                )
            }
            when (result) {
                is NetworkResult.Success -> {
                    _roomBillingSettingState.value = result.data.data
                    _actionState.value = RoomActionUiState.Success("Thiết lập thanh toán riêng cho phòng thành công!")
                    loadRoomPoliciesAndSettings(roomId, buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = RoomActionUiState.Error(result.error?.message ?: "Lưu cài đặt thanh toán thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = RoomActionUiState.Error("Đã xảy ra lỗi khi lưu cài đặt thanh toán")
                }
            }
        }
    }

    fun inheritBuildingBillingSetting(roomId: String, buildingId: String) {
        val repo = policySettingRepository ?: return
        val existing = _roomBillingSettingState.value ?: return
        _actionState.value = RoomActionUiState.Loading
        viewModelScope.launch {
            when (val result = repo.updateBillingSetting(existing.id, BillingSettingUpdateRequestDto(isActive = false))) {
                is NetworkResult.Success -> {
                    _roomBillingSettingState.value = result.data.data
                    _actionState.value = RoomActionUiState.Success("Đã chuyển về kế thừa cài đặt thanh toán của tòa nhà!")
                    loadRoomPoliciesAndSettings(roomId, buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = RoomActionUiState.Error(result.error?.message ?: "Cập nhật thất bại")
                }
                else -> {
                    _actionState.value = RoomActionUiState.Error("Lỗi khi chuyển về kế thừa tòa nhà")
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = RoomActionUiState.Idle
    }
}

