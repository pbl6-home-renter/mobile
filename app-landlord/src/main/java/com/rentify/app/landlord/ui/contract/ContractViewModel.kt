package com.rentify.app.landlord.ui.contract

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.contract.ContractCreateRequestDto
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractListResponseDto
import com.rentify.app.core.network.dto.contract.ContractMemberAddRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberLeaveRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberListResponseDto
import com.rentify.app.core.network.dto.contract.ContractTemplateItemDto
import com.rentify.app.core.network.dto.contract.ContractUpdateRequestDto
import com.rentify.app.core.network.dto.contract.DepositSettlementRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordBuildingRepository
import com.rentify.app.landlord.data.repository.LandlordContractRepository
import com.rentify.app.landlord.data.repository.LandlordMediaRepository
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
    private val contractRepository: LandlordContractRepository,
    private val buildingRepository: LandlordBuildingRepository? = null,
    private val mediaRepository: LandlordMediaRepository? = null
) : BaseViewModel() {

    private val _contractListState = MutableStateFlow<ContractListUiState>(ContractListUiState.Loading)
    val contractListState: StateFlow<ContractListUiState> = _contractListState.asStateFlow()

    private val _contractDetailState = MutableStateFlow<ContractDetailUiState>(ContractDetailUiState.Idle)
    val contractDetailState: StateFlow<ContractDetailUiState> = _contractDetailState.asStateFlow()

    private val _contractMembersState = MutableStateFlow<ContractMembersUiState>(ContractMembersUiState.Idle)
    val contractMembersState: StateFlow<ContractMembersUiState> = _contractMembersState.asStateFlow()

    private val _approvedBuildings = MutableStateFlow<List<BuildingMyListResponseDto>>(emptyList())
    val approvedBuildings: StateFlow<List<BuildingMyListResponseDto>> = _approvedBuildings.asStateFlow()

    private val _contractTemplates = MutableStateFlow<List<ContractTemplateItemDto>>(emptyList())
    val contractTemplates: StateFlow<List<ContractTemplateItemDto>> = _contractTemplates.asStateFlow()

    private val _actionState = MutableStateFlow<ContractActionUiState>(ContractActionUiState.Idle)
    val actionState: StateFlow<ContractActionUiState> = _actionState.asStateFlow()

    private var lastStatus: String? = null
    private var lastBuildingId: String? = null

    fun loadContracts(
        status: String? = lastStatus,
        buildingId: String? = lastBuildingId,
        roomId: String? = null
    ) {
        lastStatus = status
        lastBuildingId = buildingId
        _contractListState.value = ContractListUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.getContracts(
                status = status,
                roomId = roomId,
                buildingId = buildingId
            )) {
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

    fun loadApprovedBuildings() {
        val repo = buildingRepository ?: return
        viewModelScope.launch {
            when (val result = repo.getMyBuildings(status = "approved")) {
                is NetworkResult.Success -> {
                    _approvedBuildings.value = result.data.data
                }
                else -> {
                    _approvedBuildings.value = emptyList()
                }
            }
        }
    }

    fun loadContractTemplates() {
        viewModelScope.launch {
            when (val result = contractRepository.getContractTemplates()) {
                is NetworkResult.Success -> {
                    _contractTemplates.value = result.data.data
                }
                else -> {
                    _contractTemplates.value = emptyList()
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

    fun updateContract(contractId: String, vehicleCount: Int?, description: String?) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            val req = ContractUpdateRequestDto(vehicleCount = vehicleCount, description = description)
            when (val result = contractRepository.updateContract(contractId, req)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Cập nhật số xe & ghi chú hợp đồng thành công!", result.data.data)
                    loadContractDetail(contractId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Cập nhật hợp đồng thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi cập nhật hợp đồng")
                }
            }
        }
    }

    fun applyContractTemplate(contractId: String, templateKey: String) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.applyContractTemplate(contractId, templateKey)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Đã áp dụng mẫu hợp đồng thành công!", result.data.data)
                    loadContractDetail(contractId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Áp mẫu hợp đồng thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi áp mẫu hợp đồng")
                }
            }
        }
    }

    fun generateDraftDocumentPdf(contractId: String, onPdfReady: (ByteArray) -> Unit) {
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            when (val result = contractRepository.generateDraftDocument(contractId)) {
                is NetworkResult.Success -> {
                    _actionState.value = ContractActionUiState.Success("Đã sinh bản nháp PDF hợp đồng!")
                    onPdfReady(result.data)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Không thể sinh bản nháp PDF (hãy kiểm tra đã áp mẫu chưa)")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi sinh bản nháp PDF")
                }
            }
        }
    }

    fun uploadSignedContractFile(
        contractId: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        onMediaUploaded: (String) -> Unit
    ) {
        val repo = mediaRepository ?: return
        _actionState.value = ContractActionUiState.Loading
        viewModelScope.launch {
            val result = repo.uploadMedia(
                ownerType = "contract",
                ownerId = contractId,
                purpose = "signed_contract",
                fileBytes = fileBytes,
                fileName = fileName,
                contentType = contentType
            )
            when (result) {
                is NetworkResult.Success -> {
                    onMediaUploaded(result.data.id)
                    _actionState.value = ContractActionUiState.Success("Đã tải lên tệp hợp đồng đã ký! Bạn có thể bấm Kích hoạt hợp đồng.")
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = ContractActionUiState.Error(result.error?.message ?: "Tải lên bản hợp đồng đã ký thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = ContractActionUiState.Error("Lỗi kết nối mạng khi tải tệp")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = ContractActionUiState.Error("Đã xảy ra lỗi khi tải tệp hợp đồng")
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
