package com.absensi.app.di

import com.absensi.app.ui.absen.AndroidImageProcessor
import com.absensi.app.ui.absen.AndroidLocationService
import com.absensi.app.ui.absen.ImageProcessor
import com.absensi.app.ui.absen.LocationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AbsenModule {

    @Binds
    @Singleton
    abstract fun bindLocationService(impl: AndroidLocationService): LocationService

    @Binds
    @Singleton
    abstract fun bindImageProcessor(impl: AndroidImageProcessor): ImageProcessor
}
