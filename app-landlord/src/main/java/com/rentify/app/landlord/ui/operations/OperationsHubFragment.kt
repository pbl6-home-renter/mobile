package com.rentify.app.landlord.ui.operations

import android.content.Intent
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.FragmentOperationsHubBinding
import com.rentify.app.landlord.ui.billing.LandlordInvoicesActivity
import com.rentify.app.landlord.ui.billing.LandlordMeterReadingsActivity
import com.rentify.app.landlord.ui.contract.LandlordContractsActivity

class OperationsHubFragment : BaseFragment<FragmentOperationsHubBinding>(FragmentOperationsHubBinding::inflate) {

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = false)

        binding.cardHubContracts.setOnClickListener {
            startActivity(Intent(requireContext(), LandlordContractsActivity::class.java))
        }

        binding.cardHubMeters.setOnClickListener {
            startActivity(Intent(requireContext(), LandlordMeterReadingsActivity::class.java))
        }

        binding.cardHubInvoices.setOnClickListener {
            startActivity(Intent(requireContext(), LandlordInvoicesActivity::class.java))
        }

        binding.cardHubIssues.setOnClickListener {
            startActivity(Intent(requireContext(), LandlordIssuesActivity::class.java))
        }
    }

    companion object {
        fun newInstance(): OperationsHubFragment = OperationsHubFragment()
    }
}

