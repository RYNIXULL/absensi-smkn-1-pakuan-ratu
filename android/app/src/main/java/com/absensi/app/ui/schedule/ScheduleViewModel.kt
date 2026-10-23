package com.absensi.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.repository.MasterDataRepository
import com.absensi.app.ui.common.ToastMessage
import com.absensi.app.ui.common.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val schedules: List<ScheduleDto> = emptyList(),
    val groupedSchedules: Map<Int, List<ScheduleDto>> = emptyMap(),
    val selectedDay: Int? = null, // null = all days
    val toast: ToastMessage? = null
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        // Set initial selected day to current day of week (1=Mon..7=Sun) or null
        try {
            val todayDayOfWeek = LocalDate.now().dayOfWeek.value // 1=Mon, 7=Sun
            _uiState.update { it.copy(selectedDay = todayDayOfWeek) }
        } catch (_: Exception) {
            // fallback if older java time issue
        }
        loadSchedules()
    }

    fun loadSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.getMeSchedules()
            result.onSuccess { list ->
                // Sort by day_of_week, then start_time
                val sorted = list.sortedWith(compareBy({ it.day_of_week }, { it.start_time }))
                val grouped = sorted.groupBy { it.day_of_week }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        schedules = sorted,
                        groupedSchedules = grouped
                    )
                }
            }.onFailure { err ->
                val errorMsg = err.message ?: "Gagal memuat jadwal pelajaran"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(errorMsg, ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun selectDay(day: Int?) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
