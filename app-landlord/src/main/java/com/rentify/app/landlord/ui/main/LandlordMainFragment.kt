package com.rentify.app.landlord.ui.main

import android.content.Context
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.FragmentLandlordMainBinding
import com.rentify.app.landlord.ui.chat.LandlordChatFragment
import com.rentify.app.landlord.ui.dashboard.DashboardFragment
import com.rentify.app.landlord.ui.operations.OperationsHubFragment
import com.rentify.app.landlord.ui.profile.LandlordProfileFragment
import com.rentify.app.landlord.ui.property.PropertiesFragment

class LandlordMainFragment : BaseFragment<FragmentLandlordMainBinding>(FragmentLandlordMainBinding::inflate),
    DashboardFragment.DashboardListener,
    LandlordProfileFragment.LandlordProfileListener {

    interface LandlordMainListener {
        fun onLandlordLogoutRequested()
    }

    private var listener: LandlordMainListener? = null
    private var activeFragment: Fragment? = null

    private val dashboardFragment by lazy { DashboardFragment.newInstance() }
    private val propertiesFragment by lazy { PropertiesFragment.newInstance() }
    private val operationsFragment by lazy { OperationsHubFragment.newInstance() }
    private val chatFragment by lazy { LandlordChatFragment.newInstance() }
    private val profileFragment by lazy { LandlordProfileFragment.newInstance() }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? LandlordMainListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { _, insets -> insets }
        binding.root.applySystemBarsPadding(top = false, bottom = true)
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.isItemActiveIndicatorEnabled = false

        switchTab(dashboardFragment, TAG_DASHBOARD)

        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    switchTab(dashboardFragment, TAG_DASHBOARD)
                    true
                }
                R.id.nav_properties -> {
                    switchTab(propertiesFragment, TAG_PROPERTIES)
                    true
                }
                R.id.nav_operations -> {
                    switchTab(operationsFragment, TAG_OPERATIONS)
                    true
                }
                R.id.nav_chat -> {
                    switchTab(chatFragment, TAG_CHAT)
                    true
                }
                R.id.nav_profile -> {
                    switchTab(profileFragment, TAG_PROFILE)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchTab(targetFragment: Fragment, tag: String) {
        if (activeFragment === targetFragment) return

        val transaction = childFragmentManager.beginTransaction()

        activeFragment?.let {
            transaction.hide(it)
        }

        val existing = childFragmentManager.findFragmentByTag(tag)
        if (existing == null) {
            transaction.add(R.id.childNavContainer, targetFragment, tag)
        } else {
            transaction.show(existing)
        }

        transaction.commit()
        activeFragment = targetFragment
    }

    override fun onNavigateToProperties() {
        switchTab(propertiesFragment, TAG_PROPERTIES)
        binding.bottomNav.selectedItemId = R.id.nav_properties
    }

    override fun onNavigateToOperations(tabIndex: Int) {
        switchTab(operationsFragment, TAG_OPERATIONS)
        binding.bottomNav.selectedItemId = R.id.nav_operations
    }

    override fun onLandlordLogoutRequested() {
        listener?.onLandlordLogoutRequested()
    }

    companion object {
        private const val TAG_DASHBOARD = "tag_dashboard"
        private const val TAG_PROPERTIES = "tag_properties"
        private const val TAG_OPERATIONS = "tag_operations"
        private const val TAG_CHAT = "tag_chat"
        private const val TAG_PROFILE = "tag_profile"

        fun newInstance(): LandlordMainFragment = LandlordMainFragment()
    }
}

