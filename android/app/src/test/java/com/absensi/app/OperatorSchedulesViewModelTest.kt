package com.absensi.app

import com.absensi.app.data.remote.dto.ClassSummaryDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.remote.dto.SubjectSummaryDto
import com.absensi.app.ui.common.ToastType
import com.absensi.app.ui.operator.OperatorSchedulesViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OperatorSchedulesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMasterDataRepository
    private lateinit var viewModel: OperatorSchedulesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMasterDataRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadSchedules_success_populatesState() = runTest {
        val sampleSchedule = ScheduleDto(
            id = "sch-1",
            class_id = "c1",
            subject_id = "s1",
            teacher_id = "t1",
            location_id = "l1",
            day_of_week = 1,
            start_time = "07:30",
            end_time = "09:00",
            `class` = ClassSummaryDto("c1", "X-IPA-1"),
            subject = SubjectSummaryDto("s1", "Matematika")
        )
        fakeRepository.getSchedulesResult = Result.success(
            PaginatedResponse(
                data = listOf(sampleSchedule),
                pagination = PaginationMeta(page = 1, pageSize = 20, total = 1, totalPages = 1)
            )
        )

        viewModel = OperatorSchedulesViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.schedules.size)
        assertEquals("sch-1", state.schedules[0].id)
        assertEquals("Matematika", state.schedules[0].subject?.nama)
    }

    @Test
    fun createSchedule_conflict_showsScheduleConflictToast() = runTest {
        viewModel = OperatorSchedulesViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.createScheduleResult = Result.failure(
            Exception("Guru sudah memiliki jadwal lain pada hari dan jam yang beririsan (SCHEDULE_CONFLICT)")
        )

        viewModel.createSchedule("c1", "s1", "t1", "l1", 1, "07:30", "09:00")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.toast)
        assertEquals(ToastType.ERROR, state.toast?.type)
        assertTrue(state.toast?.message?.contains("SCHEDULE_CONFLICT") == true || state.toast?.message?.contains("beririsan") == true)
    }

    @Test
    fun deleteSchedule_success_showsToast() = runTest {
        viewModel = OperatorSchedulesViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.deleteScheduleResult = Result.success("Jadwal berhasil dihapus")

        viewModel.deleteSchedule("sch-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.toast)
        assertEquals(ToastType.SUCCESS, state.toast?.type)
    }
}
