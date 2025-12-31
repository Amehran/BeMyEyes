package com.amehran.bemyeyes.di

import com.amehran.bemyeyes.data.repository.BackendRepositoryImpl
import com.amehran.bemyeyes.domain.repository.BackendRepository
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
    abstract fun bindBackendRepository(
        backendRepositoryImpl: BackendRepositoryImpl
    ): BackendRepository
}
