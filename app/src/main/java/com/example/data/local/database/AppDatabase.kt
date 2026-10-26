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
 * Main Room Database configuration class for the application.
 *
 * Defines the database configuration, includes the [Transaction] and [Loan] entities,
 * and acts as the app's main access point to persisted local data.
 *
 * Access is provided through a thread-safe Singleton pattern using double-checked locking.
 */
@Database(
    entities = [Transaction::class, Loan::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to [TransactionDao] for performing CRUD operations on transactions.
     */
    abstract fun transactionDao(): TransactionDao

    /**
     * Provides access to [LoanDao] for tracking loans given and repayments.
     */
    abstract fun loanDao(): LoanDao

    companion object {
        private const val DATABASE_NAME = "budget_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the thread-safe singleton instance of [AppDatabase].
         *
         * Uses double-checked locking with a [@Volatile] instance to prevent race conditions
         * when multiple threads attempt to access the database concurrently.
         *
         * @param context Application context used to build the database.
         * @return The active [AppDatabase] singleton instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Alias for [getDatabase] to provide convenient singleton access.
         */
        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
