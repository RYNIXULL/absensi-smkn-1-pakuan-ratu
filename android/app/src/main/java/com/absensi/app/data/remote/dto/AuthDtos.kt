package com.absensi.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val device_id: String,
    val model: String? = null
)

@Serializable
data class LoginRequest(
    val identifier: String,
    val password: String,
    val device: DeviceDto
)

@Serializable
data class UserDto(
    val id: String,
    val role: String,
    val nama: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class ApiErrorDetail(
    val code: String,
    val message: String
)

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorDetail
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class MessageResponse(
    val message: String
)
