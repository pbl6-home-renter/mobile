package com.rentify.app.tenant.ui.search

import android.content.Context
import org.json.JSONArray

class RecentSearchManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getRecentSearches(): List<String> {
        val jsonStr = prefs.getString(KEY_SEARCHES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i)
                if (item.isNotBlank()) {
                    list.add(item)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSearch(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return

        val currentList = getRecentSearches().toMutableList()
        currentList.removeAll { it.equals(trimmed, ignoreCase = true) }
        currentList.add(0, trimmed)

        val limitedList = currentList.take(MAX_ITEMS)
        val jsonArray = JSONArray()
        limitedList.forEach { jsonArray.put(it) }

        prefs.edit().putString(KEY_SEARCHES, jsonArray.toString()).apply()
    }

    fun removeSearch(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return

        val currentList = getRecentSearches().toMutableList()
        currentList.removeAll { it.equals(trimmed, ignoreCase = true) }

        val jsonArray = JSONArray()
        currentList.forEach { jsonArray.put(it) }

        prefs.edit().putString(KEY_SEARCHES, jsonArray.toString()).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_SEARCHES).apply()
    }

    companion object {
        private const val PREF_NAME = "tenant_recent_searches_prefs"
        private const val KEY_SEARCHES = "recent_keywords"
        private const val MAX_ITEMS = 5
    }
}
