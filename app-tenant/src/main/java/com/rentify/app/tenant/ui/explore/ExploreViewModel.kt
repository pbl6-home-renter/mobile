package com.rentify.app.tenant.ui.explore

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.user.UserRepository
import com.rentify.app.tenant.data.local.TenantDevPreferences
import com.rentify.app.tenant.data.repository.TenantRoomRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.network.dto.room.RoomHubContractDto
import com.rentify.app.core.network.dto.room.RoomHubInvoiceDto
import com.rentify.app.core.network.dto.room.RoomHubLandlordDto
import com.rentify.app.core.network.dto.room.RoomHubRoomDto
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val roomRepository: TenantRoomRepository,
    private val userRepository: UserRepository,
    private val tokenDataStore: TokenDataStore,
    private val tenantDevPreferences: TenantDevPreferences
) : BaseViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<UserCurrentResponseDto?>(null)
    val currentUser: StateFlow<UserCurrentResponseDto?> = _currentUser.asStateFlow()

    private val _myRoomHub = MutableStateFlow<MyRoomHubResponseDto?>(null)
    val myRoomHub: StateFlow<MyRoomHubResponseDto?> = _myRoomHub.asStateFlow()

    private val _hasRoomVisible = MutableStateFlow(false)
    val hasRoomVisible: StateFlow<Boolean> = _hasRoomVisible.asStateFlow()

    private val _hasUnpaidInvoice = MutableStateFlow(false)
    val hasUnpaidInvoice: StateFlow<Boolean> = _hasUnpaidInvoice.asStateFlow()

    private val _roomsState = MutableStateFlow<UiState<List<RoomPublicListResponseDto>>>(UiState.Loading)
    val roomsState: StateFlow<UiState<List<RoomPublicListResponseDto>>> = _roomsState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _devTweakHasRoom = MutableStateFlow<Boolean?>(null)
    val devTweakHasRoom: StateFlow<Boolean?> = _devTweakHasRoom.asStateFlow()

    private var currentPage = 1
    private var hasNextPage = true
    private val currentRoomsList = mutableListOf<RoomPublicListResponseDto>()

    private var selectedProvince: String? = null
    private var selectedWard: String? = null

    init {
        observeDevTweak()
        checkAuthAndLoadData()
        loadRooms(isRefresh = true)
    }

    private fun observeDevTweak() {
        viewModelScope.launch {
            tenantDevPreferences.devTweakHasRoom.collect { tweakValue ->
                _devTweakHasRoom.value = tweakValue
                recomputeRoomVisibility()
            }
        }
    }

    fun checkAuthAndLoadData() {
        viewModelScope.launch {
            val token = tokenDataStore.accessToken.first()
            val loggedIn = !token.isNullOrBlank()
            _isLoggedIn.value = loggedIn

            if (loggedIn) {
                fetchCurrentUser()
                fetchMyRoomHub()
            } else {
                _currentUser.value = null
                _myRoomHub.value = null
                _hasUnpaidInvoice.value = false
                recomputeRoomVisibility()
            }
        }
    }

    private fun fetchCurrentUser() {
        launchSafe {
            when (val result = userRepository.getCurrentUser()) {
                is NetworkResult.Success -> {
                    _currentUser.value = result.data.data
                }
                else -> Unit
            }
        }
    }

    private fun fetchMyRoomHub() {
        launchSafe {
            when (val result = roomRepository.getMyRoomHub()) {
                is NetworkResult.Success -> {
                    val hubData = result.data.data
                    _myRoomHub.value = hubData
                    _hasUnpaidInvoice.value = hubData?.currentInvoice != null
                    recomputeRoomVisibility()
                }
                is NetworkResult.ApiError -> {
                    _myRoomHub.value = null
                    _hasUnpaidInvoice.value = false
                    recomputeRoomVisibility()
                }
                else -> Unit
            }
        }
    }

    private fun recomputeRoomVisibility() {
        val tweak = _devTweakHasRoom.value
        val loggedIn = _isLoggedIn.value

        if (!loggedIn) {
            _hasRoomVisible.value = false
            return
        }

        if (tweak != null) {
            _hasRoomVisible.value = tweak
            if (tweak && _myRoomHub.value == null) {
                _myRoomHub.value = createMockRoomHub()
                _hasUnpaidInvoice.value = true
            }
        } else {
            _hasRoomVisible.value = _myRoomHub.value != null
        }
    }

    fun setDevTweakHasRoom(hasRoom: Boolean?) {
        viewModelScope.launch {
            tenantDevPreferences.setDevTweakHasRoom(hasRoom)
        }
    }

    fun loadRooms(isRefresh: Boolean = false) {
        if (isRefresh) {
            currentPage = 1
            hasNextPage = true
            if (currentRoomsList.isEmpty()) {
                _roomsState.value = UiState.Loading
            }
        }

        launchSafe {
            val result = roomRepository.getPublicRooms(
                province = selectedProvince,
                ward = selectedWard,
                pageNo = currentPage,
                pageSize = 20
            )

            when (result) {
                is NetworkResult.Success -> {
                    val newRooms = result.data.data
                    val pagination = result.data.pagination

                    hasNextPage = pagination?.hasNextPage ?: (newRooms.size >= 20)

                    if (isRefresh) {
                        currentRoomsList.clear()
                    }
                    currentRoomsList.addAll(newRooms)
                    _roomsState.value = UiState.Success(currentRoomsList.toList())
                }
                is NetworkResult.ApiError -> {
                    if (currentRoomsList.isEmpty()) {
                        _roomsState.value = UiState.Error(result.error?.message ?: "Lỗi tải danh sách phòng")
                    }
                }
                is NetworkResult.NetworkError -> {
                    if (currentRoomsList.isEmpty()) {
                        _roomsState.value = UiState.Error("Không có kết nối mạng. Vui lòng kiểm tra lại.")
                    }
                }
                is NetworkResult.UnknownError -> {
                    if (currentRoomsList.isEmpty()) {
                        _roomsState.value = UiState.Error("Đã xảy ra lỗi không xác định. Vui lòng thử lại.")
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !hasNextPage) return

        _isLoadingMore.value = true
        currentPage++

        viewModelScope.launch {
            val result = roomRepository.getPublicRooms(
                province = selectedProvince,
                ward = selectedWard,
                pageNo = currentPage,
                pageSize = 20
            )

            _isLoadingMore.value = false

            when (result) {
                is NetworkResult.Success -> {
                    val newRooms = result.data.data
                    val pagination = result.data.pagination

                    hasNextPage = pagination?.hasNextPage ?: (newRooms.isNotEmpty())
                    currentRoomsList.addAll(newRooms)
                    _roomsState.value = UiState.Success(currentRoomsList.toList())
                }
                else -> {
                    currentPage--
                }
            }
        }
    }

    fun filterByArea(areaName: String) {
        selectedWard = areaName
        loadRooms(isRefresh = true)
    }

    fun clearAreaFilter() {
        selectedWard = null
        selectedProvince = null
        loadRooms(isRefresh = true)
    }

    fun toggleFavorite(room: RoomPublicListResponseDto) {
        val originalFavorited = room.isFavorited
        val updatedFavorited = !originalFavorited

        val updatedList = currentRoomsList.map { item ->
            if (item.id == room.id) item.copy(isFavorited = updatedFavorited) else item
        }
        currentRoomsList.clear()
        currentRoomsList.addAll(updatedList)
        _roomsState.value = UiState.Success(currentRoomsList.toList())

        viewModelScope.launch {
            val result = if (updatedFavorited) {
                roomRepository.addFavoriteRoom(room.id)
            } else {
                roomRepository.removeFavoriteRoom(room.id)
            }

            if (result !is NetworkResult.Success) {
                val revertedList = currentRoomsList.map { item ->
                    if (item.id == room.id) item.copy(isFavorited = originalFavorited) else item
                }
                currentRoomsList.clear()
                currentRoomsList.addAll(revertedList)
                _roomsState.value = UiState.Success(currentRoomsList.toList())
            }
        }
    }

    private fun createMockRoomHub(): MyRoomHubResponseDto {
        return MyRoomHubResponseDto(
            room = RoomHubRoomDto(
                id = "mock-room-id",
                name = "Phòng 305",
                roomType = "Gác lửng",
                area = "25.0",
                rentPrice = 3500000,
                buildingId = "mock-building-id",
                buildingName = "Nhà trọ An Bình",
                address = "K82/12 Hà Huy Tập, P. Thanh Khê"
            ),
            landlord = RoomHubLandlordDto(
                id = "mock-landlord-id",
                name = "Nguyễn Văn Chủ",
                phone = "0909123456"
            ),
            contract = RoomHubContractDto(
                id = "mock-contract-id",
                contractCode = "HD-2026-0012",
                rentalType = "LONG_TERM",
                startDate = "2026-01-01",
                endDate = "2027-01-01",
                status = "Đang hiệu lực"
            ),
            currentInvoice = RoomHubInvoiceDto(
                id = "mock-invoice-id",
                code = "INV-2026-10-001",
                period = "tháng 10",
                totalAmount = 3850000,
                remainingAmount = 3850000,
                dueDate = "05/11",
                invoiceStatus = "issued"
            ),
            activeIssuesCount = 0
        )
    }
}
