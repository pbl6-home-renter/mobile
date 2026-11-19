package com.rentify.app.tenant.ui.matching.profile

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.data.repository.administrative.AdministrativeUnitsRepository
import com.rentify.app.core.data.repository.administrative.AdministrativeUnitsRepositoryImpl
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.AdministrativeUnitsApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import com.rentify.app.core.network.dto.matching.RoommateProfileMyResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileSaveRequestDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.component.RentifyTextField
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ActivityEditRoommateProfileBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.rentify.app.core.ui.R as CoreR

class EditRoommateProfileActivity : BaseActivity<ActivityEditRoommateProfileBinding>(ActivityEditRoommateProfileBinding::inflate) {

    private val viewModel: MyRoommateProfileViewModel by viewModels {
        MyRoommateProfileViewModelFactory(this)
    }

    private val administrativeUnitsRepository: AdministrativeUnitsRepository by lazy {
        AdministrativeUnitsRepositoryImpl(NetworkClient.createService<AdministrativeUnitsApiService>())
    }

    private var provinces: List<AdministrativeProvinceDto> = emptyList()
    private var wards: List<AdministrativeWardDto> = emptyList()
    private var selectedProvince: AdministrativeProvinceDto? = null
    private var selectedWardName: String? = null
    private var savedProvinceName: String? = null
    private var savedWardName: String? = null
    private var selectedMoveInDate: LocalDate? = null
    private var isSaving = false

    private val displayDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbarEditProfile.setNavigationOnClickListener { finish() }

        binding.btnSaveProfile.setOnSingleClickListener {
            saveProfile()
        }

        binding.edtBio.setOnFocusChangeListener { _, hasFocus ->
            binding.cardBio.isActivated = hasFocus
        }

        setupRequiredFieldWatchers()
        setupPickers()
        loadProvinces()
        updateSaveButtonState()

