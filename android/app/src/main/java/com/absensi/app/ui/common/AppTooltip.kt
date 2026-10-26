package com.absensi.app.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Komponen pembungkus Tooltip Material 3 yang elegan dan konsisten.
 * Tooltip akan tampil saat ikon atau fitur ditekan lama (long-press) di layar sentuh
 * atau saat kursor mouse diarahkan di atasnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTooltip(
    tooltip: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (tooltip.isBlank()) {
        content()
        return
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = tooltip,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Komponen IconButton dengan tooltip bawaan.
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        AppTooltip(tooltip = tooltip) {
            content()
        }
    }
}
