package com.absensi.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChallengeDto(
    val nonce: String,
    val expiresAt: String,
    val serverTime: String
)

@Serializable
data class SessionScheduleLocationDto(
    val id: String? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius_m: Int? = null
)

@Serializable
data class SessionScheduleTeacherDto(
    val id: String? = null,
    val nama: String? = null
)

@Serializable
data class SessionScheduleClassDto(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class SessionScheduleSubjectDto(
    val id: String? = null,
    val name: String? = null,
    val code: String? = null
)

@Serializable
data class SessionScheduleDto(
    val id: String,
    val teacher_id: String? = null,
    val teacher: SessionScheduleTeacherDto? = null,
    @kotlinx.serialization.SerialName("class")
    val classInfo: SessionScheduleClassDto? = null,
    val subject: SessionScheduleSubjectDto? = null,
    val location: SessionScheduleLocationDto? = null,
    val day_of_week: Int? = null,
    val start_time: String? = null,
    val end_time: String? = null
)

@Serializable
data class SessionDto(
    val id: String,
    val schedule_id: String,
    val tanggal: String,
    val status: String,
    val open_at: String,
    val late_after: String,
    val close_at: String,
    val opened_by: String? = null,
    val schedule: SessionScheduleDto? = null
)

@Serializable
data class AttendanceStudentDto(
    val id: String,
    val nama: String,
    val nomor_induk: String
)

@Serializable
data class AttendanceRecordDto(
    val id: String,
    val session_id: String,
    val student_id: String,
    val status: String,
    val checkin_at: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy_m: Double? = null,
    val distance_m: Double? = null,
    val selfie_key: String? = null,
    val catatan: String? = null,
    val session: SessionDto? = null,
    val student: AttendanceStudentDto? = null
)

@Serializable
data class CreateManualSessionRequest(
    val schedule_id: String,
    val tanggal: String? = null
)

@Serializable
data class CorrectionRequest(
    val status: String,
    val catatan: String
)

@Serializable
data class SignedUrlDto(
    val url: String,
    val expires_in: Int
)
