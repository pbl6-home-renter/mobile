package com.rentify.app.landlord.ui.operations

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportListResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportUpdateRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordIssueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface IssueListUiState {
    object Loading : IssueListUiState
    data class Success(val issues: List<IssueReportListResponseDto>) : IssueListUiState
    data class Error(val message: String) : IssueListUiState
}

sealed interface IssueDetailUiState {
    object Loading : IssueDetailUiState
    data class Success(val detail: IssueReportDetailResponseDto) : IssueDetailUiState
    data class Error(val message: String) : IssueDetailUiState
}

sealed interface IssueActionUiState {
    object Idle : IssueActionUiState
    object Loading : IssueActionUiState
    data class Success(val message: String) : IssueActionUiState
    data class Error(val message: String) : IssueActionUiState
}

class IssueViewModel(
    private val issueRepository: LandlordIssueRepository
) : BaseViewModel() {

    private val _issueListState = MutableStateFlow<IssueListUiState>(IssueListUiState.Loading)
    val issueListState: StateFlow<IssueListUiState> = _issueListState.asStateFlow()

    private val _issueDetailState = MutableStateFlow<IssueDetailUiState>(IssueDetailUiState.Loading)
    val issueDetailState: StateFlow<IssueDetailUiState> = _issueDetailState.asStateFlow()

    private val _actionState = MutableStateFlow<IssueActionUiState>(IssueActionUiState.Idle)
    val actionState: StateFlow<IssueActionUiState> = _actionState.asStateFlow()

    fun loadIssues(
        status: String? = null,
        roomId: String? = null,
        buildingId: String? = null
    ) {
        _issueListState.value = IssueListUiState.Loading
        viewModelScope.launch {
            when (val result = issueRepository.getIssues(
                status = status,
                roomId = roomId,
                buildingId = buildingId
            )) {
                is NetworkResult.Success -> {
                    _issueListState.value = IssueListUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _issueListState.value = IssueListUiState.Error(
                        result.error?.message ?: "Không thể tải danh sách sự cố"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _issueListState.value = IssueListUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _issueListState.value = IssueListUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun loadIssueDetail(issueId: String) {
        _issueDetailState.value = IssueDetailUiState.Loading
        viewModelScope.launch {
            when (val result = issueRepository.getIssueDetail(issueId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _issueDetailState.value = IssueDetailUiState.Success(detail)
                    } else {
                        _issueDetailState.value = IssueDetailUiState.Error("Không tìm thấy thông tin sự cố")
                    }
                }
                is NetworkResult.ApiError -> {
                    _issueDetailState.value = IssueDetailUiState.Error(
                        result.error?.message ?: "Không thể tải chi tiết sự cố"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _issueDetailState.value = IssueDetailUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _issueDetailState.value = IssueDetailUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun updateIssueStatus(
        issueId: String,
        status: String,
        assignedTo: String? = null,
        repairCost: Long? = null,
        resolutionNote: String? = null
    ) {
        _actionState.value = IssueActionUiState.Loading
        viewModelScope.launch {
            val request = IssueReportUpdateRequestDto(
                status = status,
                assignedTo = assignedTo,
                repairCost = repairCost,
                resolutionNote = resolutionNote
            )
            when (val result = issueRepository.updateIssue(issueId, request)) {
                is NetworkResult.Success -> {
                    val msg = when (status) {
                        "in_progress" -> "Đã tiếp nhận xử lý sự cố"
                        "resolved" -> "Đã nghiệm thu và hoàn tất sự cố"
                        "cancelled" -> "Đã hủy sự cố"
                        else -> "Đã cập nhật sự cố thành công"
                    }
                    _actionState.value = IssueActionUiState.Success(msg)
                    loadIssueDetail(issueId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = IssueActionUiState.Error(
                        result.error?.message ?: "Lỗi cập nhật sự cố"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = IssueActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = IssueActionUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun cancelIssue(issueId: String) {
        _actionState.value = IssueActionUiState.Loading
        viewModelScope.launch {
            when (val result = issueRepository.cancelIssue(issueId)) {
                is NetworkResult.Success -> {
                    _actionState.value = IssueActionUiState.Success("Đã hủy báo cáo sự cố")
                    loadIssueDetail(issueId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = IssueActionUiState.Error(
                        result.error?.message ?: "Lỗi hủy sự cố"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = IssueActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = IssueActionUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = IssueActionUiState.Idle
    }
}

