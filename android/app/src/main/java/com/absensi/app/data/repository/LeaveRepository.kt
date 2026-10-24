package com.absensi.app.data.repository

import com.absensi.app.data.remote.LeaveApi
import com.absensi.app.data.remote.dto.LeaveRequestDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.ReviewLeaveRequest
import com.absensi.app.data.remote.dto.SignedUrlDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

interface LeaveRepository {
    suspend fun createLeaveRequest(
        sessionId: String,
        tipe: String,
        alasan: String,
        attachmentBytes: ByteArray? = null,
        fileName: String? = null,
        mimeType: String? = null
    ): Result<LeaveRequestDto>

    suspend fun getLeaveRequests(
        status: String? = null,
        tipe: String? = null,
        sessionId: String? = null,
        classId: String? = null,
        studentId: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<PaginatedResponse<LeaveRequestDto>>

    suspend fun reviewLeaveRequest(
        id: String,
        status: String,
        catatan: String? = null
    ): Result<LeaveRequestDto>

    suspend fun getAttachment(id: String): Result<SignedUrlDto>
}

@Singleton
class LeaveRepositoryImpl @Inject constructor(
    private val leaveApi: LeaveApi
) : LeaveRepository {

    override suspend fun createLeaveRequest(
        sessionId: String,
        tipe: String,
        alasan: String,
        attachmentBytes: ByteArray?,
        fileName: String?,
        mimeType: String?
    ): Result<LeaveRequestDto> {
        return runCatching {
            val sessionPart = sessionId.toRequestBody("text/plain".toMediaTypeOrNull())
            val tipePart = tipe.toRequestBody("text/plain".toMediaTypeOrNull())
            val alasanPart = alasan.toRequestBody("text/plain".toMediaTypeOrNull())

            val filePart = if (attachmentBytes != null && attachmentBytes.isNotEmpty()) {
                val resolvedMime = mimeType ?: "application/octet-stream"
                val resolvedName = fileName ?: "lampiran_${System.currentTimeMillis()}"
                val body = attachmentBytes.toRequestBody(resolvedMime.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("file", resolvedName, body)
            } else {
                null
            }

            val response = leaveApi.createLeaveRequest(sessionPart, tipePart, alasanPart, filePart)
            response.data
        }
    }

    override suspend fun getLeaveRequests(
        status: String?,
        tipe: String?,
        sessionId: String?,
        classId: String?,
        studentId: String?,
        page: Int,
        pageSize: Int
    ): Result<PaginatedResponse<LeaveRequestDto>> {
        return runCatching {
            leaveApi.getLeaveRequests(
                status = status,
                tipe = tipe,
                sessionId = sessionId,
                classId = classId,
                studentId = studentId,
                page = page,
                pageSize = pageSize
            )
        }
    }

    override suspend fun reviewLeaveRequest(
        id: String,
        status: String,
        catatan: String?
    ): Result<LeaveRequestDto> {
        return runCatching {
            val response = leaveApi.reviewLeaveRequest(
                id = id,
                body = ReviewLeaveRequest(status = status, catatan = catatan)
            )
            response.data
        }
    }

    override suspend fun getAttachment(id: String): Result<SignedUrlDto> {
        return runCatching {
            val response = leaveApi.getAttachment(id)
            response.data
        }
    }
}
