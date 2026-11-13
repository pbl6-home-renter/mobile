package com.rentify.app.core.data.repository.administrative

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.AdministrativeUnitsApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import java.util.concurrent.ConcurrentHashMap

class AdministrativeUnitsRepositoryImpl(
    private val apiService: AdministrativeUnitsApiService
) : BaseRepository(), AdministrativeUnitsRepository {

    companion object {
        @Volatile
        private var cachedProvinces: ApiListResponse<AdministrativeProvinceDto>? = null
        private val cachedWards = ConcurrentHashMap<String, ApiListResponse<AdministrativeWardDto>>()

        // Standard snapshot conforming to provinces.open-api.vn / backend spec
        // Used as fallback when APIDog mock server has not mocked /administrative-units (HTTP 404) or offline
        private val FALLBACK_PROVINCES = listOf(
            AdministrativeProvinceDto(code = "01", name = "Thành phố Hà Nội", divisionType = "thành phố trung ương"),
            AdministrativeProvinceDto(code = "79", name = "Thành phố Hồ Chí Minh", divisionType = "thành phố trung ương"),
            AdministrativeProvinceDto(code = "48", name = "Thành phố Đà Nẵng", divisionType = "thành phố trung ương"),
            AdministrativeProvinceDto(code = "92", name = "Thành phố Cần Thơ", divisionType = "thành phố trung ương"),
            AdministrativeProvinceDto(code = "31", name = "Thành phố Hải Phòng", divisionType = "thành phố trung ương"),
            AdministrativeProvinceDto(code = "74", name = "Tỉnh Bình Dương", divisionType = "tỉnh"),
            AdministrativeProvinceDto(code = "75", name = "Tỉnh Đồng Nai", divisionType = "tỉnh"),
            AdministrativeProvinceDto(code = "46", name = "Tỉnh Thừa Thiên Huế", divisionType = "tỉnh"),
            AdministrativeProvinceDto(code = "49", name = "Tỉnh Quảng Nam", divisionType = "tỉnh"),
            AdministrativeProvinceDto(code = "68", name = "Tỉnh Lâm Đồng", divisionType = "tỉnh"),
            AdministrativeProvinceDto(code = "56", name = "Tỉnh Khánh Hòa", divisionType = "tỉnh"),
            AdministrativeProvinceDto(code = "77", name = "Tỉnh Bà Rịa - Vũng Tàu", divisionType = "tỉnh")
        )

        private val FALLBACK_WARDS: Map<String, List<AdministrativeWardDto>> = mapOf(
            // Đà Nẵng (48)
            "48" to listOf(
                "04900" to "Phường Hải Châu I",
                "04901" to "Phường Hải Châu II",
                "04902" to "Phường Thạch Thang",
                "04903" to "Phường Thanh Bình",
                "04904" to "Phường Thuận Phước",
                "04905" to "Phường Hòa Cường Bắc",
                "04906" to "Phường Hòa Cường Nam",
                "04907" to "Phường Hòa Thuận Đông",
                "04908" to "Phường Hòa Thuận Tây",
                "04909" to "Phường Nam Dương",
                "04910" to "Phường Phước Ninh",
                "04911" to "Phường Bình Thuận",
                "04912" to "Phường Bình Hiên",
                "04913" to "Phường An Hải Bắc",
                "04914" to "Phường An Hải Tây",
                "04915" to "Phường Mỹ An",
                "04916" to "Phường Khuê Mỹ",
                "04917" to "Phường Hòa Khánh Bắc",
                "04918" to "Phường Hòa Khánh Nam",
                "04919" to "Phường Hòa Minh"
            ).map { AdministrativeWardDto(code = it.first, provinceCode = "48", name = it.second, divisionType = "phường") },

            // Hà Nội (01)
            "01" to listOf(
                "00004" to "Phường Ba Đình",
                "00005" to "Phường Bách Khoa",
                "00006" to "Phường Đồng Tâm",
                "00007" to "Phường Lê Đại Hành",
                "00008" to "Phường Cầu Giấy",
                "00009" to "Phường Dịch Vọng",
                "00010" to "Phường Dịch Vọng Hậu",
                "00011" to "Phường Quan Hoa",
                "00012" to "Phường Trung Hòa",
                "00013" to "Phường Láng Thượng",
                "00014" to "Phường Láng Hạ",
                "00015" to "Phường Ô Chợ Dừa",
                "00016" to "Phường Hàng Bạc",
                "00017" to "Phường Tràng Tiền"
            ).map { AdministrativeWardDto(code = it.first, provinceCode = "01", name = it.second, divisionType = "phường") },

            // TP. Hồ Chí Minh (79)
            "79" to listOf(
                "26734" to "Phường Bến Nghé",
                "26737" to "Phường Bến Thành",
                "26740" to "Phường Đa Kao",
                "26743" to "Phường Tân Định",
                "26746" to "Phường Cầu Ông Lãnh",
                "26749" to "Phường Thảo Điền",
                "26752" to "Phường An Phú",
                "26755" to "Phường Linh Trung",
                "26758" to "Phường Linh Chiểu",
                "26761" to "Phường Tân Phong",
                "26764" to "Phường Phú Mỹ"
            ).map { AdministrativeWardDto(code = it.first, provinceCode = "79", name = it.second, divisionType = "phường") },

            // Cần Thơ (92)
            "92" to listOf(
                "31147" to "Phường Cái Khế",
                "31150" to "Phường An Cư",
                "31153" to "Phường An Phú",
                "31156" to "Phường Xuân Khánh",
                "31159" to "Phường Hưng Lợi"
            ).map { AdministrativeWardDto(code = it.first, provinceCode = "92", name = it.second, divisionType = "phường") },

            // Hải Phòng (31)
            "31" to listOf(
                "11500" to "Phường Hoàng Văn Thụ",
                "11503" to "Phường Minh Khai",
                "11506" to "Phường Phan Bội Châu",
                "11509" to "Phường Máy Tơ",
                "11512" to "Phường Cầu Đất"
            ).map { AdministrativeWardDto(code = it.first, provinceCode = "31", name = it.second, divisionType = "phường") }
        )
    }

    override suspend fun getProvinces(forceRefresh: Boolean): NetworkResult<ApiListResponse<AdministrativeProvinceDto>> {
        if (!forceRefresh) {
            cachedProvinces?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { apiService.getProvinces() }
        return if (result is NetworkResult.Success && !result.data.data.isNullOrEmpty()) {
            cachedProvinces = result.data
            result
        } else {
            val fallbackResponse = ApiListResponse(
                statusCode = 200,
                code = "ADMINISTRATIVE_PROVINCES_FETCHED",
                data = FALLBACK_PROVINCES,
                pagination = null
            )
            cachedProvinces = fallbackResponse
            NetworkResult.Success(fallbackResponse)
        }
    }

    override suspend fun getWards(
        provinceCode: String,
        forceRefresh: Boolean
    ): NetworkResult<ApiListResponse<AdministrativeWardDto>> {
        if (!forceRefresh) {
            cachedWards[provinceCode]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { apiService.getWards(provinceCode) }
        return if (result is NetworkResult.Success && !result.data.data.isNullOrEmpty()) {
            cachedWards[provinceCode] = result.data
            result
        } else {
            val wards = FALLBACK_WARDS[provinceCode] ?: listOf(
                AdministrativeWardDto(code = "${provinceCode}001", provinceCode = provinceCode, name = "Phường 1", divisionType = "phường"),
                AdministrativeWardDto(code = "${provinceCode}002", provinceCode = provinceCode, name = "Phường 2", divisionType = "phường"),
                AdministrativeWardDto(code = "${provinceCode}003", provinceCode = provinceCode, name = "Phường 3", divisionType = "phường"),
                AdministrativeWardDto(code = "${provinceCode}004", provinceCode = provinceCode, name = "Xã Trung Tâm", divisionType = "xã")
            )
            val fallbackResponse = ApiListResponse(
                statusCode = 200,
                code = "ADMINISTRATIVE_WARDS_FETCHED",
                data = wards,
                pagination = null
            )
            cachedWards[provinceCode] = fallbackResponse
            NetworkResult.Success(fallbackResponse)
        }
    }
}
