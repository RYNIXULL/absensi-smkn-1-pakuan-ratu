package com.absensi.app.ui.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.SubjectDto
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

data class SubjectsUiState(
    val isLoading: Boolean = false,
    val subjects: List<SubjectDto> = emptyList(),
    val pagination: PaginationMeta = PaginationMeta(1, 20, 0, 1),
    val searchQuery: String = "",
    val toast: ToastMessage? = null
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val repository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsUiState())
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    fun loadSubjects(page: Int = _uiState.value.pagination.page) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val q = _uiState.value.searchQuery.ifBlank { null }
            val res = repository.getSubjects(page, 20, q)

            res.onSuccess { r ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        subjects = r.data,
                        pagination = r.pagination
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memuat mata pelajaran", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        loadSubjects(page = 1)
    }

    fun createSubject(nama: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.createSubject(nama)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Mata pelajaran berhasil ditambahkan", ToastType.SUCCESS)
                    )
                }
                loadSubjects(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal menambah mapel", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun updateSubject(id: String, nama: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.updateSubject(id, nama)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Mata pelajaran berhasil diperbarui", ToastType.SUCCESS)
                    )
                }
                loadSubjects()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memperbarui mapel", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.deleteSubject(id)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Mata pelajaran berhasil dihapus", ToastType.SUCCESS)
                    )
                }
                loadSubjects(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal menghapus mapel", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
