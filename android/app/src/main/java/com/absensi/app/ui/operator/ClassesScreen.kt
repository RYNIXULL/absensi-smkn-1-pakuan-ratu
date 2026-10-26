package com.absensi.app.ui.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.ui.common.AppConfirmDialog
import com.absensi.app.ui.common.AppPaginationControl
import com.absensi.app.ui.common.AppToastHost
import com.absensi.app.ui.common.AppTooltip

@Composable
fun ClassesScreen(
    viewModel: ClassesViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedClassToEdit by remember { mutableStateOf<ClassDto?>(null) }
    var selectedClassToDelete by remember { mutableStateOf<ClassDto?>(null) }

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
                    text = "Manajemen Kelas",
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
                placeholder = { Text("Cari nama kelas atau tahun ajaran...", color = Color(0xFF64748B)) },
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

            // Classes List
            if (uiState.isLoading && uiState.classes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (uiState.classes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Class, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada data kelas", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.classes) { classItem ->
                        ClassItemCard(
                            classDto = classItem,
                            onEdit = {
                                selectedClassToEdit = classItem
                                showAddEditDialog = true
                            },
                            onDelete = {
                                selectedClassToDelete = classItem
                            },
                            onManageMembers = {
                                viewModel.openMembersManager(classItem)
                            }
                        )
                    }
                }

                AppPaginationControl(
                    currentPage = uiState.pagination.page,
                    totalPages = uiState.pagination.totalPages,
                    onPrevious = { viewModel.loadClasses(uiState.pagination.page - 1) },
                    onNext = { viewModel.loadClasses(uiState.pagination.page + 1) }
                )
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                selectedClassToEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFF2563EB),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            AppTooltip(tooltip = "Tambah Kelas Baru") {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Kelas")
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
            AddEditClassDialog(
                initialClass = selectedClassToEdit,
                onDismiss = { showAddEditDialog = false },
                onConfirm = { nama, tahunAjaran ->
                    if (selectedClassToEdit != null) {
                        viewModel.updateClass(selectedClassToEdit!!.id, nama, tahunAjaran)
                    } else {
                        viewModel.createClass(nama, tahunAjaran)
                    }
                    showAddEditDialog = false
                }
            )
        }

        // Delete Confirm Dialog
        selectedClassToDelete?.let { c ->
            AppConfirmDialog(
                show = true,
                title = "Hapus Kelas",
                message = "Yakin ingin menghapus kelas '${c.nama}'? Penghapusan akan dicegah jika kelas masih memiliki jadwal atau anggota.",
                onConfirm = { viewModel.deleteClass(c.id) },
                onDismiss = { selectedClassToDelete = null }
            )
        }

        // Class Members Management Dialog
        uiState.activeClassForMembers?.let { activeClass ->
            ClassMembersManagerDialog(
                classDto = activeClass,
                uiState = uiState,
                onAddMember = { userId -> viewModel.addMember(userId) },
                onRemoveMember = { userId -> viewModel.removeMember(userId) },
                onDismiss = { viewModel.closeMembersManager() }
            )
        }
    }
}

@Composable
fun ClassItemCard(
    classDto: ClassDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageMembers: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Class, contentDescription = null, tint = Color(0xFF38BDF8))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = classDto.nama, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Tahun Ajaran: ${classDto.tahun_ajaran}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }

                AppTooltip(tooltip = "Edit data kelas") {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                }
                AppTooltip(tooltip = "Hapus kelas ini") {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppTooltip(tooltip = "Buka daftar anggota dan kelola siswa") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable(onClick = onManageMembers)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${classDto._count?.members ?: 0} Anggota terdaftar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Kelola Anggota",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditClassDialog(
    initialClass: ClassDto?,
    onDismiss: () -> Unit,
    onConfirm: (nama: String, tahunAjaran: String) -> Unit
) {
    var nama by remember { mutableStateOf(initialClass?.nama ?: "") }
    var tahunAjaran by remember { mutableStateOf(initialClass?.tahun_ajaran ?: "2024/2025") }

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
                text = if (initialClass == null) "Tambah Kelas Baru" else "Edit Kelas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text("Nama Kelas (misal: X RPL 1)", color = Color(0xFF94A3B8)) },
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

            OutlinedTextField(
                value = tahunAjaran,
                onValueChange = { tahunAjaran = it },
                label = { Text("Tahun Ajaran (misal: 2024/2025)", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                    Text("Batal", color = Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        if (nama.isNotBlank() && tahunAjaran.isNotBlank()) {
                            onConfirm(nama.trim(), tahunAjaran.trim())
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

@Composable
fun ClassMembersManagerDialog(
    classDto: ClassDto,
    uiState: ClassesUiState,
    onAddMember: (userId: String) -> Unit,
    onRemoveMember: (userId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kelola Anggota",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(text = "Kelas: ${classDto.nama}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${uiState.currentMembers.size} Anggota",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Button(
                    onClick = { showAddPicker = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.currentMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada anggota di kelas ini", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.currentMembers) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = member.user.nama, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    text = "${member.user.role?.uppercase()} • ${member.user.nomor_induk}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            AppTooltip(tooltip = "Keluarkan anggota dari kelas") {
                                IconButton(onClick = { onRemoveMember(member.user_id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Keluarkan", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPicker) {
        val existingUserIds = remember(uiState.currentMembers) {
            uiState.currentMembers.map { it.user_id }.toSet()
        }
        val candidates = remember(uiState.availableUsersForMember, existingUserIds) {
            uiState.availableUsersForMember.filter { it.id !in existingUserIds }
        }

        Dialog(onDismissRequest = { showAddPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Pilih Pengguna untuk Ditambahkan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (candidates.isEmpty()) {
                    Text("Semua siswa/guru sudah menjadi anggota kelas", color = Color(0xFF94A3B8), fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates) { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        onAddMember(candidate.id)
                                        showAddPicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = candidate.nama, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "${candidate.role.uppercase()} • ${candidate.nomor_induk}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Pilih", tint = Color(0xFF38BDF8))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showAddPicker = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        }
    }
}
