# Prompt Eksekusi untuk Agen (Antigravity)

Prasyarat: `AGENTS.md` ada di root proyek dan `docs/RND.md` ada di folder `docs/`.

## Cara memakai
1. Buka folder proyek di Antigravity. Gunakan mode yang meminta rencana dulu (Planning), dan atur kebijakan review supaya agen berhenti untuk persetujuanmu sebelum menjalankan perintah berisiko.
2. Tempel **PROMPT MASTER** sekali di awal sesi.
3. Setelah agen menyatakan siap, tempel prompt fase satu per satu (F0, F1, dst.). Jangan lanjut ke fase berikutnya sebelum gerbang verifikasi fase sebelumnya lulus dan kamu yang memeriksa hasilnya.
4. Jika sesi baru dibuka, tempel ulang PROMPT MASTER lalu tulis: "Lanjutkan dari fase Fx. Periksa kondisi repo dulu."

---

## PROMPT MASTER (tempel pertama)

```
Peranmu: engineer senior full-stack (Android Kotlin + Node.js/TypeScript) yang mengeksekusi proyek aplikasi absensi berbasis lokasi + selfie.

SUMBER KEBENARAN
- Baca AGENTS.md dan docs/RND.md sampai selesai sebelum melakukan apa pun. Keduanya mengikat.
- Jika instruksiku bertentangan dengan dokumen, sebutkan konfliknya dan tanyakan mana yang berlaku. Jangan menebak.
- Jika ada hal yang ambigu atau belum diputuskan di dokumen, tulis sebagai "Pertanyaan terbuka" dan ajukan sebelum menulis kode yang bergantung padanya.

CARA KERJA (berlaku untuk SETIAP fase)
1. PLAN dulu: tulis rencana implementasi berisi daftar berkas yang akan dibuat/diubah, keputusan teknis, dan risiko. Berhenti dan tunggu persetujuanku sebelum menulis kode.
2. IMPLEMENT sesuai rencana yang disetujui, dalam langkah kecil. Satu commit per unit kerja yang koheren.
3. VERIFY: jalankan build, lint, dan tes yang relevan. Tempelkan output ringkas perintah dan hasilnya. Jangan klaim "selesai" tanpa bukti hasil perintah.
4. REPORT: akhiri dengan ringkasan: (a) apa yang selesai, (b) apa yang diuji dan hasilnya, (c) yang belum selesai atau ditunda beserta alasannya, (d) keputusan desain yang menyimpang dari docs/RND.md, (e) tindakan manual yang harus kulakukan (mis. buat kunci, setup konsol).
5. BERHENTI setelah REPORT. Jangan mulai fase berikutnya sebelum aku menulis "lanjut".

BATASAN KERAS
- Jangan mengerjakan di luar fase yang diminta.
- Jangan menambah dependensi besar, mengganti stack, atau mengubah kontrak API/skema yang sudah ada tanpa persetujuan.
- Jangan menulis rahasia ke repo. Semua konfigurasi sensitif lewat .env dan .env.example.
- Jangan melonggarkan aturan keamanan/anti-kecurangan demi membuat tes lulus. Jika tes gagal, perbaiki kodenya, bukan aturannya.
- Jangan mengubah/menghapus tes yang gagal agar hijau. Jelaskan penyebabnya.
- Jika sebuah perintah gagal 2 kali dengan penyebab yang sama, berhenti, jelaskan diagnosis, dan usulkan alternatif.
- Hal yang tidak bisa kamu verifikasi (perangkat fisik, konsol Google, Play Console), nyatakan jelas sebagai "belum terverifikasi" dan beri langkah verifikasi manual.

BAHASA
- Penjelasan kepadaku dalam Bahasa Indonesia. Kode, nama variabel, dan komentar kode dalam bahasa Inggris. Teks UI dan pesan error untuk pengguna dalam Bahasa Indonesia.

Balas hanya dengan: (1) ringkasan 5-8 poin pemahamanmu tentang proyek, (2) daftar pertanyaan terbuka (jika ada), (3) konfirmasi siap menerima prompt fase F0. Jangan menulis kode dulu.
```

