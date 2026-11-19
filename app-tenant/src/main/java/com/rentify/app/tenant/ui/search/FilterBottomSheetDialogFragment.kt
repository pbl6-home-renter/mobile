package com.rentify.app.tenant.ui.search

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rentify.app.core.data.repository.administrative.AdministrativeUnitsRepository
import com.rentify.app.core.data.repository.administrative.AdministrativeUnitsRepositoryImpl
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.AdministrativeUnitsApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
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

    private var selectedGenderPolicy: String = RoomFilter.default().genderPolicy

    private val regularTypeface: Typeface? by lazy {
        context?.let { ResourcesCompat.getFont(it, CoreUiR.font.plus_jakarta_sans) }
    }

    private val boldTypeface: Typeface? by lazy {
        regularTypeface?.let { Typeface.create(it, Typeface.BOLD) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        currentFilter = arguments?.getSerializable(ARG_FILTER) as? RoomFilter ?: RoomFilter.default()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_bottom_sheet_filter_rounded)
        }
        return dialog
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

        // Đảm bảo footer không bị Navigation Bar 3 phím của hệ thống che khuất
        binding.layoutApplyContainer.applySystemBarsPadding(top = false, bottom = true)

        setupSpinners()
        setupPriceRange()
        setupGenderToggle()
        setupButtons()

        applyFilterToUi(currentFilter)
    }

    private fun setupSpinners() {
        val ctx = context ?: return
        binding.actvProvince.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, listOf("Đang tải danh sách...")))
        setupWardEmpty()

        binding.layoutProvinceContainer.setOnSingleClickListener {
            binding.actvProvince.showDropDown()
        }
        binding.actvProvince.setOnSingleClickListener {
            binding.actvProvince.showDropDown()
        }

        binding.layoutWardContainer.setOnSingleClickListener {
            if (binding.actvWard.isEnabled) {
                binding.actvWard.showDropDown()
            }
        }
        binding.actvWard.setOnSingleClickListener {
            if (binding.actvWard.isEnabled) {
                binding.actvWard.showDropDown()
            }
        }

        binding.actvProvince.setOnItemClickListener { _, _, position, _ ->
            if (position <= 0) {
                selectedProvinceCode = null
                binding.actvProvince.setText("", false)
                binding.actvProvince.hint = "Tỉnh / Thành phố"
                setupWardEmpty()
            } else {
                val province = provincesList.getOrNull(position - 1) ?: return@setOnItemClickListener
                selectedProvinceCode = province.code
                binding.actvWard.isEnabled = true
                binding.actvWard.setText("", false)
                binding.actvWard.hint = "Xã / Phường"
                loadWards(province.code, restoreWard = null)
            }
            updateResetButtonState()
        }

        binding.actvWard.setOnItemClickListener { _, _, position, _ ->
            if (position <= 0) {
                binding.actvWard.setText("", false)
                binding.actvWard.hint = "Xã / Phường"
            } else {
                val ward = wardsList.getOrNull(position - 1) ?: return@setOnItemClickListener
                binding.actvWard.setText(ward.name, false)
            }
            updateResetButtonState()
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
                    val provinceNames = mutableListOf("Tất cả tỉnh/thành phố")
                    provinceNames.addAll(list.map { it.name })

                    val adapter = ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, provinceNames)
                    b.actvProvince.setAdapter(adapter)

                    val initialProvince = currentFilter.province
                    if (!initialProvince.isNullOrBlank() && !initialProvince.contains("Tất cả")) {
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
                        b.actvProvince.setText("", false)
                        b.actvProvince.hint = "Tỉnh / Thành phố"
                        setupWardEmpty()
                    }
                    updateResetButtonState()
                }
                is NetworkResult.ApiError, is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    val b = _binding ?: return@launch
                    val ctx = context ?: return@launch
                    val fallback = listOf("Tất cả tỉnh/thành phố", "Thành phố Hồ Chí Minh", "Thành phố Hà Nội", "Thành phố Đà Nẵng", "Thành phố Cần Thơ", "Thành phố Hải Phòng")
                    b.actvProvince.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, fallback))
                    setupWardEmpty()
                    updateResetButtonState()
                }
            }
        }
    }

    private fun loadWards(provinceCode: String, restoreWard: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val b = _binding ?: return@launch
            val ctx = context ?: return@launch
            b.actvWard.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, listOf("Đang tải xã/phường...")))

            when (val result = administrativeUnitsRepository.getWards(provinceCode)) {
                is NetworkResult.Success -> {
                    val currentBinding = _binding ?: return@launch
                    val currentCtx = context ?: return@launch
                    val list = result.data.data ?: emptyList()
                    wardsList = list
                    val wardNames = mutableListOf("Tất cả xã/phường")
                    wardNames.addAll(list.map { it.name })

                    val adapter = ArrayAdapter(currentCtx, android.R.layout.simple_dropdown_item_1line, wardNames)
                    currentBinding.actvWard.setAdapter(adapter)

                    if (!restoreWard.isNullOrBlank() && !restoreWard.contains("Tất cả")) {
                        val matchedWard = wardsList.find { it.name.equals(restoreWard, ignoreCase = true) }
                        if (matchedWard != null) {
                            currentBinding.actvWard.setText(matchedWard.name, false)
                        } else {
                            currentBinding.actvWard.setText("", false)
                            currentBinding.actvWard.hint = "Xã / Phường"
                        }
                    } else {
                        currentBinding.actvWard.setText("", false)
                        currentBinding.actvWard.hint = "Xã / Phường"
                    }
                    updateResetButtonState()
                }
                is NetworkResult.ApiError, is NetworkResult.NetworkError, is NetworkResult.UnknownError -> {
                    setupWardEmpty()
                    updateResetButtonState()
                }
            }
        }
    }

    private fun setupWardEmpty() {
        val b = _binding ?: return
        val ctx = context ?: return
        wardsList = emptyList()
        b.actvWard.setText("", false)
        b.actvWard.hint = "Xã / Phường"
        b.actvWard.isEnabled = false
        b.actvWard.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, listOf("Vui lòng chọn Tỉnh/Thành phố trước")))
    }

    private fun setupPriceRange() {
        binding.sliderPriceRange.addOnChangeListener { slider, _, fromUser ->
            val values = slider.values
            val minVal = values[0]
            val maxVal = values[1]
            updatePriceRangeLabel(minVal, maxVal)
            if (fromUser) {
                updateQuickPriceChips(minVal, maxVal)
            }
            updateResetButtonState()
        }

        binding.chipPriceUnder2m.setOnSingleClickListener {
            setSliderRange(0f, 2f)
            updateQuickPriceChips(0f, 2f)
            updateResetButtonState()
        }

        binding.chipPrice2to4m.setOnSingleClickListener {
            setSliderRange(2f, 4f)
            updateQuickPriceChips(2f, 4f)
            updateResetButtonState()
        }

        binding.chipPrice4to7m.setOnSingleClickListener {
            setSliderRange(4f, 7f)
            updateQuickPriceChips(4f, 7f)
            updateResetButtonState()
        }

        binding.chipPriceAbove7m.setOnSingleClickListener {
            setSliderRange(7f, 15f)
            updateQuickPriceChips(7f, 15f)
            updateResetButtonState()
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

    private fun updateQuickPriceChips(min: Float, max: Float) {
        updatePillState(binding.chipPriceUnder2m, min == 0f && max == 2f)
        updatePillState(binding.chipPrice2to4m, min == 2f && max == 4f)
        updatePillState(binding.chipPrice4to7m, min == 4f && max == 7f)
        updatePillState(binding.chipPriceAbove7m, min == 7f && max == 15f)
    }

    private fun setupGenderToggle() {
        binding.chipGenderAll.setOnSingleClickListener {
            selectedGenderPolicy = "any"
            updateGenderPills()
            updateResetButtonState()
        }
        binding.chipGenderMale.setOnSingleClickListener {
            selectedGenderPolicy = "male"
            updateGenderPills()
            updateResetButtonState()
        }
        binding.chipGenderFemale.setOnSingleClickListener {
            selectedGenderPolicy = "female"
            updateGenderPills()
            updateResetButtonState()
        }
    }

    private fun updateGenderPills() {
        updatePillState(binding.chipGenderAll, selectedGenderPolicy == "any")
        updatePillState(binding.chipGenderMale, selectedGenderPolicy == "male")
        updatePillState(binding.chipGenderFemale, selectedGenderPolicy == "female")
    }

    private fun updatePillState(view: TextView, isSelected: Boolean) {
        val ctx = context ?: return
        if (isSelected) {
            view.setBackgroundResource(R.drawable.bg_chip_pill_active)
            view.setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_chip_active_text))
            view.typeface = boldTypeface
        } else {
            view.setBackgroundResource(R.drawable.bg_chip_pill_default)
            view.setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_text_secondary))
            view.typeface = regularTypeface
        }
    }

    private fun isFilterModified(): Boolean {
        val current = collectFilterFromUi()
        val default = RoomFilter.default()
        return current.province != default.province ||
            current.ward != default.ward ||
            current.minPrice != default.minPrice ||
            current.maxPrice != default.maxPrice ||
            current.genderPolicy != default.genderPolicy
    }

    private fun updateResetButtonState() {
        val hasChanges = isFilterModified()
        binding.btnResetFilter.isEnabled = hasChanges
        binding.btnResetFilter.alpha = if (hasChanges) 1.0f else 0.4f
    }

    private fun setupButtons() {
        binding.btnResetFilter.setOnSingleClickListener {
            if (isFilterModified()) {
                resetFiltersToDefault()
            }
        }

        binding.btnApplyFilter.setOnSingleClickListener {
            val filter = collectFilterFromUi()
            onFilterApplied?.invoke(filter)
            dismiss()
        }
    }

    private fun applyFilterToUi(filter: RoomFilter) {
        val b = _binding ?: return
        if (provincesList.isNotEmpty()) {
            val initialProvince = filter.province
            if (!initialProvince.isNullOrBlank() && !initialProvince.contains("Tất cả")) {
                b.actvProvince.setText(initialProvince, false)
                val matched = provincesList.find { it.name.equals(initialProvince, ignoreCase = true) }
                if (matched != null) {
                    selectedProvinceCode = matched.code
                    b.actvWard.isEnabled = true
                    loadWards(matched.code, restoreWard = filter.ward)
                } else {
                    setupWardEmpty()
                }
            } else {
                b.actvProvince.setText("", false)
                b.actvProvince.hint = "Tỉnh / Thành phố"
                setupWardEmpty()
            }
        }

        val minM = if (filter.minPrice != null) (filter.minPrice / 1_000_000f).coerceIn(0f, 15f) else 0f
        val maxM = if (filter.maxPrice != null) (filter.maxPrice / 1_000_000f).coerceIn(0f, 15f) else 15f
        setSliderRange(minM, maxM)
        updateQuickPriceChips(minM, maxM)

        selectedGenderPolicy = filter.genderPolicy
        updateGenderPills()

        updateResetButtonState()
    }

    private fun resetFiltersToDefault() {
        selectedProvinceCode = null
        currentFilter = RoomFilter.default()
        applyFilterToUi(RoomFilter.default())
        binding.actvProvince.setText("", false)
        binding.actvProvince.hint = "Tỉnh / Thành phố"
        setupWardEmpty()
        updateResetButtonState()
    }

    private fun collectFilterFromUi(): RoomFilter {
        val provinceText = binding.actvProvince.text?.toString()?.trim()
        val wardText = binding.actvWard.text?.toString()?.trim()

        val selectedProvince = if (provinceText.isNullOrBlank() || provinceText.contains("Tất cả") || provinceText.contains("Đang tải")) null else provinceText
        val selectedWard = if (wardText.isNullOrBlank() || wardText.contains("Tất cả") || wardText.contains("Đang tải") || wardText.contains("Vui lòng")) null else wardText

        val priceValues = binding.sliderPriceRange.values
        val minPrice = if (priceValues.isNotEmpty() && priceValues[0] > 0.05f) (priceValues[0] * 1_000_000).toLong() else null
        val maxPrice = if (priceValues.size >= 2 && priceValues[1] < 14.95f) (priceValues[1] * 1_000_000).toLong() else null

        val genderPolicy = selectedGenderPolicy

        return currentFilter.copy(
            province = selectedProvince,
            ward = selectedWard,
            minPrice = minPrice,
            maxPrice = maxPrice,
            genderPolicy = genderPolicy
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
