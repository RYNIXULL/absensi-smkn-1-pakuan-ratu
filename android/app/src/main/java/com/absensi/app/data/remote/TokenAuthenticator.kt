package com.absensi.app.data.remote

import com.absensi.app.data.local.AuthPreferences
import com.absensi.app.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val authApiProvider: Provider<AuthApi>
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if retry fails
        if (responseCount(response) >= 2) {
            return null
        }

        synchronized(lock) {
            val currentToken = runBlocking { authPreferences.getAccessTokenSync() }
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // If token has already been refreshed by another thread while this request was waiting
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = runBlocking { authPreferences.getRefreshTokenSync() } ?: run {
                runBlocking { authPreferences.clearSession() }
                return null
            }

            try {
                val refreshCall = authApiProvider.get().refreshTokenCall(RefreshTokenRequest(refreshToken))
                val refreshResponse = refreshCall.execute()

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val newTokens = refreshResponse.body()!!
                    runBlocking {
                        authPreferences.updateTokens(newTokens.accessToken, newTokens.refreshToken)
                    }

                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                } else {
                    // Refresh token invalid or expired -> Logout
                    runBlocking { authPreferences.clearSession() }
                    return null
                }
            } catch (e: Exception) {
                return null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
