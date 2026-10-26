package com.absensi.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppPaginationControl(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = currentPage > 1,
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(if (currentPage > 1) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
            )
        ) {
            AppTooltip(tooltip = "Halaman sebelumnya") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Sebelumnya",
                    tint = if (currentPage > 1) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Hal $currentPage dari $totalPages",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.width(16.dp))

        OutlinedButton(
            onClick = onNext,
            enabled = currentPage < totalPages,
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(if (currentPage < totalPages) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
            )
        ) {
            AppTooltip(tooltip = "Halaman berikutnya") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Berikutnya",
                    tint = if (currentPage < totalPages) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}
