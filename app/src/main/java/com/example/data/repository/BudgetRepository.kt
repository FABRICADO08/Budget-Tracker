package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.SavingsGoal
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Date
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FinanceCloudBackup(
    val profile: UserProfile?,
    val expenses: List<Expense>,
    val savingsGoals: List<SavingsGoal>
)

class BudgetRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val exists = docRef.get().await().exists()
            if (!exists) {
                val data = hashMapOf<String, Any>(
                    "userId" to uid,
                    "dailyBudget" to profile.dailyBudget,
                    "currencySymbol" to profile.currencySymbol,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data).await()
            } else {
                val updates = hashMapOf<String, Any>(
                    "dailyBudget" to profile.dailyBudget,
                    "currencySymbol" to profile.currencySymbol,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Expenses ---

    fun observeExpenses(userId: String): Flow<List<Expense>> {
        val path = "users/$userId/expenses"
        val query = db.collection("users").document(userId).collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        return query.snapshots()
            .map { snapshot -> snapshot.toObjects(Expense::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addExpense(expense: Expense): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("expenses")
        val docRef = colRef.document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to expense.title,
            "amount" to expense.amount,
            "category" to expense.category,
            "date" to expense.date,
            "type" to expense.type,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (expense.note.isNotBlank()) {
            payload["note"] = expense.note
        }
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateExpense(expense: Expense) {
        val uid = requireUserId()
        require(expense.id.isNotBlank()) { "Expense ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("expenses").document(expense.id)
        val updates = hashMapOf<String, Any>(
            "title" to expense.title,
            "amount" to expense.amount,
            "category" to expense.category,
            "date" to expense.date,
            "type" to expense.type,
            "note" to expense.note,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteExpense(expenseId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Savings Goals ---

    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> {
        val path = "users/$userId/savingsGoals"
        val query = db.collection("users").document(userId).collection("savingsGoals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        return query.snapshots()
            .map { snapshot -> snapshot.toObjects(SavingsGoal::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addSavingsGoal(goal: SavingsGoal): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("savingsGoals")
        val docRef = colRef.document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to goal.title,
            "targetAmount" to goal.targetAmount,
            "currentAmount" to goal.currentAmount,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (goal.targetDate.isNotBlank()) payload["targetDate"] = goal.targetDate
        if (goal.category.isNotBlank()) payload["category"] = goal.category
        if (goal.colorHex.isNotBlank()) payload["colorHex"] = goal.colorHex

        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal) {
        val uid = requireUserId()
        require(goal.id.isNotBlank()) { "Goal ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("savingsGoals").document(goal.id)
        val updates = hashMapOf<String, Any>(
            "title" to goal.title,
            "targetAmount" to goal.targetAmount,
            "currentAmount" to goal.currentAmount,
            "targetDate" to goal.targetDate,
            "category" to goal.category,
            "colorHex" to goal.colorHex,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun addContributionToGoal(goalId: String, currentAmount: Double, additionalAmount: Double) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("savingsGoals").document(goalId)
        val newAmount = (currentAmount + additionalAmount).coerceAtLeast(0.0)
        val updates = hashMapOf<String, Any>(
            "currentAmount" to newAmount,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSavingsGoal(goalId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("savingsGoals").document(goalId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
    /**
     * Exports only documents below the signed-in user's own Firestore document.
     * IDs are retained so restore can recreate stable document paths; credentials and tokens
     * are never part of this payload.
     */
    suspend fun exportBackupData(userId: String): FinanceCloudBackup {
        val uid = requireUserId()
        require(uid == userId) { "Backup can only access the currently signed-in account." }
        val userRef = db.collection("users").document(uid)
        val profileSnapshot = userRef.get().await()
        val hasProfileFields = profileSnapshot.contains("dailyBudget") ||
            profileSnapshot.contains("currencySymbol") ||
            profileSnapshot.contains("userId")
        val profile = if (hasProfileFields) {
            profileSnapshot.toObject(UserProfile::class.java)?.copy(id = uid, userId = uid)
        } else {
            null
        }
        val expenseDocs = userRef.collection("expenses").get().await().documents
        val goalDocs = userRef.collection("savingsGoals").get().await().documents
        val expenses = expenseDocs.mapNotNull { doc ->
            doc.toObject(Expense::class.java)?.copy(id = doc.id, userId = uid)
        }
        val goals = goalDocs.mapNotNull { doc ->
            doc.toObject(SavingsGoal::class.java)?.copy(id = doc.id, userId = uid)
        }
        return FinanceCloudBackup(profile, expenses, goals)
    }

    /**
     * Restores the profile and this user's expenses/savings goals. On any write failure, it
     * attempts to reinstate the pre-restore cloud snapshot before surfacing the error.
     */
    suspend fun restoreBackupData(
        userId: String,
        backup: FinanceCloudBackup,
        replaceExisting: Boolean
    ) {
        val uid = requireUserId()
        require(uid == userId) { "Restore can only target the currently signed-in account." }
        validateCloudBackup(backup)

        val before = exportBackupData(uid)
        try {
            applyCloudBackup(uid, backup, replaceExisting)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                runCatching { applyCloudBackup(uid, before, replaceExisting = true) }
                    .onFailure(cancelled::addSuppressed)
            }
            throw cancelled
        } catch (error: Exception) {
            try {
                applyCloudBackup(uid, before, replaceExisting = true)
            } catch (rollbackError: Exception) {
                error.addSuppressed(rollbackError)
                throw IllegalStateException(
                    "Cloud restore failed, and automatic rollback also failed. Preserve the backup file and review this account before retrying.",
                    error
                )
            }
            throw IllegalStateException("Cloud restore failed. The previous cloud data was restored.", error)
        }
    }

    private suspend fun applyCloudBackup(
        uid: String,
        backup: FinanceCloudBackup,
        replaceExisting: Boolean
    ) {
        val userRef = db.collection("users").document(uid)
        val existing = exportBackupData(uid)

        if (replaceExisting) {
            existing.expenses.forEach { expense ->
                if (expense.id.isNotBlank()) userRef.collection("expenses").document(expense.id).delete().await()
            }
            existing.savingsGoals.forEach { goal ->
                if (goal.id.isNotBlank()) userRef.collection("savingsGoals").document(goal.id).delete().await()
            }
        }

        if (backup.profile != null && (replaceExisting || existing.profile == null)) {
            val profile = backup.profile
            val values = hashMapOf<String, Any>(
                "userId" to uid,
                "dailyBudget" to profile.dailyBudget,
                "currencySymbol" to profile.currencySymbol,
                "createdAt" to (existing.profile?.createdAt ?: profile.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to (profile.updatedAt ?: FieldValue.serverTimestamp())
            )
            userRef.set(values, com.google.firebase.firestore.SetOptions.merge()).await()
        } else if (replaceExisting && backup.profile == null && existing.profile != null) {
            // Clear only known profile fields; preserve the root user document and any unrelated
            // account metadata that this backup format does not own.
            userRef.update(
                mapOf(
                    "dailyBudget" to FieldValue.delete(),
                    "currencySymbol" to FieldValue.delete(),
                    "createdAt" to FieldValue.delete(),
                    "updatedAt" to FieldValue.delete()
                )
            ).await()
        }

        val knownExpenses = if (replaceExisting) emptyList() else existing.expenses
        val knownExpenseIds = knownExpenses.map { it.id }.toHashSet()
        val knownExpenseKeys = knownExpenses.map(::expenseBackupKey).toHashSet()
        backup.expenses.forEach { expense ->
            if (!replaceExisting && (expense.id in knownExpenseIds || expenseBackupKey(expense) in knownExpenseKeys)) {
                return@forEach
            }
            val collection = userRef.collection("expenses")
            val requestedId = expense.id.takeIf { it.isNotBlank() && !it.contains('/') && it !in knownExpenseIds }
            val docRef = requestedId?.let(collection::document) ?: collection.document()
            val values = hashMapOf<String, Any>(
                "userId" to uid,
                "title" to expense.title,
                "amount" to expense.amount,
                "category" to expense.category,
                "date" to expense.date,
                "type" to expense.type,
                "createdAt" to (expense.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to (expense.updatedAt ?: FieldValue.serverTimestamp())
            )
            if (expense.note.isNotBlank()) values["note"] = expense.note
            docRef.set(values).await()
            knownExpenseIds.add(docRef.id)
            knownExpenseKeys.add(expenseBackupKey(expense))
        }

        val knownGoals = if (replaceExisting) emptyList() else existing.savingsGoals
        val knownGoalIds = knownGoals.map { it.id }.toHashSet()
        val knownGoalKeys = knownGoals.map(::goalBackupKey).toHashSet()
        backup.savingsGoals.forEach { goal ->
            if (!replaceExisting && (goal.id in knownGoalIds || goalBackupKey(goal) in knownGoalKeys)) {
                return@forEach
            }
            val collection = userRef.collection("savingsGoals")
            val requestedId = goal.id.takeIf { it.isNotBlank() && !it.contains('/') && it !in knownGoalIds }
            val docRef = requestedId?.let(collection::document) ?: collection.document()
            val values = hashMapOf<String, Any>(
                "userId" to uid,
                "title" to goal.title,
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "targetDate" to goal.targetDate,
                "category" to goal.category,
                "colorHex" to goal.colorHex,
                "createdAt" to (goal.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to (goal.updatedAt ?: FieldValue.serverTimestamp())
            )
            docRef.set(values).await()
            knownGoalIds.add(docRef.id)
            knownGoalKeys.add(goalBackupKey(goal))
        }
    }

    private fun validateCloudBackup(backup: FinanceCloudBackup) {
        backup.profile?.let {
            require(it.dailyBudget.isFinite() && it.dailyBudget >= 0.0) { "Backup contains an invalid daily budget." }
        }
        require(backup.expenses.size <= 50_000) { "Backup contains too many cloud expenses." }
        require(backup.savingsGoals.size <= 10_000) { "Backup contains too many savings goals." }
        backup.expenses.forEach {
            require(it.title.isNotBlank() && it.date.isNotBlank()) { "Backup contains an incomplete expense." }
            require(it.amount.isFinite() && it.amount >= 0.0) { "Backup contains an invalid expense amount." }
            require(it.type == "income" || it.type == "expense") { "Backup contains an unsupported expense type." }
            require(it.id.isBlank() || !it.id.contains('/')) { "Backup contains an invalid expense ID." }
        }
        backup.savingsGoals.forEach {
            require(it.title.isNotBlank()) { "Backup contains a savings goal without a name." }
            require(it.targetAmount.isFinite() && it.targetAmount >= 0.0) { "Backup contains an invalid savings target." }
            require(it.currentAmount.isFinite() && it.currentAmount >= 0.0) { "Backup contains an invalid saved amount." }
            require(it.id.isBlank() || !it.id.contains('/')) { "Backup contains an invalid savings goal ID." }
        }
    }

    private fun expenseBackupKey(expense: Expense): String =
        listOf(
            expense.date, expense.amount.toString(), expense.type, expense.category.trim().lowercase(),
            expense.title.trim().lowercase(), expense.note.trim().lowercase(),
            expense.createdAt?.toDate()?.time?.toString().orEmpty()
        ).joinToString("|")

    private fun goalBackupKey(goal: SavingsGoal): String =
        listOf(
            goal.title.trim().lowercase(), goal.targetAmount.toString(), goal.targetDate,
            goal.category.trim().lowercase(), goal.createdAt?.toDate()?.time?.toString().orEmpty()
        ).joinToString("|")

}
