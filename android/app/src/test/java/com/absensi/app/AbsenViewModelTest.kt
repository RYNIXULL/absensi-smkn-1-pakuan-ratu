package com.absensi.app

import android.graphics.Bitmap
import android.location.Location
import androidx.lifecycle.SavedStateHandle
import com.absensi.app.data.local.AuthPreferences
import com.absensi.app.data.local.DeviceIdProvider
import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.data.remote.dto.ChallengeDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.SessionDto
import com.absensi.app.data.remote.dto.SignedUrlDto
import com.absensi.app.data.repository.AttendanceRepository
import com.absensi.app.security.IntegrityManager
import com.absensi.app.security.MockCheckResult
import com.absensi.app.security.MockLocationChecker
import com.absensi.app.security.RootCheckResult
import com.absensi.app.security.RootChecker
import com.absensi.app.ui.absen.AbsenState
import com.absensi.app.ui.absen.AbsenViewModel
import com.absensi.app.ui.absen.ImageProcessor
import com.absensi.app.ui.absen.LocationResult
import com.absensi.app.ui.absen.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

class FakeLocationService : LocationService {
    var isEnabled: Boolean = true
    var locationResult: LocationResult? = null

    override fun isLocationEnabled(): Boolean = isEnabled
    override suspend fun getCurrentLocation(): LocationResult? = locationResult
}

class FakeImageProcessor : ImageProcessor {
    var processedBytes: ByteArray = byteArrayOf(1, 2, 3, 4)

    override suspend fun processSelfie(
        bitmap: Bitmap,
        studentName: String,
        timestamp: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ): ByteArray = processedBytes
}

class FakeMockLocationChecker : MockLocationChecker {
    var isMock: Boolean = false
    var mockEnvironmentResult: MockCheckResult = MockCheckResult(false)

    override fun isMockLocation(location: Location?): Boolean = isMock
    override fun isMockLocation(locationResult: LocationResult?): Boolean = isMock || locationResult?.isMock == true
    override fun checkMockEnvironment(): MockCheckResult = mockEnvironmentResult
}

class FakeRootChecker : RootChecker {
    var isRooted: Boolean = false
    var rootReasons: List<String> = emptyList()

    override fun checkRoot(): RootCheckResult = RootCheckResult(isRooted, rootReasons)
    override fun isDeviceRooted(): Boolean = isRooted
}

class FakeIntegrityManager : IntegrityManager {
    var tokenResult: Result<String> = Result.success("test_integrity_token")
    var lastRequestedNonce: String? = null

    override suspend fun requestToken(nonce: String): Result<String> {
        lastRequestedNonce = nonce
        return tokenResult
    }
}

class FakeAttendanceRepository : AttendanceRepository {
    var challengeResult: Result<ChallengeDto> = Result.success(
        ChallengeDto("nonce-123", "2026-10-23T08:02:00.000Z", "2026-10-23T08:00:00.000Z")
    )
    var sessionsResult: Result<PaginatedResponse<SessionDto>> = Result.success(
        PaginatedResponse(
            data = listOf(
                SessionDto(
                    id = "session-1",
                    schedule_id = "sch-1",
                    tanggal = "2026-10-23",
                    status = "terbuka",
                    open_at = "2026-10-23T07:15:00.000Z",
                    late_after = "2026-10-23T07:45:00.000Z",
                    close_at = "2026-10-23T09:00:00.000Z"
                )
            ),
            pagination = PaginationMeta(1, 20, 1, 1)
        )
    )
    var submitResult: Result<AttendanceRecordDto> = Result.success(
        AttendanceRecordDto(
            id = "att-1",
            session_id = "session-1",
            student_id = "student-1",
            status = "hadir",
            checkin_at = "2026-10-23T07:30:00.000Z"
        )
    )

    var lastSubmittedIntegrityToken: String? = null
    var lastSubmittedMockFlag: Boolean = false
    var lastSubmittedAccuracy: Float = 0f

    override suspend fun getSessions(
        status: String?,
        tanggal: String?,
        classId: String?,
        scheduleId: String?,
        page: Int,
        pageSize: Int
    ): Result<PaginatedResponse<SessionDto>> = sessionsResult

    override suspend fun openManualSession(scheduleId: String, tanggal: String?): Result<SessionDto> =
        Result.failure(NotImplementedError())

    override suspend fun closeSession(sessionId: String): Result<SessionDto> =
        Result.failure(NotImplementedError())