---

## F0: Kerangka proyek

```
FASE F0: kerangka proyek. Ikuti prosedur PLAN -> IMPLEMENT -> VERIFY -> REPORT.

TARGET
Monorepo dengan struktur bagian 10 docs/RND.md: /backend, /android, /docs.

BACKEND
- Node 20+, TypeScript strict (noImplicitAny, strictNullChecks), Express, Prisma, PostgreSQL 16.
- Dependensi: express, zod, prisma, @prisma/client, argon2, jsonwebtoken, pino, pino-http, helmet, cors, express-rate-limit, multer, sharp, exceljs, node-cron, dotenv. Dev: typescript, tsx, vitest, supertest, eslint, prettier, @types/*.
- Skrip npm: dev, build, start, lint, test, test:watch, prisma:migrate, prisma:seed.
- Prisma schema setara 1:1 dengan DDL bagian 7 (termasuk enum, unique, index, tabel attendance_challenges dan refresh_tokens). Buat migrasi awal.
- Seed: satu operator (nomor_induk dan password awal dari env SEED_OPERATOR_ID dan SEED_OPERATOR_PASSWORD, hash argon2id), satu lokasi contoh, satu kelas, satu mapel.
- src/config/env.ts: validasi environment dengan zod; aplikasi gagal start bila env tidak valid.
- Pola error global: kelas AppError(code, message, status) dan middleware error yang mengembalikan { error: { code, message } }. Endpoint GET /api/v1/health.
- docker-compose.yml: service postgres:16 (volume, healthcheck) dan service backend opsional untuk dev.
- .env.example lengkap (DATABASE_URL, JWT_ACCESS_SECRET, JWT_REFRESH_SECRET, ACCESS_TTL=15m, REFRESH_TTL=7d, STORAGE_DRIVER=local|s3, ATTENDANCE_MAX_ACCURACY_M=50, OPEN_BEFORE_MIN=10, LATE_AFTER_MIN=15, INTEGRITY_ENFORCE=false, dll.) dan .gitignore yang mengecualikan .env.

ANDROID
- Proyek Kotlin + Jetpack Compose (Material 3), Gradle Kotlin DSL dengan version catalog (libs.versions.toml), minSdk 26, targetSdk versi stabil terbaru.
- Modul tunggal :app dengan paket sesuai bagian 10. Hilt terpasang (Application class, AppModule). Retrofit + OkHttp + kotlinx.serialization terpasang dengan BASE_URL dari BuildConfig (debug: http://10.0.2.2:PORT/api/v1/).
- Navigation Compose dengan graf kosong dan satu layar placeholder. Tema Material 3 (terang/gelap).

LAINNYA
- README.md di root: cara menjalankan backend (docker compose up -d, migrasi, seed, npm run dev) dan Android.
- ESLint + Prettier backend; ktlint atau konfigurasi lint bawaan Android.

GERBANG VERIFIKASI (tempelkan hasilnya)
- docker compose up -d berhasil; npx prisma migrate dev dan seed berhasil.
- npm run build, npm run lint, npm test lulus (minimal satu tes health check).
- GET /api/v1/health mengembalikan 200.
- ./gradlew assembleDebug dan ./gradlew test sukses.
```

---

## F1: Autentikasi, RBAC, ikat perangkat

