package com.absensi.app.data.repository

import android.content.Context
import android.os.Environment
import com.absensi.app.data.remote.ExportApi
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface ExportRepository {
    suspend fun downloadReport(
        startDate: String? = null,
        endDate: String? = null,
        classId: String? = null,
        subjectId: String? = null,
        studentId: String? = null,
        status: String? = null
    ): Result<ByteArray>

    fun saveReportToFile(
        context: Context,
        bytes: ByteArray,
        filename: String
    ): Result<File>
}

@Singleton
class ExportRepositoryImpl @Inject constructor(
    private val exportApi: ExportApi
) : ExportRepository {

    override suspend fun downloadReport(
        startDate: String?,
        endDate: String?,
        classId: String?,
        subjectId: String?,
        studentId: String?,
        status: String?
    ): Result<ByteArray> {
        return runCatching {
            val responseBody = exportApi.downloadAttendanceReport(
                startDate = startDate,
                endDate = endDate,
                classId = classId,
                subjectId = subjectId,
                studentId = studentId,
                status = status
            )
            responseBody.bytes()
        }
    }

    override fun saveReportToFile(
        context: Context,
        bytes: ByteArray,
        filename: String
    ): Result<File> {
        return runCatching {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(downloadDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            file
        }
    }
}
