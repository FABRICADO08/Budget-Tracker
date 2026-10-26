package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One-time decision for records created before local financial data had an owner.
 *
 * A blank ownerUid means the user chose to leave the legacy records unassigned. The singleton
 * row prevents a later account on a shared device from being offered the same legacy data.
 */
@Entity(tableName = "legacy_data_ownership")
data class LegacyDataOwnership(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "owner_uid")
    val ownerUid: String,
    @ColumnInfo(name = "decision_made_at")
    val decisionMadeAt: Long = System.currentTimeMillis()
)