```
FASE F1: autentikasi, RBAC, pengikatan perangkat (FR-AUTH-01..04, bagian 2 RND.md).

BACKEND
- POST /auth/login: body { identifier (nomor_induk atau email), password, device: { device_id, model } }.
  - Verifikasi argon2id. Pesan error sama untuk "user tidak ada" dan "password salah" (cegah enumerasi). Tolak bila is_active=false.
  - Keluaran: { accessToken, refreshToken, user: { id, role, nama } }.
- JWT access (TTL dari env, klaim: sub, role). Refresh token acak (32 byte), disimpan sebagai HASH (sha256) di refresh_tokens; rotasi tiap refresh; deteksi reuse -> cabut semua token user tersebut.
- POST /auth/refresh, POST /auth/logout, POST /auth/change-password (verifikasi password lama).
- Rate limiting login: maks 5 percobaan per 15 menit per kombinasi IP + identifier. Respons 429 dengan kode TOO_MANY_ATTEMPTS.
- Pengikatan perangkat (hanya role siswa):
  - Belum punya perangkat aktif -> perangkat pertama otomatis aktif dan disetujui.
  - Sudah punya perangkat aktif tetapi device_id berbeda -> buat baris devices status 'menunggu', balas 403 DEVICE_PENDING tanpa token.
  - Operator: GET /devices/requests, PATCH /devices/:id (setujui = aktifkan perangkat baru dan nonaktifkan yang lama; tolak).
  - Operator dan guru boleh banyak perangkat.
- Middleware: authenticate (verifikasi JWT, muat user dan pastikan masih aktif), requireRole(...roles), dan helper requireTeacherOfClass (guru hanya boleh data kelasnya). Terapkan konsisten di seluruh router.
- GET /me. CRUD dasar pengguna oleh operator: GET/POST/PATCH /users, POST /users/:id/reset-password (password sementara acak, kembalikan sekali).
- Setiap aksi sensitif (login gagal berulang, reset password, persetujuan perangkat) ditulis ke audit_logs.

ANDROID
- Layar Login (validasi input, state loading/error, pesan Bahasa Indonesia untuk setiap kode error).
- Penyimpanan token aman (DataStore dengan enkripsi berbasis Android Keystore atau EncryptedSharedPreferences).
- OkHttp Interceptor menambah Authorization; Authenticator menangani 401 dengan refresh sekali lalu mengulang request; gagal refresh -> logout.
- device_id: gunakan ID acak yang dibuat sekali dan disimpan di penyimpanan aman aplikasi (bukan IMEI/serial). Dokumentasikan konsekuensinya (hapus data aplikasi = perangkat dianggap baru).
- Navigasi berdasarkan role ke tiga graf kosong (siswa/guru/operator). Layar tunggu untuk DEVICE_PENDING.

TES WAJIB
- Backend (vitest + supertest): login sukses/gagal, akun nonaktif, rate limit, rotasi dan reuse refresh token, perangkat pertama vs perangkat kedua siswa, 403 untuk setiap kombinasi role yang dilarang pada endpoint operator, guru tidak bisa mengakses kelas lain.
- Android: unit test ViewModel login dan logika Authenticator.

GERBANG VERIFIKASI
- Semua tes lulus. Tabel hasil uji RBAC (endpoint x role) ditampilkan di laporan.
- Login tiga role berhasil di emulator dan masing-masing masuk ke graf yang benar.
```

---

## F2: Master data dan jadwal

```
FASE F2: master data dan jadwal (FR-MST-01..03, FR-JDW-01..03).

BACKEND (semua operator-only kecuali dinyatakan lain)
- CRUD: /classes, /subjects, /locations, /schedules. Input divalidasi zod. Daftar mendukung pagination (page, pageSize<=100), pencarian q, dan filter relevan. Hapus bersifat mencegah bila data masih direferensikan (kode IN_USE) atau soft-delete (is_active) sesuai tabel.
- /classes/:id/members: POST (tambah, bisa banyak user_id sekaligus), DELETE. Validasi: siswa/guru, tidak duplikat.
- Locations: latitude -90..90, longitude -180..180, radius_m 10..1000.
- Schedules: validasi guru berperan guru, start_time < end_time, tidak ada bentrok jadwal guru yang sama pada hari dan jam yang beririsan (kode SCHEDULE_CONFLICT).
- GET /me/schedules untuk guru dan siswa (siswa berdasarkan keanggotaan kelas, guru berdasarkan teacher_id), terurut hari dan jam.
- Impor pengguna: POST /users/import (multipart .xlsx) dan GET /users/import/template. Parsing dengan exceljs; kembalikan laporan per baris (berhasil/gagal + alasan); proses dalam transaksi per batch; password awal acak per user dan dikembalikan dalam file hasil impor yang diunduh sekali.
- Pengaturan FR-JDW-03 (guru boleh ubah jadwal) lewat tabel/konfigurasi sederhana; default: tidak.

ANDROID (operator)
- Layar daftar + form untuk Pengguna, Kelas (+ kelola anggota), Mapel, Lokasi, Jadwal.
- Form lokasi: input manual lat/lng/radius dan tombol "Gunakan lokasi saya sekarang" (Fused Location). Peta bersifat opsional; jangan menambah ketergantungan peta bila tidak perlu.
- Layar Jadwal untuk guru dan siswa (read-only) dikelompokkan per hari.
- Komponen umum: state loading/empty/error yang konsisten, dialog konfirmasi hapus, pagination sederhana.

TES WAJIB
- Backend: validasi tiap CRUD, konflik jadwal, IN_USE, impor Excel (baris valid/invalid), RBAC pada semua endpoint baru.
- Android: unit test ViewModel utama.

GERBANG VERIFIKASI
- Semua tes lulus. Operator dapat membuat alur lengkap: lokasi -> kelas -> anggota -> mapel -> jadwal, lalu siswa dan guru melihat jadwal yang benar.
```

