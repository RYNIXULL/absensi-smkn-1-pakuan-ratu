package com.absensi.app.ui.absen

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.local.AuthPreferences
import com.absensi.app.data.local.DeviceIdProvider
import com.absensi.app.data.remote.dto.SessionDto
import com.absensi.app.data.repository.AttendanceRepository
import com.absensi.app.ui.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AbsenViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val locationService: LocationService,
    private val imageProcessor: ImageProcessor,
    private val authPreferences: AuthPreferences,
    private val deviceIdProvider: DeviceIdProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow<AbsenState>(AbsenState.CheckingPermissions)
    val state: StateFlow<AbsenState> = _state.asStateFlow()

    private var currentSessionId: String? = savedStateHandle.get<String>("sessionId")
    private var cachedSession: SessionDto? = null

    init {
        currentSessionId?.let {
            // Initial check triggered by UI lifecycle after permission checking
        }
    }

    fun setSession(session: SessionDto) {
        this.cachedSession = session
        this.currentSessionId = session.id
    }

    fun onPermissionsChecked(
        hasLocationPermission: Boolean,
        hasCameraPermission: Boolean,
        isPermanentlyDenied: Boolean = false
    ) {
        if (!hasLocationPermission || !hasCameraPermission) {
            _state.value = AbsenState.PermissionDenied(isPermanentlyDenied)
            return
        }

        if (!locationService.isLocationEnabled()) {
            _state.value = AbsenState.LocationOff
            return
        }

        val sessionId = currentSessionId
        if (sessionId != null) {
            fetchChallengeAndLocation(sessionId)
        } else {
            _state.value = AbsenState.Error(
                code = "SESSION_NOT_SPECIFIED",
                message = "Sesi absensi tidak ditentukan.",
                suggestion = "Kembali ke beranda dan pilih sesi aktif."
            )
        }
    }

    fun retryLocationCheck() {
        if (!locationService.isLocationEnabled()) {
            _state.value = AbsenState.LocationOff
            return
        }
        val sessionId = currentSessionId ?: return
        fetchChallengeAndLocation(sessionId)
    }

    fun refreshState() {
        val sessionId = currentSessionId ?: return
        fetchChallengeAndLocation(sessionId)
    }

    private fun fetchChallengeAndLocation(sessionId: String) {
        viewModelScope.launch {
            _state.value = AbsenState.CheckingPermissions

            // 1. Get or find session
            var session = cachedSession
            if (session == null) {
                val sessionsResult = attendanceRepository.getSessions(status = "terbuka")
                session = sessionsResult.getOrNull()?.data?.find { it.id == sessionId }
                if (session == null) {
                    _state.value = AbsenState.Error(
                        code = "SESSION_CLOSED",
                        message = "Sesi absensi tidak ditemukan atau tidak sedang terbuka.",
                        suggestion = "Pastikan guru telah membuka sesi absensi untuk jadwal hari ini."
                    )
                    return@launch
                }
                cachedSession = session
            }

            // 2. Request challenge nonce
            val challengeResult = attendanceRepository.createChallenge(sessionId)
            val challenge = challengeResult.getOrElse { throwable ->
                val error = ErrorMapper.parseDetailedError(throwable)
                _state.value = AbsenState.Error(
                    code = error.code,
                    message = error.message,
                    suggestion = error.suggestion
                )
                return@launch
            }

            // 3. Acquire GPS location
            val location = locationService.getCurrentLocation()
            if (location == null) {
                _state.value = AbsenState.Error(
                    code = "LOCATION_UNAVAILABLE",
                    message = "Gagal mendapatkan koordinat GPS. Pastikan sinyal GPS aktif.",
                    suggestion = "Bawalah ponsel ke tempat terbuka dan tekan tombol Coba Lagi."
                )
                return@launch
            }

            // 4. Block if mock location detected
            if (location.isMock) {
                _state.value = AbsenState.Error(
                    code = "MOCK_LOCATION",
                    message = "Lokasi tiruan (Fake GPS) terdeteksi dan dilarang keras.",
                    suggestion = "Nonaktifkan fitur Mock Location di Opsi Pengembang ponsel Anda."
                )
                return@launch
            }

            _state.value = AbsenState.Ready(
                session = session,
                challenge = challenge,
                lat = location.latitude,
                lng = location.longitude,
                accuracy = location.accuracy,
                isMock = location.isMock
            )
        }
    }

    fun captureAndSubmit(bitmap: Bitmap) {
        val currentState = _state.value
        if (currentState !is AbsenState.Ready) return

        val session = currentState.session
        val challenge = currentState.challenge
        val lat = currentState.lat
        val lng = currentState.lng
        val accuracy = currentState.accuracy
        val isMock = currentState.isMock

        if (isMock) {
            _state.value = AbsenState.Error(
                code = "MOCK_LOCATION",
                message = "Lokasi tiruan (Fake GPS) terdeteksi dan dilarang keras.",
                suggestion = "Nonaktifkan fitur Mock Location di Opsi Pengembang ponsel Anda."
            )
            return
        }

        viewModelScope.launch {
            _state.value = AbsenState.Capturing(session, challenge)

            try {
                // Get student name from AuthPreferences
                val studentName = authPreferences.userName.first() ?: "Siswa"
                val deviceId = deviceIdProvider.getDeviceId()

                // Process image with Canvas watermark and JPEG compression
                val selfieBytes = imageProcessor.processSelfie(
                    bitmap = bitmap,
                    studentName = studentName,
                    timestamp = challenge.serverTime,
                    latitude = lat,
                    longitude = lng,
                    accuracy = accuracy
                )

                _state.value = AbsenState.Uploading(session)

                val submitResult = attendanceRepository.submitAttendance(
                    sessionId = session.id,
                    lat = lat,
                    lng = lng,
                    accuracy = accuracy,
                    nonce = challenge.nonce,
                    deviceId = deviceId,
                    mockFlag = isMock,
                    integrityToken = null, // Stubbed for Phase F3
                    selfieBytes = selfieBytes
                )

                submitResult.fold(
                    onSuccess = { attendance ->
                        _state.value = AbsenState.Success(attendance)
                    },
                    onFailure = { throwable ->
                        val error = ErrorMapper.parseDetailedError(throwable)
                        _state.value = AbsenState.Error(
                            code = error.code,
                            message = error.message,
                            suggestion = error.suggestion
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = AbsenState.Error(
                    code = "PROCESSING_ERROR",
                    message = e.message ?: "Gagal memproses swafoto kehadiran.",
                    suggestion = "Silakan coba ambil swafoto kembali."
                )
            }
        }
    }
}
