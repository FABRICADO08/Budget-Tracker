# Budget Tracker (South African Rand - ZAR)

A modern, offline-first personal finance and expense tracking Android application built with **Kotlin**, **Jetpack Compose**, and **Room Database**, enhanced with **Gemini AI** capabilities.

---

## 📱 Features

### 1. Expense Tracking & Transactions
- **South African Rand (`R`) Currency**: Formatted for ZAR across all screens, transactions, and analytics.
- **Customizable Monthly Budget Target**: Set spending goals in Rands with live progress bars and warning alerts if spending exceeds the target limit.
- **Financial Statement & Report Export**: Share or export comprehensive financial statements (totals, itemized expenses, and debt balances) directly via Android's native share sheet.
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
- **1-Tap Friendly Reminders**: Pre-composed polite reminder message shareable via WhatsApp, SMS, or email in 1 tap.
- **Mark as Repaid Flow**: Records the exact date when a borrower returns the money and marks the record with a green "Paid Back" badge.
- **Unmark & Delete**: Easily unmark loans or remove completed debt records.

### 4. Gemini AI Financial Chatbot
- **Conversational Financial Advisor**: Multi-turn financial guidance powered by `gemini-3.8-flash`.
- **Search Grounding**: Grounded with Google Search for up-to-date financial rates, savings strategies, and smart purchasing decisions.
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
- **Delete (`@Delete` & `@Query`)**:
  - `delete(transaction: Transaction)`
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
                    // Register explicit Room migrations before changing the schema version.
                    // Destructive fallback is intentionally disabled to protect financial data.
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

### Database migration safety

The app intentionally does not use Room's destructive migration fallback. Before incrementing the database version or changing an entity, add and test an explicit `Migration`. An unknown migration path should fail visibly rather than silently erase transactions or loans. Existing records use the current `Double` amount representation; converting to integer minor units must be handled as a separate, tested data migration.

### South African currency formatting

The shared `ZarMoneyFormatter` uses the `en-ZA` locale and ZAR currency rules for statement exports and chartboard labels. Existing persisted amounts remain unchanged by this formatter.

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3)
- **Local Storage**: Room Persistence Library (KSP)
- **AI & Cloud**:
  - Firebase Authentication (Google Sign-In)
  - Firebase AI Logic (`gemini-3.8-flash` with Google Search Tool, with `gemini-3.1-flash-lite` fallback)
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


## Category Budget Planner

The **Budgets** tab lets a signed-in user set monthly spending limits per category, view actual expenses and remaining amounts, pause limits, and carry forward unused budget amounts. Category budgets are stored in Room and scoped to the signed-in Firebase UID. On first opening the upgraded screen, prior account-scoped SharedPreferences limits are copied into the current month's Room records without deleting the old preferences.


## Finance upgrade branch

The `upgrade/phase2-financial-core-latest` branch adds an explicit Room v2→v3 migration and persisted models for category budgets, recurring schedules/occurrences, budget threshold alerts, and loan repayments. Legacy paid-in-full loans are represented in the repayment history during migration. It also adds reusable budget and recurrence calculations, a CSV parsing foundation, and tests for the migration and core rules.

The recurring-schedule UI and WorkManager reminders, full CSV file-picker/column-mapping/review workflow, backup/restore, integer-minor-unit migration for existing transaction/loan amounts, and complete user-data isolation for legacy transaction/loan records remain outstanding. Android tests/build have not been executed in this environment. Before merging, run:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
