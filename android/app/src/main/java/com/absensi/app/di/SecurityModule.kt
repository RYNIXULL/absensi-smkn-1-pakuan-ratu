package com.absensi.app.di

import com.absensi.app.security.AndroidMockLocationChecker
import com.absensi.app.security.AndroidRootChecker
import com.absensi.app.security.IntegrityManager
import com.absensi.app.security.MockLocationChecker
import com.absensi.app.security.PlayIntegrityManager
import com.absensi.app.security.RootChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindMockLocationChecker(impl: AndroidMockLocationChecker): MockLocationChecker

    @Binds
    @Singleton
    abstract fun bindRootChecker(impl: AndroidRootChecker): RootChecker

    @Binds
    @Singleton
    abstract fun bindIntegrityManager(impl: PlayIntegrityManager): IntegrityManager
}
