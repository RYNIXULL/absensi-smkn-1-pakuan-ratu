package com.absensi.app.di

import com.absensi.app.data.repository.AuthRepository
import com.absensi.app.data.repository.AuthRepositoryImpl
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMasterDataRepository(impl: com.absensi.app.data.repository.MasterDataRepositoryImpl): com.absensi.app.data.repository.MasterDataRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: com.absensi.app.data.repository.AttendanceRepositoryImpl): com.absensi.app.data.repository.AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindLeaveRepository(impl: com.absensi.app.data.repository.LeaveRepositoryImpl): com.absensi.app.data.repository.LeaveRepository
}
