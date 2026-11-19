package com.rentify.app.landlord.ui.property

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import com.rentify.app.core.network.dto.billing.BillingSettingCreateRequestDto
import com.rentify.app.core.network.dto.billing.BillingSettingResponseDto
import com.rentify.app.core.network.dto.billing.BillingSettingUpdateRequestDto
import com.rentify.app.core.network.dto.billing.RateItemDto
import com.rentify.app.core.network.dto.billing.RatePolicyResponseDto
import com.rentify.app.core.network.dto.billing.RatePolicyUpdateRequestDto
import com.rentify.app.core.network.dto.building.BuildingCreateRequestDto
import com.rentify.app.core.network.dto.building.BuildingMyDetailResponseDto
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.building.BuildingUpdateRequestDto
import com.rentify.app.core.network.dto.building.BuildingVietQrConfigRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.data.repository.administrative.AdministrativeUnitsRepository
import com.rentify.app.landlord.data.repository.LandlordBuildingRepository
import com.rentify.app.landlord.data.repository.LandlordMediaRepository
import com.rentify.app.landlord.data.repository.LandlordPolicySettingRepository
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
    private val administrativeUnitsRepository: AdministrativeUnitsRepository,
    private val mediaRepository: LandlordMediaRepository? = null,
    private val policySettingRepository: LandlordPolicySettingRepository? = null
) : BaseViewModel() {

    private val _buildingListState = MutableStateFlow<BuildingListUiState>(BuildingListUiState.Loading)
    val buildingListState: StateFlow<BuildingListUiState> = _buildingListState.asStateFlow()

    private val _buildingDetailState = MutableStateFlow<BuildingDetailUiState>(BuildingDetailUiState.Idle)
    val buildingDetailState: StateFlow<BuildingDetailUiState> = _buildingDetailState.asStateFlow()

    private val _ratePolicyState = MutableStateFlow<RatePolicyResponseDto?>(null)
    val ratePolicyState: StateFlow<RatePolicyResponseDto?> = _ratePolicyState.asStateFlow()

    private val _billingSettingState = MutableStateFlow<BillingSettingResponseDto?>(null)
    val billingSettingState: StateFlow<BillingSettingResponseDto?> = _billingSettingState.asStateFlow()

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

    fun uploadOwnershipProof(buildingId: String, fileBytes: ByteArray, fileName: String, contentType: String) {
        val repo = mediaRepository ?: return
        _actionState.value = BuildingActionUiState.Loading
        viewModelScope.launch {
            val result = repo.uploadMedia(
                ownerType = "building",
                ownerId = buildingId,
                purpose = "ownership_proof",
                fileBytes = fileBytes,
                fileName = fileName,
                contentType = contentType
            )
            when (result) {
                is NetworkResult.Success -> {
                    _actionState.value = BuildingActionUiState.Success("Đăng ảnh minh chứng sở hữu thành công!", result.data)
                    loadBuildingDetail(buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BuildingActionUiState.Error(result.error?.message ?: "Tải ảnh minh chứng thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BuildingActionUiState.Error("Lỗi kết nối mạng khi tải ảnh")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BuildingActionUiState.Error("Đã xảy ra lỗi khi tải ảnh minh chứng")
                }
            }
        }
    }

    fun loadBuildingPoliciesAndSettings(buildingId: String) {
        val repo = policySettingRepository ?: return
        viewModelScope.launch {
            when (val rateRes = repo.getRatePolicies(scope = "building", buildingId = buildingId)) {
                is NetworkResult.Success -> {
                    _ratePolicyState.value = rateRes.data.data.firstOrNull { it.isActive }
                        ?: rateRes.data.data.firstOrNull()
                }
                else -> _ratePolicyState.value = null
            }
            when (val billRes = repo.getBillingSettings(scope = "building", buildingId = buildingId)) {
                is NetworkResult.Success -> {
                    _billingSettingState.value = billRes.data.data.firstOrNull { it.isActive }
                        ?: billRes.data.data.firstOrNull()
                }
                else -> _billingSettingState.value = null
            }
        }
    }

    fun saveBuildingRatePolicies(buildingId: String, rates: List<RateItemDto>) {
        val repo = policySettingRepository ?: return
        _actionState.value = BuildingActionUiState.Loading
        viewModelScope.launch {
            val existing = _ratePolicyState.value
            val result = if (existing != null) {
                repo.updateRatePolicy(existing.id, RatePolicyUpdateRequestDto(rates = rates, isActive = true))
            } else {
                repo.saveBuildingRatePoliciesBatch(buildingId, rates)
            }
            when (result) {
                is NetworkResult.Success -> {
                    _ratePolicyState.value = result.data.data
                    _actionState.value = BuildingActionUiState.Success("Thiết lập biểu giá dịch vụ thành công!")
                    loadBuildingDetail(buildingId)
                    loadBuildingPoliciesAndSettings(buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BuildingActionUiState.Error(result.error?.message ?: "Lưu biểu giá thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BuildingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BuildingActionUiState.Error("Đã xảy ra lỗi khi lưu biểu giá dịch vụ")
                }
            }
        }
    }

    fun saveBuildingBillingSetting(
        buildingId: String,
        paymentDueDay: Int,
        remindDay: Int?,
        botEnabled: Boolean
    ) {
        val repo = policySettingRepository ?: return
        _actionState.value = BuildingActionUiState.Loading
        viewModelScope.launch {
            val existing = _billingSettingState.value
            val result = if (existing != null) {
                repo.updateBillingSetting(
                    billingSettingId = existing.id,
                    request = BillingSettingUpdateRequestDto(
                        paymentDueDay = paymentDueDay,
                        remindDay = remindDay,
                        botEnabled = botEnabled,
                        isActive = true
                    )
                )
            } else {
                repo.createBillingSetting(
                    request = BillingSettingCreateRequestDto(
                        scope = "building",
                        buildingId = buildingId,
                        paymentDueDay = paymentDueDay,
                        remindDay = remindDay,
                        botEnabled = botEnabled
                    )
                )
            }
            when (result) {
                is NetworkResult.Success -> {
                    _billingSettingState.value = result.data.data
                    _actionState.value = BuildingActionUiState.Success("Thiết lập thanh toán & nhắc nợ thành công!")
                    loadBuildingPoliciesAndSettings(buildingId)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = BuildingActionUiState.Error(result.error?.message ?: "Lưu cài đặt thanh toán thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _actionState.value = BuildingActionUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _actionState.value = BuildingActionUiState.Error("Đã xảy ra lỗi khi lưu cài đặt thanh toán")
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = BuildingActionUiState.Idle
    }
}
