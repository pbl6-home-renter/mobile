package com.rentify.app.landlord.ui.chat

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConversationListUiState {
    object Loading : ConversationListUiState
    data class Success(val conversations: List<ConversationListResponseDto>) : ConversationListUiState
    data class Error(val message: String) : ConversationListUiState
}

class LandlordChatViewModel(
    private val chatRepository: LandlordChatRepository
) : BaseViewModel() {

    private val _conversationListState = MutableStateFlow<ConversationListUiState>(ConversationListUiState.Loading)
    val conversationListState: StateFlow<ConversationListUiState> = _conversationListState.asStateFlow()

    fun loadConversations(kind: String? = null, q: String? = null) {
        _conversationListState.value = ConversationListUiState.Loading
        viewModelScope.launch {
            when (val result = chatRepository.getConversations(kind = kind, q = q)) {
                is NetworkResult.Success -> {
                    _conversationListState.value = ConversationListUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _conversationListState.value = ConversationListUiState.Error(
                        result.error?.message ?: "Không thể tải danh sách cuộc trò chuyện"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _conversationListState.value = ConversationListUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _conversationListState.value = ConversationListUiState.Error("Lỗi không xác định")
                }
            }
        }
    }
}

