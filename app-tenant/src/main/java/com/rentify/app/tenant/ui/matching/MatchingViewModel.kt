package com.rentify.app.tenant.ui.matching

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.matching.MatchRequestListResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestUserDto
import com.rentify.app.core.network.dto.matching.RoommateProfileListResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileMyResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileSaveRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.TenantChatRepository
import com.rentify.app.tenant.data.repository.TenantMatchingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface MyProfileStatus {
    data object Loading : MyProfileStatus
    data object NoProfile : MyProfileStatus
    data class Hidden(val profile: RoommateProfileMyResponseDto) : MyProfileStatus
    data class Visible(val profile: RoommateProfileMyResponseDto) : MyProfileStatus
}

sealed interface MatchingUiEvent {
    data class ShowToast(val message: String) : MatchingUiEvent
    data class OpenChat(val conversationId: String, val recipientName: String, val recipientAvatarUrl: String?) : MatchingUiEvent
    data object RequestSentSuccess : MatchingUiEvent
    data class RequireProfileVisibility(val reason: String) : MatchingUiEvent
    data object RequireCreateProfile : MatchingUiEvent
}

class MatchingViewModel(
    private val matchingRepository: TenantMatchingRepository,
    private val chatRepository: TenantChatRepository,
    private val tokenDataStore: TokenDataStore
) : BaseViewModel() {

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    private val _myProfileStatus = MutableStateFlow<MyProfileStatus>(MyProfileStatus.Loading)
    val myProfileStatus: StateFlow<MyProfileStatus> = _myProfileStatus.asStateFlow()

    private val _feedState = MutableStateFlow<UiState<List<RoommateProfileListResponseDto>>>(UiState.Idle)
    val feedState: StateFlow<UiState<List<RoommateProfileListResponseDto>>> = _feedState.asStateFlow()

    private val _requestsState = MutableStateFlow<UiState<List<MatchRequestListResponseDto>>>(UiState.Idle)
    val requestsState: StateFlow<UiState<List<MatchRequestListResponseDto>>> = _requestsState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<MatchingUiEvent>()
    val uiEvent: SharedFlow<MatchingUiEvent> = _uiEvent.asSharedFlow()

    private var currentAreaFilter: String? = null
    private var currentDirection: String = "incoming"

    init {
        checkAuthAndLoad()
    }

    fun checkAuthAndLoad() {
        viewModelScope.launch {
            val token = tokenDataStore.accessToken.first()
            val loggedIn = !token.isNullOrBlank()
            _isLoggedIn.value = loggedIn

            if (loggedIn) {
                loadMyProfile()
                loadFeed(isRefresh = true)
                loadRequests(currentDirection, isRefresh = true)
            } else {
                _myProfileStatus.value = MyProfileStatus.Loading
                _feedState.value = UiState.Idle
                _requestsState.value = UiState.Idle
            }
        }
    }

    fun loadMyProfile() {
        viewModelScope.launch {
            _myProfileStatus.value = MyProfileStatus.Loading
            when (val result = matchingRepository.getMyRoommateProfile()) {
                is NetworkResult.Success -> {
                    val profile = result.data.data
                    if (profile != null) {
                        if (profile.isVisible) {
                            _myProfileStatus.value = MyProfileStatus.Visible(profile)
                        } else {
                            _myProfileStatus.value = MyProfileStatus.Hidden(profile)
                        }
                    } else {
                        _myProfileStatus.value = MyProfileStatus.NoProfile
                    }
                }
                is NetworkResult.ApiError -> {
                    _myProfileStatus.value = MyProfileStatus.NoProfile
                }
                is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    _myProfileStatus.value = MyProfileStatus.NoProfile
                }
            }
        }
    }

    fun quickEnableVisibility() {
        val current = _myProfileStatus.value
        if (current !is MyProfileStatus.Hidden) return

        val profile = current.profile
        viewModelScope.launch {
            val request = RoommateProfileSaveRequestDto(
                birthYear = profile.birthYear,
                occupation = profile.occupation,
                preferredProvince = profile.preferredProvince,
                preferredDistrict = profile.preferredDistrict,
                preferredArea = profile.preferredArea,
                budgetMin = profile.budgetMin,
                budgetMax = profile.budgetMax,
                moveInOn = profile.moveInOn,
                lifestyle = profile.lifestyle,
                personality = profile.personality,
                isVisible = true,
                bio = profile.bio
            )
            when (val result = matchingRepository.saveMyRoommateProfile(request)) {
                is NetworkResult.Success -> {
                    _myProfileStatus.value = MyProfileStatus.Visible(profile.copy(isVisible = true))
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Đã bật hiển thị hồ sơ ghép bạn thành công!"))
                    loadFeed(isRefresh = true)
                }
                is NetworkResult.ApiError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.error?.message ?: "Không thể cập nhật trạng thái"))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.exception?.message ?: "Đã có lỗi xảy ra"))
                }
            }
        }
    }

    fun loadFeed(isRefresh: Boolean = false, areaFilter: String? = currentAreaFilter) {
        currentAreaFilter = areaFilter
        if (isRefresh || _feedState.value !is UiState.Success) {
            _feedState.value = UiState.Loading
        }

        viewModelScope.launch {
            when (val result = matchingRepository.getRoommateProfiles(preferredArea = areaFilter)) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    _feedState.value = UiState.Success(list)
                }
                is NetworkResult.ApiError -> {
                    _feedState.value = UiState.Error(result.error?.message ?: "Không thể tải danh sách bạn ở")
                }
                is NetworkResult.NetworkError -> {
                    _feedState.value = UiState.Error("Không có kết nối mạng, vui lòng thử lại")
                }
                is NetworkResult.UnknownError -> {
                    _feedState.value = UiState.Error(result.exception?.message ?: "Đã có lỗi xảy ra")
                }
            }
        }
    }

    fun loadRequests(direction: String = currentDirection, isRefresh: Boolean = false) {
        currentDirection = direction
        if (isRefresh || _requestsState.value !is UiState.Success) {
            _requestsState.value = UiState.Loading
        }

        viewModelScope.launch {
            when (val result = matchingRepository.getMatchRequests(direction = direction)) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    _requestsState.value = UiState.Success(list)
                }
                is NetworkResult.ApiError -> {
                    _requestsState.value = UiState.Error(result.error?.message ?: "Không thể tải danh sách yêu cầu")
                }
                is NetworkResult.NetworkError -> {
                    _requestsState.value = UiState.Error("Không có kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _requestsState.value = UiState.Error(result.exception?.message ?: "Đã có lỗi xảy ra")
                }
            }
        }
    }

    fun sendMatchRequest(targetId: String) {
        when (val status = _myProfileStatus.value) {
            is MyProfileStatus.NoProfile -> {
                viewModelScope.launch {
                    _uiEvent.emit(MatchingUiEvent.RequireCreateProfile)
                }
                return
            }
            is MyProfileStatus.Hidden -> {
                viewModelScope.launch {
                    _uiEvent.emit(
                        MatchingUiEvent.RequireProfileVisibility(
                            "Hồ sơ của bạn đang ở chế độ ẩn. Vui lòng bật hiển thị hồ sơ để đối phương có thể xem thông tin của bạn trước khi kết nối."
                        )
                    )
                }
                return
            }
            is MyProfileStatus.Loading, is MyProfileStatus.Visible -> {
                // Cho phép gửi nếu đang Visible (hoặc đang tải)
            }
        }

        viewModelScope.launch {
            when (val result = matchingRepository.createMatchRequest(targetId)) {
                is NetworkResult.Success -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Đã gửi lời mời ghép đôi thành công!"))
                    _uiEvent.emit(MatchingUiEvent.RequestSentSuccess)
                    loadRequests(currentDirection, isRefresh = true)
                }
                is NetworkResult.ApiError -> {
                    val msg = when (result.error?.code) {
                        "DUPLICATE_MATCH_REQUEST" -> "Bạn đã gửi lời mời tới người này rồi"
                        "SELF_MATCHING_NOT_ALLOWED" -> "Không thể gửi lời mời cho chính mình"
                        else -> result.error?.message ?: "Không thể gửi lời mời ghép đôi"
                    }
                    _uiEvent.emit(MatchingUiEvent.ShowToast(msg))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.exception?.message ?: "Lỗi không xác định"))
                }
            }
        }
    }

    fun acceptRequest(request: MatchRequestListResponseDto) {
        viewModelScope.launch {
            when (val result = matchingRepository.acceptMatchRequest(request.id)) {
                is NetworkResult.Success -> {
                    val conversationId = result.data.data?.conversationId
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Đã chấp nhận lời mời ghép đôi!"))
                    loadRequests(currentDirection, isRefresh = true)
                    if (!conversationId.isNullOrBlank()) {
                        _uiEvent.emit(
                            MatchingUiEvent.OpenChat(
                                conversationId = conversationId,
                                recipientName = request.requester.name,
                                recipientAvatarUrl = request.requester.avatarUrl
                            )
                        )
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.error?.message ?: "Lỗi khi chấp nhận yêu cầu"))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.exception?.message ?: "Lỗi không xác định"))
                }
            }
        }
    }

    fun rejectRequest(request: MatchRequestListResponseDto) {
        viewModelScope.launch {
            when (val result = matchingRepository.rejectMatchRequest(request.id)) {
                is NetworkResult.Success -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Đã từ chối lời mời ghép đôi"))
                    loadRequests(currentDirection, isRefresh = true)
                }
                is NetworkResult.ApiError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.error?.message ?: "Lỗi khi từ chối yêu cầu"))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.exception?.message ?: "Lỗi không xác định"))
                }
            }
        }
    }

    fun withdrawRequest(request: MatchRequestListResponseDto) {
        viewModelScope.launch {
            when (val result = matchingRepository.withdrawMatchRequest(request.id)) {
                is NetworkResult.Success -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Đã rút lại lời mời ghép đôi"))
                    loadRequests(currentDirection, isRefresh = true)
                }
                is NetworkResult.ApiError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.error?.message ?: "Lỗi khi rút lời mời"))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.exception?.message ?: "Lỗi không xác định"))
                }
            }
        }
    }

    fun contactUser(user: MatchRequestUserDto) {
        viewModelScope.launch {
            when (val result = chatRepository.getOrCreateDirectConversation(user.id)) {
                is NetworkResult.Success -> {
                    val conversation = result.data.data
                    if (conversation != null) {
                        _uiEvent.emit(
                            MatchingUiEvent.OpenChat(
                                conversationId = conversation.id,
                                recipientName = user.name,
                                recipientAvatarUrl = user.avatarUrl
                            )
                        )
                    } else {
                        _uiEvent.emit(MatchingUiEvent.ShowToast("Không thể tạo cuộc hội thoại"))
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast(result.error?.message ?: "Lỗi khi kết nối"))
                }
                is NetworkResult.NetworkError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Không có kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiEvent.emit(MatchingUiEvent.ShowToast("Đã có lỗi xảy ra"))
                }
            }
        }
    }
}

