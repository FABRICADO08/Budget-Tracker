package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LegacyDataOwnership

@Dao
interface LegacyDataOwnershipDao {
    @Query("SELECT * FROM legacy_data_ownership WHERE id = 1 LIMIT 1")
    suspend fun getDecision(): LegacyDataOwnership?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDecisionIfAbsent(decision: LegacyDataOwnership): Long
}
