package com.absensi.app

import android.content.Context
import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.data.remote.dto.ClassSummaryDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.repository.AttendanceRepository
import com.absensi.app.data.repository.ExportRepository
import com.absensi.app.data.repository.MasterDataRepository
import com.absensi.app.ui.rekap.RekapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RekapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var exportRepository: ExportRepository
    private lateinit var masterDataRepository: MasterDataRepository
    private lateinit var viewModel: RekapViewModel

    private val sampleClass = ClassDto(
        id = "class-1",
        nama = "XII RPL 1",
        tahun_ajaran = "2026/2027"
    )

    private val sampleRecords = listOf(
        AttendanceRecordDto(
            id = "att-1",
            session_id = "sess-1",
            student_id = "stu-1",
            status = "hadir",
            checkin_at = "2026-10-24T07:15:00Z"
        ),
        AttendanceRecordDto(
            id = "att-2",
            session_id = "sess-1",
            student_id = "stu-2",
            status = "terlambat",
            checkin_at = "2026-10-24T07:35:00Z"
        ),
        AttendanceRecordDto(
            id = "att-3",
            session_id = "sess-1",
            student_id = "stu-3",
            status = "izin"
        ),
        AttendanceRecordDto(
            id = "att-4",
            session_id = "sess-1",
            student_id = "stu-4",
            status = "sakit"
        ),
        AttendanceRecordDto(
            id = "att-5",
            session_id = "sess-1",
            student_id = "stu-5",
            status = "alpa"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        attendanceRepository = mock()
        exportRepository = mock()
        masterDataRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsClassesAndAttendances_populatesMetricsCorrectly() = runTest {
        whenever(masterDataRepository.getClasses(eq(1), eq(100), anyOrNull(), anyOrNull()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = listOf(sampleClass),
                        pagination = PaginationMeta(page = 1, pageSize = 100, total = 1, totalPages = 1)
                    )
                )
            )

        whenever(attendanceRepository.getAttendances(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), eq(1), eq(50)))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = sampleRecords,
                        pagination = PaginationMeta(page = 1, pageSize = 50, total = 5, totalPages = 1)
                    )
                )
            )

        viewModel = RekapViewModel(attendanceRepository, exportRepository, masterDataRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.classes.size)
        assertEquals("class-1", state.classes[0].id)
        assertEquals(5, state.attendances.size)
        assertEquals(5, state.totalCount)
        assertEquals(1, state.hadirCount)
        assertEquals(1, state.terlambatCount)
        assertEquals(1, state.izinCount)
        assertEquals(1, state.sakitCount)
        assertEquals(1, state.alpaCount)
        assertNull(state.errorMessage)
    }

    @Test
    fun init_whenGetClassesFails403_fallbackToMySchedules() = runTest {
        whenever(masterDataRepository.getClasses(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(RuntimeException("HTTP 403 Forbidden")))

        whenever(masterDataRepository.getMeSchedules())
            .thenReturn(
                Result.success(
                    listOf(
                        ScheduleDto(
                            id = "sched-1",
                            class_id = "class-guru-1",
                            subject_id = "subj-1",
                            teacher_id = "guru-1",
                            location_id = "loc-1",
                            day_of_week = 1,
                            start_time = "07:00",
                            end_time = "08:30",
                            `class` = ClassSummaryDto(id = "class-guru-1", nama = "XI TKJ 2", tahun_ajaran = "2026/2027")
                        )
                    )
                )
            )

        whenever(attendanceRepository.getAttendances(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = emptyList(),
                        pagination = PaginationMeta(page = 1, pageSize = 50, total = 0, totalPages = 0)
                    )
                )
            )

        viewModel = RekapViewModel(attendanceRepository, exportRepository, masterDataRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.classes.size)
        assertEquals("class-guru-1", state.classes[0].id)
        assertEquals("XI TKJ 2", state.classes[0].nama)
    }

    @Test
    fun setQuickDateFilter_hariIni_setsDatesAndReloads() = runTest {
        whenever(masterDataRepository.getClasses(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 100, 0, 0))))

        whenever(attendanceRepository.getAttendances(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 50, 0, 0))))

        viewModel = RekapViewModel(attendanceRepository, exportRepository, masterDataRepository)
        advanceUntilIdle()

        viewModel.setQuickDateFilter("hari_ini")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("hari_ini", state.quickDateFilter)
        assertNotNull(state.startDate)
        assertNotNull(state.endDate)
        assertEquals(state.startDate, state.endDate)

        viewModel.setQuickDateFilter("semua")
        advanceUntilIdle()

        val stateAll = viewModel.uiState.value
        assertEquals("semua", stateAll.quickDateFilter)
        assertNull(stateAll.startDate)
        assertNull(stateAll.endDate)
    }

    @Test
    fun setClassFilter_and_setStatusFilter_updatesState() = runTest {
        whenever(masterDataRepository.getClasses(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 100, 0, 0))))

        whenever(attendanceRepository.getAttendances(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 50, 0, 0))))

        viewModel = RekapViewModel(attendanceRepository, exportRepository, masterDataRepository)
        advanceUntilIdle()

        viewModel.setClassFilter("class-99")
        advanceUntilIdle()
        assertEquals("class-99", viewModel.uiState.value.selectedClassId)

        viewModel.setStatusFilter("hadir")
        advanceUntilIdle()
        assertEquals("hadir", viewModel.uiState.value.selectedStatus)
    }

    @Test
    fun downloadExcelReport_success_notifiesCallbackAndSetsSuccess() = runTest {
        whenever(masterDataRepository.getClasses(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 100, 0, 0))))

        whenever(attendanceRepository.getAttendances(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 50, 0, 0))))

        val dummyBytes = "ExcelDummyContent".toByteArray()
        val mockFile = mock<File>()
        whenever(mockFile.name).thenReturn("rekap.xlsx")

        whenever(exportRepository.downloadReport(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(dummyBytes))

        whenever(exportRepository.saveReportToFile(any(), eq(dummyBytes), any()))
            .thenReturn(Result.success(mockFile))

        val mockContext = mock<Context>()
        var callbackInvokedWith: File? = null

        viewModel = RekapViewModel(attendanceRepository, exportRepository, masterDataRepository)
        advanceUntilIdle()

        viewModel.downloadExcelReport(mockContext) { file ->
            callbackInvokedWith = file
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isDownloading)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("rekap.xlsx"))
        assertEquals(mockFile, callbackInvokedWith)
        assertNull(state.errorMessage)
    }

    @Test
    fun downloadExcelReport_failure_setsErrorMessage() = runTest {
        whenever(masterDataRepository.getClasses(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 100, 0, 0))))

        whenever(attendanceRepository.getAttendances(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 50, 0, 0))))

        whenever(exportRepository.downloadReport(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(RuntimeException("Gagal unduh dari server")))

        val mockContext = mock<Context>()
        var callbackInvoked = false

        viewModel = RekapViewModel(attendanceRepository, exportRepository, masterDataRepository)
        advanceUntilIdle()

        viewModel.downloadExcelReport(mockContext) {
            callbackInvoked = true
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isDownloading)
        assertEquals(false, callbackInvoked)
        assertNotNull(state.errorMessage)
    }
}
