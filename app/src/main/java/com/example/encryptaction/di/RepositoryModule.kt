package com.example.encryptaction.di

import com.example.encryptaction.data.repository.FileRepositoryImpl
import com.example.encryptaction.data.repository.SessionRepositoryImpl
import com.example.encryptaction.data.repository.UserRepositoryImpl
import com.example.encryptaction.domain.repository.FileRepository
import com.example.encryptaction.domain.repository.SessionRepository
import com.example.encryptaction.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
