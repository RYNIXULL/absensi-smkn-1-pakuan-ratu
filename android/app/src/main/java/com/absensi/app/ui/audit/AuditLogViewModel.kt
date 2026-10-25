package com.absensi.app.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.AuditLogDto
import com.absensi.app.data.remote.dto.AuditStatsDto
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.repository.AuditRepository
import com.absensi.app.ui.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuditLogUiState(
    val isLoading: Boolean = false,
    val logs: List<AuditLogDto> = emptyList(),
    val stats: AuditStatsDto? = null,
    val pagination: PaginationMeta? = null,
    val page: Int = 1,
    val selectedEntity: String? = null,
    val searchQuery: String = "",
    val selectedLogForDetail: AuditLogDto? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditLogUiState())
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
        loadStats()
    }

    fun loadLogs(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, page = page) }

            val currentState = _uiState.value
            val entityParam = if (currentState.selectedEntity == "Semua") null else currentState.selectedEntity
            val queryParam = currentState.searchQuery.takeIf { it.isNotBlank() }

            val result = auditRepository.getAuditLogs(
                page = page,
                pageSize = 20,
                entity = entityParam,
                q = queryParam
            )

            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        logs = res.data,
                        pagination = res.pagination
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

    fun loadStats() {
        viewModelScope.launch {
            val result = auditRepository.getAuditStats()
            result.onSuccess { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    fun setEntityFilter(entity: String?) {
        _uiState.update { it.copy(selectedEntity = entity) }
        loadLogs(page = 1)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadLogs(page = 1)
    }

    fun selectLogForDetail(log: AuditLogDto) {
        _uiState.update { it.copy(selectedLogForDetail = log) }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedLogForDetail = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
