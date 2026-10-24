package com.absensi.app.ui.rekap

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.repository.AttendanceRepository
import com.absensi.app.data.repository.ExportRepository
import com.absensi.app.data.repository.MasterDataRepository
import com.absensi.app.ui.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RekapUiState(
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val attendances: List<AttendanceRecordDto> = emptyList(),
    val pagination: PaginationMeta? = null,
    val classes: List<ClassDto> = emptyList(),
    val selectedClassId: String? = null,
    val selectedStatus: String? = null,
    val quickDateFilter: String = "semua", // "hari_ini", "7_hari", "30_hari", "semua"
    val startDate: String? = null,
    val endDate: String? = null,
    val totalCount: Int = 0,
    val hadirCount: Int = 0,
    val terlambatCount: Int = 0,
    val izinCount: Int = 0,
    val sakitCount: Int = 0,
    val alpaCount: Int = 0,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val downloadedFile: File? = null
)

@HiltViewModel
class RekapViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val exportRepository: ExportRepository,
    private val masterDataRepository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RekapUiState())
    val uiState: StateFlow<RekapUiState> = _uiState.asStateFlow()

    init {
        loadClasses()
        loadAttendances()
    }

    fun loadClasses() {
        viewModelScope.launch {
            val classResult = masterDataRepository.getClasses(pageSize = 100)
            classResult.onSuccess { res ->
                _uiState.update { it.copy(classes = res.data) }
            }.onFailure {
                // If 403 (for Guru), fallback to my schedules to extract classes
                val schedResult = masterDataRepository.getMeSchedules()
                schedResult.onSuccess { schedules ->
                    val distinctClasses = schedules.mapNotNull { sched ->
                        sched.`class`?.let { cls ->
                            ClassDto(id = cls.id, nama = cls.nama, tahun_ajaran = cls.tahun_ajaran ?: "")
                        }
                    }.distinctBy { it.id }
                    _uiState.update { it.copy(classes = distinctClasses) }
                }
            }
        }
    }

    fun setQuickDateFilter(filter: String) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val (start, end) = when (filter) {
            "hari_ini" -> today.format(formatter) to today.format(formatter)
            "7_hari" -> today.minusDays(6).format(formatter) to today.format(formatter)
            "30_hari" -> today.minusDays(29).format(formatter) to today.format(formatter)
            else -> null to null
        }

        _uiState.update {
            it.copy(
                quickDateFilter = filter,
                startDate = start,
                endDate = end
            )
        }
        loadAttendances(page = 1)
    }

    fun setClassFilter(classId: String?) {
        _uiState.update { it.copy(selectedClassId = classId) }
        loadAttendances(page = 1)
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadAttendances(page = 1)
    }

    fun loadAttendances(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentState = _uiState.value
            val result = attendanceRepository.getAttendances(
                classId = currentState.selectedClassId,
                status = currentState.selectedStatus,
                tanggal = if (currentState.quickDateFilter == "hari_ini") currentState.startDate else null,
                page = page,
                pageSize = 50
            )

            result.onSuccess { response ->
                val list = response.data
                // Count metrics for the loaded records
                val hadir = list.count { it.status.equals("hadir", ignoreCase = true) }
                val terlambat = list.count { it.status.equals("terlambat", ignoreCase = true) }
                val izin = list.count { it.status.equals("izin", ignoreCase = true) }
                val sakit = list.count { it.status.equals("sakit", ignoreCase = true) }
                val alpa = list.count { it.status.equals("alpa", ignoreCase = true) }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        attendances = list,
                        pagination = response.pagination,
                        totalCount = response.pagination.total,
                        hadirCount = hadir,
                        terlambatCount = terlambat,
                        izinCount = izin,
                        sakitCount = sakit,
                        alpaCount = alpa
                    )
                }
            }.onFailure { err ->
                val msg = ErrorMapper.parseError(err)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun downloadExcelReport(context: Context, onFileReady: (File) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, errorMessage = null, successMessage = null) }

            val downloadResult = exportRepository.downloadReport(
                startDate = state.startDate,
                endDate = state.endDate,
                classId = state.selectedClassId,
                status = state.selectedStatus
            )

            downloadResult.onSuccess { bytes ->
                val filename = "rekap-absensi-${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}-${System.currentTimeMillis() % 10000}.xlsx"
                val fileResult = exportRepository.saveReportToFile(context, bytes, filename)

                fileResult.onSuccess { file ->
                    _uiState.update {
                        it.copy(
                            isDownloading = false,
                            successMessage = "Laporan Excel berhasil diunduh ke ${file.name}",
                            downloadedFile = file
                        )
                    }
                    onFileReady(file)
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isDownloading = false,
                            errorMessage = "Gagal menyimpan berkas ke perangkat: ${err.message}"
                        )
                    }
                }
            }.onFailure { err ->
                val msg = ErrorMapper.parseError(err)
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        errorMessage = "Gagal mengunduh laporan: $msg"
                    )
                }
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
