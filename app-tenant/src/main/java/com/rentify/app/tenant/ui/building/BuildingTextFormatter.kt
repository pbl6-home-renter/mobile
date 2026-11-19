package com.rentify.app.tenant.ui.building

import com.rentify.app.tenant.data.repository.BuildingSummary

object BuildingTextFormatter {

    fun location(building: BuildingSummary): String {
        return listOf(building.ward, building.province)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Đang cập nhật địa chỉ" }
    }

    fun fullAddress(building: BuildingSummary): String {
        return listOf(building.address, building.ward, building.province)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Đang cập nhật địa chỉ" }
    }
}
