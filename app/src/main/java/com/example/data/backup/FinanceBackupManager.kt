package com.example.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.BudgetAlert
import com.example.data.local.entity.CategoryBudget
import com.example.data.local.entity.Loan
import com.example.data.local.entity.LoanRepayment
import com.example.data.local.entity.RecurringOccurrence
import com.example.data.local.entity.RecurringTransaction
import com.example.data.local.entity.Transaction
import com.example.data.repository.BudgetRepository
import com.example.data.repository.FinanceCloudBackup
import com.example.data.model.Expense
import com.example.data.model.SavingsGoal
import com.example.data.model.UserProfile
import com.example.util.BudgetPreferences
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.Locale

enum class BackupRestoreMode { MERGE, REPLACE }

data class FinanceBackupPreview(
    val sourceUserId: String,
    val createdAtMillis: Long,
    val transactionCount: Int,
    val loanCount: Int,
    val budgetCount: Int,
    val budgetAlertCount: Int,
    val recurringScheduleCount: Int,
    val recurringOccurrenceCount: Int,
    val repaymentCount: Int,
    val cloudExpenseCount: Int,
    val savingsGoalCount: Int
)

data class FinanceBackupResult(
    val transactions: Int,
    val loans: Int,
    val budgets: Int,
    val recurringSchedules: Int,
    val repayments: Int,
    val cloudExpenses: Int,
    val savingsGoals: Int
)

private data class LocalBackupData(
    val transactions: List<Transaction>,
    val loans: List<Loan>,
    val budgets: List<CategoryBudget>,
    val alerts: List<BudgetAlert>,
    val recurringSchedules: List<RecurringTransaction>,
    val recurringOccurrences: List<RecurringOccurrence>,
    val repayments: List<LoanRepayment>,
    val monthlyBudgetCap: Double
)

private data class ParsedBackup(
    val sourceUserId: String,
    val createdAtMillis: Long,
    val local: LocalBackupData,
    val cloud: FinanceCloudBackup
)

/**
 * Versioned JSON backup for local Room records and the signed-in user's Firestore budget data.
 *
 * Room restoration is atomic. Firestore restoration is UID-scoped and takes its own cloud
 * snapshot so failed cloud writes can be rolled back; the coordinator also restores that snapshot
 * if the Room transaction fails. The JSON file contains financial data, but never credentials,
 * ID tokens, app-check tokens, or other authentication secrets.
 */
class FinanceBackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val budgetRepository: BudgetRepository
) {
    companion object {
        const val FORMAT = "budget-tracker-finance-backup"
        const val SCHEMA_VERSION = 1
        const val MAX_BACKUP_BYTES = 25 * 1024 * 1024
        private const val MAX_RECORDS_PER_COLLECTION = 50_000
    }

    suspend fun createBackup(userId: String): String {
        requireCurrentUser(userId)
        val local = readLocalSnapshot(userId)
        val cloud = budgetRepository.exportBackupData(userId)
        val root = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAtMillis", System.currentTimeMillis())
            .put("currency", "ZAR")
            .put("sourceUserId", userId)
            .put(
                "settings",
                JSONObject().put("monthlyBudgetCap", local.monthlyBudgetCap)
            )
            .put("local", localToJson(local))
            .put("cloud", cloudToJson(cloud))
        val encoded = root.toString(2)
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "Backup is larger than the supported 25 MB limit."
        }
        return encoded
    }

    /** Parses and validates the entire file before any database or cloud data is changed. */
    fun inspectBackup(json: String): FinanceBackupPreview {
        val parsed = parseAndValidate(json)
        return FinanceBackupPreview(
            sourceUserId = parsed.sourceUserId,
            createdAtMillis = parsed.createdAtMillis,
            transactionCount = parsed.local.transactions.size,
            loanCount = parsed.local.loans.size,
            budgetCount = parsed.local.budgets.size,
            budgetAlertCount = parsed.local.alerts.size,
            recurringScheduleCount = parsed.local.recurringSchedules.size,
            recurringOccurrenceCount = parsed.local.recurringOccurrences.size,
            repaymentCount = parsed.local.repayments.size,
            cloudExpenseCount = parsed.cloud.expenses.size,
            savingsGoalCount = parsed.cloud.savingsGoals.size
        )
    }

    suspend fun restoreBackup(
        json: String,
        currentUserId: String,
        mode: BackupRestoreMode
    ): FinanceBackupResult {
        requireCurrentUser(currentUserId)
        val backup = parseAndValidate(json) // Complete validation happens before the first write.
        val priorCloudSnapshot = budgetRepository.exportBackupData(currentUserId)

        try {
            budgetRepository.restoreBackupData(
                userId = currentUserId,
                backup = backup.cloud,
                replaceExisting = mode == BackupRestoreMode.REPLACE
            )
            restoreLocalAtomically(backup.local, currentUserId, mode)
            if (mode == BackupRestoreMode.REPLACE) {
                BudgetPreferences.setMonthlyBudgetCap(context, currentUserId, backup.local.monthlyBudgetCap)
            }
        } catch (cancelled: CancellationException) {
            // The Room transaction rolls itself back. Restore cloud state even if the caller was
            // cancelled after the cloud write finished but before Room restoration completed.
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                runCatching {
                    budgetRepository.restoreBackupData(
                        userId = currentUserId,
                        backup = priorCloudSnapshot,
                        replaceExisting = true
                    )
                }.onFailure(cancelled::addSuppressed)
            }
            throw cancelled
        } catch (error: Exception) {
            try {
                budgetRepository.restoreBackupData(
                    userId = currentUserId,
                    backup = priorCloudSnapshot,
                    replaceExisting = true
                )
            } catch (rollbackError: Exception) {
                error.addSuppressed(rollbackError)
                throw IllegalStateException(
                    "Restore failed and cloud rollback could not be completed. Keep the backup file and verify this account before trying again.",
                    error
                )
            }
            throw IllegalStateException(
                "Restore failed. Local database changes were rolled back and the previous cloud data was restored.",
                error
            )
        }

        return FinanceBackupResult(
            transactions = backup.local.transactions.size,
            loans = backup.local.loans.size,
            budgets = backup.local.budgets.size,
            recurringSchedules = backup.local.recurringSchedules.size,
            repayments = backup.local.repayments.size,
            cloudExpenses = backup.cloud.expenses.size,
            savingsGoals = backup.cloud.savingsGoals.size
        )
    }

    private fun requireCurrentUser(userId: String) {
        require(userId.isNotBlank()) { "A signed-in user is required to back up or restore data." }
        require(budgetRepository.requireUserId() == userId) {
            "The signed-in account changed. Reopen backup and restore and try again."
        }
    }

    private suspend fun readLocalSnapshot(userId: String): LocalBackupData = LocalBackupData(
        transactions = database.transactionDao().getAllForBackup(userId),
        loans = database.loanDao().getAllForBackup(userId),
        budgets = database.categoryBudgetDao().getAllForUser(userId),
        alerts = database.budgetAlertDao().getAllForUser(userId),
        recurringSchedules = database.recurringTransactionDao().getAllForUser(userId),
        recurringOccurrences = database.recurringTransactionDao().getAllOccurrencesForUser(userId),
        repayments = database.loanRepaymentDao().getAllForUser(userId),
        monthlyBudgetCap = BudgetPreferences.getMonthlyBudgetCap(context, userId)
    )

    private suspend fun restoreLocalAtomically(
        backup: LocalBackupData,
        userId: String,
        mode: BackupRestoreMode
    ) = database.withTransaction {
        val txDao = database.transactionDao()
        val loanDao = database.loanDao()
        val budgetDao = database.categoryBudgetDao()
        val alertDao = database.budgetAlertDao()
        val recurringDao = database.recurringTransactionDao()
        val repaymentDao = database.loanRepaymentDao()

        if (mode == BackupRestoreMode.REPLACE) {
            txDao.deleteAll(userId)
            loanDao.getAllForBackup(userId).forEach { loanDao.deleteById(it.id, userId) }
            recurringDao.getAllForUser(userId).forEach { recurringDao.deleteForUser(it.id, userId) }
            budgetDao.deleteAllForUser(userId)
        }

        val existingTransactions = txDao.getAllForBackup(userId).toMutableList()
        val transactionIds = mutableMapOf<Long, Long>()
        val transactionKeys = existingTransactions.associateBy(::transactionKey).toMutableMap()
        backup.transactions.forEach { source ->
            val duplicate = if (mode == BackupRestoreMode.MERGE) transactionKeys[transactionKey(source)] else null
            if (duplicate != null) {
                transactionIds[source.id] = duplicate.id
            } else {
                val inserted = source.copy(id = 0L, userId = userId)
                val newId = txDao.insert(inserted)
                val saved = inserted.copy(id = newId)
                existingTransactions.add(saved)
                transactionIds[source.id] = newId
                transactionKeys[transactionKey(saved)] = saved
            }
        }

        val existingLoans = loanDao.getAllForBackup(userId).toMutableList()
        val loanIds = mutableMapOf<Long, Long>()
        val loanKeys = existingLoans.associateBy(::loanKey).toMutableMap()
        backup.loans.forEach { source ->
            val duplicate = if (mode == BackupRestoreMode.MERGE) loanKeys[loanKey(source)] else null
            if (duplicate != null) {
                loanIds[source.id] = duplicate.id
            } else {
                val inserted = source.copy(id = 0L, userId = userId)
                val newId = loanDao.insert(inserted)
                val saved = inserted.copy(id = newId)
                existingLoans.add(saved)
                loanIds[source.id] = newId
                loanKeys[loanKey(saved)] = saved
            }
        }

        val existingBudgets = budgetDao.getAllForUser(userId).toMutableList()
        val budgetIds = mutableMapOf<Long, Long>()
        val budgetKeys = existingBudgets.associateBy(::budgetKey).toMutableMap()
        backup.budgets.forEach { source ->
            val duplicate = if (mode == BackupRestoreMode.MERGE) budgetKeys[budgetKey(source)] else null
            if (duplicate != null) {
                budgetIds[source.id] = duplicate.id
            } else {
                val inserted = source.copy(id = 0L, userId = userId)
                val newId = budgetDao.insert(inserted)
                val saved = inserted.copy(id = newId)
                existingBudgets.add(saved)
                budgetIds[source.id] = newId
                budgetKeys[budgetKey(saved)] = saved
            }
        }

        val existingAlerts = alertDao.getAllForUser(userId).toMutableList()
        backup.alerts.forEach { source ->
            val newBudgetId = budgetIds[source.budgetId]
                ?: throw IllegalArgumentException("Backup contains an alert for a missing budget.")
            val alreadyExists = mode == BackupRestoreMode.MERGE &&
                existingAlerts.any {
                    it.budgetId == newBudgetId &&
                        it.month == source.month &&
                        it.thresholdPercent == source.thresholdPercent
                }
            if (!alreadyExists) {
                val saved = source.copy(id = 0L, budgetId = newBudgetId)
                alertDao.insert(saved)
                existingAlerts.add(saved)
            }
        }

        val existingSchedules = recurringDao.getAllForUser(userId).toMutableList()
        val scheduleIds = mutableMapOf<Long, Long>()
        val scheduleKeys = existingSchedules.associateBy(::scheduleKey).toMutableMap()
        backup.recurringSchedules.forEach { source ->
            val duplicate = if (mode == BackupRestoreMode.MERGE) scheduleKeys[scheduleKey(source)] else null
            if (duplicate != null) {
                scheduleIds[source.id] = duplicate.id
            } else {
                val inserted = source.copy(id = 0L, userId = userId)
                val newId = recurringDao.insert(inserted)
                val saved = inserted.copy(id = newId)
                existingSchedules.add(saved)
                scheduleIds[source.id] = newId
                scheduleKeys[scheduleKey(saved)] = saved
            }
        }

        val existingOccurrences = recurringDao.getAllOccurrencesForUser(userId).toMutableList()
        backup.recurringOccurrences.forEach { source ->
            val newScheduleId = scheduleIds[source.recurringId]
                ?: throw IllegalArgumentException("Backup contains an occurrence for a missing recurring schedule.")
            val duplicate = if (mode == BackupRestoreMode.MERGE) {
                existingOccurrences.firstOrNull { it.recurringId == newScheduleId && it.dueDate == source.dueDate }
            } else null
            if (duplicate != null) {
                return@forEach
            }
            val inserted = source.copy(
                id = 0L,
                recurringId = newScheduleId,
                transactionId = source.transactionId?.let { transactionIds[it] }
            )
            val rowId = recurringDao.insertOccurrenceIfNew(inserted)
            val actualId = if (rowId > 0L) rowId
                else recurringDao.getOccurrence(newScheduleId, source.dueDate, userId)?.id
                    ?: throw IllegalStateException("Could not restore a recurring occurrence.")
            existingOccurrences.add(inserted.copy(id = actualId))
        }

        val existingRepayments = repaymentDao.getAllForUser(userId).toMutableList()
        backup.repayments.forEach { source ->
            val newLoanId = loanIds[source.loanId]
                ?: throw IllegalArgumentException("Backup contains a repayment for a missing loan.")
            val duplicate = mode == BackupRestoreMode.MERGE && existingRepayments.any {
                it.loanId == newLoanId &&
                    it.amount == source.amount &&
                    it.paidDate == source.paidDate &&
                    it.note == source.note
            }
            if (!duplicate) {
                val saved = source.copy(id = 0L, loanId = newLoanId)
                repaymentDao.insert(saved)
                existingRepayments.add(saved)
            }
        }

        // Ensure a loan with a complete repayment history is still marked paid after a merge.
        existingLoans.forEach { loan ->
            val paid = repaymentDao.totalForLoan(loan.id, userId)
            if (paid >= loan.amount - 0.000001 && !loan.isRepaid) {
                val latestDate = repaymentDao.getAllForUser(userId)
                    .filter { it.loanId == loan.id }
                    .maxByOrNull { it.paidDate }?.paidDate ?: loan.repaidDate
                loanDao.updateRepaymentStatus(loan.id, userId, true, latestDate)
            }
        }
    }

    private fun transactionKey(value: Transaction): String =
        listOf(value.date, value.amount.toString(), value.type, value.category.trim().lowercase(Locale.ROOT), value.description.trim().lowercase(Locale.ROOT))
            .joinToString("|")

    private fun loanKey(value: Loan): String =
        listOf(value.borrowerName.trim().lowercase(Locale.ROOT), value.amount.toString(), value.lentDate, value.dueDate, value.note.trim().lowercase(Locale.ROOT))
            .joinToString("|")

    private fun budgetKey(value: CategoryBudget): String =
        listOf(value.category.trim(), value.month).joinToString("|")

    private fun scheduleKey(value: RecurringTransaction): String =
        listOf(value.description.trim().lowercase(Locale.ROOT), value.amount.toString(), value.category.trim().lowercase(Locale.ROOT), value.type, value.frequency, value.startDate)
            .joinToString("|")

    private fun localToJson(value: LocalBackupData): JSONObject = JSONObject()
        .put("transactions", JSONArray().apply { value.transactions.forEach { put(transactionToJson(it)) } })
        .put("loans", JSONArray().apply { value.loans.forEach { put(loanToJson(it)) } })
        .put("categoryBudgets", JSONArray().apply { value.budgets.forEach { put(budgetToJson(it)) } })
        .put("budgetAlerts", JSONArray().apply { value.alerts.forEach { put(alertToJson(it)) } })
        .put("recurringSchedules", JSONArray().apply { value.recurringSchedules.forEach { put(scheduleToJson(it)) } })
        .put("recurringOccurrences", JSONArray().apply { value.recurringOccurrences.forEach { put(occurrenceToJson(it)) } })
        .put("loanRepayments", JSONArray().apply { value.repayments.forEach { put(repaymentToJson(it)) } })

    private fun transactionToJson(value: Transaction) = JSONObject()
        .put("id", value.id).put("amount", value.amount).put("category", value.category)
        .put("date", value.date).put("description", value.description).put("type", value.type)
        .put("createdAt", value.createdAt)

    private fun loanToJson(value: Loan) = JSONObject()
        .put("id", value.id).put("borrowerName", value.borrowerName).put("amount", value.amount)
        .put("lentDate", value.lentDate).put("dueDate", value.dueDate).put("note", value.note)
        .put("isRepaid", value.isRepaid).put("repaidDate", value.repaidDate).put("createdAt", value.createdAt)

    private fun budgetToJson(value: CategoryBudget) = JSONObject()
        .put("id", value.id).put("category", value.category).put("month", value.month)
        .put("limitAmount", value.limitAmount).put("carryUnused", value.carryUnused)
        .put("isPaused", value.isPaused).put("createdAt", value.createdAt)

    private fun alertToJson(value: BudgetAlert) = JSONObject()
        .put("id", value.id).put("budgetId", value.budgetId).put("month", value.month)
        .put("thresholdPercent", value.thresholdPercent).put("actualAmount", value.actualAmount)
        .put("createdAt", value.createdAt)

    private fun scheduleToJson(value: RecurringTransaction) = JSONObject()
        .put("id", value.id).put("description", value.description).put("amount", value.amount)
        .put("category", value.category).put("type", value.type).put("startDate", value.startDate)
        .put("nextDueDate", value.nextDueDate).put("frequency", value.frequency)
        .put("endDate", value.endDate ?: JSONObject.NULL).put("reminderDaysBefore", value.reminderDaysBefore)
        .put("isActive", value.isActive).put("createdAt", value.createdAt)

    private fun occurrenceToJson(value: RecurringOccurrence) = JSONObject()
        .put("id", value.id).put("recurringId", value.recurringId).put("dueDate", value.dueDate)
        .put("amount", value.amount).put("status", value.status)
        .put("transactionId", value.transactionId ?: JSONObject.NULL)
        .put("reminderSentAt", value.reminderSentAt ?: JSONObject.NULL).put("createdAt", value.createdAt)

    private fun repaymentToJson(value: LoanRepayment) = JSONObject()
        .put("id", value.id).put("loanId", value.loanId).put("amount", value.amount)
        .put("paidDate", value.paidDate).put("note", value.note).put("createdAt", value.createdAt)

    private fun cloudToJson(value: FinanceCloudBackup) = JSONObject()
        .put("profile", value.profile?.let(::profileToJson) ?: JSONObject.NULL)
        .put("expenses", JSONArray().apply { value.expenses.forEach { put(expenseToJson(it)) } })
        .put("savingsGoals", JSONArray().apply { value.savingsGoals.forEach { put(goalToJson(it)) } })

    private fun profileToJson(value: UserProfile) = JSONObject()
        .put("id", value.id).put("dailyBudget", value.dailyBudget).put("currencySymbol", value.currencySymbol)
        .put("createdAtMillis", value.createdAt?.toDate()?.time ?: JSONObject.NULL)
        .put("updatedAtMillis", value.updatedAt?.toDate()?.time ?: JSONObject.NULL)

    private fun expenseToJson(value: Expense) = JSONObject()
        .put("id", value.id).put("title", value.title).put("amount", value.amount)
        .put("category", value.category).put("date", value.date).put("note", value.note).put("type", value.type)
        .put("createdAtMillis", value.createdAt?.toDate()?.time ?: JSONObject.NULL)
        .put("updatedAtMillis", value.updatedAt?.toDate()?.time ?: JSONObject.NULL)

    private fun goalToJson(value: SavingsGoal) = JSONObject()
        .put("id", value.id).put("title", value.title).put("targetAmount", value.targetAmount)
        .put("currentAmount", value.currentAmount).put("targetDate", value.targetDate)
        .put("category", value.category).put("colorHex", value.colorHex)
        .put("createdAtMillis", value.createdAt?.toDate()?.time ?: JSONObject.NULL)
        .put("updatedAtMillis", value.updatedAt?.toDate()?.time ?: JSONObject.NULL)

    private fun parseAndValidate(json: String): ParsedBackup {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "Backup is larger than the supported 25 MB limit."
        }
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT) { "This is not a Budget Tracker backup file." }
        require(root.getInt("schemaVersion") == SCHEMA_VERSION) {
            "This backup uses an unsupported schema version."
        }
        require(root.getString("currency") == "ZAR") { "This backup is not in South African Rand." }
        val sourceUid = root.getString("sourceUserId")
        require(sourceUid.isNotBlank()) { "Backup source account is missing." }
        val createdAt = root.getLong("createdAtMillis").also { require(it > 0L) { "Backup timestamp is invalid." } }
        val settings = root.getJSONObject("settings")
        val monthlyCap = settings.getDouble("monthlyBudgetCap")
        require(monthlyCap.isFinite() && monthlyCap >= 0.0) { "Backup monthly budget setting is invalid." }
        val localJson = root.getJSONObject("local")
        val local = LocalBackupData(
            transactions = localJson.array("transactions").map { it.toTransaction() },
            loans = localJson.array("loans").map { it.toLoan() },
            budgets = localJson.array("categoryBudgets").map { it.toBudget() },
            alerts = localJson.array("budgetAlerts").map { it.toAlert() },
            recurringSchedules = localJson.array("recurringSchedules").map { it.toSchedule() },
            recurringOccurrences = localJson.array("recurringOccurrences").map { it.toOccurrence() },
            repayments = localJson.array("loanRepayments").map { it.toRepayment() },
            monthlyBudgetCap = monthlyCap
        )
        val counts = listOf(
            local.transactions.size, local.loans.size, local.budgets.size, local.alerts.size,
            local.recurringSchedules.size, local.recurringOccurrences.size, local.repayments.size
        )
        require(counts.all { it <= MAX_RECORDS_PER_COLLECTION }) { "Backup has too many records." }
        val cloudJson = root.getJSONObject("cloud")
        val profile = cloudJson.optJSONObject("profile")?.toProfile()
        val cloud = FinanceCloudBackup(
            profile = profile,
            expenses = cloudJson.array("expenses").map { it.toExpense() },
            savingsGoals = cloudJson.array("savingsGoals").map { it.toSavingsGoal() }
        )
        // Validate cloud-domain records and ID paths before any write begins.
        cloud.profile?.let { require(it.dailyBudget.isFinite() && it.dailyBudget >= 0.0) { "Invalid profile budget." } }
        cloud.expenses.forEach {
            require(it.amount.isFinite() && it.amount >= 0.0) { "Invalid cloud expense amount." }
            require(it.type == "income" || it.type == "expense") { "Invalid cloud transaction type." }
            require(it.id.isBlank() || !it.id.contains('/')) { "Invalid expense ID." }
        }
        cloud.savingsGoals.forEach {
            require(it.targetAmount.isFinite() && it.targetAmount >= 0.0 && it.currentAmount.isFinite() && it.currentAmount >= 0.0) {
                "Invalid savings goal amount."
            }
            require(it.id.isBlank() || !it.id.contains('/')) { "Invalid savings goal ID." }
        }
        return ParsedBackup(sourceUid, createdAt, local, cloud)
    }

    private fun <T> JSONArray.map(parser: (JSONObject) -> T): List<T> =
        (0 until length()).map { parser(getJSONObject(it)) }

    private fun JSONObject.array(name: String): JSONArray =
        optJSONArray(name) ?: throw IllegalArgumentException("Backup is missing the '$name' array.")

    private fun JSONObject.requiredString(name: String): String =
        getString(name).also { require(it.length <= 10_000) { "Backup string is too long." } }

    private fun JSONObject.toTransaction(): Transaction = Transaction(
        id = getLong("id"),
        amount = getDouble("amount").finiteAmount(),
        category = requiredString("category"),
        date = requiredString("date"),
        description = requiredString("description"),
        type = requiredString("type").also { require(it == "income" || it == "expense") },
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toLoan(): Loan = Loan(
        id = getLong("id"),
        borrowerName = requiredString("borrowerName"),
        amount = getDouble("amount").finiteAmount(allowZero = false),
        lentDate = requiredString("lentDate"),
        dueDate = requiredString("dueDate"),
        note = requiredString("note"),
        isRepaid = getBoolean("isRepaid"),
        repaidDate = requiredString("repaidDate"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toBudget(): CategoryBudget = CategoryBudget(
        id = getLong("id"),
        userId = "",
        category = requiredString("category"),
        month = requiredString("month").also { require(it.matches(Regex("\\d{4}-\\d{2}"))) },
        limitAmount = getDouble("limitAmount").finiteAmount(allowZero = false),
        carryUnused = getBoolean("carryUnused"),
        isPaused = getBoolean("isPaused"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toAlert(): BudgetAlert = BudgetAlert(
        id = getLong("id"),
        budgetId = getLong("budgetId"),
        month = requiredString("month"),
        thresholdPercent = getInt("thresholdPercent").also { require(it in listOf(70, 90, 100)) },
        actualAmount = getDouble("actualAmount").finiteAmount(),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toSchedule(): RecurringTransaction = RecurringTransaction(
        id = getLong("id"),
        userId = "",
        description = requiredString("description"),
        amount = getDouble("amount").finiteAmount(allowZero = false),
        category = requiredString("category"),
        type = requiredString("type").also { require(it == "income" || it == "expense") },
        startDate = requiredString("startDate"),
        nextDueDate = requiredString("nextDueDate"),
        frequency = requiredString("frequency").also { require(it in setOf("WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "ANNUALLY")) },
        endDate = nullableString("endDate"),
        reminderDaysBefore = getInt("reminderDaysBefore").also { require(it in 0..30) },
        isActive = getBoolean("isActive"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toOccurrence(): RecurringOccurrence = RecurringOccurrence(
        id = getLong("id"),
        recurringId = getLong("recurringId"),
        dueDate = requiredString("dueDate"),
        amount = getDouble("amount").finiteAmount(allowZero = false),
        status = requiredString("status").also { require(it in setOf("unpaid", "paid", "skipped")) },
        transactionId = nullableLong("transactionId"),
        reminderSentAt = nullableLong("reminderSentAt"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toRepayment(): LoanRepayment = LoanRepayment(
        id = getLong("id"),
        loanId = getLong("loanId"),
        amount = getDouble("amount").finiteAmount(allowZero = false),
        paidDate = requiredString("paidDate"),
        note = requiredString("note"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toProfile(): UserProfile = UserProfile(
        id = requiredString("id"),
        userId = "",
        dailyBudget = getDouble("dailyBudget").finiteAmount(),
        currencySymbol = requiredString("currencySymbol"),
        createdAt = timestamp("createdAtMillis"),
        updatedAt = timestamp("updatedAtMillis")
    )

    private fun JSONObject.toExpense(): Expense = Expense(
        id = requiredString("id"),
        userId = "",
        title = requiredString("title"),
        amount = getDouble("amount").finiteAmount(),
        category = requiredString("category"),
        date = requiredString("date"),
        note = requiredString("note"),
        type = requiredString("type").also { require(it == "income" || it == "expense") },
        createdAt = timestamp("createdAtMillis"),
        updatedAt = timestamp("updatedAtMillis")
    )

    private fun JSONObject.toSavingsGoal(): SavingsGoal = SavingsGoal(
        id = requiredString("id"),
        userId = "",
        title = requiredString("title"),
        targetAmount = getDouble("targetAmount").finiteAmount(),
        currentAmount = getDouble("currentAmount").finiteAmount(),
        targetDate = requiredString("targetDate"),
        category = requiredString("category"),
        colorHex = requiredString("colorHex"),
        createdAt = timestamp("createdAtMillis"),
        updatedAt = timestamp("updatedAtMillis")
    )

    private fun JSONObject.timestamp(name: String): Timestamp? =
        nullableLong(name)?.let { Timestamp(Date(it)) }

    private fun JSONObject.nullableString(name: String): String? =
        if (isNull(name)) null else getString(name)

    private fun JSONObject.nullableLong(name: String): Long? =
        if (isNull(name)) null else getLong(name)

    private fun Double.finiteAmount(allowZero: Boolean = true): Double = also {
        require(isFinite() && if (allowZero) this >= 0.0 else this > 0.0) {
            "Backup contains a negative or invalid amount."
        }
    }
}
