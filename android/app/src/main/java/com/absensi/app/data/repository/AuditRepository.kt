package com.absensi.app.data.repository

import com.absensi.app.data.remote.AuditApi
import com.absensi.app.data.remote.dto.AuditLogDto
import com.absensi.app.data.remote.dto.AuditStatsDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.UpdateFcmTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

interface AuditRepository {
    suspend fun getAuditLogs(
        page: Int = 1,
        pageSize: Int = 20,
        action: String? = null,
        entity: String? = null,
        actorId: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        q: String? = null
    ): Result<PaginatedResponse<AuditLogDto>>

    suspend fun getAuditStats(): Result<AuditStatsDto>

    suspend fun updateFcmToken(deviceId: String, fcmToken: String): Result<String>
}

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val auditApi: AuditApi
) : AuditRepository {

    override suspend fun getAuditLogs(
        page: Int,
        pageSize: Int,
        action: String?,
        entity: String?,
        actorId: String?,
        startDate: String?,
        endDate: String?,
        q: String?
    ): Result<PaginatedResponse<AuditLogDto>> {
        return try {
            val response = auditApi.getAuditLogs(
                page = page,
                pageSize = pageSize,
                action = action,
                entity = entity,
                actorId = actorId,
                startDate = startDate,
                endDate = endDate,
                q = q
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAuditStats(): Result<AuditStatsDto> {
        return try {
            val response = auditApi.getAuditStats()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFcmToken(deviceId: String, fcmToken: String): Result<String> {
        return try {
            val response = auditApi.updateFcmToken(
                UpdateFcmTokenRequest(device_id = deviceId, fcm_token = fcmToken)
            )
            Result.success(response.data.message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
