package com.rentify.app.tenant.ui.search

import java.io.Serializable

data class RoomFilter(
    val keyword: String? = null,
    val province: String? = null,
    val ward: String? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val genderPolicy: String = "any",
    val sort: String = "rentPrice",
    val order: String = "asc"
) : Serializable {

    fun activeFilterCount(): Int {
        var count = 0
        if (!province.isNullOrBlank()) count++
        if (!ward.isNullOrBlank()) count++
        if (minPrice != null || maxPrice != null) count++
        if (genderPolicy != "any") count++
        if (sort != "rentPrice" || order != "asc") count++
        return count
    }

    fun isFiltered(): Boolean {
        return activeFilterCount() > 0 || !keyword.isNullOrBlank()
    }

    companion object {
        fun default(): RoomFilter = RoomFilter()
    }
}
