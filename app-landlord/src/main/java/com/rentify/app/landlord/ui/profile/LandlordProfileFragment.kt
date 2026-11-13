package com.rentify.app.landlord.ui.profile

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.user.UserRepositoryImpl
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.UserApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.user.UserChangePasswordRequestDto
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.landlord.databinding.DialogChangePasswordBinding
import com.rentify.app.landlord.databinding.FragmentLandlordProfileBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LandlordProfileFragment : BaseFragment<FragmentLandlordProfileBinding>(FragmentLandlordProfileBinding::inflate) {

    interface LandlordProfileListener {
        fun onLandlordLogoutRequested()
    }

    private var listener: LandlordProfileListener? = null
    private val tokenDataStore by lazy { TokenDataStore(requireContext().applicationContext) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? LandlordProfileListener ?: context as? LandlordProfileListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = false)

        loadProfile()

        binding.rowChangePassword.setOnSingleClickListener {
            showChangePasswordDialog()
        }

        binding.rowLogout.setOnSingleClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Chủ trọ?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    listener?.onLandlordLogoutRequested()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val token = tokenDataStore.accessToken.first()
            if (!token.isNullOrBlank()) {
                val userApi = NetworkClient.createService<UserApiService>(
                    tokenProvider = { token }
                )
                val userRepo = UserRepositoryImpl(userApi)
                when (val result = userRepo.getCurrentUser()) {
                    is NetworkResult.Success -> {
                        val user = result.data.data
                        if (user != null) {
                            binding.tvLandlordName.text = user.name.ifBlank { "Chủ nhà Rentify" }
                            binding.tvLandlordEmail.text = user.email
                            val initial = user.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "L"
                            binding.tvAvatarInitial.text = initial
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(requireContext()))

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Đổi mật khẩu") { _, _ ->
                val currentPassword = dialogBinding.etCurrentPassword.text?.toString() ?: ""
                val newPassword = dialogBinding.etNewPassword.text?.toString() ?: ""
                val confirmPassword = dialogBinding.etConfirmPassword.text?.toString() ?: ""

                if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPassword != confirmPassword) {
                    Toast.makeText(requireContext(), "Mật khẩu xác nhận không trùng khớp", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPassword.length < 8) {
                    Toast.makeText(requireContext(), "Mật khẩu mới phải có tối thiểu 8 ký tự", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val token = tokenDataStore.accessToken.first()
                    if (!token.isNullOrBlank()) {
                        val userApi = NetworkClient.createService<UserApiService>(
                            tokenProvider = { token }
                        )
                        val userRepo = UserRepositoryImpl(userApi)
                        when (val res = userRepo.changePassword(
                            UserChangePasswordRequestDto(
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                confirmPassword = confirmPassword
                            )
                        )) {
                            is NetworkResult.Success -> {
                                Toast.makeText(requireContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                            }
                            is NetworkResult.ApiError -> {
                                Toast.makeText(requireContext(), res.error?.message ?: "Lỗi đổi mật khẩu", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(requireContext(), "Lỗi kết nối khi đổi mật khẩu", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    companion object {
        fun newInstance(): LandlordProfileFragment = LandlordProfileFragment()
    }
}
