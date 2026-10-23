package com.absensi.app.ui.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.data.remote.dto.LocationDto
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.remote.dto.SubjectDto
import com.absensi.app.data.remote.dto.UserListItemDto
import com.absensi.app.data.repository.MasterDataRepository
import com.absensi.app.ui.common.ToastMessage
import com.absensi.app.ui.common.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OperatorSchedulesUiState(
    val isLoading: Boolean = false,
    val schedules: List<ScheduleDto> = emptyList(),
    val pagination: PaginationMeta = PaginationMeta(1, 20, 0, 1),
    val selectedDayOfWeek: Int? = null,
    val searchQuery: String = "",
    val classes: List<ClassDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
    val teachers: List<UserListItemDto> = emptyList(),
    val locations: List<LocationDto> = emptyList(),
    val toast: ToastMessage? = null
)

@HiltViewModel
class OperatorSchedulesViewModel @Inject constructor(
    private val repository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorSchedulesUiState())
    val uiState: StateFlow<OperatorSchedulesUiState> = _uiState.asStateFlow()

    init {
        loadPrerequisites()
        loadSchedules()
    }

    fun loadPrerequisites() {
        viewModelScope.launch {
            repository.getClasses(1, 100).onSuccess { r -> _uiState.update { it.copy(classes = r.data) } }
            repository.getSubjects(1, 100).onSuccess { r -> _uiState.update { it.copy(subjects = r.data) } }
            repository.getUsers(1, 100, role = "guru").onSuccess { r -> _uiState.update { it.copy(teachers = r.data) } }
            repository.getLocations(1, 100, isActive = "true").onSuccess { r -> _uiState.update { it.copy(locations = r.data) } }
        }
    }

    fun loadSchedules(page: Int = _uiState.value.pagination.page) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val day = _uiState.value.selectedDayOfWeek
            val q = _uiState.value.searchQuery.ifBlank { null }
            val res = repository.getSchedules(page = page, pageSize = 20, dayOfWeek = day, q = q)

            res.onSuccess { r ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        schedules = r.data,
                        pagination = r.pagination
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memuat jadwal", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun onDayFilterChanged(day: Int?) {
        _uiState.update { it.copy(selectedDayOfWeek = day) }
        loadSchedules(page = 1)
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        loadSchedules(page = 1)
    }

    fun createSchedule(
        classId: String,
        subjectId: String,
        teacherId: String,
        locationId: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.createSchedule(classId, subjectId, teacherId, locationId, dayOfWeek, startTime, endTime)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Jadwal berhasil ditambahkan", ToastType.SUCCESS)
                    )
                }
                loadSchedules(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal menambah jadwal", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun updateSchedule(
        id: String,
        classId: String?,
        subjectId: String?,
        teacherId: String?,
        locationId: String?,
        dayOfWeek: Int?,
        startTime: String?,
        endTime: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.updateSchedule(id, classId, subjectId, teacherId, locationId, dayOfWeek, startTime, endTime)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Jadwal berhasil diperbarui", ToastType.SUCCESS)
                    )
                }
                loadSchedules()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memperbarui jadwal", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.deleteSchedule(id)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Jadwal berhasil dihapus", ToastType.SUCCESS)
                    )
                }
                loadSchedules(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal menghapus jadwal", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
