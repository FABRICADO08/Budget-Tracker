package com.example.util

import android.content.Context
import java.util.Locale

/** Local category budgets are namespaced by Firebase UID for shared-device account separation. */
object CategoryBudgetPreferences {
    private const val PREFIX = "category_budget_prefs_"
    private const val KEY_PREFIX = "monthly_limit:"

    private fun prefs(context: Context, userId: String) =
        context.getSharedPreferences(PREFIX + userId, Context.MODE_PRIVATE)

    private fun key(category: String) = KEY_PREFIX + category.trim().lowercase(Locale.ROOT)

    fun getBudget(context: Context, userId: String, category: String): Double {
        if (userId.isBlank() || category.isBlank()) return 0.0
        val value = prefs(context, userId).all[key(category)]
        return when (value) {
            is String -> value.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            is Number -> value.toDouble().takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            else -> 0.0
        }
    }

    fun setBudget(context: Context, userId: String, category: String, amount: Double) {
        require(userId.isNotBlank()) { "A signed-in user is required." }
        require(category.isNotBlank()) { "Category cannot be blank." }
        require(amount.isFinite() && amount > 0.0) { "Budget must be greater than zero." }
        prefs(context, userId).edit().putString(key(category), amount.toString()).apply()
    }

    fun clearBudget(context: Context, userId: String, category: String) {
        if (userId.isNotBlank() && category.isNotBlank()) prefs(context, userId).edit().remove(key(category)).apply()
    }
}
