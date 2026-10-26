package com.example.util

import android.content.Context

/**
 * Stores the user's monthly budget cap without introducing Float rounding on new values.
 *
 * Older app versions stored this preference as a Float. Reading an old value migrates it
 * to a String-backed Double representation; this preference migration does not affect Room.
 */
object BudgetPreferences {
    private const val PREFS_NAME = "budget_user_prefs"
    private const val KEY_MONTHLY_BUDGET_CAP = "key_monthly_budget_cap"
    private const val DEFAULT_MONTHLY_BUDGET_CAP = 5000.0

    fun getMonthlyBudgetCap(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (val storedValue = prefs.all[KEY_MONTHLY_BUDGET_CAP]) {
            is String -> storedValue.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
                ?: DEFAULT_MONTHLY_BUDGET_CAP
            is Number -> storedValue.toDouble().takeIf { it.isFinite() && it >= 0.0 }
                ?.also { legacyValue ->
                    // Migrate the legacy Float/Number value to a String-backed Double.
                    prefs.edit().putString(KEY_MONTHLY_BUDGET_CAP, legacyValue.toString()).apply()
                } ?: DEFAULT_MONTHLY_BUDGET_CAP
            else -> DEFAULT_MONTHLY_BUDGET_CAP
        }
    }

    fun setMonthlyBudgetCap(context: Context, cap: Double) {
        if (!cap.isFinite() || cap < 0.0) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MONTHLY_BUDGET_CAP, cap.toString())
            .apply()
    }
}
