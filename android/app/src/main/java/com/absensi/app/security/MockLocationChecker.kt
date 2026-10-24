package com.absensi.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import com.absensi.app.ui.absen.LocationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class MockCheckResult(
    val isMockDetected: Boolean,
    val reasons: List<String> = emptyList()
)

interface MockLocationChecker {
    fun isMockLocation(location: Location?): Boolean
    fun isMockLocation(locationResult: LocationResult?): Boolean
    fun checkMockEnvironment(): MockCheckResult
}

@Singleton
class AndroidMockLocationChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : MockLocationChecker {

    private val knownMockPackages = listOf(
        "com.lexa.fakegps",
        "com.rosteam.gpsemulator",
        "com.incorporateapps.fakegps.fre",
        "com.incorporateapps.fakegps",
        "com.fakegps.mock",
        "com.mocker.mockgps",
        "com.app.fakegps",
        "ru.gavrikov.mocklocations",
        "com.gsmartstudio.fakegps",
        "com.blogspot.newprocess3.fakegps"
    )

    override fun isMockLocation(location: Location?): Boolean {
        if (location == null) return false

        // API 31+ has isMock()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    override fun isMockLocation(locationResult: LocationResult?): Boolean {
        return locationResult?.isMock == true
    }

    override fun checkMockEnvironment(): MockCheckResult {
        val reasons = mutableListOf<String>()

        // 1. Check legacy Secure.ALLOW_MOCK_LOCATION (API < 23)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                @Suppress("DEPRECATION")
                val mockEnabled = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION
                )
                if (mockEnabled != "0" && mockEnabled != null) {
                    reasons.add("Mock location system setting is enabled")
                }
            } catch (_: Exception) {
            }
        }

        // 2. Check installed Mock Location / Fake GPS applications
        val pm = context.packageManager
        for (pkg in knownMockPackages) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                reasons.add("Terdeteksi aplikasi lokasi tiruan terpasang: $pkg")
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed
            }
        }

        return MockCheckResult(
            isMockDetected = reasons.isNotEmpty(),
            reasons = reasons
        )
    }
}
