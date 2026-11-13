package com.rentify.app.tenant.ui.myroom

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.network.dto.room.RoomHubContractDto
import com.rentify.app.core.network.dto.room.RoomHubInvoiceDto
import com.rentify.app.core.network.dto.room.RoomHubLandlordDto
import com.rentify.app.core.network.dto.room.RoomHubRoomDto
import com.rentify.app.core.network.dto.room.RoomHubRoommateDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.tenant.data.local.TenantDevPreferences
import com.rentify.app.tenant.data.repository.TenantChatRepository
import com.rentify.app.tenant.data.repository.TenantContractRepository
import com.rentify.app.tenant.data.repository.TenantRoomRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface MyRoomUiState {
    data object Loading : MyRoomUiState
    data object Guest : MyRoomUiState
    data class NoRoom(val hasEndedContracts: Boolean = false) : MyRoomUiState
    data class SingleRoom(
        val hub: MyRoomHubResponseDto,
        val hasEndedContracts: Boolean = false,
        val totalActiveContracts: Int = 1
    ) : MyRoomUiState
    data class MultiRooms(
        val activeContracts: List<ContractMyListResponseDto>,
        val endedContracts: List<ContractMyListResponseDto>,
        val selectedTab: Int = 0,
        val currentSingleHub: MyRoomHubResponseDto? = null
    ) : MyRoomUiState
    data class Error(val message: String) : MyRoomUiState
}

sealed interface MyRoomUiEvent {
    data class OpenChat(
        val conversationId: String,
        val recipientName: String,
        val recipientAvatarUrl: String?
    ) : MyRoomUiEvent
    data class ShowToast(val message: String) : MyRoomUiEvent
}

