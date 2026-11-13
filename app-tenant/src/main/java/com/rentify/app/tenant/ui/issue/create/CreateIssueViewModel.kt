package com.rentify.app.tenant.ui.issue.create

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.issue.IssueReportCreateRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.tenant.data.repository.TenantIssueRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateIssueUiEvent {
    data class Success(val issueId: String, val message: String) : CreateIssueUiEvent
    data class ShowError(val message: String) : CreateIssueUiEvent
    data class ValidationError(val message: String) : CreateIssueUiEvent
}

class CreateIssueViewModel(
    private val roomId: String,
    private val issueRepository: TenantIssueRepository
) : BaseViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("PLUMBING")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedUrgency = MutableStateFlow("MEDIUM")
    val selectedUrgency: StateFlow<String> = _selectedUrgency.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CreateIssueUiEvent>()
    val uiEvent: SharedFlow<CreateIssueUiEvent> = _uiEvent.asSharedFlow()

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setUrgency(urgency: String) {
        _selectedUrgency.value = urgency
    }

    fun submitIssue(title: String, description: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(CreateIssueUiEvent.ValidationError("Vui lòng nhập tiêu đề sự cố"))
            }
            return
        }

        if (roomId.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(CreateIssueUiEvent.ShowError("Không xác định được phòng trọ để báo sự cố"))
            }
            return
        }

        val request = IssueReportCreateRequestDto(
            roomId = roomId,
            title = trimmedTitle,
            category = _selectedCategory.value,
            urgency = _selectedUrgency.value,
            description = description.trim().ifEmpty { null },
            issuePhotoMediaIds = emptyList()
        )

        _isLoading.value = true
        viewModelScope.launch {
            when (val result = issueRepository.createIssue(request)) {
                is NetworkResult.Success -> {
                    _isLoading.value = false
                    val createdIssue = result.data.data
                    val issueId = createdIssue?.id.orEmpty()
                    _uiEvent.emit(CreateIssueUiEvent.Success(issueId, "Gửi báo cáo sự cố thành công!"))
                }
                is NetworkResult.ApiError -> {
                    _isLoading.value = false
                    val message = result.error?.message ?: "Lỗi khi tạo báo cáo sự cố (${result.code})"
                    _uiEvent.emit(CreateIssueUiEvent.ShowError(message))
                }
                is NetworkResult.NetworkError -> {
                    _isLoading.value = false
                    _uiEvent.emit(CreateIssueUiEvent.ShowError("Không có kết nối mạng. Vui lòng thử lại."))
                }
                is NetworkResult.UnknownError -> {
                    _isLoading.value = false
                    _uiEvent.emit(CreateIssueUiEvent.ShowError(result.exception?.localizedMessage ?: "Đã xảy ra lỗi không xác định"))
                }
            }
        }
    }
}
