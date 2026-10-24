package com.absensi.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class RootCheckResult(
    val isRooted: Boolean,
    val reasons: List<String> = emptyList()
)

interface RootChecker {
    fun checkRoot(): RootCheckResult
    fun isDeviceRooted(): Boolean
}

@Singleton
class AndroidRootChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : RootChecker {

    private val suBinaryPaths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/xbin/busybox"
    )

    private val knownRootPackages = listOf(
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.topjohnwu.magisk",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.saurik.substrate",
        "de.robv.android.xposed.installer"
    )

    override fun isDeviceRooted(): Boolean {
        return checkRoot().isRooted
    }

    override fun checkRoot(): RootCheckResult {
        val reasons = mutableListOf<String>()

        // 1. Check test-keys
        if (checkTestKeys()) {
            reasons.add("Build tags mengandung test-keys")
        }

        // 2. Check SU binary files
        val foundSuPaths = checkSuBinaries()
        if (foundSuPaths.isNotEmpty()) {
            reasons.add("Ditemukan file SU di: ${foundSuPaths.joinToString()}")
        }

        // 3. Check execution of SU
        if (canExecuteSu()) {
            reasons.add("Perintah SU dapat dieksekusi")
        }

        // 4. Check known root management packages
        val foundPackages = checkRootPackages()
        if (foundPackages.isNotEmpty()) {
            reasons.add("Ditemukan paket root terpasang: ${foundPackages.joinToString()}")
        }

        return RootCheckResult(
            isRooted = reasons.isNotEmpty(),
            reasons = reasons
        )
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuBinaries(): List<String> {
        val detected = mutableListOf<String>()
        for (path in suBinaryPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    detected.add(path)
                }
            } catch (_: Exception) {
            }
        }
        return detected
    }

    private fun canExecuteSu(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            line != null
        } catch (_: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }

    private fun checkRootPackages(): List<String> {
        val detected = mutableListOf<String>()
        val pm = context.packageManager
        for (pkg in knownRootPackages) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                detected.add(pkg)
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed
            }
        }
        return detected
    }
}
