package com.absensi.app.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.remote.dto.SessionDto
import com.absensi.app.data.repository.AttendanceRepository
import com.absensi.app.data.repository.MasterDataRepository
import com.absensi.app.ui.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionsUiState {
    data object Loading : SessionsUiState
    data class Success(
        val sessions: List<SessionDto>,
        val schedules: List<ScheduleDto>,
        val selectedSessionAttendees: List<AttendanceRecordDto>? = null,
        val selectedSessionId: String? = null
    ) : SessionsUiState
    data class Error(val message: String) : SessionsUiState
}

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val masterDataRepository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    private var currentSessions: List<SessionDto> = emptyList()
    private var currentSchedules: List<ScheduleDto> = emptyList()
    private var currentAttendees: List<AttendanceRecordDto>? = null
    private var currentSelectedSessionId: String? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = SessionsUiState.Loading

            val sessionsResult = attendanceRepository.getSessions(page = 1, pageSize = 50)
            val schedulesResult = masterDataRepository.getSchedules(page = 1, pageSize = 50)

            if (sessionsResult.isSuccess) {
                currentSessions = sessionsResult.getOrNull()?.data ?: emptyList()
                currentSchedules = schedulesResult.getOrNull()?.data ?: emptyList()

                _uiState.value = SessionsUiState.Success(
                    sessions = currentSessions,
                    schedules = currentSchedules,
                    selectedSessionAttendees = currentAttendees,
                    selectedSessionId = currentSelectedSessionId
                )
            } else {
                _uiState.value = SessionsUiState.Error(
                    message = ErrorMapper.parseError(sessionsResult.exceptionOrNull() ?: Exception())
                )
            }
        }
    }

    fun openManualSession(scheduleId: String) {
        viewModelScope.launch {
            _uiState.value = SessionsUiState.Loading
            attendanceRepository.openManualSession(scheduleId).fold(
                onSuccess = {
                    loadData()
                },
                onFailure = { throwable ->
                    _uiState.value = SessionsUiState.Error(ErrorMapper.parseError(throwable))
                }
            )
        }
    }

    fun closeSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = SessionsUiState.Loading
            attendanceRepository.closeSession(sessionId).fold(
                onSuccess = {
                    loadData()
                },
                onFailure = { throwable ->
                    _uiState.value = SessionsUiState.Error(ErrorMapper.parseError(throwable))
                }
            )
        }
    }

    fun viewAttendees(sessionId: String) {
        viewModelScope.launch {
            currentSelectedSessionId = sessionId
            val attendeesResult = attendanceRepository.getAttendances(sessionId = sessionId)
            currentAttendees = attendeesResult.getOrNull()?.data ?: emptyList()

            _uiState.value = SessionsUiState.Success(
                sessions = currentSessions,
                schedules = currentSchedules,
                selectedSessionAttendees = currentAttendees,
                selectedSessionId = currentSelectedSessionId
            )
        }
    }

    fun clearSelectedSession() {
        currentSelectedSessionId = null
        currentAttendees = null
        _uiState.value = SessionsUiState.Success(
            sessions = currentSessions,
            schedules = currentSchedules,
            selectedSessionAttendees = null,
            selectedSessionId = null
        )
    }
}
