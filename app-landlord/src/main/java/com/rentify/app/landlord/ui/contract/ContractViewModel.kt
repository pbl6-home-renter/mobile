package com.rentify.app.landlord.ui.contract

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.contract.ContractCreateRequestDto
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractListResponseDto
import com.rentify.app.core.network.dto.contract.ContractMemberAddRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberLeaveRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberListResponseDto
import com.rentify.app.core.network.dto.contract.ContractUpdateRequestDto
import com.rentify.app.core.network.dto.contract.DepositSettlementRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordContractRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ContractListUiState {
    object Loading : ContractListUiState
    data class Success(val contracts: List<ContractListResponseDto>) : ContractListUiState
    data class Error(val message: String) : ContractListUiState
}

sealed interface ContractDetailUiState {
    object Idle : ContractDetailUiState
    object Loading : ContractDetailUiState
    data class Success(val contract: ContractDetailResponseDto) : ContractDetailUiState
    data class Error(val message: String) : ContractDetailUiState
}

sealed interface ContractMembersUiState {
    object Idle : ContractMembersUiState
    object Loading : ContractMembersUiState
    data class Success(val members: List<ContractMemberListResponseDto>) : ContractMembersUiState
    data class Error(val message: String) : ContractMembersUiState
}

sealed interface ContractActionUiState {
    object Idle : ContractActionUiState
    object Loading : ContractActionUiState
    data class Success(val message: String, val data: Any? = null) : ContractActionUiState
    data class Error(val message: String) : ContractActionUiState
}

class ContractViewModel(
    private val contractRepository: LandlordContractRepository
) : BaseViewModel() {

    private val _contractListState = MutableStateFlow<ContractListUiState>(ContractListUiState.Loading)
    val contractListState: StateFlow<ContractListUiState> = _contractListState.asStateFlow()

    private val _contractDetailState = MutableStateFlow<ContractDetailUiState>(ContractDetailUiState.Idle)
    val contractDetailState: StateFlow<ContractDetailUiState> = _contractDetailState.asStateFlow()

    private val _contractMembersState = MutableStateFlow<ContractMembersUiState>(ContractMembersUiState.Idle)
    val contractMembersState: StateFlow<ContractMembersUiState> = _contractMembersState.asStateFlow()

    private val _actionState = MutableStateFlow<ContractActionUiState>(ContractActionUiState.Idle)
    val actionState: StateFlow<ContractActionUiState> = _actionState.asStateFlow()

    fun loadContracts(status: String? = null, roomId: String? = null) {
        _contractListState.value = ContractListUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.getContracts(status = status, roomId = roomId)) {
                is NetworkResult.Success -> {
                    _contractListState.value = ContractListUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _contractListState.value = ContractListUiState.Error(result.error?.message ?: "Lỗi tải danh sách hợp đồng")
                }
                is NetworkResult.NetworkError -> {
                    _contractListState.value = ContractListUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _contractListState.value = ContractListUiState.Error("Không thể tải danh sách hợp đồng")
                }
            }
        }
    }

    fun loadContractDetail(contractId: String) {
        _contractDetailState.value = ContractDetailUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.getContractDetail(contractId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _contractDetailState.value = ContractDetailUiState.Success(detail)
                    } else {
                        _contractDetailState.value = ContractDetailUiState.Error("Không tìm thấy hợp đồng")
                    }
                }
                is NetworkResult.ApiError -> {
                    _contractDetailState.value = ContractDetailUiState.Error(result.error?.message ?: "Lỗi tải chi tiết hợp đồng")
                }
                is NetworkResult.NetworkError -> {
                    _contractDetailState.value = ContractDetailUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _contractDetailState.value = ContractDetailUiState.Error("Không thể tải chi tiết hợp đồng")
                }
            }
        }
    }

    fun loadContractMembers(contractId: String) {
        _contractMembersState.value = ContractMembersUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.getContractMembers(contractId)) {
                is NetworkResult.Success -> {
                    _contractMembersState.value = ContractMembersUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _contractMembersState.value = ContractMembersUiState.Error(result.error?.message ?: "Lỗi tải thành viên")
                }
                is NetworkResult.NetworkError -> {
                    _contractMembersState.value = ContractMembersUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _contractMembersState.value = ContractMembersUiState.Error("Không thể tải danh sách thành viên")
                }
            }
        }
    }

    fun createContract(request: ContractCreateRequestDto) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.createContract(request)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Tạo hợp đồng thành công!", result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Tạo hợp đồng thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi tạo hợp đồng")
                }
            }
        }
    }

    fun activateContract(contractId: String, signedContractMediaId: String) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.activateContract(contractId, signedContractMediaId)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Kích hoạt hợp đồng thành công!", result.data.data)
                    loadContractDetail(contractId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Kích hoạt thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi kích hoạt")
                }
            }
        }
    }

    fun completeCheckout(contractId: String, completedOn: String) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.completeCheckout(contractId, completedOn)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Hoàn tất trả phòng thành công!", result.data.data)
                    loadContractDetail(contractId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Trả phòng thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi trả phòng")
                }
            }
        }
    }

    fun depositSettlement(contractId: String, request: DepositSettlementRequestDto) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.depositSettlement(contractId, request)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Quyết toán tiền cọc thành công!")
                    loadContractDetail(contractId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Quyết toán thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi quyết toán cọc")
                }
            }
        }
    }

    fun addContractMember(request: ContractMemberAddRequestDto) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.addContractMember(request)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Thêm thành viên thành công!", result.data.data)
                    loadContractMembers(request.contractId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Thêm thành viên thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi thêm thành viên")
                }
            }
        }
    }

    fun leaveContract(tenantId: String, request: ContractMemberLeaveRequestDto) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.leaveContract(tenantId, request)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Ghi nhận rời phòng thành công!", result.data.data)
                    loadContractMembers(request.contractId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Thao tác thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi")
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ContractActionUiState.Idle
    }
}

