package com.absensi.app.ui.rekap

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.absensi.app.data.remote.dto.AttendanceRecordDto
import com.absensi.app.ui.common.AppPaginationControl
import com.absensi.app.ui.common.AppToastHost
import com.absensi.app.ui.common.ToastMessage
import com.absensi.app.ui.common.ToastType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekapScreen(
    viewModel: RekapViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var expandedClassMenu by remember { mutableStateOf(false) }

    val toast = when {
        uiState.errorMessage != null -> ToastMessage(
            message = uiState.errorMessage ?: "",
            type = ToastType.ERROR
        )
        uiState.successMessage != null -> ToastMessage(
            message = uiState.successMessage ?: "",
            type = ToastType.SUCCESS
        )
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Rekapitulasi Presensi",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Filter data dan unduh rekap berkas Excel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Quick Date Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateOptions = listOf(
                    "semua" to "Semua Periode",
                    "hari_ini" to "Hari Ini",
                    "7_hari" to "7 Hari Terakhir",
                    "30_hari" to "30 Hari Terakhir"
                )
                items(dateOptions) { (key, label) ->
                    val isSelected = uiState.quickDateFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.6f)
                                else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setQuickDateFilter(key) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Class Dropdown Selector
            val selectedClass = uiState.classes.find { it.id == uiState.selectedClassId }
            val classDisplayText = selectedClass?.nama ?: "Semua Kelas"

            ExposedDropdownMenuBox(
                expanded = expandedClassMenu,
                onExpandedChange = { expandedClassMenu = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                OutlinedTextField(
                    value = classDisplayText,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClassMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    )
                )

                ExposedDropdownMenu(
                    expanded = expandedClassMenu,
                    onDismissRequest = { expandedClassMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = { Text("Semua Kelas", color = Color.White, fontWeight = FontWeight.Medium) },
                        onClick = {
                            viewModel.setClassFilter(null)
                            expandedClassMenu = false
                        }
                    )
                    uiState.classes.forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(cls.nama, color = Color.White) },
                            onClick = {
                                viewModel.setClassFilter(cls.id)
                                expandedClassMenu = false
                            }
                        )
                    }
                }
            }

            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOptions = listOf(
                    null to "Semua",
                    "hadir" to "Hadir",
                    "terlambat" to "Terlambat",
                    "izin" to "Izin",
                    "sakit" to "Sakit",
                    "alpa" to "Alpa"
                )
                items(statusOptions) { (key, label) ->
                    val isSelected = uiState.selectedStatus == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF10B981).copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF10B981).copy(alpha = 0.6f)
                                else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setStatusFilter(key) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF34D399) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Summary Statistics Overview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hadir
                StatBadge(
                    label = "Hadir",
                    count = uiState.hadirCount,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                // Terlambat
                StatBadge(
                    label = "Terlambat",
                    count = uiState.terlambatCount,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                // Izin / Sakit
                StatBadge(
                    label = "Izin/Sakit",
                    count = uiState.izinCount + uiState.sakitCount,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                // Alpa
                StatBadge(
                    label = "Alpa",
                    count = uiState.alpaCount,
                    color = Color(0xFFF43F5E),
                    modifier = Modifier.weight(1f)
                )
            }

            // Record List / Empty / Loading
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (uiState.attendances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak ada rekam absensi untuk filter ini",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.attendances) { att ->
                        RekapRecordCard(att = att)
                    }

                    item {
                        uiState.pagination?.let { meta ->
                            AppPaginationControl(
                                currentPage = meta.page,
                                totalPages = meta.totalPages,
                                onPrevious = { viewModel.loadAttendances(meta.page - 1) },
                                onNext = { viewModel.loadAttendances(meta.page + 1) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Download Excel Action Button
            Button(
                onClick = {
                    viewModel.downloadExcelReport(context) { file ->
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(
                                    uri,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(intent, "Buka Berkas Excel"))
                        } catch (_: Exception) {
                            // File saved notification is already displayed by Toast
                        }
                    }
                },
                enabled = !uiState.isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF059669),
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                if (uiState.isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Unduh Laporan Excel (.xlsx)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Custom Toast Host
        AppToastHost(
            toast = toast,
            onDismiss = {
                viewModel.dismissError()
                viewModel.dismissSuccess()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun StatBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RekapRecordCard(att: AttendanceRecordDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Student info & Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = att.student?.nama ?: "Siswa",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "NIS: ${att.student?.nomor_induk ?: "-"}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Status Badge
                val (statusBg, statusBorder, statusColor, statusText) = when (att.status.lowercase()) {
                    "hadir" -> listOf(
                        Color(0xFF10B981).copy(alpha = 0.2f),
                        Color(0xFF10B981).copy(alpha = 0.5f),
                        Color(0xFF34D399),
                        "Hadir"
                    )
                    "terlambat" -> listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.2f),
                        Color(0xFFF59E0B).copy(alpha = 0.5f),
                        Color(0xFFFBBF24),
                        "Terlambat"
                    )
                    "izin" -> listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.2f),
                        Color(0xFF38BDF8).copy(alpha = 0.5f),
                        Color(0xFF7DD3FC),
                        "Izin"
                    )
                    "sakit" -> listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        Color(0xFF8B5CF6).copy(alpha = 0.5f),
                        Color(0xFFA78BFA),
                        "Sakit"
                    )
                    else -> listOf(
                        Color(0xFFF43F5E).copy(alpha = 0.2f),
                        Color(0xFFF43F5E).copy(alpha = 0.5f),
                        Color(0xFFFB7185),
                        "Alpa"
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg as Color)
                        .border(1.dp, statusBorder as Color, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText as String,
                        color = statusColor as Color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Class, Subject, and Date
            val subName = att.session?.schedule?.subject?.name ?: "Pelajaran"
            val clsName = att.session?.schedule?.classInfo?.name ?: "Kelas"
            val tanggal = att.session?.tanggal ?: ""

            Text(
                text = "$subName • $clsName • $tanggal",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Check-in details if present
            if (!att.checkin_at.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val distanceInfo = if (att.distance_m != null) " (Jarak: ${Math.round(att.distance_m)}m)" else ""
                Text(
                    text = "Jam: ${att.checkin_at}$distanceInfo",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            // Catatan
            if (!att.catatan.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Catatan: ${att.catatan}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}
