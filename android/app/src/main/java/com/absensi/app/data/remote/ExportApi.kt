package com.absensi.app.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ExportApi {

    @Streaming
    @GET("exports/attendances.xlsx")
    suspend fun downloadAttendanceReport(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("subject_id") subjectId: String? = null,
        @Query("student_id") studentId: String? = null,
        @Query("status") status: String? = null
    ): ResponseBody
}
