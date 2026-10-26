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
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

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
}
