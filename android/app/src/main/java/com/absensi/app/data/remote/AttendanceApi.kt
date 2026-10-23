package com.absensi.app.data.remote

import com.absensi.app.data.remote.dto.ApiResponse
import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.data.remote.dto.ChallengeDto
import com.absensi.app.data.remote.dto.CorrectionRequest
import com.absensi.app.data.remote.dto.CreateManualSessionRequest
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.SessionDto
import com.absensi.app.data.remote.dto.SignedUrlDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AttendanceApi {

    @GET("sessions")
    suspend fun getSessions(
        @Query("status") status: String? = null,
        @Query("tanggal") tanggal: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("schedule_id") scheduleId: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): PaginatedResponse<SessionDto>

    @POST("sessions")
    suspend fun openManualSession(
        @Body request: CreateManualSessionRequest
    ): ApiResponse<SessionDto>

    @PATCH("sessions/{id}/close")
    suspend fun closeSession(
        @Path("id") id: String
    ): ApiResponse<SessionDto>

    @POST("sessions/{id}/challenge")
    suspend fun createChallenge(
        @Path("id") id: String
    ): ApiResponse<ChallengeDto>

    @Multipart
    @POST("sessions/{id}/attendance")
    suspend fun submitAttendance(
        @Path("id") id: String,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("accuracy") accuracy: RequestBody,
        @Part("nonce") nonce: RequestBody,
        @Part("device_id") deviceId: RequestBody,
        @Part("mock_flag") mockFlag: RequestBody,
        @Part("integrity_token") integrityToken: RequestBody?,
        @Part selfie: MultipartBody.Part
    ): ApiResponse<AttendanceRecordDto>

    @GET("me/attendances")
    suspend fun getMyAttendances(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("status") status: String? = null,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null
    ): PaginatedResponse<AttendanceRecordDto>

    @GET("attendances")
    suspend fun getAttendances(
        @Query("session_id") sessionId: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("subject_id") subjectId: String? = null,
        @Query("status") status: String? = null,
        @Query("tanggal") tanggal: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): PaginatedResponse<AttendanceRecordDto>

    @PATCH("attendances/{id}")
    suspend fun correctAttendance(
        @Path("id") id: String,
        @Body request: CorrectionRequest
    ): ApiResponse<AttendanceRecordDto>

    @GET("attendances/{id}/selfie")
    suspend fun getSelfieUrl(
        @Path("id") id: String
    ): ApiResponse<SignedUrlDto>
}
