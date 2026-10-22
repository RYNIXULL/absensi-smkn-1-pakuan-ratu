package com.absensi.app.data.local

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("device_identity", Context.MODE_PRIVATE)

    /**
     * Menghasilkan UUID acak sekali dan menyimpannya di penyimpanan privat aplikasi.
     * Konsekuensi: Menghapus data aplikasi (Clear Storage) akan menghasilkan ID baru
     * sehingga perangkat siswa akan dianggap sebagai perangkat baru dan memerlukan
     * persetujuan ulang dari operator.
     */
    fun getDeviceId(): String {
        var deviceId = prefs.getString("device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }

    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    }
}
