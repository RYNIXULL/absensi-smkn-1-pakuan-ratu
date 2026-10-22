package com.absensi.app.data.remote

import com.absensi.app.data.remote.dto.ChangePasswordRequest
import com.absensi.app.data.remote.dto.LoginRequest
import com.absensi.app.data.remote.dto.LoginResponse
import com.absensi.app.data.remote.dto.MessageResponse
import com.absensi.app.data.remote.dto.RefreshTokenRequest
import com.absensi.app.data.remote.dto.RefreshTokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/refresh")
    fun refreshTokenCall(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest? = null): MessageResponse

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): MessageResponse
}
