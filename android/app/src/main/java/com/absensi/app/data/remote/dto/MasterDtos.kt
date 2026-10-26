package com.absensi.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginationMeta(
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 1
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T> = emptyList(),
    val pagination: PaginationMeta = PaginationMeta()
)

@Serializable
data class ApiResponse<T>(
    val data: T,
    val message: String? = null
)

@Serializable
data class SimpleMessageResponse(
    val message: String
)

// Users
@Serializable
data class UserListItemDto(
    val id: String,
    val nama: String,
    val nomor_induk: String,
    val email: String? = null,
    val role: String,
    val is_active: Boolean,
    val created_at: String? = null
)

@Serializable
data class CreateUserRequest(
    val nama: String,
    val nomor_induk: String,
    val email: String? = null,
    val role: String,
    val password: String? = null
)

@Serializable
data class ResetPasswordResponse(
    val temporaryPassword: String,
    val message: String
)

@Serializable
data class UserImportSummaryDto(
    val total: Int,
    val success: Int,
    val failed: Int
)

@Serializable
data class UserImportRowDto(
    val row: Int,
    val role: String? = null,
    val nama: String? = null,
    val nomor_induk: String? = null,
    val email: String? = null,
    val status: String,
    val reason: String? = null,
    val initial_password: String? = null
)

@Serializable
data class UserImportResponse(
    val summary: UserImportSummaryDto,
    val rows: List<UserImportRowDto>,
    val excel_base64: String? = null
)

// Classes
@Serializable
data class ClassCountDto(
    val members: Int = 0,
    val schedules: Int = 0
)

@Serializable
data class ClassDto(
    val id: String,
    val nama: String,
    val tahun_ajaran: String = "",
    val _count: ClassCountDto? = null
)

@Serializable
data class CreateClassRequest(
    val nama: String,
    val tahun_ajaran: String
)

@Serializable
data class UpdateClassRequest(
    val nama: String? = null,
    val tahun_ajaran: String? = null
)

@Serializable
data class UserSummaryDto(
    val id: String,
    val nama: String,
    val nomor_induk: String,
    val role: String? = null,
    val email: String? = null,
    val is_active: Boolean? = null
)

@Serializable
data class ClassMemberDto(
    val id: String,
    val user_id: String,
    val class_id: String,
    val user: UserSummaryDto
)

@Serializable
data class AddClassMembersRequest(
    val user_ids: List<String>
)

// Subjects
@Serializable
data class SubjectCountDto(
    val schedules: Int = 0
)

@Serializable
data class SubjectDto(
    val id: String,
    val nama: String,
    val _count: SubjectCountDto? = null
)

@Serializable
data class CreateSubjectRequest(
    val nama: String
)

@Serializable
data class UpdateSubjectRequest(
    val nama: String
)

// Locations
@Serializable
data class LocationDto(
    val id: String,
    val nama: String,
    val latitude: Double,
    val longitude: Double,
    val radius_m: Int,
    val is_active: Boolean
)

@Serializable
data class CreateLocationRequest(
    val nama: String,
    val latitude: Double,
    val longitude: Double,
    val radius_m: Int,
    val is_active: Boolean = true
)

@Serializable
data class UpdateLocationRequest(
    val nama: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius_m: Int? = null,
    val is_active: Boolean? = null
)

// Schedules
@Serializable
data class ClassSummaryDto(
    val id: String,
    val nama: String,
    val tahun_ajaran: String? = null
)

@Serializable
data class SubjectSummaryDto(
    val id: String,
    val nama: String
)

@Serializable
data class LocationSummaryDto(
    val id: String,
    val nama: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius_m: Int? = null,
    val is_active: Boolean? = null
)

@Serializable
data class ScheduleDto(
    val id: String,
    val class_id: String,
    val subject_id: String,
    val teacher_id: String,
    val location_id: String,
    val day_of_week: Int,
    val start_time: String,
    val end_time: String,
    val `class`: ClassSummaryDto? = null,
    val subject: SubjectSummaryDto? = null,
    val teacher: UserSummaryDto? = null,
    val location: LocationSummaryDto? = null
)

@Serializable
data class CreateScheduleRequest(
    val class_id: String,
    val subject_id: String,
    val teacher_id: String,
    val location_id: String,
    val day_of_week: Int,
    val start_time: String,
    val end_time: String
)

@Serializable
data class UpdateScheduleRequest(
    val class_id: String? = null,
    val subject_id: String? = null,
    val teacher_id: String? = null,
    val location_id: String? = null,
    val day_of_week: Int? = null,
    val start_time: String? = null,
    val end_time: String? = null
)

@Serializable
data class ScheduleListResponse(
    val data: List<ScheduleDto>
)
