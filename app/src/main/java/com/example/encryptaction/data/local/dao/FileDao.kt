package com.example.encryptaction.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.encryptaction.data.local.entity.EncryptedFileEntity
import com.example.encryptaction.data.local.entity.SignedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    // --- Encrypted files ---

    @Query("""
        SELECT * FROM encrypted_files
        WHERE senderId = :userId OR recipientId = :userId
        ORDER BY encryptedAt DESC
    """)
    fun observeEncryptedFilesForUser(userId: String): Flow<List<EncryptedFileEntity>>

    @Query("SELECT * FROM encrypted_files WHERE id = :id")
    suspend fun getEncryptedFileById(id: String): EncryptedFileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEncryptedFile(file: EncryptedFileEntity)

    @Update
    suspend fun updateEncryptedFile(file: EncryptedFileEntity)

    @Query("DELETE FROM encrypted_files WHERE id = :id")
    suspend fun deleteEncryptedFileById(id: String)

    // --- Signed files ---

    @Query("""
        SELECT * FROM signed_files
        WHERE signerId = :userId
        ORDER BY signedAt DESC
    """)
    fun observeSignedFilesForUser(userId: String): Flow<List<SignedFileEntity>>

    @Query("SELECT * FROM signed_files WHERE id = :id")
    suspend fun getSignedFileById(id: String): SignedFileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSignedFile(file: SignedFileEntity)

    @Update
    suspend fun updateSignedFile(file: SignedFileEntity)

    @Query("DELETE FROM signed_files WHERE id = :id")
    suspend fun deleteSignedFileById(id: String)
}
