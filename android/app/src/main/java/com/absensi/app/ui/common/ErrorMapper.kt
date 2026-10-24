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
            "SESSION_CLOSED" -> "Sesi absensi tidak sedang terbuka atau telah berakhir."
            "NOT_IN_CLASS" -> "Anda bukan siswa yang terdaftar di kelas sesi ini."
            "DEVICE_MISMATCH" -> "Absensi hanya dapat dilakukan dari perangkat resmi yang telah disetujui operator."
            "NONCE_INVALID" -> "Token challenge kedaluwarsa atau sudah digunakan. Silakan muat ulang."
            "MOCK_LOCATION" -> "Lokasi tiruan / Fake GPS terdeteksi dan dilarang oleh sistem."
            "LOW_ACCURACY" -> "Akurasi sinyal GPS terlalu rendah (> 50 meter). Pindahlah ke area terbuka."
            "OUT_OF_RADIUS" -> "Anda berada di luar radius presensi sekolah."
            "ALREADY_CHECKED_IN" -> "Anda sudah melakukan absensi pada sesi ini."
            "INVALID_SELFIE" -> "Foto selfie tidak valid atau melebihi 2 MB. Ambil swafoto kembali."
            "INTEGRITY_FAILED" -> "Integritas perangkat atau aplikasi tidak memenuhi syarat."
            "LEAVE_BACKDATE_EXCEEDED" -> "Pengajuan izin tidak boleh melewati batas maksimal (maksimal 3 hari ke belakang)."
            "LEAVE_EXISTS" -> "Anda sudah memiliki pengajuan izin yang sedang diproses untuk sesi ini."
            "ALREADY_REVIEWED" -> "Pengajuan izin ini sudah ditinjau sebelumnya dan tidak dapat diubah lagi."
            "INVALID_FILE_TYPE" -> "Format berkas lampiran tidak didukung. Harap unggah berkas JPEG, PNG, WEBP, atau PDF."
            "FILE_TOO_LARGE" -> "Ukuran berkas lampiran melebihi batas maksimal 5 MB."
            "ATTACHMENT_NOT_FOUND" -> "Berkas lampiran tidak ditemukan di penyimpanan."
            "VALIDATION_ERROR" -> fallbackMessage.ifBlank { "Format data yang dimasukkan tidak valid." }
            else -> fallbackMessage.ifBlank { "Terjadi kesalahan pada sistem." }
        }
    }

    data class DetailedError(
        val code: String,
        val message: String,
        val suggestion: String
    )

    fun parseDetailedError(throwable: Throwable, json: Json = Json { ignoreUnknownKeys = true }): DetailedError {
        if (throwable is HttpException) {
            val errorBody = throwable.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                try {
                    val parsed = json.decodeFromString<ApiErrorResponse>(errorBody)
                    val code = parsed.error.code
                    val message = mapCodeToMessage(code, parsed.error.message)
                    val suggestion = when (code) {
                        "SESSION_CLOSED" -> "Pastikan jam pelajaran berlangsung dan guru telah membuka sesi."
                        "NOT_IN_CLASS" -> "Periksa kembali jadwal kelas Anda atau hubungi guru pengampu."
                        "DEVICE_MISMATCH" -> "Gunakan perangkat utama Anda atau ajukan pergantian perangkat ke operator."
                        "NONCE_INVALID" -> "Buka ulang layar absen untuk mendapatkan token verifikasi terbaru."
                        "MOCK_LOCATION" -> "Matikan fitur 'Mock Location' di Opsi Pengembang perangkat Anda."
                        "LOW_ACCURACY" -> "Nyalakan mode Akurasi Tinggi dan berdiri di luar ruangan / dekat jendela."
                        "OUT_OF_RADIUS" -> "Dekatilah lokasi sekolah dan pastikan berada dalam radius presensi sebelum menekan tombol absen."
                        "ALREADY_CHECKED_IN" -> "Anda tidak perlu melakukan absen dua kali pada sesi yang sama."
                        "INVALID_SELFIE" -> "Pastikan wajah Anda terlihat jelas dengan pencahayaan yang cukup."
                        "INTEGRITY_FAILED" -> "Pastikan perangkat menggunakan firmware resmi, tidak di-root, dan Google Play Store terpasang."
                        else -> "Silakan coba beberapa saat lagi."
                    }
                    return DetailedError(code, message, suggestion)
                } catch (_: Exception) {
                    // fallback below
                }
            }
            return DetailedError("HTTP_${throwable.code()}", "Terjadi kesalahan pada server (HTTP ${throwable.code()})", "Periksa koneksi Anda dan coba lagi.")
        } else if (throwable is IOException) {
            return DetailedError("NETWORK_ERROR", "Gagal terhubung ke server backend.", "Pastikan perangkat terhubung ke internet dan backend aktif.")
        }
        return DetailedError("UNKNOWN", throwable.message ?: "Terjadi kesalahan tidak terduga.", "Silakan coba kembali.")
    }
}