---

## F3: Sesi dan absensi inti

```
FASE F3: sesi absensi dan absensi inti (FR-SES, FR-ABS-01..07). Play Integrity belum diimplementasikan di fase ini, tetapi mekanisme nonce wajib sudah ada.

BACKEND
- lib/haversine.ts: fungsi jarak (meter) + unit test dengan pasangan koordinat yang jaraknya diketahui (toleransi 1 m), termasuk kasus titik sama, lintas meridian, dan lintang tinggi.
- Sesi:
  - Job node-cron tiap menit: untuk jadwal yang hari ini (zona Asia/Jakarta, day_of_week 1=Senin..7=Minggu) belum punya sesi, buat sesi 'terjadwal'; open_at = start_time - OPEN_BEFORE_MIN, late_after = start_time + LATE_AFTER_MIN, close_at = end_time. Ubah status menjadi 'terbuka' saat open_at tercapai. Job idempoten (UNIQUE schedule_id+tanggal).
  - POST /sessions (buka manual oleh operator atau guru kelasnya), PATCH /sessions/:id/close, GET /sessions (terfilter per role).
- POST /sessions/:id/challenge (siswa): hasilkan nonce acak, simpan di attendance_challenges (TTL 2 menit, sekali pakai), kembalikan { nonce, expiresAt }.
- POST /sessions/:id/attendance (siswa, multipart): field lat, lng, accuracy, nonce, device_id, mock_flag, integrity_token (boleh kosong di fase ini), file selfie.
  Validasi urut sesuai bagian 6 RND.md langkah 1-12, kecuali verifikasi Play Integrity (ditandai TODO terstruktur lewat interface IntegrityVerifier dengan implementasi no-op).
  - Waktu = now() server. Status hadir/terlambat berdasar late_after.
  - Batas ukuran selfie 2 MB, hanya image/jpeg; proses dengan sharp (resize sisi terpanjang 1280, strip EXIF); simpan lewat abstraksi StorageService (implementasi local-disk untuk dev dan S3-compatible lewat env). Kunci objek: selfies/{sessionId}/{studentId}.jpg.
  - Respons gagal memakai kode: SESSION_CLOSED, NOT_IN_CLASS, DEVICE_MISMATCH, NONCE_INVALID, MOCK_LOCATION, LOW_ACCURACY, OUT_OF_RADIUS, ALREADY_CHECKED_IN, INVALID_SELFIE.
  - Penolakan dicatat di audit_logs (action ATTENDANCE_REJECTED, kode, student_id, session_id).
- GET /me/attendances (siswa, pagination), GET /attendances (operator semua, guru hanya kelasnya, filter tanggal/kelas/mapel/status), PATCH /attendances/:id (koreksi manual, catatan wajib, audit log old/new).
- GET /attendances/:id/selfie: signed URL berumur pendek (untuk driver local gunakan token HMAC + route unduh yang memverifikasi tanda tangan dan kedaluwarsa).

ANDROID (siswa)
- Beranda: jadwal hari ini + kartu sesi aktif.
- Layar Absen dengan state machine eksplisit: CheckingPermissions -> LocationOff/PermissionDenied -> Ready -> Capturing -> Uploading -> Success | Error(code).
  - Izin lokasi (ACCESS_FINE_LOCATION) dan kamera dengan penjelasan rasional; deteksi GPS mati (LocationManager/SettingsClient) dan tombol menuju pengaturan lokasi. Tombol absen nonaktif selama lokasi tidak aktif.
  - Lokasi: FusedLocationProviderClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY). Kirim lat, lng, accuracy. Hitung mock_flag dari Location.isMock (API 31+) atau isFromMockProvider (di bawahnya); jika true, tampilkan blokir dan tidak mengirim.
  - Selfie: CameraX kamera depan saja; tidak ada akses galeri. Setelah ambil: tambah watermark (nama, waktu server dari challenge, koordinat) via Canvas, kompres JPEG kualitas ~80, sisi terpanjang 1280.
  - Alur: minta challenge -> ambil lokasi + selfie -> kirim multipart via Retrofit. Petakan setiap kode error ke pesan Indonesia yang jelas dan tindakan yang disarankan.
- Layar Riwayat siswa.
- Untuk guru/operator: layar Kelola sesi (buka/tutup) dan daftar kehadiran sesi berjalan (tampilan sederhana dulu; ekspor di F6).

TES WAJIB
- Backend: Haversine; seluruh kode error di atas (satu tes per kode); batas waktu tepat di open_at/close_at/late_after; idempotensi job sesi; unik per sesi+siswa; guru kelas lain ditolak pada rekap dan koreksi; selfie non-JPEG dan terlalu besar.
- Android: unit test state machine ViewModel Absen dengan fake lokasi/kamera/repository.

GERBANG VERIFIKASI
- Semua tes lulus. Demo end-to-end di emulator dengan lokasi emulator disetel di dalam radius (berhasil) dan di luar radius (OUT_OF_RADIUS). Laporkan jujur bahwa di emulator deteksi fake GPS belum bisa dianggap teruji.
```