    override suspend fun createChallenge(sessionId: String): Result<ChallengeDto> = challengeResult

    override suspend fun submitAttendance(
        sessionId: String,
        lat: Double,
        lng: Double,
        accuracy: Float,
        nonce: String,
        deviceId: String,
        mockFlag: Boolean,
        integrityToken: String?,
        selfieBytes: ByteArray
    ): Result<AttendanceRecordDto> {
        lastSubmittedIntegrityToken = integrityToken
        lastSubmittedMockFlag = mockFlag
        lastSubmittedAccuracy = accuracy
        return submitResult
    }

    override suspend fun getMyAttendances(
        page: Int,
        pageSize: Int,
        status: String?,
        fromDate: String?,
        toDate: String?
    ): Result<PaginatedResponse<AttendanceRecordDto>> = Result.failure(NotImplementedError())

    override suspend fun getAttendances(
        sessionId: String?,
        classId: String?,
        subjectId: String?,
        status: String?,
        tanggal: String?,
        page: Int,
        pageSize: Int
    ): Result<PaginatedResponse<AttendanceRecordDto>> = Result.failure(NotImplementedError())

    override suspend fun correctAttendance(
        attendanceId: String,
        status: String,
        catatan: String
    ): Result<AttendanceRecordDto> = Result.failure(NotImplementedError())

    override suspend fun getSelfieUrl(attendanceId: String): Result<SignedUrlDto> =
        Result.failure(NotImplementedError())
}

