package com.absensi.app.ui.operator

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absensi.app.data.remote.dto.LocationDto
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.repository.MasterDataRepository
import com.absensi.app.ui.common.ToastMessage
import com.absensi.app.ui.common.ToastType
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationsUiState(
    val isLoading: Boolean = false,
    val locations: List<LocationDto> = emptyList(),
    val pagination: PaginationMeta = PaginationMeta(1, 20, 0, 1),
    val searchQuery: String = "",
    val activeFilter: String? = null,
    val isLocating: Boolean = false,
    val toast: ToastMessage? = null
)

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val repository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationsUiState())
    val uiState: StateFlow<LocationsUiState> = _uiState.asStateFlow()

    init {
        loadLocations()
    }

    fun loadLocations(page: Int = _uiState.value.pagination.page) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val q = _uiState.value.searchQuery.ifBlank { null }
            val filter = _uiState.value.activeFilter
            val res = repository.getLocations(page, 20, q, filter)

            res.onSuccess { r ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        locations = r.data,
                        pagination = r.pagination
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memuat lokasi", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        loadLocations(page = 1)
    }

    fun onFilterActiveChanged(filter: String?) {
        _uiState.update { it.copy(activeFilter = filter) }
        loadLocations(page = 1)
    }

    fun createLocation(nama: String, latitude: Double, longitude: Double, radiusM: Int, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.createLocation(nama, latitude, longitude, radiusM, isActive)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Lokasi berhasil ditambahkan", ToastType.SUCCESS)
                    )
                }
                loadLocations(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal menambah lokasi", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun updateLocation(id: String, nama: String, latitude: Double, longitude: Double, radiusM: Int, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.updateLocation(id, nama, latitude, longitude, radiusM, isActive)

            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage("Lokasi berhasil diperbarui", ToastType.SUCCESS)
                    )
                }
                loadLocations()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal memperbarui lokasi", ToastType.ERROR)
                    )
                }
            }
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.deleteLocation(id)

            res.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(msg, ToastType.SUCCESS)
                    )
                }
                loadLocations(page = 1)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = ToastMessage(err.message ?: "Gagal menghapus lokasi", ToastType.ERROR)
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(context: Context, onObtained: (lat: Double, lng: Double) -> Unit) {
        _uiState.update { it.copy(isLocating = true) }
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    _uiState.update { it.copy(isLocating = false) }
                    if (loc != null) {
                        onObtained(loc.latitude, loc.longitude)
                        _uiState.update {
                            it.copy(toast = ToastMessage("Koordinat saat ini berhasil diambil", ToastType.SUCCESS))
                        }
                    } else {
                        // Coba ambil last location jika getCurrentLocation null
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                onObtained(lastLoc.latitude, lastLoc.longitude)
                                _uiState.update {
                                    it.copy(toast = ToastMessage("Koordinat saat ini berhasil diambil", ToastType.SUCCESS))
                                }
                            } else {
                                _uiState.update {
                                    it.copy(toast = ToastMessage("Gagal memperoleh koordinat GPS. Pastikan lokasi aktif.", ToastType.ERROR))
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            toast = ToastMessage("Gagal mengambil lokasi: ${e.message}", ToastType.ERROR)
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLocating = false,
                    toast = ToastMessage("Gagal mengakses sensor lokasi", ToastType.ERROR)
                )
            }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
