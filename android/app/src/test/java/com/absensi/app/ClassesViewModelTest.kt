package com.absensi.app

import com.absensi.app.data.remote.dto.ClassCountDto
import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.data.remote.dto.ClassMemberDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.UserSummaryDto
import com.absensi.app.ui.common.ToastType
import com.absensi.app.ui.operator.ClassesViewModel
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
class ClassesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMasterDataRepository
    private lateinit var viewModel: ClassesViewModel

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
    fun loadClasses_success_populatesState() = runTest {
        val sampleClass = ClassDto(id = "c1", nama = "X-IPA-1", tahun_ajaran = "2024/2025", _count = ClassCountDto(schedules = 2, members = 30))
        fakeRepository.getClassesResult = Result.success(
            PaginatedResponse(
                data = listOf(sampleClass),
                pagination = PaginationMeta(page = 1, pageSize = 20, total = 1, totalPages = 1)
            )
        )

        viewModel = ClassesViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.classes.size)
        assertEquals("X-IPA-1", state.classes[0].nama)
        assertEquals(1, state.pagination.page)
        assertEquals(1, state.pagination.totalPages)
    }

    @Test
    fun createClass_success_showsToastAndRefreshes() = runTest {
        viewModel = ClassesViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.createClassResult = Result.success(ClassDto(id = "c2", nama = "X-IPA-2", tahun_ajaran = "2024/2025"))

        viewModel.createClass("X-IPA-2", "2024/2025")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.toast)
        assertEquals(ToastType.SUCCESS, state.toast?.type)
        assertTrue(state.toast?.message?.contains("berhasil dibuat") == true)
    }

    @Test
    fun deleteClass_inUse_showsErrorToast() = runTest {
        viewModel = ClassesViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.deleteClassResult = Result.failure(Exception("Data masih digunakan dan tidak dapat dihapus"))

        viewModel.deleteClass("c1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.toast)
        assertEquals(ToastType.ERROR, state.toast?.type)
        assertTrue(state.toast?.message?.contains("masih digunakan") == true)
    }

    @Test
    fun membersDialog_loadAndAddMembers() = runTest {
        val sampleClass = ClassDto(id = "c1", nama = "X-IPA-1", tahun_ajaran = "2024/2025")
        val sampleMember = ClassMemberDto(
            id = "m1",
            user_id = "u1",
            class_id = "c1",
            user = UserSummaryDto(id = "u1", nama = "Budi Siswa", nomor_induk = "12345", role = "siswa")
        )
        fakeRepository.getClassMembersResult = Result.success(listOf(sampleMember))

        viewModel = ClassesViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.openMembersManager(sampleClass)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("c1", state.activeClassForMembers?.id)
        assertEquals(1, state.currentMembers.size)
        assertEquals("Budi Siswa", state.currentMembers[0].user.nama)

        viewModel.addMember("u2")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.toast)
        assertEquals(ToastType.SUCCESS, viewModel.uiState.value.toast?.type)
    }
}
