package com.example.encryptaction.di

import android.content.Context
import androidx.room.Room
import com.example.encryptaction.data.local.AppDatabase
import com.example.encryptaction.data.local.dao.FileDao
import com.example.encryptaction.data.local.dao.KeyBundleDao
import com.example.encryptaction.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "encryptaction.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideKeyBundleDao(db: AppDatabase): KeyBundleDao = db.keyBundleDao()

    @Provides
    fun provideFileDao(db: AppDatabase): FileDao = db.fileDao()
}
