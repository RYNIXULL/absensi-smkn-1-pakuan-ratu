package com.absensi.app.ui.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.UserImportResponse
import com.absensi.app.data.remote.dto.UserListItemDto
import com.absensi.app.data.repository.MasterDataRepository
import com.absensi.app.ui.common.ToastMessage
import com.absensi.app.ui.common.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

data class UsersUiState(
    val isLoading: Boolean = false,
    val users: List<UserListItemDto> = emptyList(),
    val pagination: PaginationMeta = PaginationMeta(1, 20, 0, 1),
    val searchQuery: String = "",
    val selectedRole: String? = null,
    val toast: ToastMessage? = null,
    val resetPasswordResult: String? = null,
    val importResult: UserImportResponse? = null
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers(page: Int = _uiState.value.pagination.page) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val query = _uiState.value.searchQuery.ifBlank { null }
            val role = _uiState.value.selectedRole
            val result = repository.getUsers(page = page, pageSize = 20, q = query, role = role)

            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = res.data,
                        pagination = res.pagination
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memuat pengguna", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        loadUsers(page = 1)
    }

    fun onRoleFilterChanged(role: String?) {
        _uiState.update { it.copy(selectedRole = role) }
        loadUsers(page = 1)
    }

    fun createUser(nama: String, nomorInduk: String, email: String?, role: String, password: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.createUser(nama, nomorInduk, email, role, password)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Pengguna berhasil dibuat", ToastType.SUCCESS)
                    )
                }
                loadUsers(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal membuat pengguna", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun resetPassword(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.resetPassword(userId)

            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        resetPasswordResult = res.temporaryPassword,
                        toast = ToastMessage("Password baru berhasil dibuat", ToastType.SUCCESS)
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal mereset password", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun importUsers(filePart: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.importUsers(filePart)

            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        importResult = res,
                        toast = ToastMessage("Impor selesai: ${res.summary.success} berhasil, ${res.summary.failed} gagal", ToastType.SUCCESS)
                    )
                }
                loadUsers(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal mengimpor file", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }

    fun dismissResetPasswordDialog() {
        _uiState.update { it.copy(resetPasswordResult = null) }
    }

    fun dismissImportResultDialog() {
        _uiState.update { it.copy(importResult = null) }
    }
}