---

## F4: Anti-kecurangan berlapis

```
FASE F4: anti-kecurangan (bagian 6 RND.md). Prinsip: validasi server adalah lapisan utama; sinyal klien hanya pelengkap.

BACKEND
- Implementasi IntegrityVerifier nyata (lib/integrity.ts):
  - requestHash yang dikirim klien harus = base64url(SHA-256(nonce + ":" + sessionId)); server menghitung ulang dan membandingkan.
  - Dekode token lewat Google Play Integrity API (decodeIntegrityToken) memakai service account dari env (GOOGLE_APPLICATION_CREDENTIALS atau JSON di env), paket aplikasi dari env PLAY_PACKAGE_NAME.
  - Syarat lulus: requestDetails.requestPackageName sesuai, requestHash cocok, timestampMillis segar (<= 2 menit), appRecognitionVerdict = PLAY_RECOGNIZED, deviceRecognitionVerdict mengandung MEETS_DEVICE_INTEGRITY. Simpan ringkasan verdict di kolom integrity_verdict.
  - Env INTEGRITY_ENFORCE: bila false (dev), tolak hanya dicatat sebagai peringatan; bila true, gagal -> INTEGRITY_FAILED. Default produksi true.
  - Interface harus bisa di-mock di tes. Jangan memanggil jaringan pada tes unit.
- Validasi tambahan:
  - Nonce: sekali pakai (tandai used_at dalam transaksi yang sama dengan insert absensi), terikat ke user dan sesi.
  - Akurasi: tolak bila accuracy > ATTENDANCE_MAX_ACCURACY_M (LOW_ACCURACY).
  - Cek lompatan lokasi: bandingkan dengan absensi terakhir siswa; bila kecepatan implisit > 150 km/jam dalam selang waktu itu, tandai (simpan flag di catatan/audit) dan, sesuai env SPEED_CHECK_ENFORCE, tolak dengan kode SUSPICIOUS_MOVEMENT.
  - mock_flag=true dari klien tetap ditolak (MOCK_LOCATION) dan dicatat.
- Rate limit khusus endpoint absen dan challenge per user.

ANDROID (paket security/)
- IntegrityManager: gunakan Standard API Play Integrity (prepare token provider sekali, lalu request per absen dengan requestHash). Tangani error (jaringan, Play Services tidak ada) dan petakan ke pesan yang jelas.
- MockLocationChecker: Location.isMock/isFromMockProvider, ditambah sinyal pendukung: opsi pengembang aktif (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED) dan keberadaan aplikasi mock location yang dikenal. Sinyal pendukung dikirim ke server sebagai metadata, tidak otomatis memblokir (hindari false positive).
- RootChecker dan EmulatorChecker (heuristik: biner su, tag test-keys, paket Magisk, properti Build emulator). Hasilnya sinyal, bukan satu-satunya dasar penolakan; Play Integrity yang menentukan.
- Aktifkan R8/ProGuard pada build rilis dan pastikan kelas security tidak rusak.

TES WAJIB
- Backend: tabel uji untuk integrity verifier dengan token tiruan (package salah, hash salah, kedaluwarsa, verdict tidak memenuhi, sukses); nonce replay; nonce kedaluwarsa; nonce milik user lain; akurasi buruk; lompatan lokasi; mock_flag.
- Android: unit test pemetaan error dan pembentukan requestHash (harus identik dengan implementasi server; sertakan test vector yang sama di kedua sisi).

TINDAKAN MANUAL (laporkan sebagai checklist, jangan dikarang)
- Buat proyek Google Cloud, aktifkan Play Integrity API, hubungkan ke aplikasi di Play Console, buat service account dan isi env. Tandai bagian ini "belum terverifikasi" sampai kulakukan.

GERBANG VERIFIKASI
- Semua tes lulus. Dokumentasikan di docs/RND.md bagian 6 apa yang sudah teruji otomatis, apa yang butuh uji perangkat fisik (fake GPS asli, root), dan prosedur ujinya.
```

