package com.example.data.local.repository

import androidx.room.withTransaction
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.LegacyDataOwnership

data class LegacyDataOwnershipStatus(
    val unownedTransactions: Int,
    val unownedLoans: Int,
    val decisionMade: Boolean
) {
    val hasUnownedRecords: Boolean get() = unownedTransactions + unownedLoans > 0
    val needsDecision: Boolean get() = hasUnownedRecords && !decisionMade
}

/**
 * Handles ownership of records created before local data was scoped to a Firebase UID.
 *
 * Legacy records are never assigned automatically at sign-in. An explicit affirmative user
 * choice claims all remaining unowned transactions and loans for this device database, while a
 * decline records a one-time decision and leaves these rows inaccessible to signed-in accounts.
 */
class LegacyDataOwnershipRepository(private val database: AppDatabase) {
    suspend fun getStatus(): LegacyDataOwnershipStatus {
        val decision = database.legacyDataOwnershipDao().getDecision()
        return LegacyDataOwnershipStatus(
            unownedTransactions = database.transactionDao().countUnowned(),
            unownedLoans = database.loanDao().countUnowned(),
            decisionMade = decision != null
        )
    }

    suspend fun decide(currentUserId: String, claimRecords: Boolean): LegacyDataOwnershipStatus {
        require(currentUserId.isNotBlank()) { "A signed-in user is required." }
        database.withTransaction {
            val ownershipDao = database.legacyDataOwnershipDao()
            if (ownershipDao.getDecision() != null) return@withTransaction

            val transactions = database.transactionDao()
            val loans = database.loanDao()
            val count = transactions.countUnowned() + loans.countUnowned()
            if (count == 0) return@withTransaction

            if (claimRecords) {
                transactions.claimUnownedForUser(currentUserId)
                loans.claimUnownedForUser(currentUserId)
            }
            ownershipDao.insertDecisionIfAbsent(
                LegacyDataOwnership(
                    id = 1,
                    // Empty owner UID records the explicit decision to leave legacy records unassigned.
                    ownerUid = if (claimRecords) currentUserId else "",
                    decisionMadeAt = System.currentTimeMillis()
                )
            )
        }
        return getStatus()
    }
}
