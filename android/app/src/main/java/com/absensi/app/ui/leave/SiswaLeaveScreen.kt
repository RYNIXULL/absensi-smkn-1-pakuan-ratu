package com.absensi.app.ui.leave

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absensi.app.data.remote.dto.LeaveRequestDto
import com.absensi.app.ui.common.AppPaginationControl
import com.absensi.app.ui.common.AppToastHost
import com.absensi.app.ui.common.AppTooltip
import com.absensi.app.ui.common.ToastMessage
import com.absensi.app.ui.common.ToastType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiswaLeaveScreen(
    viewModel: SiswaLeaveViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                var fileName = "lampiran"
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex)
                        }
                    }
                }
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                if (bytes != null) {
                    viewModel.setAttachment(bytes, fileName, mimeType)
                }
            } catch (e: Exception) {
                // Fallback handling
            }
        }
    }

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
            .statusBarsPadding()
            .navigationBarsPadding()
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
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

                Column {
                    Text(
                        text = "Izin & Sakit Siswa",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pengajuan ketidakhadiran resmi",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Tab Switcher (Glassmorphism)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                // Tab 0: Riwayat
                AppTooltip(tooltip = "Lihat riwayat pengajuan izin dan sakit") {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (uiState.activeTab == 0) Color.White.copy(alpha = 0.2f) else Color.Transparent
                            )
                            .clickable { viewModel.setTab(0) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = if (uiState.activeTab == 0) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Riwayat",
                                color = if (uiState.activeTab == 0) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (uiState.activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Tab 1: Ajukan Izin
                AppTooltip(tooltip = "Buka formulir pengajuan izin/sakit baru") {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (uiState.activeTab == 1) Color.White.copy(alpha = 0.2f) else Color.Transparent
                            )
                            .clickable { viewModel.setTab(1) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = if (uiState.activeTab == 1) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ajukan Izin",
                                color = if (uiState.activeTab == 1) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (uiState.activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Content based on Tab
            if (uiState.activeTab == 0) {
                // Tab 0: Riwayat Pengajuan
                SiswaLeaveHistoryTab(
                    uiState = uiState,
                    onSelectFilter = { viewModel.setStatusFilter(it) },
                    onPageChange = { viewModel.loadLeaveRequests(it) },
                    onViewAttachment = { leaveId ->
                        viewModel.viewAttachment(leaveId) { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                )
            } else {
                // Tab 1: Form Pengajuan
                SiswaLeaveFormTab(
                    uiState = uiState,
                    onSelectSession = { viewModel.selectSession(it) },
                    onSelectTipe = { viewModel.setTipe(it) },
                    onAlasanChange = { viewModel.setAlasan(it) },
                    onPickFile = { filePickerLauncher.launch("*/*") },
                    onRemoveFile = { viewModel.removeAttachment() },
                    onSubmit = { viewModel.submitLeaveRequest() }
                )
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
private fun SiswaLeaveHistoryTab(
    uiState: SiswaLeaveUiState,
    onSelectFilter: (String?) -> Unit,
    onPageChange: (Int) -> Unit,
    onViewAttachment: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf(
                null to "Semua",
                "menunggu" to "Menunggu",
                "disetujui" to "Disetujui",
                "ditolak" to "Ditolak"
            )
            filterOptions.forEach { (status, label) ->
                val isSelected = uiState.selectedStatusFilter == status
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
                        .clickable { onSelectFilter(status) }
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

        if (uiState.isLoadingRequests) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (uiState.leaveRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada pengajuan izin",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.leaveRequests) { request ->
                    LeaveRequestCard(request = request, onViewAttachment = onViewAttachment)
                }

                item {
                    uiState.pagination?.let { meta ->
                        AppPaginationControl(
                            currentPage = meta.page,
                            totalPages = meta.totalPages,
                            onPrevious = { onPageChange(meta.page - 1) },
                            onNext = { onPageChange(meta.page + 1) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveRequestCard(
    request: LeaveRequestDto,
    onViewAttachment: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Type Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge (Izin / Sakit)
                val (typeBg, typeBorder, typeColor, typeIcon) = if (request.tipe == "sakit") {
                    listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        Color(0xFF8B5CF6).copy(alpha = 0.5f),
                        Color(0xFFA78BFA),
                        Icons.Default.MedicalServices
                    )
                } else {
                    listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.2f),
                        Color(0xFF38BDF8).copy(alpha = 0.5f),
                        Color(0xFF7DD3FC),
                        Icons.Default.EventBusy
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeBg as Color)
                        .border(1.dp, typeBorder as Color, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = typeIcon as androidx.compose.ui.graphics.vector.ImageVector,
                        contentDescription = null,
                        tint = typeColor as Color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = request.tipe.replaceFirstChar { it.uppercase() },
                        color = typeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status Badge
                val (statusBg, statusBorder, statusColor, statusIcon, statusText) = when (request.status) {
                    "disetujui" -> listOf(
                        Color(0xFF10B981).copy(alpha = 0.2f),
                        Color(0xFF10B981).copy(alpha = 0.5f),
                        Color(0xFF34D399),
                        Icons.Default.CheckCircle,
                        "Disetujui"
                    )
                    "ditolak" -> listOf(
                        Color(0xFFF43F5E).copy(alpha = 0.2f),
                        Color(0xFFF43F5E).copy(alpha = 0.5f),
                        Color(0xFFFB7185),
                        Icons.Default.Cancel,
                        "Ditolak"
                    )
                    else -> listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.2f),
                        Color(0xFFF59E0B).copy(alpha = 0.5f),
                        Color(0xFFFBBF24),
                        Icons.Default.Schedule,
                        "Menunggu"
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg as Color)
                        .border(1.dp, statusBorder as Color, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon as androidx.compose.ui.graphics.vector.ImageVector,
                        contentDescription = null,
                        tint = statusColor as Color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText as String,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Class, Subject, Date
            val subjectName = request.session?.schedule?.subject?.name ?: "Pelajaran"
            val className = request.session?.schedule?.classInfo?.name ?: "Kelas"
            val tanggal = request.session?.tanggal ?: ""

            Text(
                text = "$subjectName • $className",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            if (tanggal.isNotBlank()) {
                Text(
                    text = "Tanggal Sesi: $tanggal",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alasan
            Text(
                text = request.alasan,
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Review note if exists
            if (!request.review_catatan.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Catatan: ${request.review_catatan}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            // Attachment Link
            if (!request.attachment_key.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onViewAttachment(request.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFF38BDF8).copy(alpha = 0.4f), Color(0xFF818CF8).copy(alpha = 0.4f))
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF38BDF8).copy(alpha = 0.08f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lihat Dokumen Lampiran",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiswaLeaveFormTab(
    uiState: SiswaLeaveUiState,
    onSelectSession: (String) -> Unit,
    onSelectTipe: (String) -> Unit,
    onAlasanChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onSubmit: () -> Unit
) {
    var expandedSessionMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Pilih Sesi
        Column {
            Text(
                text = "Pilih Sesi Pelajaran",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val selectedSession = uiState.availableSessions.find { it.id == uiState.selectedSessionId }
            val sessionDisplayText = if (selectedSession != null) {
                val sub = selectedSession.schedule?.subject?.name ?: "Pelajaran"
                val cls = selectedSession.schedule?.classInfo?.name ?: "Kelas"
                "$sub • $cls (${selectedSession.tanggal})"
            } else {
                "Pilih sesi yang ingin diajukan..."
            }

            ExposedDropdownMenuBox(
                expanded = expandedSessionMenu,
                onExpandedChange = { expandedSessionMenu = it }
            ) {
                OutlinedTextField(
                    value = sessionDisplayText,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSessionMenu) },
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
                    expanded = expandedSessionMenu,
                    onDismissRequest = { expandedSessionMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    if (uiState.availableSessions.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Tidak ada sesi tersedia", color = Color(0xFF94A3B8)) },
                            onClick = { expandedSessionMenu = false }
                        )
                    } else {
                        uiState.availableSessions.forEach { sess ->
                            val sub = sess.schedule?.subject?.name ?: "Pelajaran"
                            val cls = sess.schedule?.classInfo?.name ?: "Kelas"
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("$sub • $cls", color = Color.White, fontWeight = FontWeight.Medium)
                                        Text("${sess.tanggal} (Status: ${sess.status})", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    onSelectSession(sess.id)
                                    expandedSessionMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Pilih Tipe: Izin / Sakit
        Column {
            Text(
                text = "Jenis Permohonan",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Izin Card
                val isIzin = uiState.tipe == "izin"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isIzin) Color(0xFF38BDF8).copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            1.dp,
                            if (isIzin) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTipe("izin") }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = if (isIzin) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Izin",
                            color = if (isIzin) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (isIzin) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }

                // Sakit Card
                val isSakit = uiState.tipe == "sakit"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSakit) Color(0xFF8B5CF6).copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            1.dp,
                            if (isSakit) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTipe("sakit") }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = if (isSakit) Color(0xFFA78BFA) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sakit",
                            color = if (isSakit) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (isSakit) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 3. Alasan
        Column {
            Text(
                text = "Alasan Ketidakhadiran",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = uiState.alasan,
                onValueChange = onAlasanChange,
                placeholder = {
                    Text(
                        text = "Tuliskan alasan permohonan izin secara lengkap (minimal 5 karakter)...",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
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
        }

        // 4. Berkas Lampiran (Surat Dokter / Bukti)
        Column {
            Text(
                text = "Dokumen Pendukung (Opsional)",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.attachmentName != null) {
                // Showing chosen file chip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF34D399).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.attachmentName ?: "Lampiran",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onRemoveFile,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus file",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onPickFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.15f))
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.04f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unggah Surat Dokter / Bukti (PDF / Foto)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 5. Submit Button
        Button(
            onClick = onSubmit,
            enabled = !uiState.isSubmitting && uiState.selectedSessionId.isNotBlank() && uiState.alasan.trim().length >= 5,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0284C7),
                disabledContainerColor = Color.White.copy(alpha = 0.1f)
            )
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Kirim Permohonan Izin",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
