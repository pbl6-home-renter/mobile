package com.rentify.app.tenant.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rentify.app.core.data.repository.administrative.AdministrativeUnitsRepository
import com.rentify.app.core.data.repository.administrative.AdministrativeUnitsRepositoryImpl
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.AdministrativeUnitsApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.BottomSheetRoomFilterBinding
import kotlinx.coroutines.launch

class FilterBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRoomFilterBinding? = null
    private val binding get() = _binding!!

    var onFilterApplied: ((RoomFilter) -> Unit)? = null

    private var currentFilter: RoomFilter = RoomFilter.default()

    private val administrativeUnitsRepository: AdministrativeUnitsRepository by lazy {
        val apiService = NetworkClient.createService<AdministrativeUnitsApiService>()
        AdministrativeUnitsRepositoryImpl(apiService)
    }

    private var provincesList: List<AdministrativeProvinceDto> = emptyList()
    private var wardsList: List<AdministrativeWardDto> = emptyList()
    private var selectedProvinceCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        currentFilter = arguments?.getSerializable(ARG_FILTER) as? RoomFilter ?: RoomFilter.default()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRoomFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupPriceRange()
        setupGenderToggle()
        setupSortToggle()
        setupButtons()

        applyFilterToUi(currentFilter)
    }

    private fun setupSpinners() {
        val ctx = context ?: return
        binding.actvProvince.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, listOf("Đang tải danh sách...")))
        setupWardEmpty()

        binding.actvProvince.setOnItemClickListener { _, _, position, _ ->
            if (position <= 0) {
                selectedProvinceCode = null
                binding.actvProvince.setText("Tất cả", false)
                setupWardEmpty()
            } else {
                val province = provincesList.getOrNull(position - 1) ?: return@setOnItemClickListener
                selectedProvinceCode = province.code
                binding.actvWard.isEnabled = true
                binding.actvWard.setText("Tất cả phường/xã", false)
                loadWards(province.code, restoreWard = null)
            }
        }

        binding.actvWard.setOnItemClickListener { _, _, position, _ ->
            if (position <= 0) {
                binding.actvWard.setText("Tất cả phường/xã", false)
            } else {
                val ward = wardsList.getOrNull(position - 1) ?: return@setOnItemClickListener
                binding.actvWard.setText(ward.name, false)
            }
        }

        loadProvinces()
    }

    private fun loadProvinces() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = administrativeUnitsRepository.getProvinces()) {
                is NetworkResult.Success -> {
                    val b = _binding ?: return@launch
                    val ctx = context ?: return@launch
                    val list = result.data.data ?: emptyList()
                    provincesList = list
                    val provinceNames = mutableListOf("Tất cả")
                    provinceNames.addAll(list.map { it.name })

                    val adapter = ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, provinceNames)
                    b.actvProvince.setAdapter(adapter)

                    val initialProvince = currentFilter.province
                    if (!initialProvince.isNullOrBlank() && initialProvince != "Tất cả") {
                        b.actvProvince.setText(initialProvince, false)
                        val matched = provincesList.find { it.name.equals(initialProvince, ignoreCase = true) }
                        if (matched != null) {
                            selectedProvinceCode = matched.code
                            b.actvWard.isEnabled = true
                            loadWards(matched.code, restoreWard = currentFilter.ward)
                        } else {
                            setupWardEmpty()
                        }
                    } else {
                        b.actvProvince.setText("Tất cả", false)
                        setupWardEmpty()
                    }
                }
                is NetworkResult.ApiError, is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    val b = _binding ?: return@launch
                    val ctx = context ?: return@launch
                    val fallback = listOf("Tất cả", "Thành phố Hồ Chí Minh", "Thành phố Hà Nội", "Thành phố Đà Nẵng", "Thành phố Cần Thơ", "Thành phố Hải Phòng")
                    b.actvProvince.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, fallback))
                    setupWardEmpty()
                }
            }
        }
    }

    private fun loadWards(provinceCode: String, restoreWard: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val b = _binding ?: return@launch
            val ctx = context ?: return@launch
            b.actvWard.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, listOf("Đang tải phường/xã...")))

            when (val result = administrativeUnitsRepository.getWards(provinceCode)) {
                is NetworkResult.Success -> {
                    val currentBinding = _binding ?: return@launch
                    val currentCtx = context ?: return@launch
                    val list = result.data.data ?: emptyList()
                    wardsList = list
                    val wardNames = mutableListOf("Tất cả phường/xã")
                    wardNames.addAll(list.map { it.name })

                    val adapter = ArrayAdapter(currentCtx, android.R.layout.simple_dropdown_item_1line, wardNames)
                    currentBinding.actvWard.setAdapter(adapter)

                    if (!restoreWard.isNullOrBlank() && restoreWard != "Tất cả phường/xã") {
                        val matchedWard = wardsList.find { it.name.equals(restoreWard, ignoreCase = true) }
                        if (matchedWard != null) {
                            currentBinding.actvWard.setText(matchedWard.name, false)
                        } else {
                            currentBinding.actvWard.setText("Tất cả phường/xã", false)
                        }
                    } else {
                        currentBinding.actvWard.setText("Tất cả phường/xã", false)
                    }
                }
                is NetworkResult.ApiError, is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    setupWardEmpty()
                }
            }
        }
    }

    private fun setupWardEmpty() {
        val b = _binding ?: return
        val ctx = context ?: return
        wardsList = emptyList()
        b.actvWard.setText("Tất cả phường/xã", false)
        b.actvWard.isEnabled = false
        b.actvWard.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, listOf("Tất cả phường/xã")))
    }

    private fun setupPriceRange() {
        binding.sliderPriceRange.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            val minVal = values[0]
            val maxVal = values[1]
            updatePriceRangeLabel(minVal, maxVal)
        }

        binding.chipPriceUnder2m.setOnClickListener {
            setSliderRange(0f, 2f)
        }

        binding.chipPrice2to4m.setOnClickListener {
            setSliderRange(2f, 4f)
        }

        binding.chipPrice4to7m.setOnClickListener {
            setSliderRange(4f, 7f)
        }

        binding.chipPriceAbove7m.setOnClickListener {
            setSliderRange(7f, 15f)
        }
    }

    private fun setSliderRange(min: Float, max: Float) {
        binding.sliderPriceRange.setValues(min, max)
        updatePriceRangeLabel(min, max)
    }

    private fun updatePriceRangeLabel(min: Float, max: Float) {
        binding.tvPriceRangeValue.text = when {
            min <= 0f && max >= 15f -> "Tất cả mức giá"
            min <= 0f -> "Dưới ${formatPriceMillion(max)}"
            max >= 15f -> "Trên ${formatPriceMillion(min)}"
            else -> "${formatPriceMillion(min)} - ${formatPriceMillion(max)}"
        }
    }

    private fun formatPriceMillion(value: Float): String {
        return if (value % 1.0f == 0.0f) {
            "${value.toInt()}tr"
        } else {
            "${value}tr"
        }
    }

    private fun setupGenderToggle() {
        // Toggle gender buttons
    }

    private fun setupSortToggle() {
        // Toggle sort buttons
    }

    private fun setupButtons() {
        binding.btnResetFilter.setOnClickListener {
            resetFiltersToDefault()
        }

        binding.btnApplyFilter.setOnClickListener {
            val filter = collectFilterFromUi()
            onFilterApplied?.invoke(filter)
            dismiss()
        }
    }

    private fun applyFilterToUi(filter: RoomFilter) {
        val b = _binding ?: return
        if (provincesList.isNotEmpty()) {
            if (!filter.province.isNullOrBlank() && filter.province != "Tất cả") {
                b.actvProvince.setText(filter.province, false)
                val matched = provincesList.find { it.name.equals(filter.province, ignoreCase = true) }
                if (matched != null) {
                    selectedProvinceCode = matched.code
                    b.actvWard.isEnabled = true
                    loadWards(matched.code, restoreWard = filter.ward)
                } else {
                    setupWardEmpty()
                }
            } else {
                b.actvProvince.setText("Tất cả", false)
                setupWardEmpty()
            }
        }

        val minM = if (filter.minPrice != null) (filter.minPrice / 1_000_000f).coerceIn(0f, 15f) else 0f
        val maxM = if (filter.maxPrice != null) (filter.maxPrice / 1_000_000f).coerceIn(0f, 15f) else 15f
        setSliderRange(minM, maxM)

        when (filter.genderPolicy) {
            "male" -> binding.toggleGender.check(R.id.btnGenderMale)
            "female" -> binding.toggleGender.check(R.id.btnGenderFemale)
            else -> binding.toggleGender.check(R.id.btnGenderAll)
        }

        if (filter.sort == "rentPrice" && filter.order == "asc") {
            binding.toggleSort.check(R.id.btnSortPriceAsc)
        } else if (filter.sort == "rentPrice" && filter.order == "desc") {
            binding.toggleSort.check(R.id.btnSortPriceDesc)
        } else {
            binding.toggleSort.check(R.id.btnSortNewest)
        }
    }

    private fun resetFiltersToDefault() {
        selectedProvinceCode = null
        currentFilter = RoomFilter.default()
        applyFilterToUi(RoomFilter.default())
        binding.actvProvince.setText("Tất cả", false)
        setupWardEmpty()
    }

    private fun collectFilterFromUi(): RoomFilter {
        val provinceText = binding.actvProvince.text?.toString()?.trim()
        val wardText = binding.actvWard.text?.toString()?.trim()

        val selectedProvince = if (provinceText.isNullOrBlank() || provinceText == "Tất cả" || provinceText.contains("Đang tải")) null else provinceText
        val selectedWard = if (wardText.isNullOrBlank() || wardText.contains("Tất cả") || wardText.contains("Đang tải")) null else wardText

        val priceValues = binding.sliderPriceRange.values
        val minPrice = if (priceValues[0] > 0f) (priceValues[0] * 1_000_000).toLong() else null
        val maxPrice = if (priceValues[1] < 15f) (priceValues[1] * 1_000_000).toLong() else null

        val genderPolicy = when (binding.toggleGender.checkedButtonId) {
            R.id.btnGenderMale -> "male"
            R.id.btnGenderFemale -> "female"
            else -> "any"
        }

        val (sort, order) = when (binding.toggleSort.checkedButtonId) {
            R.id.btnSortPriceAsc -> Pair("rentPrice", "asc")
            R.id.btnSortPriceDesc -> Pair("rentPrice", "desc")
            else -> Pair("createdAt", "desc")
        }

        return currentFilter.copy(
            province = selectedProvince,
            ward = selectedWard,
            minPrice = minPrice,
            maxPrice = maxPrice,
            genderPolicy = genderPolicy,
            sort = sort,
            order = order
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheetDialogFragment"
        private const val ARG_FILTER = "arg_filter"

        fun newInstance(filter: RoomFilter): FilterBottomSheetDialogFragment {
            return FilterBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER, filter)
                }
            }
        }
    }
}
