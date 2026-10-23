package com.absensi.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.SessionDto
import com.absensi.app.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SiswaHomeUiState {
    data object Loading : SiswaHomeUiState
    data class Success(val activeSession: SessionDto?) : SiswaHomeUiState
    data class Error(val message: String) : SiswaHomeUiState
}

@HiltViewModel
class SiswaHomeViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SiswaHomeUiState>(SiswaHomeUiState.Loading)
    val uiState: StateFlow<SiswaHomeUiState> = _uiState.asStateFlow()

    init {
        loadActiveSession()
    }

    fun loadActiveSession() {
        viewModelScope.launch {
            _uiState.value = SiswaHomeUiState.Loading
            attendanceRepository.getSessions(status = "terbuka", pageSize = 10).fold(
                onSuccess = { response ->
                    // Get the first active open session for this student
                    val activeSession = response.data.firstOrNull()
                    _uiState.value = SiswaHomeUiState.Success(activeSession)
                },
                onFailure = { throwable ->
                    _uiState.value = SiswaHomeUiState.Error(
                        message = throwable.message ?: "Gagal memuat sesi aktif."
                    )
                }
            )
        }
    }
}
