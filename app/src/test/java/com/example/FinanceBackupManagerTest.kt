package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.backup.FinanceBackupManager
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.Transaction
import kotlinx.coroutines.runBlocking
import com.example.data.repository.BudgetRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinanceBackupManagerTest : FirestoreEmulatorTestBase() {
    private lateinit var database: AppDatabase
    private lateinit var manager: FinanceBackupManager

    @Before
    fun setUpBackupManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = FinanceBackupManager(context, database, BudgetRepository(firestore))
    }

    @After
    fun closeBackupDatabase() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun validatesVersionedBackupAndReturnsRecordCountsWithoutWriting() {
        val preview = manager.inspectBackup(emptyBackup().toString())

        assertEquals("source-account", preview.sourceUserId)
        assertEquals(0, preview.transactionCount)
        assertEquals(0, preview.loanCount)
        assertEquals(0, preview.budgetCount)
        assertEquals(0, preview.cloudExpenseCount)
        assertEquals(0, preview.savingsGoalCount)
    }

    @Test
    fun rejectsUnsupportedBackupSchemaVersion() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            manager.inspectBackup(emptyBackup(schemaVersion = 99).toString())
        }
        assertEquals(true, error.message?.contains("unsupported schema version") == true)
    }

    @Test
    fun rejectsNegativeTransactionAmountsBeforeRestore() {
        val transaction = JSONObject()
            .put("id", 1L)
            .put("amount", -12.50)
            .put("category", "Groceries")
            .put("date", "2026-10-20")
            .put("description", "Invalid record")
            .put("type", "expense")
            .put("createdAt", System.currentTimeMillis())
        val root = emptyBackup()
        root.getJSONObject("local").put("transactions", JSONArray().put(transaction))

        assertThrows(IllegalArgumentException::class.java) {
            manager.inspectBackup(root.toString())
        }
    }

    private fun emptyBackup(schemaVersion: Int = FinanceBackupManager.SCHEMA_VERSION): JSONObject {
        val local = JSONObject()
            .put("transactions", JSONArray())
            .put("loans", JSONArray())
            .put("categoryBudgets", JSONArray())
            .put("budgetAlerts", JSONArray())
            .put("recurringSchedules", JSONArray())
            .put("recurringOccurrences", JSONArray())
            .put("loanRepayments", JSONArray())
        val cloud = JSONObject()
            .put("profile", JSONObject.NULL)
            .put("expenses", JSONArray())
            .put("savingsGoals", JSONArray())
        return JSONObject()
            .put("format", FinanceBackupManager.FORMAT)
            .put("schemaVersion", schemaVersion)
            .put("createdAtMillis", System.currentTimeMillis())
            .put("currency", "ZAR")
            .put("sourceUserId", "source-account")
            .put("settings", JSONObject().put("monthlyBudgetCap", 5000.0))
            .put("local", local)
            .put("cloud", cloud)
    }
    @Test
    fun rejectsDuplicateCloudRecordIdsBeforeRestore() {
        val expense = JSONObject()
            .put("id", "same-id")
            .put("title", "Rent")
            .put("amount", 8000.0)
            .put("category", "Housing")
            .put("date", "2026-10-01")
            .put("note", "")
            .put("type", "expense")
            .put("createdAtMillis", JSONObject.NULL)
            .put("updatedAtMillis", JSONObject.NULL)
        val root = emptyBackup()
        root.getJSONObject("cloud").put("expenses", JSONArray().put(expense).put(expense))

        val error = assertThrows(IllegalArgumentException::class.java) {
            manager.inspectBackup(root.toString())
        }
        assertEquals(true, error.message?.contains("duplicate cloud expense IDs") == true)
    }

    @Test
    fun replaceRestorePreservesOtherAccountsLocalRows() = runBlocking {
        val uid = signInTestUser("backup_restore_test@example.com")
        database.transactionDao().insert(
            Transaction(
                amount = 89.90,
                category = "Groceries",
                date = "2026-10-20",
                description = "Original owned record",
                userId = uid
            )
        )
        val otherAccountId = database.transactionDao().insert(
            Transaction(
                amount = 500.0,
                category = "Rent",
                date = "2026-10-01",
                description = "Another account record",
                userId = "other-account"
            )
        )

        val backup = manager.createBackup(uid)
        database.transactionDao().insert(
            Transaction(
                amount = 12.50,
                category = "Food & Dining",
                date = "2026-10-25",
                description = "Should be replaced",
                userId = uid
            )
        )

        val result = manager.restoreBackup(backup, uid, com.example.data.backup.BackupRestoreMode.REPLACE)
        val restored = database.transactionDao().getAllForBackup(uid)
        assertEquals(1, result.transactions)
        assertEquals(1, restored.size)
        assertTrue(restored.single().id > 0L)
        assertEquals("Original owned record", restored.single().description)
        assertNotNull(database.transactionDao().getTransactionById(otherAccountId, "other-account"))
    }

}
