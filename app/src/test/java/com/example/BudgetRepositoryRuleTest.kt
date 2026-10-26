package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Expense
import com.example.data.model.SavingsGoal
import com.example.data.model.UserProfile
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BudgetRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticatedUserCannotAccessRepository() = runBlocking {
        auth.signOut()
        val repo = BudgetRepository(firestore)
        try {
            repo.requireUserId()
            fail("Expected IllegalStateException for unauthenticated user")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("User must be signed in with Google") == true)
        }
    }

    @Test
    fun authenticatedUserCanSaveAndObserveUserProfile() = runBlocking {
        val uid = signInTestUser("budget_user1@example.com")
        val repo = BudgetRepository(firestore)

        val profile = UserProfile(
            userId = uid,
            dailyBudget = 750.0,
            currencySymbol = "R"
        )
        repo.saveUserProfile(profile)

        val observed = repo.observeUserProfile(uid).first()
        assertNotNull(observed)
        assertEquals(750.0, observed?.dailyBudget ?: 0.0, 0.01)
        assertEquals("R", observed?.currencySymbol)
    }

    @Test
    fun authenticatedUserCanManageExpenses() = runBlocking {
        val uid = signInTestUser("budget_user2@example.com")
        val repo = BudgetRepository(firestore)

        val expense = Expense(
            title = "Morning Coffee",
            amount = 5.25,
            category = "Food & Dining",
            date = "2026-10-24",
            type = "expense",
            note = "Latte"
        )
        val expenseId = repo.addExpense(expense)
        assertTrue(expenseId.isNotBlank())

        val expenses = repo.observeExpenses(uid).first()
        assertTrue(expenses.isNotEmpty())
        val saved = expenses.first { it.id == expenseId }
        assertEquals("Morning Coffee", saved.title)
        assertEquals(5.25, saved.amount, 0.01)

        // Update
        repo.updateExpense(saved.copy(amount = 6.0, note = "Large latte"))
        val updatedList = repo.observeExpenses(uid).first()
        val updated = updatedList.first { it.id == expenseId }
        assertEquals(6.0, updated.amount, 0.01)

        // Delete
        repo.deleteExpense(expenseId)
        val afterDelete = repo.observeExpenses(uid).first()
        assertTrue(afterDelete.none { it.id == expenseId })
    }

    @Test
    fun authenticatedUserCanManageSavingsGoals() = runBlocking {
        val uid = signInTestUser("budget_user3@example.com")
        val repo = BudgetRepository(firestore)

        val goal = SavingsGoal(
            title = "Vacation Fund",
            targetAmount = 1500.0,
            currentAmount = 300.0,
            targetDate = "2026-12-25",
            category = "Travel",
            colorHex = "#2196F3"
        )
        val goalId = repo.addSavingsGoal(goal)
        assertTrue(goalId.isNotBlank())

        val goals = repo.observeSavingsGoals(uid).first()
        assertTrue(goals.isNotEmpty())
        val saved = goals.first { it.id == goalId }
        assertEquals("Vacation Fund", saved.title)
        assertEquals(300.0, saved.currentAmount, 0.01)

        // Add contribution
        repo.addContributionToGoal(goalId, saved.currentAmount, 200.0)
        val updatedGoals = repo.observeSavingsGoals(uid).first()
        val updated = updatedGoals.first { it.id == goalId }
        assertEquals(500.0, updated.currentAmount, 0.01)

        // Delete
        repo.deleteSavingsGoal(goalId)
        val afterDelete = repo.observeSavingsGoals(uid).first()
        assertTrue(afterDelete.none { it.id == goalId })
    }
}
