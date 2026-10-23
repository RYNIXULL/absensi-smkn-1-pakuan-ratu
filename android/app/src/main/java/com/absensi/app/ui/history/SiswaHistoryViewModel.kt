package com.absensi.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.data.repository.AttendanceRepository
import com.absensi.app.ui.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SiswaHistoryUiState {
    data object Loading : SiswaHistoryUiState
    data class Success(val attendances: List<AttendanceRecordDto>, val total: Int) : SiswaHistoryUiState
    data class Error(val message: String) : SiswaHistoryUiState
}

@HiltViewModel
class SiswaHistoryViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SiswaHistoryUiState>(SiswaHistoryUiState.Loading)
    val uiState: StateFlow<SiswaHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = SiswaHistoryUiState.Loading
            attendanceRepository.getMyAttendances(page = 1, pageSize = 50).fold(
                onSuccess = { response ->
                    _uiState.value = SiswaHistoryUiState.Success(
                        attendances = response.data,
                        total = response.pagination.total
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = SiswaHistoryUiState.Error(
                        message = ErrorMapper.parseError(throwable)
                    )
                }
            )
        }
    }
}
