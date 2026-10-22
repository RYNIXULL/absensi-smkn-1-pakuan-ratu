package com.absensi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.ApiErrorResponse
import com.absensi.app.data.repository.AuthRepository
import com.absensi.app.ui.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val role: String, val userName: String) : LoginUiState
    data object DevicePending : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(identifier: String, password: String) {
        if (identifier.isBlank()) {
            _uiState.value = LoginUiState.Error("Nomor induk atau email tidak boleh kosong.")
            return
        }

        if (password.isBlank()) {
            _uiState.value = LoginUiState.Error("Password tidak boleh kosong.")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            try {
                val response = authRepository.login(identifier.trim(), password)
                _uiState.value = LoginUiState.Success(
                    role = response.user.role,
                    userName = response.user.nama
                )
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                var isPending = false
                if (!errorBody.isNullOrBlank()) {
                    try {
                        val parsed = json.decodeFromString<ApiErrorResponse>(errorBody)
                        if (parsed.error.code == "DEVICE_PENDING") {
                            isPending = true
                        }
                    } catch (_: Exception) {
                        // ignore parse failure
                    }
                }

                if (isPending) {
                    _uiState.value = LoginUiState.DevicePending
                } else {
                    val message = ErrorMapper.parseError(e, json)
                    _uiState.value = LoginUiState.Error(message)
                }
            } catch (e: Exception) {
                val message = ErrorMapper.parseError(e, json)
                _uiState.value = LoginUiState.Error(message)
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
