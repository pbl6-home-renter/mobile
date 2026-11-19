package com.rentify.app.landlord.ui.property

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.network.dto.billing.BillingSettingResponseDto
import com.rentify.app.core.network.dto.billing.RateItemDto
import com.rentify.app.core.ui.extension.showMessage
import com.rentify.app.landlord.R
import java.text.NumberFormat
import java.util.Locale

object PolicySettingBottomSheetHelper {

    data class Option(val label: String, val code: String)

    val SERVICE_TYPES = listOf(
        Option("Điện", "electricity"),
        Option("Nước", "water"),
        Option("Internet / Wifi", "wifi"),
        Option("Vệ sinh / Rác", "cleaning"),
        Option("Gửi xe", "parking"),
        Option("Bảo trì", "maintenance"),
        Option("Khác", "other")
    )

    val RATE_KINDS = listOf(
        Option("Cố định / đơn vị", "flat"),
        Option("Cố định theo kỳ", "fixed_per_period"),
        Option("Theo người", "per_head"),
        Option("Theo diện tích (m²)", "per_area_m2"),
        Option("Theo số xe", "per_vehicle")
    )

    val BILLING_UNITS = listOf(
        Option("kWh", "kwh"),
        Option("m³", "m3"),
        Option("phòng", "room"),
        Option("người", "person"),
        Option("m²", "m2"),
        Option("xe", "vehicle"),
        Option("ngày", "day"),
        Option("lần", "occurrence")
    )

