package com.absensi.app.security

import android.content.Context
import android.util.Base64
import com.absensi.app.BuildConfig
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface IntegrityManager {
    suspend fun requestToken(nonce: String): Result<String>
}

@Singleton
class PlayIntegrityManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IntegrityManager {

    private val integrityManager = IntegrityManagerFactory.create(context)

    override suspend fun requestToken(nonce: String): Result<String> {
        return try {
            val token = requestPlayIntegrityTokenInternal(nonce)
            Result.success(token)
        } catch (e: Exception) {
            // In debug builds, if Play Integrity fails (e.g. emulator without Play Store or dev keys),
            // provide fallback simulated token to allow developer testing.
            if (BuildConfig.DEBUG) {
                val fallbackToken = generateSimulatedToken(nonce)
                Result.success(fallbackToken)
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun requestPlayIntegrityTokenInternal(nonce: String): String =
        suspendCancellableCoroutine { continuation ->
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()

            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener { response: IntegrityTokenResponse ->
                    continuation.resume(response.token())
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }

    private fun generateSimulatedToken(nonce: String): String {
        val payload = JSONObject().apply {
            put("requestDetails", JSONObject().apply {
                put("requestPackageName", context.packageName)
                put("nonce", nonce)
                put("timestampMillis", System.currentTimeMillis())
            })
            put("deviceIntegrity", JSONObject().apply {
                put("deviceRecognitionVerdict", JSONArray().apply {
                    put("MEETS_DEVICE_INTEGRITY")
                })
            })
            put("accountDetails", JSONObject().apply {
                put("appLicensingVerdict", "LICENSED")
            })
        }
        val encodedPayload = Base64.encodeToString(
            payload.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return "mock.$encodedPayload.dev_signature"
    }
}
