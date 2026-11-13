package com.rentify.app.tenant.ui.chat.group

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.tenant.data.repository.TenantChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GroupInfoViewModel(
    val conversationId: String,
    private val chatRepository: TenantChatRepository,
    private val tokenDataStore: TokenDataStore
) : BaseViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _conversationDetail = MutableStateFlow<ConversationDetailResponseDto?>(null)
    val conversationDetail: StateFlow<ConversationDetailResponseDto?> = _conversationDetail.asStateFlow()

    private val _members = MutableStateFlow<List<ConversationMemberDto>>(emptyList())
    val members: StateFlow<List<ConversationMemberDto>> = _members.asStateFlow()

    private val _isCurrentUserAdmin = MutableStateFlow(false)
    val isCurrentUserAdmin: StateFlow<Boolean> = _isCurrentUserAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _leaveSuccessEvent = MutableSharedFlow<Unit>()
    val leaveSuccessEvent: SharedFlow<Unit> = _leaveSuccessEvent.asSharedFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            val myId = tokenDataStore.userId.first()
            _currentUserId.value = myId

            val detailResult = chatRepository.getConversationDetail(conversationId)
            if (detailResult is NetworkResult.Success) {
                _conversationDetail.value = detailResult.data.data
            }

            val membersResult = chatRepository.getConversationMembers(conversationId)
            _isLoading.value = false

            if (membersResult is NetworkResult.Success) {
                val memberList = membersResult.data.data ?: emptyList()
                _members.value = memberList

                val me = memberList.firstOrNull { it.userId == myId }
                _isCurrentUserAdmin.value = me?.role.equals("admin", ignoreCase = true)
            } else if (membersResult is NetworkResult.ApiError) {
                _toastEvents.emit(membersResult.error?.message ?: "Không thể tải danh sách thành viên")
            }
        }
    }

    fun updateGroupName(newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.updateConversationMetadata(conversationId, title = trimmed)
            _isLoading.value = false

            when (result) {
                is NetworkResult.Success -> {
                    val updatedMeta = result.data.data
                    if (updatedMeta != null) {
                        _conversationDetail.value = _conversationDetail.value?.copy(
                            title = updatedMeta.title
                        )
                    }
                    _toastEvents.emit("Đã cập nhật tên nhóm")
                }
                is NetworkResult.ApiError -> {
                    _toastEvents.emit(result.error?.message ?: "Lỗi khi cập nhật tên nhóm")
                }
                else -> {
                    _toastEvents.emit("Không thể cập nhật tên nhóm")
                }
            }
        }
    }

    fun addMembers(userIds: List<String>) {
        if (userIds.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.addConversationMembers(conversationId, userIds)
            _isLoading.value = false

            when (result) {
                is NetworkResult.Success -> {
                    _toastEvents.emit("Đã thêm thành viên")
                    loadData()
                }
                is NetworkResult.ApiError -> {
                    _toastEvents.emit(result.error?.message ?: "Lỗi khi thêm thành viên")
                }
                else -> {
                    _toastEvents.emit("Không thể thêm thành viên")
                }
            }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.removeConversationMember(conversationId, userId)
            _isLoading.value = false

            when (result) {
                is NetworkResult.Success -> {
                    _toastEvents.emit("Đã xóa thành viên khỏi nhóm")
                    loadData()
                }
                is NetworkResult.ApiError -> {
                    _toastEvents.emit(result.error?.message ?: "Lỗi khi xóa thành viên")
                }
                else -> {
                    _toastEvents.emit("Không thể xóa thành viên")
                }
            }
        }
    }

    fun updateMemberRole(userId: String, newRole: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.updateMemberRole(conversationId, userId, newRole)
            _isLoading.value = false

            when (result) {
                is NetworkResult.Success -> {
                    val roleLabel = if (newRole == "admin") "Trưởng nhóm" else "Thành viên"
                    _toastEvents.emit("Đã phân quyền thành $roleLabel")
                    loadData()
                }
                is NetworkResult.ApiError -> {
                    _toastEvents.emit(result.error?.message ?: "Lỗi khi cập nhật quyền")
                }
                else -> {
                    _toastEvents.emit("Không thể cập nhật quyền")
                }
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.leaveConversation(conversationId)
            _isLoading.value = false

            when (result) {
                is NetworkResult.Success -> {
                    _toastEvents.emit("Đã rời khỏi nhóm")
                    _leaveSuccessEvent.emit(Unit)
                }
                is NetworkResult.ApiError -> {
                    _toastEvents.emit(result.error?.message ?: "Lỗi khi rời khỏi nhóm")
                }
                else -> {
                    _toastEvents.emit("Không thể rời khỏi nhóm")
                }
            }
        }
    }
}
