package com.absensi.app.data.remote

import com.absensi.app.data.remote.dto.AddClassMembersRequest
import com.absensi.app.data.remote.dto.ApiResponse
import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.data.remote.dto.ClassMemberDto
import com.absensi.app.data.remote.dto.CreateClassRequest
import com.absensi.app.data.remote.dto.CreateLocationRequest
import com.absensi.app.data.remote.dto.CreateScheduleRequest
import com.absensi.app.data.remote.dto.CreateSubjectRequest
import com.absensi.app.data.remote.dto.CreateUserRequest
import com.absensi.app.data.remote.dto.LocationDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.ResetPasswordResponse
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.remote.dto.ScheduleListResponse
import com.absensi.app.data.remote.dto.SimpleMessageResponse
import com.absensi.app.data.remote.dto.SubjectDto
import com.absensi.app.data.remote.dto.UpdateClassRequest
import com.absensi.app.data.remote.dto.UpdateLocationRequest
import com.absensi.app.data.remote.dto.UpdateScheduleRequest
import com.absensi.app.data.remote.dto.UpdateSubjectRequest
import com.absensi.app.data.remote.dto.UserImportResponse
import com.absensi.app.data.remote.dto.UserListItemDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MasterDataApi {
    // Users
    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("q") q: String? = null,
        @Query("role") role: String? = null
    ): Response<PaginatedResponse<UserListItemDto>>

    @POST("users")
    suspend fun createUser(
        @Body request: CreateUserRequest
    ): Response<ApiResponse<UserListItemDto>>

    @POST("users/{id}/reset-password")
    suspend fun resetPassword(
        @Path("id") id: String
    ): Response<ResetPasswordResponse>

    @Multipart
    @POST("users/import")
    suspend fun importUsers(
        @Part file: MultipartBody.Part
    ): Response<UserImportResponse>

    // Classes
    @GET("classes")
    suspend fun getClasses(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("q") q: String? = null,
        @Query("tahun_ajaran") tahunAjaran: String? = null
    ): Response<PaginatedResponse<ClassDto>>

    @POST("classes")
    suspend fun createClass(
        @Body request: CreateClassRequest
    ): Response<ApiResponse<ClassDto>>

    @PATCH("classes/{id}")
    suspend fun updateClass(
        @Path("id") id: String,
        @Body request: UpdateClassRequest
    ): Response<ApiResponse<ClassDto>>

    @DELETE("classes/{id}")
    suspend fun deleteClass(
        @Path("id") id: String
    ): Response<SimpleMessageResponse>

    @GET("classes/{id}/members")
    suspend fun getClassMembers(
        @Path("id") id: String
    ): Response<ApiResponse<List<ClassMemberDto>>>

    @POST("classes/{id}/members")
    suspend fun addClassMembers(
        @Path("id") id: String,
        @Body request: AddClassMembersRequest
    ): Response<SimpleMessageResponse>

    @DELETE("classes/{id}/members/{userId}")
    suspend fun removeClassMember(
        @Path("id") id: String,
        @Path("userId") userId: String
    ): Response<SimpleMessageResponse>

    // Subjects
    @GET("subjects")
    suspend fun getSubjects(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("q") q: String? = null
    ): Response<PaginatedResponse<SubjectDto>>

    @POST("subjects")
    suspend fun createSubject(
        @Body request: CreateSubjectRequest
    ): Response<ApiResponse<SubjectDto>>

    @PATCH("subjects/{id}")
    suspend fun updateSubject(
        @Path("id") id: String,
        @Body request: UpdateSubjectRequest
    ): Response<ApiResponse<SubjectDto>>

    @DELETE("subjects/{id}")
    suspend fun deleteSubject(
        @Path("id") id: String
    ): Response<SimpleMessageResponse>

    // Locations
    @GET("locations")
    suspend fun getLocations(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("q") q: String? = null,
        @Query("is_active") isActive: String? = null
    ): Response<PaginatedResponse<LocationDto>>

    @POST("locations")
    suspend fun createLocation(
        @Body request: CreateLocationRequest
    ): Response<ApiResponse<LocationDto>>

    @PATCH("locations/{id}")
    suspend fun updateLocation(
        @Path("id") id: String,
        @Body request: UpdateLocationRequest
    ): Response<ApiResponse<LocationDto>>

    @DELETE("locations/{id}")
    suspend fun deleteLocation(
        @Path("id") id: String
    ): Response<SimpleMessageResponse>

    // Schedules
    @GET("schedules")
    suspend fun getSchedules(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("class_id") classId: String? = null,
        @Query("teacher_id") teacherId: String? = null,
        @Query("day_of_week") dayOfWeek: Int? = null,
        @Query("q") q: String? = null
    ): Response<PaginatedResponse<ScheduleDto>>

    @POST("schedules")
    suspend fun createSchedule(
        @Body request: CreateScheduleRequest
    ): Response<ApiResponse<ScheduleDto>>

    @PATCH("schedules/{id}")
    suspend fun updateSchedule(
        @Path("id") id: String,
        @Body request: UpdateScheduleRequest
    ): Response<ApiResponse<ScheduleDto>>

    @DELETE("schedules/{id}")
    suspend fun deleteSchedule(
        @Path("id") id: String
    ): Response<SimpleMessageResponse>

    // Personal schedules for guru and siswa
    @GET("me/schedules")
    suspend fun getMeSchedules(): Response<ScheduleListResponse>
}
