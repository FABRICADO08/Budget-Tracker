# Budget Tracker (South African Rand - ZAR)

A modern, offline-first personal finance and expense tracking Android application built with **Kotlin**, **Jetpack Compose**, and **Room Database**, enhanced with **Gemini AI** capabilities.

---

## 📱 Features

### 1. Expense Tracking & Transactions
- **South African Rand (`R`) Currency**: Formatted for ZAR across all screens, transactions, and analytics.
- **Search & Filtering**: Search bar to instantly filter transactions by category or description.
- **Receipt & Till Slip Scanner**: Capture or pick till slips and receipts using Android's Photo Picker; **Gemini AI** extracts the store name, date, and total amount directly into the expense form.
- **Add & Manage Transactions**: Categorize spending into Groceries, Food & Dining, Transportation, Bills & Utilities, Entertainment, and more.

### 2. Financial Chartboard (Analytics Board)
- **Dynamic Monthly Spending Bar Chart**: Visual monthly bars reflecting spending patterns.
- **Tap-to-Inspect**: Tap any month's bar to focus on that specific month's total expenditure and transaction count.
- **Flexible Timeframes**: Toggle views between **Last 3 Months**, **Last 6 Months**, and **Last 12 Months**.
- **Month-over-Month Comparison**: Displays percentage increase or decrease compared to the previous month.
- **Category Breakdown Spectrum**: Proportional color-coded progress bars showing expenditure share and transaction counts.
- **Cash Flow Summary**: Tracks total inflow, outflow, and net balance.
- **Loan Recovery Rate**: Visual progress of loaned funds recovered vs. still outstanding.
- **One-Tap AI Insights**: Button to feed chartboard metrics directly into the Gemini Chatbot for custom budgeting advice.

### 3. Loan & Debt Tracker
- **Lend Tracking**: Log loans given to individuals with borrower name, amount, lent date, due date, and notes.
- **Material 3 Date Pickers**: Selectable dates for loan issue dates and return dates.
- **Mark as Repaid Flow**: Records the exact date when a borrower returns the money and marks the record with a green "Paid Back" badge.
- **Unmark & Delete**: Easily unmark loans or remove completed debt records.

### 4. Gemini AI Financial Chatbot
- **Conversational Financial Advisor**: Multi-turn financial guidance powered by `gemini-3.8-flash`.
- **Search & Maps Grounding**: Grounded with Google Search for financial rates and Google Maps to find nearby stores and banking options.
- **Voice Conversations**: Speech-to-text microphone button for asking financial questions hands-free.
- **Quick Shortcuts**: Top-bar access button, bottom navigation tab, and Chartboard analysis launcher.

### 5. Authentication & Fingerprint Security
- **Google Sign-In Gate**: Authenticates user identity via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
- **Biometric App Lock**: Secure the app using device fingerprint or screen lock powered by `androidx.biometric:biometric` and `BiometricPrompt`.
- **Profile & Security Settings**: Toggle fingerprint protection on/off and sign out from the top-right profile avatar.

---

## 🗄️ Database Architecture (Room SQLite)

All user transactions, loans, and categories persist on the device using Android's **Room Persistence Library**.

### 1. Entity: `Transaction` (`com.example.data.local.entity.Transaction`)
```kotlin
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Double,
    val category: String,
    val date: String,
    val description: String,
    val type: String = "expense",
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2. DAO: `TransactionDao` (`com.example.data.local.dao.TransactionDao`)
Defines CRUD operations for the `Transaction` entity:
- **Insert (`@Insert`)**:
  - `insert(transaction: Transaction): Long`
  - `insertTransaction(transaction: Transaction): Long`
  - `insertAll(transactions: List<Transaction>)`
- **Query (`@Query`)**:
  - `getAllTransactions(): Flow<List<Transaction>>`
  - `getTransactionById(id: Long): Transaction?`
  - `getTransactionsByDate(date: String): Flow<List<Transaction>>`
  - `getTransactionsByCategory(category: String): Flow<List<Transaction>>`
  - `getTransactionsByType(type: String): Flow<List<Transaction>>`
  - `getTotalExpenseForDate(date: String): Flow<Double?>`
  - `getTotalExpenseForMonth(monthPrefix: String): Flow<Double?>`
- **Update (`@Update`)**:
  - `update(transaction: Transaction)`
  - `updateTransaction(transaction: Transaction)`
- **Delete (`@Delete` & `@Query`)**:
  - `delete(transaction: Transaction)`
  - `deleteTransaction(transaction: Transaction)`
  - `deleteById(id: Long)`
  - `deleteAll()`

### 3. Room Database: `AppDatabase` (`com.example.data.local.database.AppDatabase`)
Thread-safe singleton implementation using double-checked locking:
```kotlin
@Database(
    entities = [Transaction::class, Loan::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
```

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3)
- **Local Storage**: Room Persistence Library (KSP)
- **AI & Cloud**:
  - Firebase Authentication (Google Sign-In)
  - Firebase AI Logic (`gemini-3.8-flash` with Google Search & Google Maps Tools)
- **Security**: AndroidX Biometric (`BiometricPrompt`)
- **Architecture**: MVVM with `ViewModel`, Kotlin Coroutines, and `StateFlow`
- **Testing**: Robolectric JVM unit tests for Room and ViewModels

---

## 🧪 Testing

Run automated tests via Gradle:
```bash
gradle :app:testDebugUnitTest --tests "com.example.Transaction*"
```

Tests verify:
- Room Database CRUD operations (`insert`, `update`, `delete`, `query`, `deleteById`)
- Loan creation, repayment tracking, and date logging
- ViewModel reactive flow emissions
