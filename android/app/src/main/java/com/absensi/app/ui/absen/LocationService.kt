package com.absensi.app.ui.absen

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val isMock: Boolean
)

interface LocationService {
    fun isLocationEnabled(): Boolean
    suspend fun getCurrentLocation(): LocationResult?
}

@Singleton
class AndroidLocationService @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    override fun isLocationEnabled(): Boolean {
        if (locationManager == null) return false
        val gpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        val networkEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
        return gpsEnabled || networkEnabled
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult? = suspendCancellableCoroutine { continuation ->
        val cts = CancellationTokenSource()
        continuation.invokeOnCancellation {
            cts.cancel()
        }

        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            location.isMock
                        } else {
                            @Suppress("DEPRECATION")
                            location.isFromMockProvider
                        }
                        continuation.resume(
                            LocationResult(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                isMock = isMock
                            )
                        )
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (_: SecurityException) {
            continuation.resume(null)
        }
    }
}
