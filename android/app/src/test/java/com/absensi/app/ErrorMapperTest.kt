package com.absensi.app

import com.absensi.app.ui.common.ErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun mapCodeToMessage_invalidCredentials_returnsIndonesianMessage() {
        val result = ErrorMapper.mapCodeToMessage("INVALID_CREDENTIALS", "Invalid credentials")
        assertEquals("Nomor induk/email atau password salah.", result)
    }

    @Test
    fun mapCodeToMessage_accountInactive_returnsIndonesianMessage() {
        val result = ErrorMapper.mapCodeToMessage("ACCOUNT_INACTIVE", "Account inactive")
        assertEquals("Akun Anda telah dinonaktifkan. Silakan hubungi admin sekolah.", result)
    }

    @Test
    fun mapCodeToMessage_devicePending_returnsIndonesianMessage() {
        val result = ErrorMapper.mapCodeToMessage("DEVICE_PENDING", "Device pending")
        assertEquals("Perangkat Anda sedang menunggu persetujuan operator.", result)
    }

    @Test
    fun mapCodeToMessage_tooManyAttempts_returnsIndonesianMessage() {
        val result = ErrorMapper.mapCodeToMessage("TOO_MANY_ATTEMPTS", "Too many attempts")
        assertEquals("Terlalu banyak percobaan login. Silakan tunggu 15 menit.", result)
    }

    @Test
    fun mapCodeToMessage_unknownCode_returnsFallbackMessage() {
        val result = ErrorMapper.mapCodeToMessage("SOME_RANDOM_CODE", "Pesan kustom")
        assertEquals("Pesan kustom", result)
    }
}
