package com.rentify.app.landlord.ui.property

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import com.rentify.app.core.network.dto.building.BuildingCreateRequestDto
import com.rentify.app.core.network.dto.building.BuildingMyDetailResponseDto
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.building.BuildingUpdateRequestDto
import com.rentify.app.core.network.dto.building.BuildingVietQrConfigRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.AdministrativeUnitsRepository
import com.rentify.app.landlord.data.repository.LandlordBuildingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BuildingListUiState {
    object Loading : BuildingListUiState
    data class Success(val buildings: List<BuildingMyListResponseDto>) : BuildingListUiState
    data class Error(val message: String) : BuildingListUiState
}

sealed interface BuildingDetailUiState {
    object Idle : BuildingDetailUiState
    object Loading : BuildingDetailUiState
    data class Success(val building: BuildingMyDetailResponseDto) : BuildingDetailUiState
    data class Error(val message: String) : BuildingDetailUiState
}

sealed interface BuildingActionUiState {
    object Idle : BuildingActionUiState
    object Loading : BuildingActionUiState
    data class Success(val message: String, val data: Any? = null) : BuildingActionUiState
    data class Error(val message: String) : BuildingActionUiState
}

class BuildingViewModel(
    private val buildingRepository: LandlordBuildingRepository,
    private val administrativeUnitsRepository: AdministrativeUnitsRepository
) : BaseViewModel() {

    private val _buildingListState = MutableStateFlow<BuildingListUiState>(BuildingListUiState.Loading)
    val buildingListState: StateFlow<BuildingListUiState> = _buildingListState.asStateFlow()

    private val _buildingDetailState = MutableStateFlow<BuildingDetailUiState>(BuildingDetailUiState.Idle)
    val buildingDetailState: StateFlow<BuildingDetailUiState> = _buildingDetailState.asStateFlow()

    private val _provincesState = MutableStateFlow<List<AdministrativeProvinceDto>>(emptyList())
    val provincesState: StateFlow<List<AdministrativeProvinceDto>> = _provincesState.asStateFlow()

    private val _wardsState = MutableStateFlow<List<AdministrativeWardDto>>(emptyList())
    val wardsState: StateFlow<List<AdministrativeWardDto>> = _wardsState.asStateFlow()

    private val _actionState = MutableStateFlow<BuildingActionUiState>(BuildingActionUiState.Idle)
    val actionState: StateFlow<BuildingActionUiState> = _actionState.asStateFlow()

    fun loadBuildings(search: String? = null, status: String? = null) {
        _buildingListState.value = BuildingListUiState.Loading
        viewModelScope.launch {
            when (val result = buildingRepository.getMyBuildings(search = search, status = status)) {
                is NetworkResult.Success -> {
                    _buildingListState.value = BuildingListUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _buildingListState.value = BuildingListUiState.Error(result.error?.message ?: "Lỗi tải danh sách tòa nhà")
                }
                is NetworkResult.NetworkError -> {
                    _buildingListState.value = BuildingListUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _buildingListState.value = BuildingListUiState.Error("Không thể tải danh sách tòa nhà")
                }
            }
        }
    }

    fun loadBuildingDetail(buildingId: String) {
        _buildingDetailState.value = BuildingDetailUiState.Loading
        viewModelScope.launch {
            when (val result = buildingRepository.getMyBuildingDetail(buildingId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _buildingDetailState.value = BuildingDetailUiState.Success(detail)
                    } else {
                        _buildingDetailState.value = BuildingDetailUiState.Error("Không tìm thấy thông tin tòa nhà")
                    }
                }
                is NetworkResult.ApiError -> {
                    _buildingDetailState.value = BuildingDetailUiState.Error(result.error?.message ?: "Lỗi tải chi tiết tòa nhà")
                }
                is NetworkResult.NetworkError -> {
                    _buildingDetailState.value = BuildingDetailUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _buildingDetailState.value = BuildingDetailUiState.Error("Không thể tải chi tiết tòa nhà")
                }
            }
        }
    }

    fun loadProvinces() {
        viewModelScope.launch {
            when (val result = administrativeUnitsRepository.getProvinces()) {
                is NetworkResult.Success -> {
                    _provincesState.value = result.data.data
                }
                else -> {
                    _provincesState.value = emptyList()
                }
            }
        }
    }

    fun loadWards(provinceCode: String) {
        _wardsState.value = emptyList()
        viewModelScope.launch {
            when (val result = administrativeUnitsRepository.getWards(provinceCode)) {
                is NetworkResult.Success -> {
                    _wardsState.value = result.data.data
                }
                else -> {
                    _wardsState.value = emptyList()
                }
            }
        }
    }

    fun createBuilding(name: String, province: String, ward: String, address: String) {
        _actionState.value = BuildingActionUiState.Loading
        viewModelScope.launch {
            val req = BuildingCreateRequestDto(name = name, province = province, ward = ward, address = address)
            when (val result = buildingRepository.createBuilding(req)) {
                is NetworkResult.Success -> {
                    _actionState.value = BuildingActionUiState.Success("Gửi yêu cầu tạo tòa nhà thành công!", result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BuildingActionUiState.Error(result.error?.message ?: "Gửi yêu cầu thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BuildingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BuildingActionUiState.Error("Đã xảy ra lỗi khi tạo tòa nhà")
                }
            }
        }
    }

    fun updateBuilding(buildingId: String, name: String?, address: String?) {
        _actionState.value = BuildingActionUiState.Loading
        viewModelScope.launch {
            val req = BuildingUpdateRequestDto(name = name, address = address)
            when (val result = buildingRepository.updateBuilding(buildingId, req)) {
                is NetworkResult.Success -> {
                    _actionState.value = BuildingActionUiState.Success("Cập nhật tòa nhà thành công!", result.data.data)
                    loadBuildingDetail(buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BuildingActionUiState.Error(result.error?.message ?: "Cập nhật thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BuildingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BuildingActionUiState.Error("Đã xảy ra lỗi khi cập nhật")
                }
            }
        }
    }

    fun deleteBuilding(buildingId: String) {
        _actionState.value = BuildingActionUiState.Loading
        viewModelScope.launch {
            when (val result = buildingRepository.deleteBuilding(buildingId)) {
                is NetworkResult.Success -> {
                    _actionState.value = BuildingActionUiState.Success("Xóa tòa nhà thành công!")
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BuildingActionUiState.Error(result.error?.message ?: "Xóa tòa nhà thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BuildingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BuildingActionUiState.Error("Đã xảy ra lỗi khi xóa tòa nhà")
                }
            }
        }
    }

    fun configureVietQr(
        buildingId: String,
        bankCode: String,
        bankName: String,
        accountNumber: String,
        accountHolderName: String,
        branch: String?
    ) {
        _actionState.value = BuildingActionUiState.Loading
        viewModelScope.launch {
            val req = BuildingVietQrConfigRequestDto(
                bankCode = bankCode,
                bankName = bankName,
                accountNumber = accountNumber,
                accountHolderName = accountHolderName,
                branch = branch,
                isDefault = true
            )
            when (val result = buildingRepository.configureVietQr(buildingId, req)) {
                is NetworkResult.Success -> {
                    _actionState.value = BuildingActionUiState.Success("Cấu hình VietQR thành công!", result.data.data)
                    loadBuildingDetail(buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BuildingActionUiState.Error(result.error?.message ?: "Cấu hình VietQR thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BuildingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BuildingActionUiState.Error("Đã xảy ra lỗi khi cấu hình VietQR")
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = BuildingActionUiState.Idle
    }
}