---

## F5: Izin/sakit dan alpa otomatis

```
FASE F5: izin/sakit dan penutupan sesi otomatis (FR-IZN, FR-SES-03).

BACKEND
- POST /leave-requests (siswa, multipart): session_id (sesi milik kelasnya, belum ditutup atau maksimal H+N hari sesuai env LEAVE_MAX_BACKDATE_DAYS), tipe, alasan, lampiran opsional (JPEG/PNG/PDF maks 5 MB, simpan privat, kunci attachment_key). Satu pengajuan aktif per siswa per sesi.
- GET /leave-requests (siswa: miliknya; guru: kelasnya; operator: semua; filter status/tanggal/kelas).
- PATCH /leave-requests/:id (operator/guru kelas): setujui atau tolak. Saat disetujui, upsert attendances untuk siswa+sesi dengan status izin/sakit, source 'leave', dalam satu transaksi, tulis audit log.
- Job penutupan sesi: untuk sesi yang close_at terlewati dan masih 'terbuka' -> set 'ditutup', lalu untuk setiap anggota kelas (siswa) tanpa baris attendances, insert status alpa (source 'system'). Idempoten dan aman dijalankan ulang.
- Bila izin disetujui setelah sesi ditutup dan sudah terlanjur alpa, ubah menjadi izin/sakit dan catat di audit log.

ANDROID
- Siswa: form pengajuan izin (pilih sesi, tipe, alasan, lampiran dari kamera atau pemilih berkas), daftar dan status pengajuan.
- Guru/operator: daftar pengajuan menunggu, detail dengan pratinjau lampiran (signed URL), tombol setujui/tolak dengan catatan.

TES WAJIB
- Backend: alur pengajuan->persetujuan->status absensi; duplikat pengajuan; guru kelas lain ditolak; job alpa (idempoten, tidak menimpa hadir/izin); izin disetujui setelah alpa; batas ukuran dan tipe lampiran.

GERBANG VERIFIKASI
- Semua tes lulus; simulasi sesi berakhir menghasilkan alpa yang benar dan rekap konsisten.
```

