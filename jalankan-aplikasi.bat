@echo off
setlocal enabledelayedexpansion
title Peluncur Aplikasi Absensi - Emulator dan Installer

cd /d "%~dp0"

set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "EMULATOR=%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe"
set "APK_PATH=%~dp0android\app\build\outputs\apk\debug\app-debug.apk"
set "AVD_NAME=Absensi_API34"
set "AVD_DIR=%USERPROFILE%\.android\avd\%AVD_NAME%.avd"

echo ========================================================
echo      PELUNCUR APLIKASI ABSENSI (LOCAL REVIEW)
echo ========================================================
echo.

if not exist "%ADB%" (
    echo [ERROR] adb.exe tidak ditemukan di: %ADB%
    pause
    exit /b 1
)

if not exist "%EMULATOR%" (
    echo [ERROR] emulator.exe tidak ditemukan di: %EMULATOR%
    pause
    exit /b 1
)

if not exist "%APK_PATH%" (
    echo [ERROR] File APK belum ada di: %APK_PATH%
    pause
    exit /b 1
)

echo [1/4] Memeriksa perangkat Android...
"%ADB%" start-server >nul 2>&1
set "PHYSICAL="
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    echo %%a | find /i "emulator-" >nul
    if errorlevel 1 if "%%b"=="device" set "PHYSICAL=%%a"
)
tasklist /fi "imagename eq qemu-system-x86_64.exe" 2>NUL | find /i "qemu-system-x86_64.exe" >NUL
set "EMU_RUNNING=%ERRORLEVEL%"
if defined PHYSICAL (
    echo HP fisik terdeteksi: !PHYSICAL! - emulator tidak dinyalakan.
    set "ANDROID_SERIAL=!PHYSICAL!"
) else if "!EMU_RUNNING!"=="0" (
    echo Emulator sudah berjalan di layar.
) else (
    echo Membersihkan kunci sesi lama jika ada...
    del /f /q "%AVD_DIR%\*.lock" 2>nul
    rd /s /q "%AVD_DIR%\hardware-qemu.ini.lock" 2>nul
    rd /s /q "%AVD_DIR%\multiinstance.lock" 2>nul
    
    echo Menyalakan emulator %AVD_NAME% di jendela baru...
    start "" "%EMULATOR%" -avd %AVD_NAME% -gpu auto -no-snapshot-load
    echo Jendela emulator sedang terbuka di layar Anda.
)

echo.
echo [2/4] Menunggu sistem Android selesai booting...
echo (Proses boot membutuhkan waktu sekitar 30 - 60 detik)
echo.

set SECONDS=0

:WAIT_LOOP
ping -n 4 127.0.0.1 >nul
set /a SECONDS+=3

"%ADB%" shell getprop sys.boot_completed 2>nul | find "1" >nul
if "%ERRORLEVEL%"=="0" (
    goto BOOT_DONE
)

if !SECONDS! GEQ 180 (
    echo.
    echo [ERROR] Android tidak selesai booting setelah 180 detik.
    echo Coba tutup jendela emulator, lalu jalankan ulang script ini.
    echo Alternatif: colok HP Android fisik dengan USB Debugging aktif.
    echo.
    pause
    exit /b 1
)

echo [!SECONDS! detik] Menunggu sistem Android siap...
goto WAIT_LOOP

:BOOT_DONE
echo.
echo Sistem Android telah aktif dan siap (boot completed)!

echo.
echo [3/4] Memasang aplikasi Absensi ke emulator...
"%ADB%" install -r "%APK_PATH%"

echo Memberikan izin kamera dan lokasi untuk kemudahan review...
"%ADB%" shell pm grant com.absensi.app android.permission.CAMERA 2>nul
"%ADB%" shell pm grant com.absensi.app android.permission.ACCESS_FINE_LOCATION 2>nul
"%ADB%" shell pm grant com.absensi.app android.permission.ACCESS_COARSE_LOCATION 2>nul

echo.
echo [4/4] Membuka aplikasi Absensi...
"%ADB%" shell am start -n com.absensi.app/.ui.MainActivity

echo.
echo ========================================================
echo APLIKASI TELAH TERPASANG DAN DIBUKA DI EMULATOR!
echo.
echo Kredensial untuk login review (sesuai SEED di backend/.env.example):
echo - Operator : OP001
echo - Guru     : GURU001
echo - Siswa    : SISWA001
echo.
echo Backend aktif di http://localhost:3000
echo ========================================================
echo.
pause
