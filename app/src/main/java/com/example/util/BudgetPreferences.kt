package com.example.util

import android.content.Context

/** Budget caps use decimal strings; legacy numeric preferences are migrated on first read. */
object BudgetPreferences {
    private const val PREFS_NAME = "budget_user_prefs"
    private const val KEY_MONTHLY_BUDGET_CAP = "key_monthly_budget_cap"
    private const val DEFAULT_MONTHLY_BUDGET_CAP = 5000.0
    private const val USER_KEY_PREFIX = "monthly_budget_user:"

    fun getMonthlyBudgetCap(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readBudget(prefs, KEY_MONTHLY_BUDGET_CAP, DEFAULT_MONTHLY_BUDGET_CAP)
    }

    fun setMonthlyBudgetCap(context: Context, cap: Double) {
        if (!cap.isFinite() || cap < 0.0) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_MONTHLY_BUDGET_CAP, cap.toString()).apply()
    }

    /** Account-specific monthly budget, falling back to the legacy cap until customized. */
    fun getMonthlyBudgetCap(context: Context, userId: String): Double {
        if (userId.isBlank()) return getMonthlyBudgetCap(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readBudget(prefs, USER_KEY_PREFIX + userId, getMonthlyBudgetCap(context))
    }

    fun setMonthlyBudgetCap(context: Context, userId: String, cap: Double) {
        if (userId.isBlank() || !cap.isFinite() || cap < 0.0) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(USER_KEY_PREFIX + userId, cap.toString()).apply()
    }

    private fun readBudget(prefs: android.content.SharedPreferences, key: String, fallback: Double): Double {
        return when (val stored = prefs.all[key]) {
            is String -> stored.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: fallback
            is Number -> stored.toDouble().takeIf { it.isFinite() && it >= 0.0 }?.also { value ->
                prefs.edit().putString(key, value.toString()).apply()
            } ?: fallback
            else -> fallback
        }
    }
}
