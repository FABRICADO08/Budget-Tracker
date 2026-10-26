package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BudgetAlertDao
import com.example.data.local.dao.CategoryBudgetDao
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.LoanRepaymentDao
import com.example.data.local.dao.LegacyDataOwnershipDao
import com.example.data.local.dao.RecurringTransactionDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.BudgetAlert
import com.example.data.local.entity.CategoryBudget
import com.example.data.local.entity.Loan
import com.example.data.local.entity.LoanRepayment
import com.example.data.local.entity.LegacyDataOwnership
import com.example.data.local.entity.RecurringOccurrence
import com.example.data.local.entity.RecurringTransaction
import com.example.data.local.entity.Transaction

/**
 * Main Room database for locally persisted financial records.
 * Migrations are explicit so schema upgrades never silently erase financial history.
 */
@Database(
    entities = [
        Transaction::class,
        Loan::class,
        CategoryBudget::class,
        BudgetAlert::class,
        RecurringTransaction::class,
        RecurringOccurrence::class,
        LoanRepayment::class,
        LegacyDataOwnership::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
    abstract fun budgetAlertDao(): BudgetAlertDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun loanRepaymentDao(): LoanRepaymentDao
    abstract fun legacyDataOwnershipDao(): LegacyDataOwnershipDao

    companion object {
        private const val DATABASE_NAME = "budget_tracker.db"

        /**
         * Adds feature tables without rewriting or deleting existing transactions and loans.
         * Keep every migration registered when introducing a new database version.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS category_budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        user_id TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL,
                        month TEXT NOT NULL,
                        limit_amount REAL NOT NULL,
                        carry_unused INTEGER NOT NULL,
                        is_paused INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_category_budgets_user_id_category_month " +
                        "ON category_budgets(user_id, category, month)"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS budget_alerts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        budget_id INTEGER NOT NULL,
                        month TEXT NOT NULL,
                        threshold_percent INTEGER NOT NULL,
                        actual_amount REAL NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(budget_id) REFERENCES category_budgets(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_budget_alerts_budget_id_month_threshold_percent " +
                        "ON budget_alerts(budget_id, month, threshold_percent)"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS recurring_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        user_id TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        type TEXT NOT NULL,
                        start_date TEXT NOT NULL,
                        next_due_date TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        end_date TEXT,
                        reminder_days_before INTEGER NOT NULL,
                        is_active INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recurring_transactions_is_active_next_due_date " +
                        "ON recurring_transactions(is_active, next_due_date)"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS recurring_occurrences (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recurring_id INTEGER NOT NULL,
                        due_date TEXT NOT NULL,
                        amount REAL NOT NULL,
                        status TEXT NOT NULL,
                        transaction_id INTEGER,
                        reminder_sent_at INTEGER,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(recurring_id) REFERENCES recurring_transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_recurring_occurrences_recurring_id_due_date " +
                        "ON recurring_occurrences(recurring_id, due_date)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recurring_occurrences_status_due_date " +
                        "ON recurring_occurrences(status, due_date)"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS loan_repayments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        loan_id INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        paid_date TEXT NOT NULL,
                        note TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(loan_id) REFERENCES loans(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_loan_repayments_loan_id_paid_date " +
                        "ON loan_repayments(loan_id, paid_date)"
                )
                // Preserve legacy paid-in-full status as an explicit repayment history entry.
                db.execSQL(
                    """INSERT INTO loan_repayments (loan_id, amount, paid_date, note, created_at)
                       SELECT id, amount,
                              CASE WHEN repaid_date <> '' THEN repaid_date ELSE lent_date END,
                              'Imported from legacy paid-in-full record', created_at
                       FROM loans WHERE is_repaid = 1"""
                )
            }
        }

        /**
         * Adds owner UIDs without assigning old device-local records to the first account
         * silently. Existing rows remain unowned until the user explicitly accepts the claim
         * dialog; a singleton decision row records either accept or decline.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loans ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_user_id_date ON transactions(user_id, date)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_loans_user_id_created_at ON loans(user_id, created_at)"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS legacy_data_ownership (
                        id INTEGER NOT NULL PRIMARY KEY,
                        owner_uid TEXT NOT NULL,
                        decision_made_at INTEGER NOT NULL
                    )"""
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /** Backwards-compatible alias used by existing screens. */
        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
