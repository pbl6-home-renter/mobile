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

data class DirectChatNavigationTarget(
    val conversationId: String,
    val recipientUserId: String,
    val recipientName: String,
    val recipientAvatarUrl: String?
)

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

    private val _openDirectChatEvent = MutableSharedFlow<DirectChatNavigationTarget>()
    val openDirectChatEvent: SharedFlow<DirectChatNavigationTarget> = _openDirectChatEvent.asSharedFlow()

    private val _groupMetadataUpdatedEvent = MutableSharedFlow<Unit>()
    val groupMetadataUpdatedEvent: SharedFlow<Unit> = _groupMetadataUpdatedEvent.asSharedFlow()

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
                val detail = detailResult.data.data
                _conversationDetail.value = detail
                if (detail != null && detail.members.isNotEmpty()) {
                    _members.value = detail.members
                    val me = detail.members.firstOrNull { it.userId == myId }
                    _isCurrentUserAdmin.value = me?.role.equals("admin", ignoreCase = true)
                }
            }

            val membersResult = chatRepository.getConversationMembers(conversationId)
            _isLoading.value = false

            if (membersResult is NetworkResult.Success) {
                val memberList = membersResult.data.data ?: emptyList()
                _members.value = memberList

                val me = memberList.firstOrNull { it.userId == myId }
                _isCurrentUserAdmin.value = me?.role.equals("admin", ignoreCase = true)
            } else if (membersResult is NetworkResult.ApiError && _members.value.isEmpty()) {
                _toastEvents.emit(membersResult.error?.message ?: "Không thể tải danh sách thành viên")
            }
        }
    }

    /**
     * Tra cứu hội thoại 1-1 đã có giữa người dùng và member:
     * - Nếu đã có (GET /conversations?kind=direct&otherUserId=<id> trả về 1 kết quả): mở đúng hội thoại đó.
     * - Nếu chưa có: mở màn chat 1-1 trống (conversationId = "") và chỉ tạo trên server khi gửi tin đầu tiên.
     */
    fun lookupAndOpenDirectChat(member: ConversationMemberDto) {
        val targetUserId = member.userId?.takeIf { it.isNotBlank() } ?: return
        val targetName = member.displayName.ifBlank { "Người dùng" }
        val targetAvatarUrl = member.avatarUrl

        viewModelScope.launch {
            val lookupResult = chatRepository.getConversations(
                pageNo = 1,
                pageSize = 1,
                kind = "direct",
                otherUserId = targetUserId
            )

            val existingConv = (lookupResult as? NetworkResult.Success)?.data?.data?.firstOrNull()
            _openDirectChatEvent.emit(
                DirectChatNavigationTarget(
                    conversationId = existingConv?.id.orEmpty(),
                    recipientUserId = targetUserId,
                    recipientName = existingConv?.displayName?.takeIf { it.isNotBlank() } ?: targetName,
                    recipientAvatarUrl = existingConv?.avatarUrl ?: targetAvatarUrl
                )
            )
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
                            title = updatedMeta.title,
                            displayName = updatedMeta.title ?: _conversationDetail.value?.displayName
                        )
                    }
                    _groupMetadataUpdatedEvent.emit(Unit)
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
                    _groupMetadataUpdatedEvent.emit(Unit)
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
                    _groupMetadataUpdatedEvent.emit(Unit)
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