class MyRoomViewModel(
    private val roomRepository: TenantRoomRepository,
    private val chatRepository: TenantChatRepository,
    private val contractRepository: TenantContractRepository,
    private val tokenDataStore: TokenDataStore,
    private val tenantDevPreferences: TenantDevPreferences
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<MyRoomUiState>(MyRoomUiState.Loading)
    val uiState: StateFlow<MyRoomUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<MyRoomUiEvent>()
    val uiEvent: SharedFlow<MyRoomUiEvent> = _uiEvent.asSharedFlow()

    private var currentHubData: MyRoomHubResponseDto? = null
    private var cachedActiveContracts: List<ContractMyListResponseDto> = emptyList()
    private var cachedEndedContracts: List<ContractMyListResponseDto> = emptyList()

    init {
        observeDevTweak()
    }

    private fun observeDevTweak() {
        viewModelScope.launch {
            tenantDevPreferences.devTweakHasRoom.collect {
                loadRoomHub()
            }
        }
    }

    fun loadRoomHub(isPullToRefresh: Boolean = false) {
        if (!isPullToRefresh && _uiState.value !is MyRoomUiState.SingleRoom && _uiState.value !is MyRoomUiState.MultiRooms) {
            _uiState.value = MyRoomUiState.Loading
        }

        viewModelScope.launch {
            val token = tokenDataStore.accessToken.first()
            if (token.isNullOrBlank()) {
                currentHubData = null
                _uiState.value = MyRoomUiState.Guest
                return@launch
            }

            val tweak = tenantDevPreferences.devTweakHasRoom.first()
            if (tweak == false) {
                currentHubData = null
                _uiState.value = MyRoomUiState.NoRoom()
                return@launch
            }

            if (tweak == true) {
                val mock = createMockRoomHub()
                currentHubData = mock
                cachedActiveContracts = listOf(
                    ContractMyListResponseDto(
                        id = mock.contract.id,
                        contractCode = mock.contract.contractCode,
                        roomId = mock.room.id,
                        roomName = mock.room.name,
                        buildingName = mock.room.buildingName,
                        status = "active",
                        rentalType = mock.contract.rentalType,
                        rentPrice = mock.room.rentPrice.toDouble(),
                        startDate = mock.contract.startDate,
                        endDate = mock.contract.endDate
                    )
                )
                cachedEndedContracts = emptyList()
                _uiState.value = MyRoomUiState.SingleRoom(
                    hub = mock,
                    hasEndedContracts = false,
                    totalActiveContracts = 1
                )
                return@launch
            }

            // Fetch contract list & room hub
            val contractsResult = contractRepository.getMyContracts(pageSize = 50)
            val hubResult = roomRepository.getMyRoomHub()

            val contracts: List<ContractMyListResponseDto> = when (contractsResult) {
                is NetworkResult.Success -> contractsResult.data.data
                else -> emptyList()
            }

            val activeContracts = contracts.filter {
                it.status.equals("active", ignoreCase = true) || it.status.equals("signed", ignoreCase = true)
            }
            val endedContracts = contracts.filter {
                !it.status.equals("active", ignoreCase = true) && !it.status.equals("signed", ignoreCase = true)
            }

            cachedActiveContracts = activeContracts
            cachedEndedContracts = endedContracts

            var hub: MyRoomHubResponseDto? = null
            var hubErrorMessage: String? = null

            when (hubResult) {
                is NetworkResult.Success -> {
                    hub = hubResult.data.data
                    currentHubData = hub
                }
                is NetworkResult.ApiError -> {
                    if (hubResult.code != 404 && hubResult.error?.code != "ACTIVE_CONTRACT_NOT_FOUND") {
                        hubErrorMessage = hubResult.error?.message ?: "Lỗi khi tải thông tin phòng"
                    }
                }
                is NetworkResult.NetworkError -> {
                    hubErrorMessage = "Không có kết nối mạng"
                }
                is NetworkResult.UnknownError -> {
                    hubErrorMessage = hubResult.exception?.message ?: "Đã có lỗi xảy ra"
                }
            }

            // Decide display state
            if (activeContracts.size == 1 && hub != null) {
                _uiState.value = MyRoomUiState.SingleRoom(
                    hub = hub,
                    hasEndedContracts = endedContracts.isNotEmpty(),
                    totalActiveContracts = 1
                )
            } else if (activeContracts.size > 1) {
                _uiState.value = MyRoomUiState.MultiRooms(
                    activeContracts = activeContracts,
                    endedContracts = endedContracts,
                    selectedTab = 0,
                    currentSingleHub = hub
                )
            } else if (activeContracts.isEmpty()) {
                if (endedContracts.isNotEmpty()) {
                    _uiState.value = MyRoomUiState.MultiRooms(
                        activeContracts = emptyList(),
                        endedContracts = endedContracts,
                        selectedTab = 1,
                        currentSingleHub = null
                    )
                } else if (hub != null) {
                    _uiState.value = MyRoomUiState.SingleRoom(
                        hub = hub,
                        hasEndedContracts = false,
                        totalActiveContracts = 1
                    )
                } else if (hubErrorMessage != null) {
                    _uiState.value = MyRoomUiState.Error(hubErrorMessage)
                } else {
                    currentHubData = null
                    _uiState.value = MyRoomUiState.NoRoom()
                }
            } else {
                // activeContracts.size == 1 but hub was null (e.g. hub 404)
                _uiState.value = MyRoomUiState.MultiRooms(
                    activeContracts = activeContracts,
                    endedContracts = endedContracts,
                    selectedTab = 0,
                    currentSingleHub = null
                )
            }
        }
    }

    fun toggleViewAllContracts() {
        val currentState = _uiState.value
        if (currentState is MyRoomUiState.SingleRoom) {
            _uiState.value = MyRoomUiState.MultiRooms(
                activeContracts = cachedActiveContracts,
                endedContracts = cachedEndedContracts,
                selectedTab = 0,
                currentSingleHub = currentState.hub
            )
        } else if (currentState is MyRoomUiState.MultiRooms) {
            val hub = currentState.currentSingleHub ?: currentHubData
            if (hub != null && cachedActiveContracts.size == 1) {
                _uiState.value = MyRoomUiState.SingleRoom(
                    hub = hub,
                    hasEndedContracts = cachedEndedContracts.isNotEmpty(),
                    totalActiveContracts = 1
                )
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        val currentState = _uiState.value
        if (currentState is MyRoomUiState.MultiRooms) {
            _uiState.value = currentState.copy(selectedTab = tabIndex)
        }
    }

    fun contactLandlord() {
        val landlord = currentHubData?.landlord ?: return
        viewModelScope.launch {
            when (val result = chatRepository.getOrCreateDirectConversation(landlord.id)) {
                is NetworkResult.Success -> {
                    val conversation = result.data.data
                    if (conversation != null) {
                        _uiEvent.emit(
                            MyRoomUiEvent.OpenChat(
                                conversationId = conversation.id,
                                recipientName = landlord.name,
                                recipientAvatarUrl = landlord.avatarUrl
                            )
                        )
                    } else {
                        _uiEvent.emit(MyRoomUiEvent.ShowToast("Không thể tạo cuộc hội thoại với chủ trọ"))
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiEvent.emit(MyRoomUiEvent.ShowToast(result.error?.message ?: "Lỗi khi kết nối với chủ trọ"))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(MyRoomUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(MyRoomUiEvent.ShowToast("Lỗi: ${result.exception?.message ?: "Không xác định"}"))
                }
            }
        }
    }

    fun contactRoommate(roommate: RoomHubRoommateDto) {
        viewModelScope.launch {
            when (val result = chatRepository.getOrCreateDirectConversation(roommate.id)) {
                is NetworkResult.Success -> {
                    val conversation = result.data.data
                    if (conversation != null) {
                        _uiEvent.emit(
                            MyRoomUiEvent.OpenChat(
                                conversationId = conversation.id,
                                recipientName = roommate.name,
                                recipientAvatarUrl = roommate.avatarUrl
                            )
                        )
                    } else {
                        _uiEvent.emit(MyRoomUiEvent.ShowToast("Không thể tạo cuộc hội thoại với bạn cùng phòng"))
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiEvent.emit(MyRoomUiEvent.ShowToast(result.error?.message ?: "Lỗi khi kết nối trò chuyện"))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(MyRoomUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(MyRoomUiEvent.ShowToast("Lỗi: ${result.exception?.message ?: "Không xác định"}"))
                }
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
                address = "K82/12 Hà Huy Tập, P. Thanh Khê, TP Đà Nẵng",
                photoUrls = listOf("https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800")
            ),
            landlord = RoomHubLandlordDto(
                id = "mock-landlord-id",
                name = "Nguyễn Văn Chủ",
                phone = "0909123456",
                avatarUrl = null
            ),
            contract = RoomHubContractDto(
                id = "mock-contract-id",
                contractCode = "HD-2026-0012",
                rentalType = "LONG_TERM",
                startDate = "2026-01-01",
                endDate = "2027-01-01",
                status = "active",
                signedContractUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
            ),
            currentInvoice = RoomHubInvoiceDto(
                id = "mock-invoice-id",
                code = "INV-2026-10-001",
                period = "2026-10",
                totalAmount = 3850000,
                remainingAmount = 3850000,
                dueDate = "2026-11-05T23:59:59Z",
                invoiceStatus = "issued",
                vietQrPaymentUrl = "https://img.vietqr.io/image/970422-0987654321-compact2.png?amount=3850000&addInfo=INV202610001"
            ),
            roommates = listOf(
                RoomHubRoommateDto(
                    id = "mock-mate-1",
                    name = "Trần Minh Tâm",
                    phone = "0912345678",
                    avatarUrl = null,
                    isRepresentative = true
                )
            ),
            activeIssuesCount = 0
        )
    }
}
