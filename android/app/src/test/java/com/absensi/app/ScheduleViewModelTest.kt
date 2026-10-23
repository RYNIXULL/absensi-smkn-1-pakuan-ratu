package com.absensi.app

import com.absensi.app.data.remote.dto.ClassSummaryDto
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.remote.dto.SubjectSummaryDto
import com.absensi.app.ui.common.ToastType
import com.absensi.app.ui.schedule.ScheduleViewModel
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
class ScheduleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMasterDataRepository
    private lateinit var viewModel: ScheduleViewModel

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
    fun loadSchedules_sortsAndGroupsByDay() = runTest {
        val sTuesday = ScheduleDto(
            id = "sch-2",
            class_id = "c1",
            subject_id = "s1",
            teacher_id = "t1",
            location_id = "l1",
            day_of_week = 2,
            start_time = "07:30",
            end_time = "09:00",
            `class` = ClassSummaryDto("c1", "X-IPA-1"),
            subject = SubjectSummaryDto("s1", "Fisika")
        )
        val sMondayAfternoon = ScheduleDto(
            id = "sch-1b",
            class_id = "c1",
            subject_id = "s2",
            teacher_id = "t1",
            location_id = "l1",
            day_of_week = 1,
            start_time = "10:00",
            end_time = "11:30",
            `class` = ClassSummaryDto("c1", "X-IPA-1"),
            subject = SubjectSummaryDto("s2", "Biologi")
        )
        val sMondayMorning = ScheduleDto(
            id = "sch-1a",
            class_id = "c1",
            subject_id = "s3",
            teacher_id = "t1",
            location_id = "l1",
            day_of_week = 1,
            start_time = "07:30",
            end_time = "09:00",
            `class` = ClassSummaryDto("c1", "X-IPA-1"),
            subject = SubjectSummaryDto("s3", "Matematika")
        )

        // Unsorted input
        fakeRepository.getMeSchedulesResult = Result.success(listOf(sTuesday, sMondayAfternoon, sMondayMorning))

        viewModel = ScheduleViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.schedules.size)
        // Check sorting: Monday 07:30, Monday 10:00, Tuesday 07:30
        assertEquals("sch-1a", state.schedules[0].id)
        assertEquals("sch-1b", state.schedules[1].id)
        assertEquals("sch-2", state.schedules[2].id)

        // Check grouped: 2 on day 1, 1 on day 2
        assertEquals(2, state.groupedSchedules[1]?.size)
        assertEquals(1, state.groupedSchedules[2]?.size)
    }

    @Test
    fun selectDay_updatesSelectedDay() = runTest {
        viewModel = ScheduleViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.selectDay(3)
        assertEquals(3, viewModel.uiState.value.selectedDay)

        viewModel.selectDay(null)
        assertEquals(null, viewModel.uiState.value.selectedDay)
    }

    @Test
    fun loadSchedules_failure_showsErrorToast() = runTest {
        fakeRepository.getMeSchedulesResult = Result.failure(Exception("Gagal menghubungi server"))

        viewModel = ScheduleViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.toast)
        assertEquals(ToastType.ERROR, state.toast?.type)
        assertTrue(state.toast?.message?.contains("Gagal") == true)
    }
}
