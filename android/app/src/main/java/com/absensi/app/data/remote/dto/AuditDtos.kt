package com.absensi.app.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuditActorDto(
    val id: String,
    val nama: String,
    val nomor_induk: String,
    val role: String
)

@Serializable
data class AuditLogDto(
    val id: String,
    val actor_id: String? = null,
    val actor: AuditActorDto? = null,
    val action: String,
    val entity: String,
    val entity_id: String? = null,
    val old_value: JsonElement? = null,
    val new_value: JsonElement? = null,
    val created_at: String
)

@Serializable
data class TopActionDto(
    val action: String,
    val count: Int
)

@Serializable
data class AuditStatsDto(
    val totalLogs: Int,
    val todayLogs: Int,
    val topActions: List<TopActionDto> = emptyList()
)

@Serializable
data class AuditStatsResponse(
    val data: AuditStatsDto
)

@Serializable
data class UpdateFcmTokenRequest(
    val device_id: String,
    val fcm_token: String
)

@Serializable
data class UpdateFcmTokenResponse(
    val data: UpdateFcmTokenResultDto
)

@Serializable
data class UpdateFcmTokenResultDto(
    val message: String,
    val device_id: String
)
