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

The current Room schema is **version 4** and includes transactions, loans, category budgets, budget alerts, recurring schedules and occurrences, repayment history, and a one-time legacy ownership decision.

- `MIGRATION_2_3` adds finance tables and backfills legacy paid-in-full loan repayments.
- `MIGRATION_3_4` adds the `user_id` owner column to transactions and loans, plus owner/date indexes and the legacy ownership decision table.
- Both migrations are registered by the database builder. No destructive migration fallback is enabled.
- Transaction and loan DAOs require the Firebase UID for reads, updates, and deletes. New writes override entity-provided ownership with the authenticated UID.
- Existing transactions and loans are migrated with a blank owner; the app asks the user before assigning those records to the first account. A one-time decision prevents a second account on the device from claiming the same records.

---

### Database migration safety

The app intentionally does not use Room's destructive migration fallback. Before incrementing the database version or changing an entity, add and test an explicit `Migration`. An unknown migration path should fail visibly rather than silently erase transactions or loans. Existing records use the current `Double` amount representation; converting to integer minor units must be handled as a separate, tested data migration.

### Local account ownership

Transactions and loans are stored locally in Room and tagged with the authenticated Firebase UID. Every production query and mutation is owner-scoped. Room remains the local source of truth; this update does not imply cloud synchronization of these records.

Rows written by earlier versions do not contain an owner. They remain unassigned after migration until the user explicitly claims them. If the user declines, the data is preserved in the local database but hidden from signed-in accounts. This avoids automatically attaching one device's past financial history to whichever account signs in next.

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


## Premium UI and finance workflow upgrades

- **Dashboard overview:** a high-contrast finance summary card highlights current-month spending, transaction count, and shortcuts to budgets and recurring bills.
- **Persistent category budgets:** Room-backed monthly category limits support pause/resume, optional unused-budget carryover, and persisted 70%, 90%, and 100% threshold alerts. Budget changes do not rewrite transactions.
- **Recurring bills and income:** create weekly, biweekly, monthly, quarterly, or annual schedules, see upcoming due dates, and record each occurrence as a transaction once. The schedule and occurrence tables use unique keys to prevent duplicate payment records.
- **Bank CSV import:** choose a statement file, map date/description/amount/category/type columns, preview rows, and skip duplicate-looking entries before import. CSV parsing supports quoted fields, comma/semicolon/tab delimiters, and common South African amount formats.
- **Partial loan repayments:** record repayment amounts, dates and notes; the repository rejects repayments greater than the remaining balance and keeps repayment history in Room.
- **Recurring notifications:** a unique WorkManager worker checks active schedules every six hours, filters schedules by the signed-in Firebase UID, and records that a due-date occurrence was reminded to avoid repeated notifications. Android 13+ notification permission is respected. WorkManager delivery is best-effort rather than exact-time.
- **Build checks:** GitHub Actions runs JVM unit tests (including Firebase emulator tests) and assembles a debug APK for pull requests.

Recurring reminders are checked by WorkManager approximately every six hours for the currently signed-in account. Notifications require permission on Android 13+ and may be delayed by Android battery optimization; they are not exact-time alarms. The recurring screen includes a reminder-permission control and an immediate check action.

## Category Budget Planner

The **Budgets** tab lets a signed-in user set a monthly spending limit for common categories and categories already present in their transactions. It calculates current-month expense totals from local Room transactions, displays progress bars and remaining amounts, and flags categories that exceed their limits. Category limits are saved locally under the signed-in Firebase UID, so separate accounts on the same device have separate category-budget preferences.

Monthly and category limits use decimal-string storage to preserve cents. The feature does not alter existing transactions or the Room schema, so it requires no database migration.


## Versioned backup and restore

The **Account & Security → Backup & Restore** screen exports a versioned JSON file through Android's Storage Access Framework and restores a selected JSON file only after a preview and explicit choice.

The backup includes the current account's Room transactions, loans, loan repayments, category budgets, budget threshold alerts, recurring schedules and occurrences, the per-account monthly budget cap, plus that account's Firestore profile, expenses, and savings goals. It does not include passwords, Firebase ID tokens, App Check tokens, Gemini credentials, or biometric secrets.

Restore validates the file format, schema version, collection sizes, ownership target, and monetary fields before writing. **Merge without duplicates** preserves existing data and adds records that are not already present. **Replace this account's records** replaces only the signed-in user's local Room rows and Firestore expenses/savings goals/profile; other accounts' rows are left alone. IDs are regenerated/remapped for Room records and foreign-key relationships are reconnected.

Room restoration uses a single database transaction. Firestore data is backed up in memory before restore and the app attempts to roll it back on failure. Because Room and Firestore are separate storage systems, there is no cross-system ACID transaction; if the app reports a rollback failure, retain the JSON file and review the account before trying again.

Treat exported JSON files as sensitive financial records. Store backups somewhere private and avoid sharing them publicly.
