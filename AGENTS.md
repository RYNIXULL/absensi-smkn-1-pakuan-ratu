# AGENTS.md

## Proyek
Aplikasi Android absensi berbasis lokasi + selfie (role: operator, guru, siswa) dengan backend Node.js. Spesifikasi lengkap ada di `docs/RND.md`. Baca file itu sebelum mengerjakan tugas apa pun. Jika kode dan dokumen bertentangan, hentikan dan tanyakan, jangan menebak.

## Stack
- Android: Kotlin, Jetpack Compose (Material 3), MVVM + Repository, Hilt, Retrofit + OkHttp + kotlinx.serialization, CameraX, Fused Location Provider, Play Integrity, FCM, DataStore.
- Backend: Node.js 20+, Express, TypeScript (strict), Prisma, PostgreSQL, zod, exceljs, sharp, node-cron, pino.
- Lokal: Docker + docker-compose.

## Perintah
- Backend: `cd backend && npm install && npm run dev` | `npm test` | `npm run lint` | `npx prisma migrate dev`
- Database lokal: `docker compose up -d`
- Android: `cd android && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint`

## Aturan wajib
1. Kerjakan per fase sesuai bagian 12 di `docs/RND.md`. Jangan mengerjakan fase berikutnya sebelum kriteria selesai fase saat ini terpenuhi.
2. Semua validasi penting dilakukan di backend: waktu server, jarak Haversine, role, kepemilikan data, status sesi. Jangan pernah mempercayai jam, jarak, atau status dari klien.
3. RBAC dipaksa di middleware backend. Menyembunyikan menu di aplikasi tidak dianggap sebagai pembatasan akses.
4. Jangan simpan rahasia (kunci API, kredensial DB, kunci JWT) di repo. Gunakan `.env` (sudah di `.gitignore`) dan sediakan `.env.example`.
5. Perubahan skema database hanya lewat migrasi Prisma. Jangan ubah database secara manual.
6. Selfie dan lampiran disimpan di bucket privat; akses hanya lewat signed URL. Jangan simpan gambar di database.
7. Waktu disimpan UTC (`timestamptz`), ditampilkan WIB (Asia/Jakarta).
8. Password di-hash dengan argon2id. Jangan pernah mencatat password, token, atau isi selfie di log.
9. Setiap endpoint baru wajib punya validasi input (zod), penanganan error seragam `{ error: { code, message } }`, dan tes.
10. Aplikasi Android tidak boleh menyediakan jalur absen lewat galeri. Selfie hanya dari kamera depan via CameraX.

## Gaya kode
- TypeScript strict, tanpa `any` kecuali dijelaskan. Nama berkas `camelCase.ts`; modul backend dikelompokkan per fitur di `src/modules/<fitur>/`.
- Kotlin: ikuti Kotlin coding conventions; satu ViewModel per layar; state via `StateFlow`; tanpa logika bisnis di Composable.
- Teks UI dan pesan error untuk pengguna dalam Bahasa Indonesia. Nama kode (variabel, fungsi) dalam bahasa Inggris, kecuali istilah domain yang sudah ada di skema (mis. `nomor_induk`).
- Commit kecil dan terfokus dengan pesan jelas (format: `feat:`, `fix:`, `chore:`, `test:`).

## Definisi selesai (Definition of Done)
- Kode lolos lint, build, dan seluruh tes.
- Ada tes untuk logika baru (minimal unit test; integration test untuk endpoint).
- Tidak ada rahasia atau data pribadi di commit.
- `docs/RND.md` diperbarui bila ada keputusan desain yang berubah.
- Ringkas perubahan: apa yang dikerjakan, apa yang diuji, dan hal yang belum selesai.

## Hal yang harus ditanyakan dulu ke pengembang
- Menambah dependensi besar atau mengganti stack.
- Mengubah skema yang sudah dimigrasi atau kontrak API yang sudah dipakai klien.
- Melonggarkan aturan keamanan atau anti-kecurangan di bagian 6 `docs/RND.md`.