---

## F6: Rekap dan ekspor Excel

```
FASE F6: rekap dan ekspor Excel (FR-EXP, bagian 9 RND.md).

BACKEND
- GET /exports/attendances.xlsx (operator, guru). Query: from, to (wajib, maks rentang sesuai env EXPORT_MAX_RANGE_DAYS), classId, subjectId, studentId, status. Untuk guru, filter kelas dipaksa ke kelas yang ia ajar di lapisan query (bukan hanya validasi parameter).
- Gunakan exceljs mode streaming (WorkbookWriter) supaya memori stabil untuk data besar.
- Sheet "Rekap": kolom persis bagian 9 (No, Tanggal, Hari, Kelas, Mapel, Guru, Nomor induk, Nama siswa, Status, Jam absen WIB, Nama lokasi, Latitude, Longitude, Jarak ke titik, Akurasi GPS, Tautan selfie, Catatan, Sumber). Header tebal, freeze pane, auto filter, lebar kolom wajar. Waktu dikonversi ke WIB. Tautan selfie berupa hyperlink ke signed URL berumur panjang yang wajar (env EXPORT_LINK_TTL_HOURS, default 168), dan jelaskan di sheet bahwa tautan kedaluwarsa.
- Sheet "Ringkasan": per siswa, jumlah hadir/terlambat/izin/sakit/alpa dan persentase kehadiran, ditambah baris judul berisi filter yang dipakai dan waktu pembuatan.
- Nama berkas: absensi_{kelas|semua}_{from}_{to}.xlsx. Header Content-Disposition benar.
- Catat setiap ekspor di audit_logs (siapa, filter).
- Indeks database dievaluasi dengan EXPLAIN untuk query ekspor; tambahkan indeks yang diperlukan lewat migrasi.

ANDROID
- Guru dan operator: layar Rekap dengan filter (rentang tanggal, kelas, mapel, status) dan daftar hasil.
- Tombol Unduh Excel: simpan ke folder Unduhan lewat MediaStore (tanpa izin penyimpanan lama) atau Storage Access Framework, tampilkan notifikasi/snackbar dengan aksi Buka dan Bagikan.

TES WAJIB
- Backend: isi dan urutan kolom (baca ulang file yang dihasilkan dengan exceljs), konversi WIB, filter, pembatasan guru (guru tidak bisa mengekspor kelas lain walau memanipulasi parameter), rentang melebihi batas ditolak, data kosong menghasilkan file valid.

GERBANG VERIFIKASI
- Semua tes lulus. Hasilkan contoh file dari data seed dan lampirkan jalurnya di laporan; jelaskan cara memeriksanya manual.
```

---

## F7: Notifikasi, audit, polesan

```
FASE F7: notifikasi (FR-NTF), log audit (FR-AUD), polesan UI.

BACKEND
- Integrasi firebase-admin (kredensial dari env). Modul notifications dengan interface sehingga bisa di-mock di tes.
- Kirim push: (a) sesi dibuka -> ke perangkat aktif siswa kelas tersebut, (b) pengingat sebelum jadwal (job, offset dari env), (c) hasil persetujuan izin -> ke siswa terkait, (d) permintaan perangkat baru -> ke operator.
- Penanganan token tidak valid: hapus/nonaktifkan fcm_token yang ditolak FCM.
- Endpoint PATCH /devices/me/fcm-token untuk memperbarui token.
- GET /audit-logs (operator): filter actor, action, entity, rentang waktu, pagination. Pastikan semua aksi di RND.md bagian FR-AUD tercatat konsisten (periksa dan lengkapi yang terlewat di fase sebelumnya).

ANDROID
- Firebase Messaging Service, channel notifikasi, permintaan izin POST_NOTIFICATIONS (API 33+), deep link dari notifikasi ke layar terkait.
- Layar Log Audit untuk operator.
- Polesan: state loading/empty/error konsisten di semua layar, dukungan tema gelap, pesan error Indonesia seragam, penanganan offline (pesan jelas + tombol coba lagi; absen TIDAK boleh dibuat offline-first), aksesibilitas dasar (contentDescription, ukuran sentuh).

TES WAJIB
- Backend: pemicu notifikasi dengan notifier tiruan, penanganan token tidak valid, filter audit.

GERBANG VERIFIKASI
- Semua tes lulus. Checklist manual notifikasi di perangkat/emulator dengan Google Play Services dilaporkan sebagai "belum terverifikasi" bila tidak bisa dijalankan.
```

