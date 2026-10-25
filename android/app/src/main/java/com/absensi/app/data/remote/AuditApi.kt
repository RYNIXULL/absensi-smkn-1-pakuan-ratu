package com.absensi.app.data.remote

import com.absensi.app.data.remote.dto.AuditLogDto
import com.absensi.app.data.remote.dto.AuditStatsResponse
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.UpdateFcmTokenRequest
import com.absensi.app.data.remote.dto.UpdateFcmTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuditApi {

    @GET("audit-logs")
    suspend fun getAuditLogs(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("action") action: String? = null,
        @Query("entity") entity: String? = null,
        @Query("actor_id") actorId: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("q") q: String? = null
    ): PaginatedResponse<AuditLogDto>

    @GET("audit-logs/stats")
    suspend fun getAuditStats(): AuditStatsResponse

    @POST("devices/fcm-token")
    suspend fun updateFcmToken(
        @Body request: UpdateFcmTokenRequest
    ): UpdateFcmTokenResponse
}
