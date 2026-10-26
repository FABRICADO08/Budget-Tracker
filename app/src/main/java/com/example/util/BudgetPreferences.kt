package com.example.util

import android.content.Context

object BudgetPreferences {
    private const val PREFS_NAME = "budget_user_prefs"
    private const val KEY_MONTHLY_BUDGET_CAP = "key_monthly_budget_cap"

    fun getMonthlyBudgetCap(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_MONTHLY_BUDGET_CAP, 5000.0f).toDouble()
    }

    fun setMonthlyBudgetCap(context: Context, cap: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_MONTHLY_BUDGET_CAP, cap.toFloat()).apply()
    }
}
