package com.absensi.app.ui.common

import com.absensi.app.data.remote.dto.ApiErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

object ErrorMapper {

    fun parseError(throwable: Throwable, json: Json = Json { ignoreUnknownKeys = true }): String {
        return when (throwable) {
            is HttpException -> {
                val errorBody = throwable.response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    try {
                        val parsed = json.decodeFromString<ApiErrorResponse>(errorBody)
                        mapCodeToMessage(parsed.error.code, parsed.error.message)
                    } catch (_: Exception) {
                        "Terjadi kesalahan pada server (HTTP ${throwable.code()})"
                    }
                } else {
                    "Terjadi kesalahan pada server (HTTP ${throwable.code()})"
                }
            }
            is IOException -> "Gagal terhubung ke server. Periksa koneksi internet Anda."
            else -> throwable.message ?: "Terjadi kesalahan tidak terduga."
        }
    }

    fun mapCodeToMessage(code: String, fallbackMessage: String): String {
        return when (code) {
            "INVALID_CREDENTIALS" -> "Nomor induk/email atau password salah."
            "ACCOUNT_INACTIVE" -> "Akun Anda telah dinonaktifkan. Silakan hubungi admin sekolah."
            "DEVICE_PENDING" -> "Perangkat Anda sedang menunggu persetujuan operator."
            "TOO_MANY_ATTEMPTS" -> "Terlalu banyak percobaan login. Silakan tunggu 15 menit."
            "UNAUTHORIZED" -> "Sesi telah berakhir. Silakan login kembali."
            "FORBIDDEN" -> "Akses ditolak untuk peran akun Anda."
            "IN_USE" -> "Data tidak dapat dihapus karena masih digunakan atau terhubung ke data lain."
            "SCHEDULE_CONFLICT" -> "Terdapat bentrok jadwal guru pada hari dan jam yang beririsan."
            "INVALID_TEACHER_ROLE" -> "Pengguna yang dipilih bukan seorang guru."
            "INACTIVE_LOCATION" -> "Lokasi yang dipilih berstatus tidak aktif."
            "DUPLICATE_MEMBER" -> "Pengguna sudah menjadi anggota kelas ini."
            "INVALID_MEMBER_ROLE" -> "Anggota kelas harus berperan sebagai siswa atau guru."
            "VALIDATION_ERROR" -> fallbackMessage.ifBlank { "Format data yang dimasukkan tidak valid." }
            else -> fallbackMessage.ifBlank { "Terjadi kesalahan pada sistem." }
        }
    }
}
