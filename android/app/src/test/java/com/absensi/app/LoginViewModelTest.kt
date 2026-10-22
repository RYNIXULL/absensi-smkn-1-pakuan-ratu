package com.absensi.app

import com.absensi.app.data.remote.dto.LoginResponse
import com.absensi.app.data.remote.dto.UserDto
import com.absensi.app.data.repository.AuthRepository
import com.absensi.app.ui.auth.LoginUiState
import com.absensi.app.ui.auth.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class FakeAuthRepository : AuthRepository {
    override val userRole: Flow<String?> = flowOf(null)
    override val userName: Flow<String?> = flowOf(null)

    var loginResult: Result<LoginResponse>? = null

    override suspend fun login(identifier: String, password: String): LoginResponse {
        return loginResult?.getOrThrow() ?: throw RuntimeException("Not configured")
    }

    override suspend fun logout() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = LoginViewModel(fakeRepository, Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_isIdle() {
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun login_blankIdentifier_setsErrorState() {
        viewModel.login("", "password123")
        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        assertEquals("Nomor induk atau email tidak boleh kosong.", (viewModel.uiState.value as LoginUiState.Error).message)
    }

    @Test
    fun login_blankPassword_setsErrorState() {
        viewModel.login("SISWA001", "")
        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        assertEquals("Password tidak boleh kosong.", (viewModel.uiState.value as LoginUiState.Error).message)
    }

    @Test
    fun login_success_emitsSuccessState() = runTest {
        val successResponse = LoginResponse(
            accessToken = "mock-access-token",
            refreshToken = "mock-refresh-token",
            user = UserDto(id = "user-1", role = "siswa", nama = "Ahmad Siswa")
        )
        fakeRepository.loginResult = Result.success(successResponse)

        viewModel.login("SISWA001", "SecretPass123!")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoginUiState.Success)
        val successState = viewModel.uiState.value as LoginUiState.Success
        assertEquals("siswa", successState.role)
        assertEquals("Ahmad Siswa", successState.userName)
    }

    @Test
    fun login_devicePending_emitsDevicePendingState() = runTest {
        val errorJson = """{"error":{"code":"DEVICE_PENDING","message":"Perangkat baru terdeteksi"}}"""
        val responseBody = errorJson.toResponseBody("application/json".toMediaType())
        val httpException = HttpException(Response.error<LoginResponse>(403, responseBody))

        fakeRepository.loginResult = Result.failure(httpException)

        viewModel.login("SISWA001", "SecretPass123!")
        advanceUntilIdle()

        assertEquals(LoginUiState.DevicePending, viewModel.uiState.value)
    }

    @Test
    fun resetState_resetsUiStateToIdle() {
        viewModel.login("", "")
        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        viewModel.resetState()
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }
}
