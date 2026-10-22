package com.absensi.app.data.repository

import com.absensi.app.data.local.AuthPreferences
import com.absensi.app.data.local.DeviceIdProvider
import com.absensi.app.data.remote.AuthApi
import com.absensi.app.data.remote.dto.DeviceDto
import com.absensi.app.data.remote.dto.LoginRequest
import com.absensi.app.data.remote.dto.LoginResponse
import com.absensi.app.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val userRole: Flow<String?>
    val userName: Flow<String?>
    suspend fun login(identifier: String, password: String): LoginResponse
    suspend fun logout()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val authPreferences: AuthPreferences,
    private val deviceIdProvider: DeviceIdProvider
) : AuthRepository {
    override val userRole: Flow<String?> = authPreferences.userRole
    override val userName: Flow<String?> = authPreferences.userName

    override suspend fun login(identifier: String, password: String): LoginResponse {
        val deviceId = deviceIdProvider.getDeviceId()
        val deviceModel = deviceIdProvider.getDeviceModel()

        val request = LoginRequest(
            identifier = identifier,
            password = password,
            device = DeviceDto(
                device_id = deviceId,
                model = deviceModel
            )
        )

        val response = authApi.login(request)
        authPreferences.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.user.id,
            userName = response.user.nama,
            userRole = response.user.role
        )
        return response
    }

    override suspend fun logout() {
        val refreshToken = authPreferences.getRefreshTokenSync()
        try {
            if (refreshToken != null) {
                authApi.logout(RefreshTokenRequest(refreshToken))
            }
        } catch (_: Exception) {
            // Local cleanup continues even if network fails
        } finally {
            authPreferences.clearSession()
        }
    }
}