    fun formatRateSummary(rate: RateItemDto): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        val priceVal = rate.unitPrice?.toDoubleOrNull()?.toLong() ?: 0L
        val formattedPrice = if (rate.rateKind == "tiered") {
            "Theo bậc thang"
        } else {
            "${numberFormat.format(priceVal)} đ"
        }
        val unitLabel = BILLING_UNITS.firstOrNull { it.code == rate.billingUnit }?.label ?: rate.billingUnit
        val kindLabel = RATE_KINDS.firstOrNull { it.code == rate.rateKind }?.label ?: rate.rateKind
        return "${rate.name}: $formattedPrice / $unitLabel ($kindLabel)"
    }

    private class BlockHolder(
        val rootView: View,
        var typeCode: String,
        var rateKindCode: String,
        var billingUnitCode: String,
        val tvTitle: TextView,
        val btnRemove: MaterialButton,
        val tilName: TextInputLayout,
        val etName: TextInputEditText,
        val tilPrice: TextInputLayout,
        val etPrice: TextInputEditText
    )

    fun showRatePoliciesSheet(
        context: Context,
        initialRates: List<RateItemDto>,
        canInheritBuilding: Boolean = false,
        onInheritBuilding: (() -> Unit)? = null,
        onSave: (List<RateItemDto>) -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_rate_policies, null)
        dialog.setContentView(sheetView)

        val container = sheetView.findViewById<LinearLayout>(R.id.layoutRateBlocksContainer)
        val btnAdd = sheetView.findViewById<MaterialButton>(R.id.btnAddRateBlock)
        val btnInherit = sheetView.findViewById<MaterialButton>(R.id.btnInheritBuildingRate)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelRatePolicies)
        val btnSave = sheetView.findViewById<MaterialButton>(R.id.btnSaveRatePolicies)

        if (canInheritBuilding && onInheritBuilding != null) {
            btnInherit.visibility = View.VISIBLE
            btnInherit.setOnClickListener {
                onInheritBuilding()
                dialog.dismiss()
            }
        } else {
            btnInherit.visibility = View.GONE
        }

        val holders = mutableListOf<BlockHolder>()

        fun refreshTitles() {
            holders.forEachIndexed { index, holder ->
                holder.tvTitle.text = "Dịch vụ #${index + 1}"
                holder.btnRemove.visibility = if (holders.size > 1) View.VISIBLE else View.GONE
            }
        }

        fun addBlock(prefill: RateItemDto) {
            val blockView = LayoutInflater.from(context).inflate(R.layout.item_rate_policy_block, container, false)
            val tvTitle = blockView.findViewById<TextView>(R.id.tvRateBlockTitle)
            val btnRemove = blockView.findViewById<MaterialButton>(R.id.btnRemoveRateBlock)
            val actvType = blockView.findViewById<AutoCompleteTextView>(R.id.actvServiceType)
            val tilName = blockView.findViewById<TextInputLayout>(R.id.tilServiceName)
            val etName = blockView.findViewById<TextInputEditText>(R.id.etServiceName)
            val actvKind = blockView.findViewById<AutoCompleteTextView>(R.id.actvRateKind)
            val actvUnit = blockView.findViewById<AutoCompleteTextView>(R.id.actvBillingUnit)
            val tilPrice = blockView.findViewById<TextInputLayout>(R.id.tilUnitPrice)
            val etPrice = blockView.findViewById<TextInputEditText>(R.id.etUnitPrice)

            val safeKind = if (prefill.rateKind == "tiered") "flat" else prefill.rateKind
            val holder = BlockHolder(
                rootView = blockView,
                typeCode = prefill.type,
                rateKindCode = safeKind,
                billingUnitCode = prefill.billingUnit,
                tvTitle = tvTitle,
                btnRemove = btnRemove,
                tilName = tilName,
                etName = etName,
                tilPrice = tilPrice,
                etPrice = etPrice
            )

            // Type dropdown
            val typeAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, SERVICE_TYPES.map { it.label })
            actvType.setAdapter(typeAdapter)
            val initTypeOpt = SERVICE_TYPES.firstOrNull { it.code == prefill.type } ?: SERVICE_TYPES.first()
            actvType.setText(initTypeOpt.label, false)

            // Kind dropdown
            val kindAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, RATE_KINDS.map { it.label })
            actvKind.setAdapter(kindAdapter)
            val initKindOpt = RATE_KINDS.firstOrNull { it.code == safeKind } ?: RATE_KINDS.first()
            actvKind.setText(initKindOpt.label, false)

            // Unit dropdown
            val unitAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, BILLING_UNITS.map { it.label })
            actvUnit.setAdapter(unitAdapter)
            val initUnitOpt = BILLING_UNITS.firstOrNull { it.code == prefill.billingUnit } ?: BILLING_UNITS.first()
            actvUnit.setText(initUnitOpt.label, false)

            etName.setText(prefill.name)
            val cleanPrice = prefill.unitPrice?.substringBefore(".") ?: "0"
            etPrice.setText(cleanPrice)

            actvType.setOnItemClickListener { _, _, position, _ ->
                val selected = SERVICE_TYPES.getOrNull(position) ?: return@setOnItemClickListener
                holder.typeCode = selected.code
                when (selected.code) {
                    "electricity" -> {
                        if (etName.text.isNullOrBlank()) etName.setText("Tiền điện sinh hoạt")
                        holder.billingUnitCode = "kwh"
                        actvUnit.setText("kWh", false)
                    }
                    "water" -> {
                        if (etName.text.isNullOrBlank()) etName.setText("Tiền nước sinh hoạt")
                        holder.billingUnitCode = "m3"
                        actvUnit.setText("m³", false)
                    }
                    "wifi" -> {
                        if (etName.text.isNullOrBlank()) etName.setText("Internet / Wifi")
                        holder.rateKindCode = "fixed_per_period"
                        actvKind.setText("Cố định theo kỳ", false)
                        holder.billingUnitCode = "room"
                        actvUnit.setText("phòng", false)
                    }
                    "cleaning" -> {
                        if (etName.text.isNullOrBlank()) etName.setText("Phí vệ sinh / rác")
                        holder.rateKindCode = "per_head"
                        actvKind.setText("Theo người", false)
                        holder.billingUnitCode = "person"
                        actvUnit.setText("người", false)
                    }
                    "parking" -> {
                        if (etName.text.isNullOrBlank()) etName.setText("Phí gửi xe")
                        holder.rateKindCode = "per_vehicle"
                        actvKind.setText("Theo số xe", false)
                        holder.billingUnitCode = "vehicle"
                        actvUnit.setText("xe", false)
                    }
                }
            }

            actvKind.setOnItemClickListener { _, _, position, _ ->
                val selected = RATE_KINDS.getOrNull(position) ?: return@setOnItemClickListener
                holder.rateKindCode = selected.code
            }

            actvUnit.setOnItemClickListener { _, _, position, _ ->
                val selected = BILLING_UNITS.getOrNull(position) ?: return@setOnItemClickListener
                holder.billingUnitCode = selected.code
            }

            btnRemove.setOnClickListener {
                if (holders.size > 1) {
                    holders.remove(holder)
                    container.removeView(blockView)
                    refreshTitles()
                }
            }

            holders.add(holder)
            container.addView(blockView)
            refreshTitles()
        }

        val seeds = if (initialRates.isNotEmpty()) {
            initialRates
        } else {
            listOf(
                RateItemDto(
                    type = "electricity",
                    name = "Tiền điện sinh hoạt",
                    rateKind = "flat",
                    unitPrice = "4000",
                    billingUnit = "kwh"
                ),
                RateItemDto(
                    type = "water",
                    name = "Tiền nước sinh hoạt",
                    rateKind = "flat",
                    unitPrice = "15000",
                    billingUnit = "m3"
                )
            )
        }

        seeds.forEach { addBlock(it) }

        btnAdd.setOnClickListener {
            addBlock(
                RateItemDto(
                    type = "wifi",
                    name = "Internet / Wifi",
                    rateKind = "fixed_per_period",
                    unitPrice = "100000",
                    billingUnit = "room"
                )
            )
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val collected = mutableListOf<RateItemDto>()
            for (h in holders) {
                val name = h.etName.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    h.tilName.error = "Nhập tên dịch vụ"
                    return@setOnClickListener
                } else {
                    h.tilName.error = null
                }

                val priceRaw = h.etPrice.text?.toString()?.trim().orEmpty()
                val priceLong = priceRaw.toLongOrNull()
                if (priceLong == null || priceLong < 0L) {
                    h.tilPrice.error = "Nhập đơn giá hợp lệ"
                    return@setOnClickListener
                } else {
                    h.tilPrice.error = null
                }

                collected.add(
                    RateItemDto(
                        type = h.typeCode,
                        name = name,
                        rateKind = h.rateKindCode,
                        unitPrice = priceLong.toString(),
                        steps = null,
                        billingUnit = h.billingUnitCode
                    )
                )
            }

            if (collected.isEmpty()) {
                dialog.showMessage("Vui lòng thêm ít nhất 1 dịch vụ")
                return@setOnClickListener
            }

            onSave(collected)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showBillingSettingsSheet(
        context: Context,
        currentSetting: BillingSettingResponseDto?,
        canInheritBuilding: Boolean = false,
        onInheritBuilding: (() -> Unit)? = null,
        onSave: (paymentDueDay: Int, remindDay: Int?, botEnabled: Boolean) -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_billing_settings, null)
        dialog.setContentView(sheetView)

        val tilDueDay = sheetView.findViewById<TextInputLayout>(R.id.tilPaymentDueDay)
        val etDueDay = sheetView.findViewById<TextInputEditText>(R.id.etPaymentDueDay)
        val tilRemindDay = sheetView.findViewById<TextInputLayout>(R.id.tilRemindDay)
        val etRemindDay = sheetView.findViewById<TextInputEditText>(R.id.etRemindDay)
        val cbBot = sheetView.findViewById<MaterialCheckBox>(R.id.cbBotEnabled)
        val btnInherit = sheetView.findViewById<MaterialButton>(R.id.btnInheritBuildingBilling)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelBillingSetting)
        val btnSave = sheetView.findViewById<MaterialButton>(R.id.btnSaveBillingSetting)

        etDueDay.setText((currentSetting?.paymentDueDay ?: 5).toString())
        etRemindDay.setText(currentSetting?.remindDay?.toString() ?: "")
        cbBot.isChecked = currentSetting?.botEnabled ?: true

        if (canInheritBuilding && onInheritBuilding != null) {
            btnInherit.visibility = View.VISIBLE
            btnInherit.setOnClickListener {
                onInheritBuilding()
                dialog.dismiss()
            }
        } else {
            btnInherit.visibility = View.GONE
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val dueDay = etDueDay.text?.toString()?.trim()?.toIntOrNull()
            if (dueDay == null || dueDay !in 1..28) {
                tilDueDay.error = "Vui lòng nhập ngày từ 1 đến 28"
                return@setOnClickListener
            } else {
                tilDueDay.error = null
            }

            val remindRaw = etRemindDay.text?.toString()?.trim().orEmpty()
            val remindDay: Int? = if (remindRaw.isEmpty()) {
                null
            } else {
                val parsed = remindRaw.toIntOrNull()
                if (parsed == null || parsed !in 1..28) {
                    tilRemindDay.error = "Ngày nhắc nộp phải từ 1 đến 28"
                    return@setOnClickListener
                }
                parsed
            }
            tilRemindDay.error = null

            onSave(dueDay, remindDay, cbBot.isChecked)
            dialog.dismiss()
        }

        dialog.show()
    }
}
