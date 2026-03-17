package com.example.encryptaction.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.encryptaction.data.local.entity.KeyBundleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyBundleDao {
    @Query("SELECT * FROM key_bundles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<KeyBundleEntity>>

    @Query("SELECT * FROM key_bundles WHERE userId = :userId")
    suspend fun getByUserId(userId: String): KeyBundleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(bundle: KeyBundleEntity)

    @Query("DELETE FROM key_bundles WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}
