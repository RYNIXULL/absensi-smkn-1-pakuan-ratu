package com.absensi.app.data.repository

import com.absensi.app.data.remote.AttendanceApi
import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.data.remote.dto.ChallengeDto
import com.absensi.app.data.remote.dto.CorrectionRequest
import com.absensi.app.data.remote.dto.CreateManualSessionRequest
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.SessionDto
import com.absensi.app.data.remote.dto.SignedUrlDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

interface AttendanceRepository {
    suspend fun getSessions(
        status: String? = null,
        tanggal: String? = null,
        classId: String? = null,
        scheduleId: String? = null,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<PaginatedResponse<SessionDto>>

    suspend fun openManualSession(
        scheduleId: String,
        tanggal: String? = null
    ): Result<SessionDto>

    suspend fun closeSession(sessionId: String): Result<SessionDto>

    suspend fun createChallenge(sessionId: String): Result<ChallengeDto>

    suspend fun submitAttendance(
        sessionId: String,
        lat: Double,
        lng: Double,
        accuracy: Float,
        nonce: String,
        deviceId: String,
        mockFlag: Boolean,
        integrityToken: String?,
        selfieBytes: ByteArray
    ): Result<AttendanceRecordDto>

    suspend fun getMyAttendances(
        page: Int = 1,
        pageSize: Int = 20,
        status: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<PaginatedResponse<AttendanceRecordDto>>

    suspend fun getAttendances(
        sessionId: String? = null,
        classId: String? = null,
        subjectId: String? = null,
        status: String? = null,
        tanggal: String? = null,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<PaginatedResponse<AttendanceRecordDto>>

    suspend fun correctAttendance(
        attendanceId: String,
        status: String,
        catatan: String
    ): Result<AttendanceRecordDto>

    suspend fun getSelfieUrl(attendanceId: String): Result<SignedUrlDto>
}

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceApi: AttendanceApi
) : AttendanceRepository {

    override suspend fun getSessions(
        status: String?,
        tanggal: String?,
        classId: String?,
        scheduleId: String?,
        page: Int,
        pageSize: Int
    ): Result<PaginatedResponse<SessionDto>> {
        return runCatching {
            attendanceApi.getSessions(status, tanggal, classId, scheduleId, page, pageSize)
        }
    }

    override suspend fun openManualSession(
        scheduleId: String,
        tanggal: String?
    ): Result<SessionDto> {
        return runCatching {
            val response = attendanceApi.openManualSession(
                CreateManualSessionRequest(schedule_id = scheduleId, tanggal = tanggal)
            )
            response.data
        }
    }

    override suspend fun closeSession(sessionId: String): Result<SessionDto> {
        return runCatching {
            val response = attendanceApi.closeSession(sessionId)
            response.data
        }
    }

    override suspend fun createChallenge(sessionId: String): Result<ChallengeDto> {
        return runCatching {
            val response = attendanceApi.createChallenge(sessionId)
            response.data
        }
    }

    override suspend fun submitAttendance(
        sessionId: String,
        lat: Double,
        lng: Double,
        accuracy: Float,
        nonce: String,
        deviceId: String,
        mockFlag: Boolean,
        integrityToken: String?,
        selfieBytes: ByteArray
    ): Result<AttendanceRecordDto> {
        return runCatching {
            val textPlain = "text/plain".toMediaTypeOrNull()
            val imageJpeg = "image/jpeg".toMediaTypeOrNull()

            val latPart = lat.toString().toRequestBody(textPlain)
            val lngPart = lng.toString().toRequestBody(textPlain)
            val accuracyPart = accuracy.toString().toRequestBody(textPlain)
            val noncePart = nonce.toRequestBody(textPlain)
            val deviceIdPart = deviceId.toRequestBody(textPlain)
            val mockFlagPart = mockFlag.toString().toRequestBody(textPlain)
            val integrityTokenPart = integrityToken?.toRequestBody(textPlain)

            val selfieRequestBody = selfieBytes.toRequestBody(imageJpeg)
            val selfiePart = MultipartBody.Part.createFormData("selfie", "selfie.jpg", selfieRequestBody)

            val response = attendanceApi.submitAttendance(
                id = sessionId,
                lat = latPart,
                lng = lngPart,
                accuracy = accuracyPart,
                nonce = noncePart,
                deviceId = deviceIdPart,
                mockFlag = mockFlagPart,
                integrityToken = integrityTokenPart,
                selfie = selfiePart
            )
            response.data
        }
    }

    override suspend fun getMyAttendances(
        page: Int,
        pageSize: Int,
        status: String?,
        fromDate: String?,
        toDate: String?
    ): Result<PaginatedResponse<AttendanceRecordDto>> {
        return runCatching {
            attendanceApi.getMyAttendances(page, pageSize, status, fromDate, toDate)
        }
    }

    override suspend fun getAttendances(
        sessionId: String?,
        classId: String?,
        subjectId: String?,
        status: String?,
        tanggal: String?,
        page: Int,
        pageSize: Int
    ): Result<PaginatedResponse<AttendanceRecordDto>> {
        return runCatching {
            attendanceApi.getAttendances(sessionId, classId, subjectId, status, tanggal, page, pageSize)
        }
    }

    override suspend fun correctAttendance(
        attendanceId: String,
        status: String,
        catatan: String
    ): Result<AttendanceRecordDto> {
        return runCatching {
            val response = attendanceApi.correctAttendance(
                id = attendanceId,
                request = CorrectionRequest(status = status, catatan = catatan)
            )
            response.data
        }
    }

    override suspend fun getSelfieUrl(attendanceId: String): Result<SignedUrlDto> {
        return runCatching {
            val response = attendanceApi.getSelfieUrl(attendanceId)
            response.data
        }
    }
}
