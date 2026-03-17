package com.example.encryptaction.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.encryptaction.data.local.dao.FileDao
import com.example.encryptaction.data.local.dao.KeyBundleDao
import com.example.encryptaction.data.local.dao.UserDao
import com.example.encryptaction.data.local.entity.EncryptedFileEntity
import com.example.encryptaction.data.local.entity.KeyBundleEntity
import com.example.encryptaction.data.local.entity.SignedFileEntity
import com.example.encryptaction.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        KeyBundleEntity::class,
        EncryptedFileEntity::class,
        SignedFileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun keyBundleDao(): KeyBundleDao
    abstract fun fileDao(): FileDao
}
