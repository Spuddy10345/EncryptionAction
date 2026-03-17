package com.example.encryptaction.domain.repository

import com.example.encryptaction.domain.model.EncryptedFile
import com.example.encryptaction.domain.model.SignedFile
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    /** All encrypted file records for the current user (sent or received). */
    fun observeEncryptedFiles(userId: String): Flow<List<EncryptedFile>>

    suspend fun getEncryptedFile(id: String): EncryptedFile?

    suspend fun saveEncryptedFile(file: EncryptedFile): EncryptedFile

    suspend fun updateEncryptedFile(file: EncryptedFile)

    suspend fun deleteEncryptedFile(id: String)

    /** All signed file records for the current user. */
    fun observeSignedFiles(userId: String): Flow<List<SignedFile>>

    suspend fun getSignedFile(id: String): SignedFile?

    suspend fun saveSignedFile(file: SignedFile): SignedFile

    suspend fun updateSignedFile(file: SignedFile)

    suspend fun deleteSignedFile(id: String)
}
