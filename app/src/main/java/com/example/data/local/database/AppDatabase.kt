package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.Loan
import com.example.data.local.entity.Transaction

/**
 * Main Room database for locally persisted financial records.
 *
 * Important: destructive migration fallback is deliberately disabled. A schema upgrade
 * without a registered migration must fail visibly rather than silently deleting a user's
 * transactions and loans. Register and test every migration before incrementing [version].
 */
@Database(
    entities = [Transaction::class, Loan::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun loanDao(): LoanDao

    companion object {
        private const val DATABASE_NAME = "budget_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the thread-safe singleton database instance.
         *
         * Do not add fallbackToDestructiveMigration(): the database contains user-owned
         * financial data. When changing the schema, add an explicit Room Migration and
         * cover it with a migration test before increasing the database version.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }

        /** Backwards-compatible alias used by existing screens. */
        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
