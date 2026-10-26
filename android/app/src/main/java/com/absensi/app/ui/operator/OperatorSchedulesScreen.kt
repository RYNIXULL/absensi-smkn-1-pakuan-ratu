package com.absensi.app.ui.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.ui.common.AppConfirmDialog
import com.absensi.app.ui.common.AppPaginationControl
import com.absensi.app.ui.common.AppToastHost
import com.absensi.app.ui.common.AppTooltip

fun getDayName(day: Int): String {
    return when (day) {
        1 -> "Senin"
        2 -> "Selasa"
        3 -> "Rabu"
        4 -> "Kamis"
        5 -> "Jumat"
        6 -> "Sabtu"
        7 -> "Minggu"
        else -> "Hari $day"
    }
}

@Composable
fun OperatorSchedulesScreen(
    viewModel: OperatorSchedulesViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedScheduleToEdit by remember { mutableStateOf<ScheduleDto?>(null) }
    var selectedScheduleToDelete by remember { mutableStateOf<ScheduleDto?>(null) }

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
                        .background(Color.White.copy(alpha = 0.1f))
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
                    text = "Jadwal Pelajaran",
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
                placeholder = { Text("Cari mapel, kelas, guru, lokasi...", color = Color(0xFF64748B)) },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Day Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayChip(
                    label = "Semua Hari",
                    isSelected = uiState.selectedDayOfWeek == null,
                    onClick = { viewModel.onDayFilterChanged(null) }
                )
                (1..7).forEach { d ->
                    DayChip(
                        label = getDayName(d),
                        isSelected = uiState.selectedDayOfWeek == d,
                        onClick = { viewModel.onDayFilterChanged(d) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Schedules List
            if (uiState.isLoading && uiState.schedules.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (uiState.schedules.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada jadwal pada filter ini", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.schedules) { schedule ->
                        ScheduleItemCard(
                            schedule = schedule,
                            onEdit = {
                                selectedScheduleToEdit = schedule
                                showAddEditDialog = true
                            },
                            onDelete = { selectedScheduleToDelete = schedule }
                        )
                    }
                }

                AppPaginationControl(
                    currentPage = uiState.pagination.page,
                    totalPages = uiState.pagination.totalPages,
                    onPrevious = { viewModel.loadSchedules(uiState.pagination.page - 1) },
                    onNext = { viewModel.loadSchedules(uiState.pagination.page + 1) }
                )
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                selectedScheduleToEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFF2563EB),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            AppTooltip(tooltip = "Tambah Jadwal Pelajaran Baru") {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Jadwal")
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
            AddEditScheduleDialog(
                initialSchedule = selectedScheduleToEdit,
                uiState = uiState,
                onDismiss = { showAddEditDialog = false },
                onConfirm = { classId, subjectId, teacherId, locationId, dayOfWeek, start, end ->
                    if (selectedScheduleToEdit != null) {
                        viewModel.updateSchedule(selectedScheduleToEdit!!.id, classId, subjectId, teacherId, locationId, dayOfWeek, start, end)
                    } else {
                        viewModel.createSchedule(classId, subjectId, teacherId, locationId, dayOfWeek, start, end)
                    }
                    showAddEditDialog = false
                }
            )
        }

        // Delete Confirm Dialog
        selectedScheduleToDelete?.let { s ->
            val className = s.`class`?.nama ?: "-"
            AppConfirmDialog(
                show = true,
                title = "Hapus Jadwal",
                message = "Yakin ingin menghapus jadwal ${s.subject?.nama} ($className)? Bila sudah terhubung ke sesi absensi, penghapusan akan dicegah.",
                onConfirm = { viewModel.deleteSchedule(s.id) },
                onDismiss = { selectedScheduleToDelete = null }
            )
        }
    }
}

@Composable
fun DayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF2563EB) else Color.White.copy(alpha = 0.08f))
            .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ScheduleItemCard(
    schedule: ScheduleDto,
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.3f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = getDayName(schedule.day_of_week),
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${schedule.start_time} - ${schedule.end_time}",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                AppTooltip(tooltip = "Edit jadwal pelajaran") {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }
                AppTooltip(tooltip = "Hapus jadwal ini") {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = schedule.subject?.nama ?: "Mata Pelajaran",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val clsName = schedule.`class`?.nama ?: "-"
                Text(
                    text = "Kelas: $clsName",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = schedule.location?.nama ?: "-",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Guru: ${schedule.teacher?.nama ?: "-"}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun AddEditScheduleDialog(
    initialSchedule: ScheduleDto?,
    uiState: OperatorSchedulesUiState,
    onDismiss: () -> Unit,
    onConfirm: (classId: String, subjectId: String, teacherId: String, locationId: String, dayOfWeek: Int, start: String, end: String) -> Unit
) {
    var selectedClassId by remember { mutableStateOf(initialSchedule?.class_id ?: uiState.classes.firstOrNull()?.id ?: "") }
    var selectedSubjectId by remember { mutableStateOf(initialSchedule?.subject_id ?: uiState.subjects.firstOrNull()?.id ?: "") }
    var selectedTeacherId by remember { mutableStateOf(initialSchedule?.teacher_id ?: uiState.teachers.firstOrNull()?.id ?: "") }
    var selectedLocationId by remember { mutableStateOf(initialSchedule?.location_id ?: uiState.locations.firstOrNull()?.id ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(initialSchedule?.day_of_week ?: 1) }
    var startTime by remember { mutableStateOf(initialSchedule?.start_time ?: "07:30") }
    var endTime by remember { mutableStateOf(initialSchedule?.end_time ?: "09:00") }

    var expandedClass by remember { mutableStateOf(false) }
    var expandedSubject by remember { mutableStateOf(false) }
    var expandedTeacher by remember { mutableStateOf(false) }
    var expandedLocation by remember { mutableStateOf(false) }
    var expandedDay by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Text(
                text = if (initialSchedule == null) "Tambah Jadwal Baru" else "Edit Jadwal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Class Selector
            Box {
                val selectedClassName = uiState.classes.find { it.id == selectedClassId }?.nama ?: "Pilih Kelas"
                SelectorField(label = "Kelas", value = selectedClassName, onClick = { expandedClass = true })
                DropdownMenu(expanded = expandedClass, onDismissRequest = { expandedClass = false }) {
                    uiState.classes.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.nama) },
                            onClick = { selectedClassId = c.id; expandedClass = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subject Selector
            Box {
                val selectedSubjectName = uiState.subjects.find { it.id == selectedSubjectId }?.nama ?: "Pilih Mata Pelajaran"
                SelectorField(label = "Mata Pelajaran", value = selectedSubjectName, onClick = { expandedSubject = true })
                DropdownMenu(expanded = expandedSubject, onDismissRequest = { expandedSubject = false }) {
                    uiState.subjects.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.nama) },
                            onClick = { selectedSubjectId = s.id; expandedSubject = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Teacher Selector
            Box {
                val selectedTeacherName = uiState.teachers.find { it.id == selectedTeacherId }?.nama ?: "Pilih Guru"
                SelectorField(label = "Guru Pengajar", value = selectedTeacherName, onClick = { expandedTeacher = true })
                DropdownMenu(expanded = expandedTeacher, onDismissRequest = { expandedTeacher = false }) {
                    uiState.teachers.forEach { t ->
                        DropdownMenuItem(
                            text = { Text("${t.nama} (${t.nomor_induk})") },
                            onClick = { selectedTeacherId = t.id; expandedTeacher = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location Selector
            Box {
                val selectedLocationName = uiState.locations.find { it.id == selectedLocationId }?.nama ?: "Pilih Lokasi"
                SelectorField(label = "Titik Lokasi", value = selectedLocationName, onClick = { expandedLocation = true })
                DropdownMenu(expanded = expandedLocation, onDismissRequest = { expandedLocation = false }) {
                    uiState.locations.forEach { l ->
                        DropdownMenuItem(
                            text = { Text("${l.nama} (Radius ${l.radius_m}m)") },
                            onClick = { selectedLocationId = l.id; expandedLocation = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day Selector
            Box {
                SelectorField(label = "Hari", value = getDayName(dayOfWeek), onClick = { expandedDay = true })
                DropdownMenu(expanded = expandedDay, onDismissRequest = { expandedDay = false }) {
                    (1..7).forEach { d ->
                        DropdownMenuItem(
                            text = { Text(getDayName(d)) },
                            onClick = { dayOfWeek = d; expandedDay = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Times
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Mulai (HH:mm)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
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
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Selesai (HH:mm)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
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

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                    Text("Batal", color = Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        if (selectedClassId.isNotBlank() && selectedSubjectId.isNotBlank() &&
                            selectedTeacherId.isNotBlank() && selectedLocationId.isNotBlank()
                        ) {
                            onConfirm(selectedClassId, selectedSubjectId, selectedTeacherId, selectedLocationId, dayOfWeek, startTime.trim(), endTime.trim())
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
fun SelectorField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = value, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8))
        }
    }
}
