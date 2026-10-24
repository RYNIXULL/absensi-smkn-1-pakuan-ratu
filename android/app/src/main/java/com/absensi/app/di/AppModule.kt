package com.absensi.app.di

import com.absensi.app.BuildConfig
import com.absensi.app.data.remote.AuthApi
import com.absensi.app.data.remote.AuthInterceptor
import com.absensi.app.data.remote.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMasterDataApi(retrofit: Retrofit): com.absensi.app.data.remote.MasterDataApi {
        return retrofit.create(com.absensi.app.data.remote.MasterDataApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAttendanceApi(retrofit: Retrofit): com.absensi.app.data.remote.AttendanceApi {
        return retrofit.create(com.absensi.app.data.remote.AttendanceApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLeaveApi(retrofit: Retrofit): com.absensi.app.data.remote.LeaveApi {
        return retrofit.create(com.absensi.app.data.remote.LeaveApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExportApi(retrofit: Retrofit): com.absensi.app.data.remote.ExportApi {
        return retrofit.create(com.absensi.app.data.remote.ExportApi::class.java)
    }
}
