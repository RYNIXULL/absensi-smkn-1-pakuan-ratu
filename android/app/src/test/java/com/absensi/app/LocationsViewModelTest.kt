package com.absensi.app

import com.absensi.app.data.remote.dto.LocationDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.ui.common.ToastType
import com.absensi.app.ui.operator.LocationsViewModel
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
class LocationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMasterDataRepository
    private lateinit var viewModel: LocationsViewModel

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
    fun loadLocations_success_populatesLocations() = runTest {
        val sampleLoc = LocationDto(
            id = "loc-1",
            nama = "Gedung Utama",
            latitude = -6.200000,
            longitude = 106.816666,
            radius_m = 50,
            is_active = true
        )
        fakeRepository.getLocationsResult = Result.success(
            PaginatedResponse(
                data = listOf(sampleLoc),
                pagination = PaginationMeta(page = 1, pageSize = 20, total = 1, totalPages = 1)
            )
        )

        viewModel = LocationsViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.locations.size)
        assertEquals("Gedung Utama", state.locations[0].nama)
        assertEquals(50, state.locations[0].radius_m)
        assertTrue(state.locations[0].is_active)
    }

    @Test
    fun createLocation_success_showsSuccessToast() = runTest {
        viewModel = LocationsViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.createLocationResult = Result.success(
            LocationDto(id = "loc-2", nama = "Lab Komputer", latitude = -6.21, longitude = 106.82, radius_m = 30, is_active = true)
        )

        viewModel.createLocation("Lab Komputer", -6.21, 106.82, 30, true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.toast)
        assertEquals(ToastType.SUCCESS, state.toast?.type)
        assertTrue(state.toast?.message?.contains("berhasil ditambahkan") == true)
    }

    @Test
    fun deleteLocation_success_showsToast() = runTest {
        viewModel = LocationsViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.deleteLocationResult = Result.success("Lokasi dinonaktifkan")

        viewModel.deleteLocation("loc-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.toast)
        assertEquals(ToastType.SUCCESS, state.toast?.type)
    }
}
