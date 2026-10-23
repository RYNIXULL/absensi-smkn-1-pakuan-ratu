package com.absensi.app.ui.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.data.remote.dto.ClassMemberDto
import com.absensi.app.data.remote.dto.PaginationMeta
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
import javax.inject.Inject

data class ClassesUiState(
    val isLoading: Boolean = false,
    val classes: List<ClassDto> = emptyList(),
    val pagination: PaginationMeta = PaginationMeta(1, 20, 0, 1),
    val searchQuery: String = "",
    val tahunAjaranQuery: String = "",
    val activeClassForMembers: ClassDto? = null,
    val currentMembers: List<ClassMemberDto> = emptyList(),
    val availableUsersForMember: List<UserListItemDto> = emptyList(),
    val toast: ToastMessage? = null
)

@HiltViewModel
class ClassesViewModel @Inject constructor(
    private val repository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassesUiState())
    val uiState: StateFlow<ClassesUiState> = _uiState.asStateFlow()

    init {
        loadClasses()
    }

    fun loadClasses(page: Int = _uiState.value.pagination.page) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val q = _uiState.value.searchQuery.ifBlank { null }
            val tahun = _uiState.value.tahunAjaranQuery.ifBlank { null }
            val res = repository.getClasses(page, 20, q, tahun)

            res.onSuccess { r ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        classes = r.data,
                        pagination = r.pagination
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memuat kelas", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        loadClasses(page = 1)
    }

    fun createClass(nama: String, tahunAjaran: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.createClass(nama, tahunAjaran)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Kelas berhasil dibuat", ToastType.SUCCESS)
                    )
                }
                loadClasses(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal membuat kelas", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun updateClass(id: String, nama: String, tahunAjaran: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.updateClass(id, nama, tahunAjaran)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Kelas berhasil diperbarui", ToastType.SUCCESS)
                    )
                }
                loadClasses()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memperbarui kelas", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun deleteClass(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.deleteClass(id)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Kelas berhasil dihapus", ToastType.SUCCESS)
                    )
                }
                loadClasses(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal menghapus kelas", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun openMembersManager(classDto: ClassDto) {
        _uiState.update { it.copy(activeClassForMembers = classDto) }
        loadMembers(classDto.id)
        loadCandidateUsers()
    }

    fun closeMembersManager() {
        _uiState.update { it.copy(activeClassForMembers = null, currentMembers = emptyList()) }
        loadClasses()
    }

    private fun loadMembers(classId: String) {
        viewModelScope.launch {
            val res = repository.getClassMembers(classId)
            res.onSuccess { members ->
                _uiState.update { it.copy(currentMembers = members) }
            }
        }
    }

    private fun loadCandidateUsers() {
        viewModelScope.launch {
            val res = repository.getUsers(page = 1, pageSize = 100)
            res.onSuccess { paginated ->
                _uiState.update { it.copy(availableUsersForMember = paginated.data.filter { u -> u.role == "siswa" || u.role == "guru" }) }
            }
        }
    }

    fun addMember(userId: String) {
        val classId = _uiState.value.activeClassForMembers?.id ?: return
        viewModelScope.launch {
            val res = repository.addClassMembers(classId, listOf(userId))
            res.onSuccess {
                _uiState.update {
                    it.copy(toast = ToastMessage("Anggota berhasil ditambahkan", ToastType.SUCCESS))
                }
                loadMembers(classId)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(toast = ToastMessage(err.message ?: "Gagal menambah anggota", ToastType.ERROR))
                }
            }
        }
    }

    fun removeMember(userId: String) {
        val classId = _uiState.value.activeClassForMembers?.id ?: return
        viewModelScope.launch {
            val res = repository.removeClassMember(classId, userId)
            res.onSuccess {
                _uiState.update {
                    it.copy(toast = ToastMessage("Anggota berhasil dikeluarkan", ToastType.SUCCESS))
                }
                loadMembers(classId)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(toast = ToastMessage(err.message ?: "Gagal menghapus anggota", ToastType.ERROR))
                }
            }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
