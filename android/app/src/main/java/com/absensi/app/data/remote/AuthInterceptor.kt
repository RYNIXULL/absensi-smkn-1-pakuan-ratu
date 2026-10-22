package com.absensi.app.data.remote

import com.absensi.app.data.local.AuthPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding token for public auth endpoints (login, refresh)
        val path = originalRequest.url.encodedPath
        if (path.contains("auth/login") || path.contains("auth/refresh")) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { authPreferences.getAccessTokenSync() }
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
