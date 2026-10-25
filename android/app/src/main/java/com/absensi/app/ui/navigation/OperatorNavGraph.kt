package com.absensi.app.ui.navigation

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OperatorHomeScreen(
    onNavigateToUsers: () -> Unit,
    onNavigateToClasses: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToLeaveApproval: () -> Unit,
    onNavigateToRekap: () -> Unit,
    onNavigateToAuditLog: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Dashboard Operator",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Kelola master data sekolah, pengguna, geofence presensi, dan jadwal pelajaran.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Menu Items Section Title
            item {
                Text(
                    text = "Master Data & Pengaturan",
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // 1. Pengguna
            item {
                OperatorMenuCard(
                    title = "Manajemen Pengguna",
                    subtitle = "Siswa, Guru, Operator & Impor Excel",
                    icon = Icons.Default.People,
                    iconTint = Color(0xFF38BDF8),
                    onClick = onNavigateToUsers
                )
            }

            // 2. Kelas
            item {
                OperatorMenuCard(
                    title = "Manajemen Kelas",
                    subtitle = "Daftar kelas & anggota siswa/wali",
                    icon = Icons.Default.School,
                    iconTint = Color(0xFF818CF8),
                    onClick = onNavigateToClasses
                )
            }

            // 3. Mata Pelajaran
            item {
                OperatorMenuCard(
                    title = "Mata Pelajaran",
                    subtitle = "Kurikulum & daftar mata pelajaran",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconTint = Color(0xFF34D399),
                    onClick = onNavigateToSubjects
                )
            }

            // 4. Lokasi Presensi
            item {
                OperatorMenuCard(
                    title = "Lokasi Presensi",
                    subtitle = "Koordinat GPS & radius geofencing",
                    icon = Icons.Default.LocationOn,
                    iconTint = Color(0xFFFBBF24),
                    onClick = onNavigateToLocations
                )
            }

            // 5. Jadwal Pelajaran
            item {
                OperatorMenuCard(
                    title = "Jadwal Pelajaran",
                    subtitle = "Alokasi kelas, mapel, guru & deteksi bentrok",
                    icon = Icons.Default.CalendarMonth,
                    iconTint = Color(0xFFF472B6),
                    onClick = onNavigateToSchedules
                )
            }

            // 6. Sesi Absensi
            item {
                OperatorMenuCard(
                    title = "Sesi Absensi",
                    subtitle = "Buka/tutup sesi dan pantau kehadiran siswa",
                    icon = Icons.Default.MeetingRoom,
                    iconTint = Color(0xFF10B981),
                    onClick = onNavigateToSessions
                )
            }

            // 7. Persetujuan Izin Siswa
            item {
                OperatorMenuCard(
                    title = "Persetujuan Izin Siswa",
                    subtitle = "Tinjau dan verifikasi permohonan izin seluruh siswa",
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    iconTint = Color(0xFFA78BFA),
                    onClick = onNavigateToLeaveApproval
                )
            }

            // 8. Rekapitulasi & Ekspor Excel
            item {
                OperatorMenuCard(
                    title = "Rekapitulasi & Ekspor Excel",
                    subtitle = "Laporan kehadiran per periode, kelas & unduh .xlsx",
                    icon = Icons.Default.Assessment,
                    iconTint = Color(0xFF0284C7),
                    onClick = onNavigateToRekap
                )
            }

            // 9. Log Audit Sistem
            item {
                OperatorMenuCard(
                    title = "Log Audit Sistem",
                    subtitle = "Riwayat audit keamanan, perubahan data & verifikasi aksi",
                    icon = Icons.Default.Security,
                    iconTint = Color(0xFFE11D48),
                    onClick = onNavigateToAuditLog
                )
            }

            // Spacer & Logout
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.85f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Keluar dari Akun", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OperatorMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.18f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