---

## F8: Hardening dan rilis

```
FASE F8: hardening, pengujian menyeluruh, build rilis.

BACKEND
- helmet, CORS dengan allowlist dari env, batas ukuran body dan field multipart, timeout, penanganan SIGTERM (graceful shutdown), log terstruktur tanpa data sensitif (pastikan tidak ada password, token, koordinat lengkap bersama identitas di log level info).
- npm audit dan perbaiki kerentanan; jalankan pemeriksaan secret scanning sederhana pada repo.
- Dockerfile produksi backend (multi-stage, pengguna non-root) dan contoh docker-compose.prod.yml. Dokumen deployment ringkas (variabel env, migrasi, backup database, retensi selfie sesuai env SELFIE_RETENTION_DAYS dengan job pembersih).

ANDROID
- Build rilis: R8 aktif (minify + shrink resources), aturan ProGuard untuk Retrofit/serialization/Hilt, signing config dari variabel lokal (jangan commit keystore), BASE_URL produksi lewat build variant. Nonaktifkan cleartext traffic pada rilis; pertimbangkan network security config.
- Pastikan tidak ada log debug berisi token/koordinat pada rilis.

PENGUJIAN MENYELURUH
- Jalankan seluruh skenario bagian 13 docs/RND.md dan hasilkan docs/TESTING.md berisi tabel: skenario, cara uji (otomatis/manual), hasil, bukti. Skenario yang butuh perangkat fisik atau akses konsol ditandai "manual - belum dijalankan" dengan langkah rinci.
- Uji beban ringan endpoint absen (mis. 50 siswa bersamaan pada satu sesi) dan laporkan latensi p95.

REVIEW KEAMANAN AKHIR
- Telaah mandiri kode terhadap daftar: authz di setiap endpoint (cari endpoint tanpa middleware), IDOR (akses data milik user lain lewat id), injeksi (semua query lewat Prisma/parameter), unggah berkas, kebocoran data di respons (password_hash tidak pernah keluar), enumerasi akun, brute force, kedaluwarsa signed URL. Laporkan temuan dengan tingkat keparahan dan perbaiki yang kritis/tinggi.

GERBANG VERIFIKASI
- Semua tes lulus; docs/TESTING.md lengkap; APK rilis terbangun; daftar risiko tersisa dan tindakan manual ditulis di laporan akhir.
```

---

## Prompt pendukung

### Perbaikan bug
```
Ada bug: [deskripsi singkat + langkah reproduksi + hasil yang diharapkan vs aktual + log/error].
Prosedur: (1) reproduksi dulu dan tulis tes yang gagal, (2) jelaskan akar masalah, (3) perbaiki dengan perubahan sekecil mungkin, (4) pastikan tes baru dan seluruh tes lama lulus, (5) laporkan berkas yang berubah. Jangan melakukan refaktor di luar kebutuhan perbaikan.
```

### Lanjut sesi baru
```
Sesi baru. Baca AGENTS.md dan docs/RND.md. Periksa kondisi repo (git log, git status, hasil tes terakhir) dan simpulkan fase mana yang sudah selesai, setengah jalan, atau belum. Jangan mengubah apa pun. Laporkan temuanmu dan usulkan langkah berikutnya.
```

### Audit kesesuaian terhadap dokumen
```
Bandingkan implementasi saat ini dengan docs/RND.md bagian 3 (FR), 6 (validasi), 7 (skema), dan 8 (API). Buat tabel: item dokumen | status (sesuai/menyimpang/belum ada) | berkas bukti. Jangan mengubah kode; hanya laporkan. Tandai penyimpangan yang berisiko pada keamanan atau akurasi data.
```
