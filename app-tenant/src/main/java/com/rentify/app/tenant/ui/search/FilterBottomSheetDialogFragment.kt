package com.rentify.app.tenant.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.BottomSheetRoomFilterBinding

class FilterBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRoomFilterBinding? = null
    private val binding get() = _binding!!

    var onFilterApplied: ((RoomFilter) -> Unit)? = null

    private var currentFilter: RoomFilter = RoomFilter.default()

    private val provinces = listOf("Tất cả", "Thành phố Hồ Chí Minh", "Thành phố Hà Nội", "Thành phố Đà Nẵng", "Cần Thơ", "Hải Phòng")
    private val wards = listOf("Tất cả phường/xã", "Bách Khoa", "Đồng Tâm", "Láng Thượng", "Dịch Vọng", "Bến Nghé", "Thảo Điền")

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
        val provinceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, provinces)
        binding.actvProvince.setAdapter(provinceAdapter)

        val wardAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, wards)
        binding.actvWard.setAdapter(wardAdapter)
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
        // Province & Ward
        binding.actvProvince.setText(filter.province ?: "Tất cả", false)
        binding.actvWard.setText(filter.ward ?: "Tất cả phường/xã", false)

        // Price
        val minM = if (filter.minPrice != null) (filter.minPrice / 1_000_000f).coerceIn(0f, 15f) else 0f
        val maxM = if (filter.maxPrice != null) (filter.maxPrice / 1_000_000f).coerceIn(0f, 15f) else 15f
        setSliderRange(minM, maxM)

        // Gender
        when (filter.genderPolicy) {
            "male" -> binding.toggleGender.check(R.id.btnGenderMale)
            "female" -> binding.toggleGender.check(R.id.btnGenderFemale)
            else -> binding.toggleGender.check(R.id.btnGenderAll)
        }

        // Sort
        if (filter.sort == "rentPrice" && filter.order == "asc") {
            binding.toggleSort.check(R.id.btnSortPriceAsc)
        } else if (filter.sort == "rentPrice" && filter.order == "desc") {
            binding.toggleSort.check(R.id.btnSortPriceDesc)
        } else {
            binding.toggleSort.check(R.id.btnSortNewest)
        }
    }

    private fun resetFiltersToDefault() {
        applyFilterToUi(RoomFilter.default())
    }

    private fun collectFilterFromUi(): RoomFilter {
        val provinceText = binding.actvProvince.text?.toString()?.trim()
        val wardText = binding.actvWard.text?.toString()?.trim()

        val selectedProvince = if (provinceText.isNullOrBlank() || provinceText == "Tất cả") null else provinceText
        val selectedWard = if (wardText.isNullOrBlank() || wardText.contains("Tất cả")) null else wardText

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