@OptIn(ExperimentalCoroutinesApi::class)
class AbsenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAttendanceRepository
    private lateinit var fakeLocationService: FakeLocationService
    private lateinit var fakeImageProcessor: FakeImageProcessor
    private lateinit var fakeMockLocationChecker: FakeMockLocationChecker
    private lateinit var fakeRootChecker: FakeRootChecker
    private lateinit var fakeIntegrityManager: FakeIntegrityManager
    private lateinit var fakeAuthPreferences: AuthPreferences
    private lateinit var fakeDeviceIdProvider: DeviceIdProvider
    private lateinit var viewModel: AbsenViewModel

    private val testSession = SessionDto(
        id = "session-1",
        schedule_id = "sch-1",
        tanggal = "2026-10-23",
        status = "terbuka",
        open_at = "2026-10-23T07:15:00.000Z",
        late_after = "2026-10-23T07:45:00.000Z",
        close_at = "2026-10-23T09:00:00.000Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeRepository = FakeAttendanceRepository()
        fakeLocationService = FakeLocationService().apply {
            isEnabled = true
            locationResult = LocationResult(
                latitude = -6.175392,
                longitude = 106.827153,
                accuracy = 15.0f,
                isMock = false
            )
        }
        fakeImageProcessor = FakeImageProcessor()
        fakeMockLocationChecker = FakeMockLocationChecker()
        fakeRootChecker = FakeRootChecker()
        fakeIntegrityManager = FakeIntegrityManager()

        fakeAuthPreferences = mock<AuthPreferences>().apply {
            whenever(userName).thenReturn(flowOf("Budi Santoso"))
        }

        fakeDeviceIdProvider = mock<DeviceIdProvider>().apply {
            whenever(getDeviceId()).thenReturn("device-12345")
        }

        val savedStateHandle = SavedStateHandle(mapOf("sessionId" to "session-1"))

        viewModel = AbsenViewModel(
            attendanceRepository = fakeRepository,
            locationService = fakeLocationService,
            imageProcessor = fakeImageProcessor,
            mockLocationChecker = fakeMockLocationChecker,
            rootChecker = fakeRootChecker,
            integrityManager = fakeIntegrityManager,
            authPreferences = fakeAuthPreferences,
            deviceIdProvider = fakeDeviceIdProvider,
            savedStateHandle = savedStateHandle
        )
        viewModel.setSession(testSession)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is CheckingPermissions`() {
        assertEquals(AbsenState.CheckingPermissions, viewModel.state.value)
    }

    @Test
    fun `when permissions denied, state transitions to PermissionDenied`() {
        viewModel.onPermissionsChecked(
            hasLocationPermission = false,
            hasCameraPermission = true
        )
        assertTrue(viewModel.state.value is AbsenState.PermissionDenied)
    }

    @Test
    fun `when location is disabled, state transitions to LocationOff`() {
        fakeLocationService.isEnabled = false

        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )

        assertEquals(AbsenState.LocationOff, viewModel.state.value)
    }

    @Test
    fun `when permissions granted and location enabled, transitions to Ready`() = runTest {
        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Expected Ready state but got $state", state is AbsenState.Ready)
        val ready = state as AbsenState.Ready
        assertEquals(-6.175392, ready.lat, 0.000001)
        assertEquals(106.827153, ready.lng, 0.000001)
        assertEquals(15.0f, ready.accuracy)
        assertEquals(false, ready.isMock)
    }

    @Test
    fun `when device is rooted, blocks and transitions to Error ROOT_DETECTED`() = runTest {
        fakeRootChecker.isRooted = true
        fakeRootChecker.rootReasons = listOf("Ditemukan file SU")

        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Expected Error state but got $state", state is AbsenState.Error)
        val error = state as AbsenState.Error
        assertEquals("ROOT_DETECTED", error.code)
        assertTrue(error.message.contains("Root"))
    }

    @Test
    fun `when location is mock (Fake GPS), blocks and transitions to Error MOCK_LOCATION`() = runTest {
        fakeMockLocationChecker.isMock = true

        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Expected Error state but got $state", state is AbsenState.Error)
        val error = state as AbsenState.Error
        assertEquals("MOCK_LOCATION", error.code)
    }

    @Test
    fun `when GPS accuracy is worse than 50m, transitions to Error LOW_ACCURACY`() = runTest {
        fakeLocationService.locationResult = LocationResult(
            latitude = -6.175392,
            longitude = 106.827153,
            accuracy = 75.0f, // > 50m!
            isMock = false
        )

        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Expected Error state but got $state", state is AbsenState.Error)
        val error = state as AbsenState.Error
        assertEquals("LOW_ACCURACY", error.code)
        assertTrue(error.message.contains("50"))
    }

    @Test
    fun `when location is unavailable, transitions to Error LOCATION_UNAVAILABLE`() = runTest {
        fakeLocationService.locationResult = null // GPS signal lost

        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Expected Error state but got $state", state is AbsenState.Error)
        val error = state as AbsenState.Error
        assertEquals("LOCATION_UNAVAILABLE", error.code)
    }

    @Test
    fun `when captureAndSubmit called and submit succeeds, requests Play Integrity token and transitions to Success`() = runTest {
        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value is AbsenState.Ready)

        val mockBitmap = mock<Bitmap>()
        viewModel.captureAndSubmit(mockBitmap)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Expected Success state but got $state", state is AbsenState.Success)
        val success = state as AbsenState.Success
        assertEquals("hadir", success.attendance.status)
        assertEquals("test_integrity_token", fakeRepository.lastSubmittedIntegrityToken)
        assertEquals("nonce-123", fakeIntegrityManager.lastRequestedNonce)
    }

    @Test
    fun `when submit fails with INTEGRITY_FAILED, transitions to Error INTEGRITY_FAILED with suggestion`() = runTest {
        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )
        advanceUntilIdle()

        val errorBody = """{"error":{"code":"INTEGRITY_FAILED","message":"Integritas perangkat gagal"}}"""
            .toResponseBody("application/json".toMediaType())
        val httpException = HttpException(Response.error<Any>(400, errorBody))
        fakeRepository.submitResult = Result.failure(httpException)

        val mockBitmap = mock<Bitmap>()
        viewModel.captureAndSubmit(mockBitmap)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Expected Error state but got $state", state is AbsenState.Error)
        val error = state as AbsenState.Error
        assertEquals("INTEGRITY_FAILED", error.code)
        assertTrue(error.suggestion.contains("firmware") || error.suggestion.contains("Play Store"))
    }

    @Test
    fun `when submit fails with OUT_OF_RADIUS, transitions to Error OUT_OF_RADIUS with suggestion`() = runTest {
        viewModel.onPermissionsChecked(
            hasLocationPermission = true,
            hasCameraPermission = true
        )
        advanceUntilIdle()

        val errorBody = """{"error":{"code":"OUT_OF_RADIUS","message":"Di luar radius"}}"""
            .toResponseBody("application/json".toMediaType())
        val httpException = HttpException(Response.error<Any>(400, errorBody))
        fakeRepository.submitResult = Result.failure(httpException)

        val mockBitmap = mock<Bitmap>()
        viewModel.captureAndSubmit(mockBitmap)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Expected Error state but got $state", state is AbsenState.Error)
        val error = state as AbsenState.Error
        assertEquals("OUT_OF_RADIUS", error.code)
        assertTrue(error.suggestion.contains("radius"))
    }
}
