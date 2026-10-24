package com.absensi.app.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.LeaveRequestDto
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.repository.LeaveRepository
import com.absensi.app.ui.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaveApprovalUiState(
    val isLoading: Boolean = false,
    val isProcessingAction: Boolean = false,
    val requests: List<LeaveRequestDto> = emptyList(),
    val pagination: PaginationMeta? = null,
    val selectedStatus: String? = "menunggu",
    val reviewTarget: LeaveRequestDto? = null,
    val reviewAction: String? = null, // "disetujui" or "ditolak"
    val reviewCatatan: String = "",
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class LeaveApprovalViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaveApprovalUiState())
    val uiState: StateFlow<LeaveApprovalUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadRequests(page = 1)
    }

    fun loadRequests(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val status = _uiState.value.selectedStatus
            val result = leaveRepository.getLeaveRequests(
                status = status,
                page = page,
                pageSize = 20
            )

            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        requests = response.data,
                        pagination = response.pagination
                    )
                }
            }.onFailure { err ->
                val msg = ErrorMapper.parseError(err)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun openReviewDialog(request: LeaveRequestDto, action: String) {
        _uiState.update {
            it.copy(
                reviewTarget = request,
                reviewAction = action,
                reviewCatatan = ""
            )
        }
    }

    fun closeReviewDialog() {
        _uiState.update {
            it.copy(
                reviewTarget = null,
                reviewAction = null,
                reviewCatatan = ""
            )
        }
    }

    fun setReviewCatatan(catatan: String) {
        _uiState.update { it.copy(reviewCatatan = catatan) }
    }

    fun submitReview() {
        val target = _uiState.value.reviewTarget ?: return
        val action = _uiState.value.reviewAction ?: return
        val catatan = _uiState.value.reviewCatatan.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true, errorMessage = null, successMessage = null) }

            val result = leaveRepository.reviewLeaveRequest(
                id = target.id,
                status = action,
                catatan = catatan
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isProcessingAction = false,
                        reviewTarget = null,
                        reviewAction = null,
                        reviewCatatan = "",
                        successMessage = "Pengajuan izin berhasil ${if (action == "disetujui") "disetujui" else "ditolak"}."
                    )
                }
                loadRequests(page = _uiState.value.pagination?.page ?: 1)
            }.onFailure { err ->
                val msg = ErrorMapper.parseError(err)
                _uiState.update {
                    it.copy(
                        isProcessingAction = false,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun getAttachmentUrl(leaveId: String, onUrlLoaded: (String) -> Unit) {
        viewModelScope.launch {
            val result = leaveRepository.getAttachment(leaveId)
            result.onSuccess { signed ->
                onUrlLoaded(signed.url)
            }.onFailure { err ->
                val msg = ErrorMapper.parseError(err)
                _uiState.update { it.copy(errorMessage = msg) }
            }
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
