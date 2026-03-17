package com.example.encryptaction.data.repository

import com.example.encryptaction.data.local.dao.FileDao
import com.example.encryptaction.data.local.mapper.toDomain
import com.example.encryptaction.data.local.mapper.toEntity
import com.example.encryptaction.domain.model.EncryptedFile
import com.example.encryptaction.domain.model.SignedFile
import com.example.encryptaction.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileDao: FileDao
) : FileRepository {

    override fun observeEncryptedFiles(userId: String): Flow<List<EncryptedFile>> =
        fileDao.observeEncryptedFilesForUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getEncryptedFile(id: String): EncryptedFile? =
        fileDao.getEncryptedFileById(id)?.toDomain()

    override suspend fun saveEncryptedFile(file: EncryptedFile): EncryptedFile {
        fileDao.insertEncryptedFile(file.toEntity())
        return file
    }

    override suspend fun updateEncryptedFile(file: EncryptedFile) {
        fileDao.updateEncryptedFile(file.toEntity())
    }

    override suspend fun deleteEncryptedFile(id: String) {
        fileDao.deleteEncryptedFileById(id)
    }

    override fun observeSignedFiles(userId: String): Flow<List<SignedFile>> =
        fileDao.observeSignedFilesForUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSignedFile(id: String): SignedFile? =
        fileDao.getSignedFileById(id)?.toDomain()

    override suspend fun saveSignedFile(file: SignedFile): SignedFile {
        fileDao.insertSignedFile(file.toEntity())
        return file
    }

    override suspend fun updateSignedFile(file: SignedFile) {
        fileDao.updateSignedFile(file.toEntity())
    }

    override suspend fun deleteSignedFile(id: String) {
        fileDao.deleteSignedFileById(id)
    }
}
