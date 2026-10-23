package com.absensi.app.ui.absen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface ImageProcessor {
    suspend fun processSelfie(
        bitmap: Bitmap,
        studentName: String,
        timestamp: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ): ByteArray
}

@Singleton
class AndroidImageProcessor @Inject constructor() : ImageProcessor {

    override suspend fun processSelfie(
        bitmap: Bitmap,
        studentName: String,
        timestamp: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ): ByteArray = withContext(Dispatchers.Default) {
        // 1. Resize if longest dimension exceeds 1280
        val maxDim = 1280
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height

        val (scaledWidth, scaledHeight) = if (srcWidth > maxDim || srcHeight > maxDim) {
            if (srcWidth >= srcHeight) {
                val ratio = srcHeight.toFloat() / srcWidth
                maxDim to (maxDim * ratio).toInt()
            } else {
                val ratio = srcWidth.toFloat() / srcHeight
                (maxDim * ratio).toInt() to maxDim
            }
        } else {
            srcWidth to srcHeight
        }

        val workingBitmap = if (scaledWidth != srcWidth || scaledHeight != srcHeight) {
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        // 2. Add Watermark via Canvas
        val canvas = Canvas(workingBitmap)
        val bannerHeight = (scaledHeight * 0.16f).coerceAtLeast(100f)

        // Semi-transparent dark frosted banner
        val bgPaint = Paint().apply {
            color = Color.argb(175, 15, 23, 42) // Dark slate #0F172A
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            Rect(0, (scaledHeight - bannerHeight).toInt(), scaledWidth, scaledHeight),
            bgPaint
        )

        // Text paint
        val textSize = (bannerHeight / 4.2f).coerceAtLeast(20f)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            this.textSize = textSize * 1.1f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }

        val detailPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0") // Slate-200
            this.textSize = textSize * 0.9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        val paddingLeft = (scaledWidth * 0.04f).coerceAtLeast(24f)
        val startY = scaledHeight - bannerHeight + (bannerHeight * 0.28f)
        val lineSpacing = bannerHeight * 0.26f

        // Line 1: Student Name
        canvas.drawText(studentName.ifBlank { "Siswa" }, paddingLeft, startY, titlePaint)

        // Line 2: Server Time
        val timeDisplay = if (timestamp.contains("T")) {
            timestamp.replace("T", " ").substringBefore(".")
        } else {
            timestamp
        }
        canvas.drawText("Waktu: $timeDisplay WIB", paddingLeft, startY + lineSpacing, detailPaint)

        // Line 3: Coordinates & Accuracy
        val coordsText = String.format(
            Locale.US,
            "Lokasi: %.6f, %.6f (Akurasi: ±%.1fm)",
            latitude,
            longitude,
            accuracy
        )
        canvas.drawText(coordsText, paddingLeft, startY + (lineSpacing * 2), detailPaint)

        // 3. Compress JPEG 80%
        val outputStream = ByteArrayOutputStream()
        workingBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.toByteArray()
    }
}
