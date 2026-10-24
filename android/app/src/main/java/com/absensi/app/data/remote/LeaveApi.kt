package com.absensi.app.data.remote

import com.absensi.app.data.remote.dto.ApiResponse
import com.absensi.app.data.remote.dto.LeaveRequestDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.ReviewLeaveRequest
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

interface LeaveApi {

    @Multipart
    @POST("leave-requests")
    suspend fun createLeaveRequest(
        @Part("session_id") sessionId: RequestBody,
        @Part("tipe") tipe: RequestBody,
        @Part("alasan") alasan: RequestBody,
        @Part file: MultipartBody.Part?
    ): ApiResponse<LeaveRequestDto>

    @GET("leave-requests")
    suspend fun getLeaveRequests(
        @Query("status") status: String? = null,
        @Query("tipe") tipe: String? = null,
        @Query("session_id") sessionId: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("student_id") studentId: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): PaginatedResponse<LeaveRequestDto>

    @PATCH("leave-requests/{id}")
    suspend fun reviewLeaveRequest(
        @Path("id") id: String,
        @Body body: ReviewLeaveRequest
    ): ApiResponse<LeaveRequestDto>

    @GET("leave-requests/{id}/attachment")
    suspend fun getAttachment(
        @Path("id") id: String
    ): ApiResponse<SignedUrlDto>
}
