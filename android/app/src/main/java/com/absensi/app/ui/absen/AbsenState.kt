package com.absensi.app.ui.absen

import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.data.remote.dto.ChallengeDto
import com.absensi.app.data.remote.dto.SessionDto

sealed interface AbsenState {
    data object CheckingPermissions : AbsenState
    data object LocationOff : AbsenState
    data class PermissionDenied(val isPermanentlyDenied: Boolean = false) : AbsenState
    data class Ready(
        val session: SessionDto,
        val challenge: ChallengeDto,
        val lat: Double,
        val lng: Double,
        val accuracy: Float,
        val isMock: Boolean
    ) : AbsenState
    data class Capturing(val session: SessionDto, val challenge: ChallengeDto) : AbsenState
    data class Uploading(val session: SessionDto) : AbsenState
    data class Success(val attendance: AttendanceRecordDto) : AbsenState
    data class Error(val code: String, val message: String, val suggestion: String) : AbsenState
}
