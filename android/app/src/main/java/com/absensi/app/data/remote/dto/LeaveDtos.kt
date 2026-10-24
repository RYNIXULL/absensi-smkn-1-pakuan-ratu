package com.absensi.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaveUserDto(
    val id: String,
    val nama: String,
    val nomor_induk: String? = null
)

@Serializable
data class LeaveReviewerDto(
    val id: String,
    val nama: String
)

@Serializable
data class LeaveSessionScheduleDto(
    @SerialName("class")
    val classInfo: SessionScheduleClassDto? = null,
    val subject: SessionScheduleSubjectDto? = null,
    val teacher: SessionScheduleTeacherDto? = null
)

@Serializable
data class LeaveSessionDto(
    val id: String,
    val tanggal: String,
    val status: String,
    val schedule: LeaveSessionScheduleDto? = null
)

@Serializable
data class LeaveRequestDto(
    val id: String,
    val session_id: String,
    val student_id: String,
    val tipe: String, // "izin" or "sakit"
    val alasan: String,
    val attachment_key: String? = null,
    val status: String, // "menunggu", "disetujui", "ditolak"
    val created_at: String,
    val reviewed_at: String? = null,
    val reviewer_id: String? = null,
    val review_catatan: String? = null,
    val student: LeaveUserDto? = null,
    val reviewer: LeaveReviewerDto? = null,
    val session: LeaveSessionDto? = null
)

@Serializable
data class ReviewLeaveRequest(
    val status: String, // "disetujui" or "ditolak"
    val catatan: String? = null
)
