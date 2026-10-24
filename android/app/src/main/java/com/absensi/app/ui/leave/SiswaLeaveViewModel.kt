package com.absensi.app.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.LeaveRequestDto
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.SessionDto
import com.absensi.app.data.repository.AttendanceRepository
import com.absensi.app.data.repository.LeaveRepository
import com.absensi.app.ui.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SiswaLeaveUiState(
    val activeTab: Int = 0, // 0: Daftar Pengajuan, 1: Ajukan Izin / Sakit
    val isLoadingRequests: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val isSubmitting: Boolean = false,
    val leaveRequests: List<LeaveRequestDto> = emptyList(),
    val pagination: PaginationMeta? = null,
    val selectedStatusFilter: String? = null,
    val availableSessions: List<SessionDto> = emptyList(),
    val selectedSessionId: String = "",
    val tipe: String = "izin", // "izin" or "sakit"
    val alasan: String = "",
    val attachmentBytes: ByteArray? = null,
    val attachmentName: String? = null,
    val attachmentMime: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val selectedAttachmentUrl: String? = null
)

@HiltViewModel
class SiswaLeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SiswaLeaveUiState())
    val uiState: StateFlow<SiswaLeaveUiState> = _uiState.asStateFlow()

    init {
        loadLeaveRequests()
        loadAvailableSessions()
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
        if (index == 0) {
            loadLeaveRequests()
        } else if (index == 1 && _uiState.value.availableSessions.isEmpty()) {
            loadAvailableSessions()
        }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
        loadLeaveRequests(page = 1)
    }

    fun loadLeaveRequests(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRequests = true, errorMessage = null) }
            val filter = _uiState.value.selectedStatusFilter
            val result = leaveRepository.getLeaveRequests(
                status = filter,
                page = page,
                pageSize = 20
            )
            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoadingRequests = false,
                        leaveRequests = response.data,
                        pagination = response.pagination
                    )
                }
            }.onFailure { err ->
                val msg = ErrorMapper.parseError(err)
                _uiState.update {
                    it.copy(
                        isLoadingRequests = false,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun loadAvailableSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSessions = true) }
            val result = attendanceRepository.getSessions(pageSize = 30)
            result.onSuccess { response ->
                val sessions = response.data
                _uiState.update {
                    it.copy(
                        isLoadingSessions = false,
                        availableSessions = sessions,
                        selectedSessionId = if (it.selectedSessionId.isBlank() && sessions.isNotEmpty()) {
                            sessions.first().id
                        } else {
                            it.selectedSessionId
                        }
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingSessions = false) }
            }
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.update { it.copy(selectedSessionId = sessionId) }
    }

    fun setTipe(tipe: String) {
        _uiState.update { it.copy(tipe = tipe) }
    }

    fun setAlasan(alasan: String) {
        _uiState.update { it.copy(alasan = alasan) }
    }

    fun setAttachment(bytes: ByteArray, name: String, mime: String) {
        _uiState.update {
            it.copy(
                attachmentBytes = bytes,
                attachmentName = name,
                attachmentMime = mime
            )
        }
    }

    fun removeAttachment() {
        _uiState.update {
            it.copy(
                attachmentBytes = null,
                attachmentName = null,
                attachmentMime = null
            )
        }
    }

    fun submitLeaveRequest() {
        val state = _uiState.value
        if (state.selectedSessionId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Silakan pilih sesi pelajaran terlebih dahulu.") }
            return
        }
        if (state.alasan.trim().length < 5) {
            _uiState.update { it.copy(errorMessage = "Alasan permohonan harus diisi minimal 5 karakter.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            val result = leaveRepository.createLeaveRequest(
                sessionId = state.selectedSessionId,
                tipe = state.tipe,
                alasan = state.alasan.trim(),
                attachmentBytes = state.attachmentBytes,
                fileName = state.attachmentName,
                mimeType = state.attachmentMime
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "Pengajuan ${state.tipe.replaceFirstChar { c -> c.uppercase() }} berhasil dikirim.",
                        alasan = "",
                        attachmentBytes = null,
                        attachmentName = null,
                        attachmentMime = null,
                        activeTab = 0 // Switch to requests list
                    )
                }
                loadLeaveRequests(page = 1)
            }.onFailure { err ->
                val msg = ErrorMapper.parseError(err)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun viewAttachment(leaveId: String, onUrlLoaded: (String) -> Unit) {
        viewModelScope.launch {
            val result = leaveRepository.getAttachment(leaveId)
            result.onSuccess { signed ->
                _uiState.update { it.copy(selectedAttachmentUrl = signed.url) }
                onUrlLoaded(signed.url)
            }.onFailure { err ->
                val msg = ErrorMapper.parseError(err)
                _uiState.update { it.copy(errorMessage = msg) }
            }
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
