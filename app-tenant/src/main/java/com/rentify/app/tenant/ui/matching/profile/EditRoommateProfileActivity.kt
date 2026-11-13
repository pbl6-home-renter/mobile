package com.rentify.app.tenant.ui.matching.profile

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rentify.app.core.network.dto.matching.LifestyleDto
import com.rentify.app.core.network.dto.matching.RoommateProfileMyResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileSaveRequestDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ActivityEditRoommateProfileBinding
import kotlinx.coroutines.launch

class EditRoommateProfileActivity : BaseActivity<ActivityEditRoommateProfileBinding>(ActivityEditRoommateProfileBinding::inflate) {

    private val viewModel: MyRoommateProfileViewModel by viewModels {
        MyRoommateProfileViewModelFactory(this)
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbarEditProfile.setNavigationOnClickListener { finish() }

        binding.btnSaveProfile.setOnSingleClickListener {
            saveProfile()
        }

        observeViewModel()
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
                        binding.pbEditLoading.isVisible = loading
                        binding.btnSaveProfile.isEnabled = !loading
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

        profile.birthYear?.let { binding.edtBirthYear.setText(it.toString()) }
        profile.occupation?.let { binding.edtOccupation.setText(it) }
        profile.preferredProvince?.let { binding.edtPreferredProvince.setText(it) }
        profile.preferredDistrict?.let { binding.edtPreferredDistrict.setText(it) }
        profile.preferredArea?.let { binding.edtPreferredArea.setText(it) }
        profile.budgetMin?.let { binding.edtBudgetMin.setText(it.toString()) }
        profile.budgetMax?.let { binding.edtBudgetMax.setText(it.toString()) }
        profile.moveInOn?.let { binding.edtMoveInOn.setText(it) }
        profile.bio?.let { binding.edtBio.setText(it) }

        val habits = profile.lifestyle
        binding.cbCleanAndTidy.isChecked = habits?.cleanAndTidy == true
        binding.cbEarlyBird.isChecked = habits?.earlyBird == true
        binding.cbNightOwl.isChecked = habits?.nightOwl == true
        binding.cbPetFriendly.isChecked = habits?.petFriendly == true
        binding.cbCooking.isChecked = habits?.cooking == true
        binding.cbSmoking.isChecked = habits?.smoking == true
        binding.cbGuestsAllowed.isChecked = habits?.guestsAllowed == true
    }

    private fun saveProfile() {
        val gender = when {
            binding.rbMale.isChecked -> "male"
            binding.rbFemale.isChecked -> "female"
            else -> "other"
        }

        val birthYear = binding.edtBirthYear.text?.toString()?.trim()?.toIntOrNull()
        val occupation = binding.edtOccupation.text?.toString()?.trim()
        val province = binding.edtPreferredProvince.text?.toString()?.trim()
        val district = binding.edtPreferredDistrict.text?.toString()?.trim()
        val area = binding.edtPreferredArea.text?.toString()?.trim()
        val budgetMin = binding.edtBudgetMin.text?.toString()?.trim()?.toLongOrNull()
        val budgetMax = binding.edtBudgetMax.text?.toString()?.trim()?.toLongOrNull()

        if (budgetMin != null && budgetMax != null && budgetMin > budgetMax) {
            toast("Ngân sách tối thiểu không được lớn hơn ngân sách tối đa")
            return
        }

        val moveInOn = binding.edtMoveInOn.text?.toString()?.trim()
        val bio = binding.edtBio.text?.toString()?.trim()

        val lifestyle = LifestyleDto(
            cleanAndTidy = binding.cbCleanAndTidy.isChecked,
            earlyBird = binding.cbEarlyBird.isChecked,
            nightOwl = binding.cbNightOwl.isChecked,
            petFriendly = binding.cbPetFriendly.isChecked,
            smoking = binding.cbSmoking.isChecked,
            cooking = binding.cbCooking.isChecked,
            guestsAllowed = binding.cbGuestsAllowed.isChecked
        )

        val request = RoommateProfileSaveRequestDto(
            gender = gender,
            birthYear = birthYear,
            occupation = occupation.takeIf { !it.isNullOrBlank() },
            preferredProvince = province.takeIf { !it.isNullOrBlank() },
            preferredDistrict = district.takeIf { !it.isNullOrBlank() },
            preferredArea = area.takeIf { !it.isNullOrBlank() },
            budgetMin = budgetMin,
            budgetMax = budgetMax,
            moveInOn = moveInOn.takeIf { !it.isNullOrBlank() },
            bio = bio.takeIf { !it.isNullOrBlank() },
            lifestyle = lifestyle,
            isVisible = binding.switchIsVisible.isChecked
        )

        viewModel.saveProfile(request)
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, EditRoommateProfileActivity::class.java)
        }
    }
}

