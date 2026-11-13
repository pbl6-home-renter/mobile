package com.rentify.app.tenant.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.tenant.data.repository.TenantRoomRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchResultUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val rooms: List<RoomPublicListResponseDto> = emptyList(),
    val filter: RoomFilter = RoomFilter.default(),
    val pageNo: Int = 1,
    val hasNextPage: Boolean = false,
    val totalItems: Int = 0,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SearchResultEvent {
    data class ShowToast(val message: String) : SearchResultEvent
    data class ShowError(val message: String) : SearchResultEvent
}

class SearchResultViewModel(
    private val roomRepository: TenantRoomRepository,
    private val tokenDataStore: TokenDataStore,
    initialFilter: RoomFilter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchResultUiState(filter = initialFilter))
    val uiState: StateFlow<SearchResultUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchResultEvent>()
    val events: SharedFlow<SearchResultEvent> = _events.asSharedFlow()

    init {
        checkAuth()
        loadInitial()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val token = tokenDataStore.accessToken.first()
            _uiState.update { it.copy(isLoggedIn = !token.isNullOrBlank()) }
        }
    }

    fun applyFilter(newFilter: RoomFilter) {
        _uiState.update { it.copy(filter = newFilter) }
        loadInitial()
    }

    fun updateKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed == _uiState.value.filter.keyword) return
        _uiState.update { it.copy(filter = it.filter.copy(keyword = trimmed.ifBlank { null })) }
        loadInitial()
    }

    fun clearAllFilters() {
        _uiState.update { it.copy(filter = RoomFilter.default()) }
        loadInitial()
    }

    fun refresh() {
        loadInitial(isRefresh = true)
    }

    fun loadInitial(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val filter = _uiState.value.filter
            val genderQuery = if (filter.genderPolicy != "any") filter.genderPolicy else null

            when (val result = roomRepository.getPublicRooms(
                province = filter.province,
                ward = filter.ward,
                minPrice = filter.minPrice,
                maxPrice = filter.maxPrice,
                genderPolicy = genderQuery,
                sort = filter.sort,
                order = filter.order,
                pageNo = 1,
                pageSize = 20
            )) {
                is NetworkResult.Success -> {
                    val rawList = result.data.data ?: emptyList()
                    val filteredList = filterByKeyword(rawList, filter.keyword)
                    val pagination = result.data.pagination

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            rooms = filteredList,
                            pageNo = 1,
                            hasNextPage = pagination?.hasNextPage == true,
                            totalItems = pagination?.totalItems ?: filteredList.size,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.ApiError -> {
                    val msg = result.error?.message ?: "Lỗi tải kết quả tìm kiếm"
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = msg) }
                    _events.emit(SearchResultEvent.ShowError(msg))
                }
                is NetworkResult.NetworkError -> {
                    val msg = "Lỗi kết nối mạng, vui lòng kiểm tra lại"
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = msg) }
                    _events.emit(SearchResultEvent.ShowError(msg))
                }
                is NetworkResult.UnknownError -> {
                    val msg = result.exception?.message ?: "Có lỗi xảy ra"
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = msg) }
                    _events.emit(SearchResultEvent.ShowError(msg))
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

            val filter = currentState.filter
            val genderQuery = if (filter.genderPolicy != "any") filter.genderPolicy else null

            when (val result = roomRepository.getPublicRooms(
                province = filter.province,
                ward = filter.ward,
                minPrice = filter.minPrice,
                maxPrice = filter.maxPrice,
                genderPolicy = genderQuery,
                sort = filter.sort,
                order = filter.order,
                pageNo = nextPage,
                pageSize = 20
            )) {
                is NetworkResult.Success -> {
                    val newRawList = result.data.data ?: emptyList()
                    val newFilteredList = filterByKeyword(newRawList, filter.keyword)
                    val pagination = result.data.pagination

                    _uiState.update { current ->
                        current.copy(
                            isLoadingMore = false,
                            rooms = current.rooms + newFilteredList,
                            pageNo = nextPage,
                            hasNextPage = pagination?.hasNextPage == true,
                            totalItems = pagination?.totalItems ?: (current.rooms.size + newFilteredList.size)
                        )
                    }
                }
                is NetworkResult.ApiError, is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _events.emit(SearchResultEvent.ShowToast("Không thể tải thêm kết quả"))
                }
            }
        }
    }

    fun toggleFavorite(room: RoomPublicListResponseDto) {
        if (!_uiState.value.isLoggedIn) {
            viewModelScope.launch {
                _events.emit(SearchResultEvent.ShowToast("Vui lòng đăng nhập để lưu phòng trọ"))
            }
            return
        }

        val originalFavorited = room.isFavorited
        val targetFavorited = !originalFavorited

        // Optimistic update
        _uiState.update { current ->
            current.copy(
                rooms = current.rooms.map {
                    if (it.id == room.id) it.copy(isFavorited = targetFavorited) else it
                }
            )
        }

        viewModelScope.launch {
            val result = if (targetFavorited) {
                roomRepository.addFavoriteRoom(room.id)
            } else {
                roomRepository.removeFavoriteRoom(room.id)
            }

            when (result) {
                is NetworkResult.Success -> {
                    val message = if (targetFavorited) "Đã lưu phòng ${room.name}" else "Đã bỏ lưu phòng ${room.name}"
                    _events.emit(SearchResultEvent.ShowToast(message))
                }
                is NetworkResult.ApiError, is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    // Rollback
                    _uiState.update { current ->
                        current.copy(
                            rooms = current.rooms.map {
                                if (it.id == room.id) it.copy(isFavorited = originalFavorited) else it
                            }
                        )
                    }
                    _events.emit(SearchResultEvent.ShowError("Thao tác thất bại, vui lòng thử lại"))
                }
            }
        }
    }

    private fun filterByKeyword(list: List<RoomPublicListResponseDto>, keyword: String?): List<RoomPublicListResponseDto> {
        if (keyword.isNullOrBlank()) return list
        val kw = keyword.lowercase()
        return list.filter {
            it.name.lowercase().contains(kw) ||
                    it.address?.lowercase()?.contains(kw) == true ||
                    it.district.lowercase().contains(kw) ||
                    it.province.lowercase().contains(kw)
        }
    }
}
