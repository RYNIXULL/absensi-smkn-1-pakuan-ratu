package com.absensi.app

import com.absensi.app.data.remote.dto.AuditActorDto
import com.absensi.app.data.remote.dto.AuditLogDto
import com.absensi.app.data.remote.dto.AuditStatsDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.TopActionDto
import com.absensi.app.data.repository.AuditRepository
import com.absensi.app.ui.audit.AuditLogViewModel
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
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AuditLogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var auditRepository: AuditRepository
    private lateinit var viewModel: AuditLogViewModel

    private val sampleLog = AuditLogDto(
        id = "log-1",
        actor_id = "user-1",
        actor = AuditActorDto(
            id = "user-1",
            nama = "Operator Utama",
            nomor_induk = "OP-01",
            role = "operator"
        ),
        action = "DEVICE_APPROVED",
        entity = "Device",
        entity_id = "device-1",
        created_at = "2026-10-25T08:00:00.000Z"
    )

    private val sampleStats = AuditStatsDto(
        totalLogs = 100,
        todayLogs = 15,
        topActions = listOf(
            TopActionDto(action = "LOGIN_SUCCESS", count = 40),
            TopActionDto(action = "DEVICE_APPROVED", count = 10)
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        auditRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsLogsAndStats_populatesState() = runTest {
        whenever(auditRepository.getAuditLogs(eq(1), eq(20), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = listOf(sampleLog),
                        pagination = PaginationMeta(page = 1, pageSize = 20, total = 1, totalPages = 1)
                    )
                )
            )

        whenever(auditRepository.getAuditStats())
            .thenReturn(Result.success(sampleStats))

        viewModel = AuditLogViewModel(auditRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(1, state.logs.size)
        assertEquals("log-1", state.logs[0].id)
        assertNotNull(state.stats)
        assertEquals(100, state.stats?.totalLogs)
        assertEquals(15, state.stats?.todayLogs)
        assertNull(state.errorMessage)
    }

    @Test
    fun setEntityFilter_updatesState_andReloads() = runTest {
        whenever(auditRepository.getAuditLogs(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = emptyList(),
                        pagination = PaginationMeta(page = 1, pageSize = 20, total = 0, totalPages = 0)
                    )
                )
            )
        whenever(auditRepository.getAuditStats())
            .thenReturn(Result.success(sampleStats))

        viewModel = AuditLogViewModel(auditRepository)
        advanceUntilIdle()

        viewModel.setEntityFilter("Device")
        advanceUntilIdle()

        assertEquals("Device", viewModel.uiState.value.selectedEntity)
        verify(auditRepository).getAuditLogs(
            eq(1),
            eq(20),
            anyOrNull(),
            eq("Device"),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )
    }

    @Test
    fun setSearchQuery_updatesState_andReloads() = runTest {
        whenever(auditRepository.getAuditLogs(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(
                Result.success(
                    PaginatedResponse(
                        data = emptyList(),
                        pagination = PaginationMeta(page = 1, pageSize = 20, total = 0, totalPages = 0)
                    )
                )
            )
        whenever(auditRepository.getAuditStats())
            .thenReturn(Result.success(sampleStats))

        viewModel = AuditLogViewModel(auditRepository)
        advanceUntilIdle()

        viewModel.setSearchQuery("Operator")
        advanceUntilIdle()

        assertEquals("Operator", viewModel.uiState.value.searchQuery)
        verify(auditRepository).getAuditLogs(
            eq(1),
            eq(20),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq("Operator")
        )
    }

    @Test
    fun selectLogForDetail_and_dismissDetail() = runTest {
        whenever(auditRepository.getAuditLogs(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(PaginatedResponse(emptyList(), PaginationMeta(1, 20, 0, 0))))
        whenever(auditRepository.getAuditStats())
            .thenReturn(Result.success(sampleStats))

        viewModel = AuditLogViewModel(auditRepository)
        advanceUntilIdle()

        viewModel.selectLogForDetail(sampleLog)
        assertEquals(sampleLog, viewModel.uiState.value.selectedLogForDetail)

        viewModel.dismissDetail()
        assertNull(viewModel.uiState.value.selectedLogForDetail)
    }

    @Test
    fun loadLogs_failure_setsErrorMessage() = runTest {
        whenever(auditRepository.getAuditLogs(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(RuntimeException("Gagal memuat log audit")))
        whenever(auditRepository.getAuditStats())
            .thenReturn(Result.success(sampleStats))

        viewModel = AuditLogViewModel(auditRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.errorMessage)
    }
}
