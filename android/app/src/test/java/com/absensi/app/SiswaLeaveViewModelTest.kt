package com.absensi.app

import com.absensi.app.data.remote.dto.LeaveRequestDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.SessionDto
import com.absensi.app.data.remote.dto.SignedUrlDto
import com.absensi.app.data.repository.AttendanceRepository
import com.absensi.app.data.repository.LeaveRepository
import com.absensi.app.ui.leave.SiswaLeaveViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class SiswaLeaveViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var leaveRepository: LeaveRepository
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var viewModel: SiswaLeaveViewModel

    private val sampleSession = SessionDto(
        id = "session-1",
        schedule_id = "sched-1",
        tanggal = "2026-10-24",
        status = "terbuka",
        open_at = "2026-10-24T07:00:00Z",
        late_after = "2026-10-24T07:15:00Z",
        close_at = "2026-10-24T08:30:00Z"
    )

    private val sampleLeave = LeaveRequestDto(
        id = "leave-1",
        session_id = "session-1",
        student_id = "student-1",
        tipe = "izin",
        alasan = "Ada keperluan keluarga mendesak",
        status = "menunggu",
        created_at = "2026-10-24T07:10:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        leaveRepository = mock()
        attendanceRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsLeaveRequestsAndAvailableSessions_populatesState() = runTest {
        whenever(leaveRepository.getLeaveRequests(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = listOf(sampleLeave),
                        pagination = PaginationMeta(page = 1, pageSize = 20, total = 1, totalPages = 1)
                    )
                )
            )

        whenever(attendanceRepository.getSessions(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = listOf(sampleSession),
                        pagination = PaginationMeta(page = 1, pageSize = 30, total = 1, totalPages = 1)
                    )
                )
            )

        viewModel = SiswaLeaveViewModel(leaveRepository, attendanceRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.leaveRequests.size)
        assertEquals("leave-1", state.leaveRequests[0].id)
        assertEquals(1, state.availableSessions.size)
        assertEquals("session-1", state.selectedSessionId)
        assertNull(state.errorMessage)
    }

    @Test
    fun submitLeaveRequest_validationError_whenAlasanTooShort() = runTest {
        whenever(leaveRepository.getLeaveRequests(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 20, 0, 0))))
        whenever(attendanceRepository.getSessions(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(listOf(sampleSession), PaginationMeta(1, 30, 1, 1))))

        viewModel = SiswaLeaveViewModel(leaveRepository, attendanceRepository)
        advanceUntilIdle()

        viewModel.selectSession("session-1")
        viewModel.setAlasan("abc") // < 5 chars
        viewModel.submitLeaveRequest()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("minimal 5 karakter"))
    }

    @Test
    fun submitLeaveRequest_success_clearsFormAndSwitchesTab() = runTest {
        whenever(leaveRepository.getLeaveRequests(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 20, 0, 0))))
        whenever(attendanceRepository.getSessions(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(listOf(sampleSession), PaginationMeta(1, 30, 1, 1))))

        whenever(leaveRepository.createLeaveRequest(any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(sampleLeave))

        viewModel = SiswaLeaveViewModel(leaveRepository, attendanceRepository)
        advanceUntilIdle()

        viewModel.selectSession("session-1")
        viewModel.setTipe("sakit")
        viewModel.setAlasan("Demam tinggi dan istirahat dokter")
        viewModel.submitLeaveRequest()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNotNull(state.successMessage)
        assertEquals(0, state.activeTab)
        assertEquals("", state.alasan)
    }

    @Test
    fun viewAttachment_success_updatesUrlAndInvokesCallback() = runTest {
        whenever(leaveRepository.getLeaveRequests(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 20, 0, 0))))
        whenever(attendanceRepository.getSessions(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 30, 0, 0))))

        whenever(leaveRepository.getAttachment("leave-1"))
            .thenReturn(Result.success(SignedUrlDto(url = "https://signed.url/lampiran.pdf", expires_in = 3600)))

        viewModel = SiswaLeaveViewModel(leaveRepository, attendanceRepository)
        advanceUntilIdle()

        var callbackUrl: String? = null
        viewModel.viewAttachment("leave-1") { url ->
            callbackUrl = url
        }
        advanceUntilIdle()

        assertEquals("https://signed.url/lampiran.pdf", callbackUrl)
        assertEquals("https://signed.url/lampiran.pdf", viewModel.uiState.value.selectedAttachmentUrl)
    }
}
