package com.rentify.app.tenant.ui.issue.detail

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.TenantIssueRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface IssueDetailUiEvent {
    data class ShowToast(val message: String) : IssueDetailUiEvent
    data class CancelSuccess(val message: String) : IssueDetailUiEvent
}

class IssueDetailViewModel(
    private val issueId: String,
    private val issueRepository: TenantIssueRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<IssueReportDetailResponseDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<IssueReportDetailResponseDto>> = _uiState.asStateFlow()

    private val _isCancelling = MutableStateFlow(false)
    val isCancelling: StateFlow<Boolean> = _isCancelling.asStateFlow()

    private val _uiEvent = MutableSharedFlow<IssueDetailUiEvent>()
    val uiEvent: SharedFlow<IssueDetailUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = issueRepository.getIssueDetail(issueId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _uiState.value = UiState.Success(detail)
                    } else {
                        _uiState.value = UiState.Error("Không tìm thấy thông tin sự cố")
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiState.value = UiState.Error(result.error?.message ?: "Lỗi khi tải chi tiết sự cố (${result.code})")
                }
                is NetworkResult.NetworkError -> {
                    _uiState.value = UiState.Error("Không có kết nối mạng. Vui lòng thử lại.")
                }
                is NetworkResult.UnknownError -> {
                    _uiState.value = UiState.Error(result.exception?.localizedMessage ?: "Đã xảy ra lỗi không xác định")
                }
            }
        }
    }

    fun cancelIssue() {
        if (_isCancelling.value) return
        _isCancelling.value = true
        viewModelScope.launch {
            when (val result = issueRepository.cancelIssue(issueId)) {
                is NetworkResult.Success -> {
                    _isCancelling.value = false
                    _uiEvent.emit(IssueDetailUiEvent.CancelSuccess("Đã hủy báo cáo sự cố"))
                    loadDetail()
                }
                is NetworkResult.ApiError -> {
                    _isCancelling.value = false
                    val msg = result.error?.message ?: "Lỗi khi hủy báo cáo sự cố (${result.code})"
                    _uiEvent.emit(IssueDetailUiEvent.ShowToast(msg))
                }
                is NetworkResult.NetworkError -> {
                    _isCancelling.value = false
                    _uiEvent.emit(IssueDetailUiEvent.ShowToast("Không có kết nối mạng. Vui lòng thử lại."))
                }
                is NetworkResult.UnknownError -> {
                    _isCancelling.value = false
                    _uiEvent.emit(IssueDetailUiEvent.ShowToast(result.exception?.localizedMessage ?: "Đã xảy ra lỗi không xác định"))
                }
            }
        }
    }
}
