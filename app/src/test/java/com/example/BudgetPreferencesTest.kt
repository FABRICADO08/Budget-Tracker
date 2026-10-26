package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BudgetPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BudgetPreferencesTest {
    private lateinit var context: Context
    private val preferencesName = "budget_user_prefs"
    private val budgetKey = "key_monthly_budget_cap"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun preservesPrecisionWhenSavingBudgetCap() {
        BudgetPreferences.setMonthlyBudgetCap(context, 1234.56)

        assertEquals(1234.56, BudgetPreferences.getMonthlyBudgetCap(context), 0.000001)
        assertTrue(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                .all[budgetKey] is String
        )
    }

    @Test
    fun migratesLegacyFloatPreferenceToString() {
        val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        prefs.edit().putFloat(budgetKey, 1234.56f).commit()
        val legacyValue = prefs.getFloat(budgetKey, 0f).toDouble()

        assertEquals(legacyValue, BudgetPreferences.getMonthlyBudgetCap(context), 0.0)
        assertTrue(prefs.all[budgetKey] is String)
    }

    @Test
    fun ignoresInvalidNegativeBudgetCap() {
        BudgetPreferences.setMonthlyBudgetCap(context, 100.25)
        BudgetPreferences.setMonthlyBudgetCap(context, -10.0)

        assertEquals(100.25, BudgetPreferences.getMonthlyBudgetCap(context), 0.000001)
    }
}