        observeViewModel()
    }

    private fun setupRequiredFieldWatchers() {
        binding.rgGender.setOnCheckedChangeListener { _, _ -> updateSaveButtonState() }
        binding.edtBirthYear.editText.doAfterTextChanged { updateSaveButtonState() }
        binding.edtOccupation.editText.doAfterTextChanged { updateSaveButtonState() }
        binding.edtBio.doAfterTextChanged { updateSaveButtonState() }
        binding.edtOccupation.editText.filters = arrayOf(InputFilter.LengthFilter(OCCUPATION_MAX_LENGTH))
        binding.edtBio.filters = arrayOf(InputFilter.LengthFilter(BIO_MAX_LENGTH))
    }

    private fun setupPickers() {
        binding.edtPreferredProvince.asPicker { showProvinceOptions() }
        binding.edtPreferredDistrict.asPicker { showWardOptions() }
        binding.edtMoveInOn.asPicker(CoreR.drawable.ic_calendar) { showMoveInDatePicker() }
    }

    private fun RentifyTextField.asPicker(
        iconRes: Int = CoreR.drawable.ic_chevron_down,
        onClick: () -> Unit
    ) {
        editText.inputType = InputType.TYPE_NULL
        editText.isFocusable = false
        editText.isCursorVisible = false
        editText.setOnClickListener { onClick() }
        textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
        textInputLayout.setEndIconDrawable(iconRes)
        textInputLayout.setEndIconTintList(ContextCompat.getColorStateList(context, CoreR.color.rentify_text_muted))
        textInputLayout.setEndIconOnClickListener { onClick() }
    }

    private fun loadProvinces() {
        lifecycleScope.launch {
            when (val result = administrativeUnitsRepository.getProvinces()) {
                is NetworkResult.Success -> {
                    provinces = result.data.data ?: emptyList()
                    restoreSavedLocation()
                }
                else -> toast("Không tải được danh sách tỉnh/thành phố")
            }
        }
    }

    private fun loadWards(provinceCode: String, restoreWardName: String?) {
        lifecycleScope.launch {
            when (val result = administrativeUnitsRepository.getWards(provinceCode)) {
                is NetworkResult.Success -> {
                    if (selectedProvince?.code != provinceCode) return@launch
                    wards = result.data.data ?: emptyList()
                    restoreWardName
                        ?.let { name -> wards.find { it.name.equals(name, ignoreCase = true) } }
                        ?.let { selectWard(it.name) }
                }
                else -> toast("Không tải được danh sách phường/xã")
            }
        }
    }

    private fun restoreSavedLocation() {
        val provinceName = savedProvinceName ?: return
        val province = provinces.find { it.name.equals(provinceName, ignoreCase = true) } ?: return
        selectProvince(province, restoreWardName = savedWardName)
    }

    private fun selectProvince(province: AdministrativeProvinceDto, restoreWardName: String? = null) {
        selectedProvince = province
        binding.edtPreferredProvince.text = province.name
        selectedWardName = null
        wards = emptyList()
        binding.edtPreferredDistrict.text = ""
        updateSaveButtonState()
        loadWards(province.code, restoreWardName)
    }

    private fun selectWard(name: String) {
        selectedWardName = name
        binding.edtPreferredDistrict.text = name
        updateSaveButtonState()
    }

    private fun showProvinceOptions() {
        if (provinces.isEmpty()) {
            toast("Đang tải danh sách tỉnh/thành phố")
            return
        }
        showOptions(binding.edtPreferredProvince, provinces.map { it.name }) { index ->
            val province = provinces[index]
            if (province.code != selectedProvince?.code) selectProvince(province)
        }
    }

    private fun showWardOptions() {
        if (selectedProvince == null) {
            toast("Vui lòng chọn tỉnh/thành phố trước")
            return
        }
        if (wards.isEmpty()) {
            toast("Đang tải danh sách phường/xã")
            return
        }
        showOptions(binding.edtPreferredDistrict, wards.map { it.name }) { index ->
            selectWard(wards[index].name)
        }
    }

    private fun showOptions(anchor: View, options: List<String>, onSelect: (Int) -> Unit) {
        ListPopupWindow(this).apply {
            anchorView = anchor
            width = anchor.width
            height = if (options.size > MAX_VISIBLE_OPTIONS) {
                (POPUP_MAX_HEIGHT_DP * resources.displayMetrics.density).toInt()
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(this@EditRoommateProfileActivity, R.drawable.bg_popup_sort_menu))
            verticalOffset = (POPUP_VERTICAL_OFFSET_DP * resources.displayMetrics.density).toInt()
            setAdapter(ArrayAdapter(this@EditRoommateProfileActivity, android.R.layout.simple_dropdown_item_1line, options))
            setOnItemClickListener { _, _, position, _ ->
                onSelect(position)
                dismiss()
            }
            show()
        }
    }

    private fun showMoveInDatePicker() {
        val initial = selectedMoveInDate ?: LocalDate.now()
        DatePickerDialog(
            this,
            { _, year, month, day -> setMoveInDate(LocalDate.of(year, month + 1, day)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).apply {
            datePicker.minDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.show()
    }

    private fun setMoveInDate(date: LocalDate?) {
        selectedMoveInDate = date
        binding.edtMoveInOn.text = date?.format(displayDateFormat).orEmpty()
    }

    private fun isProfileComplete(): Boolean {
        val birthYear = binding.edtBirthYear.text.trim().toIntOrNull()
        return binding.rgGender.checkedRadioButtonId != View.NO_ID &&
            birthYear != null && birthYear in MIN_BIRTH_YEAR..LocalDate.now().year &&
            binding.edtOccupation.text.isNotBlank() &&
            !binding.edtBio.text.isNullOrBlank() &&
            selectedProvince != null &&
            !selectedWardName.isNullOrBlank()
    }

    private fun updateSaveButtonState() {
        binding.btnSaveProfile.isEnabled = isProfileComplete() && !isSaving
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> binding.pbEditLoading.isVisible = true
                            is UiState.Success -> {
                                binding.pbEditLoading.isVisible = false
                                state.data?.let { populateExistingProfile(it) }
                            }
                            is UiState.Error -> {
                                binding.pbEditLoading.isVisible = false
                                toast(state.message)
                            }
                            is UiState.Idle -> Unit
                        }
                    }
                }

                launch {
                    viewModel.saveLoading.collect { loading ->
                        isSaving = loading
                        binding.pbEditLoading.isVisible = loading
                        updateSaveButtonState()
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is MyRoommateProfileUiEvent.ShowToast -> toast(event.message)
                            is MyRoommateProfileUiEvent.SaveSuccess -> finish()
                        }
                    }
                }
            }
        }
    }

    private fun populateExistingProfile(profile: RoommateProfileMyResponseDto) {
        binding.switchIsVisible.isChecked = profile.isVisible

        when (profile.gender?.lowercase()) {
            "male" -> binding.rbMale.isChecked = true
            "female" -> binding.rbFemale.isChecked = true
            "other" -> binding.rbOther.isChecked = true
        }

        profile.birthYear?.let { binding.edtBirthYear.text = it.toString() }
        profile.occupation?.let { binding.edtOccupation.text = it }
        profile.budgetMin?.let { binding.edtBudgetMin.text = it.toString() }
        profile.budgetMax?.let { binding.edtBudgetMax.text = it.toString() }
        profile.bio?.let { binding.edtBio.setText(it) }

        savedProvinceName = profile.preferredProvince
        savedWardName = profile.preferredDistrict
        restoreSavedLocation()

        profile.moveInOn?.let { value ->
            try {
                setMoveInDate(LocalDate.parse(value))
            } catch (_: DateTimeParseException) {
                setMoveInDate(null)
            }
        }

        val habits = profile.lifestyle
        binding.cbCleanAndTidy.isChecked = habits?.get("cleanAndTidy") == true
        binding.cbEarlyBird.isChecked = habits?.get("earlyBird") == true
        binding.cbNightOwl.isChecked = habits?.get("nightOwl") == true
        binding.cbPetFriendly.isChecked = habits?.get("petFriendly") == true
        binding.cbCooking.isChecked = habits?.get("cooking") == true
        binding.cbSmoking.isChecked = habits?.get("smoking") == true
        binding.cbGuestsAllowed.isChecked = habits?.get("guestsAllowed") == true
    }

    private fun saveProfile() {
        val province = selectedProvince ?: return
        val ward = selectedWardName ?: return

        val gender = when {
            binding.rbMale.isChecked -> "male"
            binding.rbFemale.isChecked -> "female"
            else -> "other"
        }

        val budgetMin = binding.edtBudgetMin.text.trim().toLongOrNull()
        val budgetMax = binding.edtBudgetMax.text.trim().toLongOrNull()

        if (budgetMin != null && budgetMax != null && budgetMin > budgetMax) {
            toast("Ngân sách tối thiểu không được lớn hơn ngân sách tối đa")
            return
        }

        val occupation = binding.edtOccupation.text.trim()
        val bio = binding.edtBio.text?.toString()?.trim()

        val lifestyle = mapOf(
            "cleanAndTidy" to binding.cbCleanAndTidy.isChecked,
            "earlyBird" to binding.cbEarlyBird.isChecked,
            "nightOwl" to binding.cbNightOwl.isChecked,
            "petFriendly" to binding.cbPetFriendly.isChecked,
            "smoking" to binding.cbSmoking.isChecked,
            "cooking" to binding.cbCooking.isChecked,
            "guestsAllowed" to binding.cbGuestsAllowed.isChecked
        )

        val request = RoommateProfileSaveRequestDto(
            gender = gender,
            birthYear = binding.edtBirthYear.text.trim().toIntOrNull(),
            occupation = occupation.takeIf { it.isNotBlank() },
            preferredProvince = province.name,
            preferredDistrict = ward,
            preferredArea = "$ward, ${province.name}",
            budgetMin = budgetMin,
            budgetMax = budgetMax,
            moveInOn = selectedMoveInDate?.toString(),
            bio = bio?.takeIf { it.isNotBlank() },
            lifestyle = lifestyle,
            isVisible = binding.switchIsVisible.isChecked
        )

        viewModel.saveProfile(request)
    }

    companion object {
        private const val MIN_BIRTH_YEAR = 1900
        private const val OCCUPATION_MAX_LENGTH = 100
        private const val BIO_MAX_LENGTH = 1000
        private const val MAX_VISIBLE_OPTIONS = 6
        private const val POPUP_MAX_HEIGHT_DP = 260
        private const val POPUP_VERTICAL_OFFSET_DP = 4

        fun createIntent(context: Context): Intent {
            return Intent(context, EditRoommateProfileActivity::class.java)
        }
    }
}
