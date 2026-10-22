# Aplikasi Absensi Berbasis Lokasi + Swafoto

Aplikasi absensi berbasis lokasi (GPS nyata) dan swafoto kamera depan langsung (anti-fraud) untuk tiga peran: **Operator**, **Guru**, dan **Siswa**.

---

## Struktur Monorepo

- `backend/`: REST API berbasis Node.js 20+, Express, TypeScript, Prisma, PostgreSQL 16.
- `android/`: Aplikasi Android berbasis Kotlin, Jetpack Compose (Material 3), Hilt, Retrofit, CameraX, Play Integrity.
- `docs/`: Spesifikasi teknis dan dokumen RnD (`docs/RND.md`).

---

## 1. Menjalankan Backend

### Prasyarat
- Node.js 20+
- Docker & Docker Compose

### Langkah Instalasi & Menjalankan:
1. Masuk ke direktori backend:
   ```bash
   cd backend
   ```
2. Salin template variabel lingkungan:
   ```bash
   cp .env.example .env
   ```
3. Pasang dependensi:
   ```bash
   npm install
   ```
4. Jalankan PostgreSQL lokal dengan Docker Compose:
   ```bash
   docker compose up -d
   ```
5. Jalankan migrasi database awal:
   ```bash
   npx prisma migrate dev --name init
   ```
6. Jalankan seed data awal:
   ```bash
   npm run prisma:seed
   ```
7. Jalankan server dalam mode development:
   ```bash
   npm run dev
   ```
   Server akan aktif di `http://localhost:3000`. Cek kesehatan server melalui:
   ```bash
   curl http://localhost:3000/api/v1/health
   ```

### Skrip Pengujian & Linting Backend:
- `npm run build` : Kompilasi TypeScript ke `dist/`
- `npm run lint`  : Pengecekan kode ESLint
- `npm test`      : Menjalankan unit & integration test (Vitest)

---

## 2. Menjalankan Android

### Prasyarat
- Android SDK (API 26+)
- JDK 17 atau 21/23 kompatibel Gradle 8.10.2

### Perintah Gradle:
1. Masuk ke direktori android:
   ```bash
   cd android
   ```
2. Build debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Menjalankan unit test:
   ```bash
   ./gradlew test
   ```
4. Menjalankan linting:
   ```bash
   ./gradlew lint
   ```
