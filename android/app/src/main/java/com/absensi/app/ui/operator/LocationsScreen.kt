package com.absensi.app.ui.operator

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.absensi.app.data.remote.dto.LocationDto
import com.absensi.app.ui.common.AppConfirmDialog
import com.absensi.app.ui.common.AppPaginationControl
import com.absensi.app.ui.common.AppToastHost
import com.absensi.app.ui.common.AppTooltip

@Composable
fun LocationsScreen(
    viewModel: LocationsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedLocationToEdit by remember { mutableStateOf<LocationDto?>(null) }
    var selectedLocationToDelete by remember { mutableStateOf<LocationDto?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    AppTooltip(tooltip = "Kembali ke Dashboard") {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Titik Lokasi Absensi",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari nama lokasi...", color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        AppTooltip(tooltip = "Hapus teks pencarian") {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus", tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Locations List
            if (uiState.isLoading && uiState.locations.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (uiState.locations.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada titik lokasi", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.locations) { location ->
                        LocationCard(
                            location = location,
                            onEdit = {
                                selectedLocationToEdit = location
                                showAddEditDialog = true
                            },
                            onDelete = { selectedLocationToDelete = location }
                        )
                    }
                }

                AppPaginationControl(
                    currentPage = uiState.pagination.page,
                    totalPages = uiState.pagination.totalPages,
                    onPrevious = { viewModel.loadLocations(uiState.pagination.page - 1) },
                    onNext = { viewModel.loadLocations(uiState.pagination.page + 1) }
                )
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                selectedLocationToEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFF2563EB),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            AppTooltip(tooltip = "Tambah Titik Lokasi Baru") {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Lokasi")
            }
        }

        // Toast Host
        AppToastHost(
            toast = uiState.toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditLocationDialog(
                initialLocation = selectedLocationToEdit,
                isLocating = uiState.isLocating,
                onRequestLocation = { onGotLocation ->
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        viewModel.requestCurrentLocation(context, onGotLocation)
                    }
                },
                onDismiss = { showAddEditDialog = false },
                onConfirm = { nama, lat, lng, radius, isActive ->
                    if (selectedLocationToEdit != null) {
                        viewModel.updateLocation(selectedLocationToEdit!!.id, nama, lat, lng, radius, isActive)
                    } else {
                        viewModel.createLocation(nama, lat, lng, radius, isActive)
                    }
                    showAddEditDialog = false
                }
            )
        }

        // Delete Confirm Dialog
        selectedLocationToDelete?.let { loc ->
            AppConfirmDialog(
                show = true,
                title = "Hapus Lokasi",
                message = "Yakin ingin menghapus titik lokasi '${loc.nama}'? Bila lokasi telah digunakan di jadwal, statusnya akan dinonaktifkan secara aman (soft-delete).",
                onConfirm = { viewModel.deleteLocation(loc.id) },
                onDismiss = { selectedLocationToDelete = null }
            )
        }
    }
}

@Composable
fun LocationCard(
    location: LocationDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (location.is_active) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (location.is_active) Color(0xFF34D399) else Color(0xFFF87171)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = location.nama, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (location.is_active) Color(0xFF065F46) else Color(0xFF7F1D1D)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (location.is_active) "Aktif" else "Nonaktif",
                            color = if (location.is_active) Color(0xFFA7F3D0) else Color(0xFFFECACA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Lat: ${location.latitude}, Lng: ${location.longitude}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Text(
                    text = "Radius Maksimal: ${location.radius_m} meter",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            AppTooltip(tooltip = "Edit titik lokasi") {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }
            }
            AppTooltip(tooltip = "Hapus titik lokasi ini") {
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AddEditLocationDialog(
    initialLocation: LocationDto?,
    isLocating: Boolean,
    onRequestLocation: (onLocationObtained: (lat: Double, lng: Double) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (nama: String, latitude: Double, longitude: Double, radiusM: Int, isActive: Boolean) -> Unit
) {
    var nama by remember { mutableStateOf(initialLocation?.nama ?: "") }
    var latText by remember { mutableStateOf(initialLocation?.latitude?.toString() ?: "") }
    var lngText by remember { mutableStateOf(initialLocation?.longitude?.toString() ?: "") }
    var radiusText by remember { mutableStateOf(initialLocation?.radius_m?.toString() ?: "50") }
    var isActive by remember { mutableStateOf(initialLocation?.is_active ?: true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onRequestLocation { lat, lng ->
                latText = lat.toString()
                lngText = lng.toString()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text(
                text = if (initialLocation == null) "Tambah Lokasi Absensi" else "Edit Lokasi Absensi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text("Nama Lokasi (misal: Kampus Utama)", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tombol "Gunakan lokasi saya sekarang"
            AppTooltip(tooltip = "Ambil koordinat GPS perangkat saat ini secara presisi") {
                OutlinedButton(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ),
                    enabled = !isLocating
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mengambil titik GPS...", color = Color(0xFF38BDF8), fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gunakan lokasi saya sekarang", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude (-90..90)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude (-180..180)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = radiusText,
                onValueChange = { radiusText = it },
                label = { Text("Radius (10 - 1000 meter)", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status Aktif", color = Color.White, fontSize = 14.sp)
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0369A1)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                    Text("Batal", color = Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull()
                        val lng = lngText.toDoubleOrNull()
                        val radius = radiusText.toIntOrNull()
                        if (nama.isNotBlank() && lat != null && lng != null && radius != null) {
                            onConfirm(nama.trim(), lat, lng, radius, isActive)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Simpan", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
