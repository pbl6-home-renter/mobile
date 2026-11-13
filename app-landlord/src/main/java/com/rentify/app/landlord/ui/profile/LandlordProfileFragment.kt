package com.rentify.app.landlord.ui.profile

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
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

        lifecycleScope.launch {
            val token = tokenDataStore.accessToken.first()
            if (!token.isNullOrBlank()) {
                val userApi = NetworkClient.createService<com.rentify.app.core.network.api.UserApiService>(
                    tokenProvider = { token }
                )
                try {
                    val resp = userApi.getCurrentUser()
                    if (resp.isSuccessful) {
                        val user = resp.body()?.data
                        binding.tvLandlordName.text = user?.name ?: "Chủ nhà Rentify"
                        binding.tvLandlordEmail.text = user?.email ?: ""
                        val initial = (user?.name ?: "L").trim().firstOrNull()?.uppercaseChar()?.toString() ?: "L"
                        binding.tvAvatarInitial.text = initial
                    }
                } catch (e: Exception) {
                    // Fallback to default
                }
            }
        }

        binding.rowLogout.setOnSingleClickListener {
            listener?.onLandlordLogoutRequested()
        }
    }

    companion object {
        fun newInstance(): LandlordProfileFragment = LandlordProfileFragment()
    }
}

