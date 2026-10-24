package com.absensi.app

import com.absensi.app.data.remote.dto.LeaveRequestDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.repository.LeaveRepository
import com.absensi.app.ui.leave.LeaveApprovalViewModel
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
class LeaveApprovalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var leaveRepository: LeaveRepository
    private lateinit var viewModel: LeaveApprovalViewModel

    private val sampleLeave = LeaveRequestDto(
        id = "leave-1",
        session_id = "session-1",
        student_id = "student-1",
        tipe = "izin",
        alasan = "Acara keluarga mendesak",
        status = "menunggu",
        created_at = "2026-10-24T07:10:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        leaveRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsPendingRequests_populatesState() = runTest {
        whenever(leaveRepository.getLeaveRequests(eq("menunggu"), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), eq(1), eq(20)))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = listOf(sampleLeave),
                        pagination = PaginationMeta(page = 1, pageSize = 20, total = 1, totalPages = 1)
                    )
                )
            )

        viewModel = LeaveApprovalViewModel(leaveRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("menunggu", state.selectedStatus)
        assertEquals(1, state.requests.size)
        assertEquals("leave-1", state.requests[0].id)
        assertNull(state.errorMessage)
    }

    @Test
    fun setStatusFilter_updatesSelectedStatus_andReloads() = runTest {
        whenever(leaveRepository.getLeaveRequests(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = emptyList(),
                        pagination = PaginationMeta(page = 1, pageSize = 20, total = 0, totalPages = 0)
                    )
                )
            )

        viewModel = LeaveApprovalViewModel(leaveRepository)
        advanceUntilIdle()

        viewModel.setStatusFilter("disetujui")
        advanceUntilIdle()

        assertEquals("disetujui", viewModel.uiState.value.selectedStatus)
        verify(leaveRepository).getLeaveRequests(eq("disetujui"), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), eq(1), eq(20))
    }

    @Test
    fun submitReview_approveSuccess_updatesUiStateAndClosesModal() = runTest {
        whenever(leaveRepository.getLeaveRequests(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = listOf(sampleLeave),
                        pagination = PaginationMeta(page = 1, pageSize = 20, total = 1, totalPages = 1)
                    )
                )
            )

        whenever(leaveRepository.reviewLeaveRequest(eq("leave-1"), eq("disetujui"), anyOrNull()))
            .thenReturn(Result.success(sampleLeave.copy(status = "disetujui")))

        viewModel = LeaveApprovalViewModel(leaveRepository)
        advanceUntilIdle()

        viewModel.openReviewDialog(sampleLeave, "disetujui")
        assertEquals(sampleLeave, viewModel.uiState.value.reviewTarget)
        assertEquals("disetujui", viewModel.uiState.value.reviewAction)

        viewModel.setReviewCatatan("Disetujui, jaga kesehatan")
        viewModel.submitReview()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.reviewTarget)
        assertNull(state.reviewAction)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("disetujui"))
        verify(leaveRepository).reviewLeaveRequest("leave-1", "disetujui", "Disetujui, jaga kesehatan")
    }

    @Test
    fun submitReview_rejectSuccess_updatesUiState() = runTest {
        whenever(leaveRepository.getLeaveRequests(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = listOf(sampleLeave),
                        pagination = PaginationMeta(page = 1, pageSize = 20, total = 1, totalPages = 1)
                    )
                )
            )

        whenever(leaveRepository.reviewLeaveRequest(eq("leave-1"), eq("ditolak"), anyOrNull()))
            .thenReturn(Result.success(sampleLeave.copy(status = "ditolak")))

        viewModel = LeaveApprovalViewModel(leaveRepository)
        advanceUntilIdle()

        viewModel.openReviewDialog(sampleLeave, "ditolak")
        viewModel.setReviewCatatan("Surat dokter tidak terlampir")
        viewModel.submitReview()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.reviewTarget)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("ditolak"))
        verify(leaveRepository).reviewLeaveRequest("leave-1", "ditolak", "Surat dokter tidak terlampir")
    }
}
