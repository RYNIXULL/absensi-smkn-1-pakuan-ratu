# RnD: Aplikasi Absensi Android Berbasis Lokasi + Selfie

Dokumen ini adalah sumber kebenaran (source of truth) proyek. Agen wajib membacanya sebelum mengerjakan tugas apa pun dan memperbarui bagian yang berubah.

## 1. Ringkasan

Aplikasi Android mirip LMS untuk absensi dan pengumpulan jadwal. Siswa hanya bisa absen bila berada di lokasi yang sudah ditentukan, dengan lokasi (GPS) aktif dan asli (bukan fake GPS), serta mengirim selfie langsung dari kamera. Operator dan guru dapat melihat dan mengunduh rekap absensi dalam format Excel lengkap dengan keterangan tempat, waktu, dan bukti.

### Tujuan
- Mencegah titip absen dan absen dari luar lokasi.
- Menyediakan bukti absensi yang bisa diaudit (lokasi, waktu server, selfie).
- Memudahkan guru dan operator merekap dan mengunduh data.

### Di luar lingkup (versi 1)
- Aplikasi iOS dan web admin.
- Pembelajaran daring (materi, tugas, nilai).
- Face recognition penuh (hanya disiapkan sebagai pengembangan lanjutan).

## 2. Peran dan hak akses (RBAC)

| Fitur | Operator | Guru | Siswa |
|---|---|---|---|
| Kelola pengguna (CRUD, impor Excel, reset password, nonaktifkan) | Ya | Tidak | Tidak |
| Kelola kelas, mapel, anggota kelas | Ya | Tidak | Tidak |
| Kelola lokasi (titik + radius) | Ya | Tidak | Tidak |
| Kelola jadwal | Semua | Kelas sendiri (opsional, lihat FR-JDW-03) | Tidak |
| Buka/tutup sesi absensi | Ya | Kelas sendiri | Tidak |
| Absen (selfie + lokasi) | Tidak | Tidak | Ya |
| Lihat absensi | Semua | Kelas sendiri | Diri sendiri |
| Koreksi status manual | Ya | Kelas sendiri | Tidak |
| Ajukan izin/sakit | Tidak | Tidak | Ya |
| Setujui/tolak izin | Ya | Kelas sendiri | Tidak |
| Unduh Excel | Semua | Kelas sendiri | Tidak |
| Lihat log audit | Ya | Tidak | Tidak |
| Setujui pergantian perangkat | Ya | Tidak | Tidak |

Aturan: pengecekan role dan kepemilikan data (misalnya guru hanya kelasnya) WAJIB di backend (middleware), bukan hanya menyembunyikan menu di aplikasi.

## 3. Kebutuhan fungsional

### Autentikasi (FR-AUTH)
- FR-AUTH-01: Login dengan nomor induk/email + password. Keluaran: access token (JWT, umur pendek) dan refresh token.
- FR-AUTH-02: Satu akun terikat ke satu perangkat aktif. Login di perangkat baru menghasilkan permintaan ganti perangkat yang menunggu persetujuan operator (siswa). Operator dan guru boleh lebih dari satu perangkat.
- FR-AUTH-03: Ganti password. Reset password oleh operator.
- FR-AUTH-04: Logout dan pencabutan refresh token.

### Master data (FR-MST)
- FR-MST-01: CRUD pengguna oleh operator, termasuk impor massal dari Excel (template disediakan).
- FR-MST-02: CRUD kelas, mapel, dan anggota kelas.
- FR-MST-03: CRUD lokasi: nama, latitude, longitude, radius (meter), status aktif.

### Jadwal (FR-JDW)
- FR-JDW-01: Jadwal berulang mingguan: kelas, mapel, guru, lokasi, hari, jam mulai, jam selesai.
- FR-JDW-02: Siswa dan guru melihat jadwal sesuai kelas/pengajaran masing-masing.
- FR-JDW-03: Pengaturan: apakah guru boleh mengubah jadwal (default: hanya operator).

### Sesi absensi (FR-SES)
- FR-SES-01: Sesi dapat dibuat otomatis dari jadwal (cron) atau dibuka manual oleh guru/operator.
- FR-SES-02: Sesi punya open_at, close_at, late_after. Di luar rentang waktu, absen ditolak.
- FR-SES-03: Saat sesi ditutup, siswa yang belum absen dan tidak punya izin disetel `alpa` otomatis.

### Absensi (FR-ABS)
- FR-ABS-01: Siswa hanya bisa absen pada sesi yang terbuka untuk kelasnya.
- FR-ABS-02: Wajib lokasi aktif; koordinat dikirim bersama akurasi.
- FR-ABS-03: Jarak ke titik lokasi dihitung di server (Haversine) dan harus <= radius.
- FR-ABS-04: Wajib selfie langsung dari kamera (tanpa galeri), diberi watermark (nama, waktu, koordinat) sebelum atau saat diunggah.
- FR-ABS-05: Waktu absen = waktu server, bukan jam perangkat.
- FR-ABS-06: Satu siswa hanya bisa absen satu kali per sesi.
- FR-ABS-07: Status: `hadir`, `terlambat`, `izin`, `sakit`, `alpa`.
- FR-ABS-08: Aplikasi menolak berjalan fitur absen bila fake GPS/root/emulator terdeteksi (lihat bagian 6).
- FR-ABS-09: Koreksi manual oleh guru/operator wajib disertai catatan dan tercatat di audit log.

### Izin (FR-IZN)
- FR-IZN-01: Siswa mengajukan izin/sakit untuk sesi/tanggal tertentu, dengan alasan dan lampiran (foto surat).
- FR-IZN-02: Guru/operator menyetujui atau menolak. Bila disetujui, status absensi menjadi izin/sakit.

### Rekap dan ekspor (FR-EXP)
- FR-EXP-01: Filter: rentang tanggal, kelas, mapel, siswa, status.
- FR-EXP-02: Unduh Excel (.xlsx) dengan kolom pada bagian 9.
- FR-EXP-03: Bukti selfie berupa tautan aman (signed URL berumur panjang yang wajar) di Excel.

### Notifikasi dan audit (FR-NTF, FR-AUD)
- FR-NTF-01: Push notification (FCM): sesi dibuka, pengingat jadwal, hasil persetujuan izin.
- FR-AUD-01: Semua perubahan data penting dicatat: siapa, kapan, nilai lama dan baru.

## 4. Kebutuhan non-fungsional
- Keamanan: HTTPS wajib, password di-hash (argon2id atau bcrypt), JWT umur pendek, rate limiting pada login dan endpoint absen, validasi input (zod), tidak ada rahasia di repo.
- Privasi: selfie dan koordinat adalah data pribadi. Simpan di storage privat, akses lewat signed URL, tetapkan masa retensi (default 1 tahun, dapat diatur).
- Kinerja: endpoint absen < 3 detik pada jaringan 4G normal. Foto dikompres di klien (maks ~500 KB, sisi terpanjang 1280 px).
- Ketersediaan: backend stateless, mudah dideploy ulang.
- Kompatibilitas: Android 8.0 (API 26) ke atas. Target SDK terbaru.
- Lokalisasi: antarmuka Bahasa Indonesia, zona waktu Asia/Jakarta (WIB) untuk tampilan; simpan UTC di database.

## 5. Arsitektur dan tech stack

```
[Android: Kotlin + Compose]  --HTTPS/JSON-->  [Backend: Node.js + Express + TS]  -->  [PostgreSQL]
        |                                              |
        +-- FCM (push)                                 +--> [Object storage privat (selfie, lampiran izin)]
        +-- Play Integrity                             +--> Verifikasi Play Integrity (server-side)
```

### Android
- Kotlin, Jetpack Compose (Material 3), arsitektur MVVM + Repository, Hilt (DI).
- Retrofit + OkHttp + kotlinx.serialization, DataStore (token), Coroutines + Flow.
- CameraX (selfie, kamera depan, tanpa galeri), Fused Location Provider (lokasi).
- Play Integrity API, Firebase Cloud Messaging.
- Navigation Compose dengan graf terpisah per role.

### Backend
- Node.js 20+, Express, TypeScript, Prisma (PostgreSQL), zod, jsonwebtoken, argon2.
- multer (unggah), sharp (kompresi/EXIF strip), exceljs (ekspor), node-cron (sesi otomatis/alpa otomatis), pino (log).
- Storage: S3-compatible atau Supabase Storage (bucket privat).
- Docker + docker-compose untuk lokal (app + postgres).

## 6. Alur absensi dan anti-kecurangan

### Alur di sisi siswa
1. Siswa membuka sesi aktif -> aplikasi meminta `challenge` (nonce sekali pakai) dari server.
2. Aplikasi cek lokasi aktif, izin lokasi dan kamera. Bila tidak aktif, tampilkan layar penjelasan dan blokir tombol absen.
3. Aplikasi mengambil lokasi (akurasi tinggi), memeriksa flag mock location, root, emulator, dan meminta Play Integrity token dengan nonce tadi.
4. Siswa ambil selfie via CameraX.
5. Aplikasi mengirim: lat, lng, accuracy, flag klien, integrity token, selfie, device_id.
6. Server memvalidasi (di bawah) dan membalas sukses/gagal beserta alasan.

### Validasi server (urutan wajib)
1. JWT valid dan role = siswa.
2. Sesi ada, status terbuka, dan waktu server dalam [open_at, close_at].
3. Siswa anggota kelas pada jadwal sesi tersebut.
4. `device_id` sama dengan perangkat aktif siswa.
5. Nonce valid, belum dipakai, belum kedaluwarsa. Verifikasi Play Integrity token (verdict perangkat dan aplikasi, nonce cocok).
6. Tolak bila flag mock location dari klien = true (tetap catat percobaan di audit).
7. Akurasi GPS <= batas (default 50 m, dapat dikonfigurasi). Tolak bila lebih buruk.
8. Hitung jarak Haversine ke lokasi sesi. Tolak bila > radius.
9. (Opsional) Cek lompatan lokasi tidak wajar terhadap lokasi absen sebelumnya (kecepatan implisit).
10. Validasi selfie: tipe JPEG, ukuran maksimum, strip EXIF, simpan ke storage privat.
11. Tentukan status: `hadir` bila waktu server <= late_after, selain itu `terlambat`.
12. Insert ke `attendances` (unique `session_id + student_id`). Tulis audit log.

### Catatan penting
- Deteksi di sisi klien bisa dibypass. Karena itu validasi server dan Play Integrity adalah lapisan utama; flag klien hanyalah lapisan tambahan.
- Jam perangkat tidak dipercaya.
- Semua penolakan mengembalikan kode error yang jelas (lihat bagian 8) supaya UI bisa menampilkan pesan yang tepat.

## 7. Skema database (PostgreSQL)

```sql
CREATE TYPE user_role AS ENUM ('operator', 'guru', 'siswa');
CREATE TYPE attendance_status AS ENUM ('hadir', 'terlambat', 'izin', 'sakit', 'alpa');
CREATE TYPE session_status AS ENUM ('terjadwal', 'terbuka', 'ditutup');
CREATE TYPE request_status AS ENUM ('menunggu', 'disetujui', 'ditolak');
CREATE TYPE leave_type AS ENUM ('izin', 'sakit');

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  role user_role NOT NULL,
  nama TEXT NOT NULL,
  nomor_induk TEXT UNIQUE NOT NULL,
  email TEXT UNIQUE,
  password_hash TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE devices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_id TEXT NOT NULL,
  model TEXT,
  fcm_token TEXT,
  status request_status NOT NULL DEFAULT 'menunggu',
  is_active BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, device_id)
);

CREATE TABLE classes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nama TEXT NOT NULL,
  tahun_ajaran TEXT NOT NULL
);

CREATE TABLE class_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE (class_id, user_id)
);

CREATE TABLE subjects (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nama TEXT NOT NULL
);

CREATE TABLE locations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nama TEXT NOT NULL,
  latitude NUMERIC(9,6) NOT NULL,
  longitude NUMERIC(9,6) NOT NULL,
  radius_m INT NOT NULL CHECK (radius_m > 0),
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE schedules (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  class_id UUID NOT NULL REFERENCES classes(id),
  subject_id UUID NOT NULL REFERENCES subjects(id),
  teacher_id UUID NOT NULL REFERENCES users(id),
  location_id UUID NOT NULL REFERENCES locations(id),
  day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  start_time TIME NOT NULL,
  end_time TIME NOT NULL
);
CREATE INDEX idx_schedules_class_day ON schedules (class_id, day_of_week);

CREATE TABLE attendance_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  schedule_id UUID NOT NULL REFERENCES schedules(id),
  opened_by UUID REFERENCES users(id),
  tanggal DATE NOT NULL,
  open_at TIMESTAMPTZ NOT NULL,
  close_at TIMESTAMPTZ NOT NULL,
  late_after TIMESTAMPTZ NOT NULL,
  status session_status NOT NULL DEFAULT 'terjadwal',
  UNIQUE (schedule_id, tanggal)
);

CREATE TABLE attendances (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id UUID NOT NULL REFERENCES attendance_sessions(id) ON DELETE CASCADE,
  student_id UUID NOT NULL REFERENCES users(id),
  device_id UUID REFERENCES devices(id),
  status attendance_status NOT NULL,
  checkin_at TIMESTAMPTZ,
  latitude NUMERIC(9,6),
  longitude NUMERIC(9,6),
  accuracy_m REAL,
  distance_m REAL,
  selfie_key TEXT,
  mock_detected BOOLEAN NOT NULL DEFAULT FALSE,
  integrity_verdict TEXT,
  source TEXT NOT NULL DEFAULT 'app',
  catatan TEXT,
  UNIQUE (session_id, student_id)
);
CREATE INDEX idx_att_student_time ON attendances (student_id, checkin_at);

CREATE TABLE leave_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id UUID NOT NULL REFERENCES users(id),
  session_id UUID NOT NULL REFERENCES attendance_sessions(id),
  tipe leave_type NOT NULL,
  alasan TEXT NOT NULL,
  attachment_key TEXT,
  status request_status NOT NULL DEFAULT 'menunggu',
  reviewed_by UUID REFERENCES users(id),
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE attendance_challenges (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  session_id UUID NOT NULL REFERENCES attendance_sessions(id),
  nonce TEXT UNIQUE NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ
);

CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ
);

CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id UUID REFERENCES users(id),
  action TEXT NOT NULL,
  entity TEXT NOT NULL,
  entity_id UUID,
  old_value JSONB,
  new_value JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Catatan: Prisma schema dibuat setara dengan DDL di atas. Pengubahan skema hanya lewat migrasi.

## 8. Desain API (REST, prefix `/api/v1`)

Format error seragam: `{ "error": { "code": "STRING", "message": "..." } }`.

| Metode | Endpoint | Role | Fungsi |
|---|---|---|---|
| POST | /auth/login | publik | Login |
| POST | /auth/refresh | publik | Perbarui token |
| POST | /auth/logout | semua | Cabut refresh token |
| POST | /auth/change-password | semua | Ganti password |
| GET | /me | semua | Profil sendiri |
| GET/POST/PATCH | /users | operator | Kelola pengguna |
| POST | /users/import | operator | Impor Excel |
| POST | /users/:id/reset-password | operator | Reset password |
| GET/POST/PATCH/DELETE | /classes, /subjects, /locations | operator | Master data |
| POST/DELETE | /classes/:id/members | operator | Anggota kelas |
| GET/POST/PATCH/DELETE | /schedules | operator (guru bila diizinkan) | Jadwal |
| GET | /me/schedules | guru, siswa | Jadwal pribadi |
| GET | /devices/requests | operator | Daftar permintaan ganti perangkat |
| PATCH | /devices/:id | operator | Setujui/tolak perangkat |
| POST | /sessions | operator, guru | Buka sesi manual |
| PATCH | /sessions/:id/close | operator, guru | Tutup sesi |
| GET | /sessions | semua (terfilter) | Daftar sesi |
| POST | /sessions/:id/challenge | siswa | Minta nonce |
| POST | /sessions/:id/attendance | siswa | Kirim absen (multipart) |
| GET | /me/attendances | siswa | Riwayat sendiri |
| GET | /attendances | operator, guru | Rekap terfilter |
| PATCH | /attendances/:id | operator, guru | Koreksi manual (wajib catatan) |
| GET | /attendances/:id/selfie | operator, guru, siswa pemilik | Signed URL selfie |
| POST | /leave-requests | siswa | Ajukan izin |
| GET | /leave-requests | semua (terfilter) | Daftar izin |
| PATCH | /leave-requests/:id | operator, guru | Setujui/tolak |
| GET | /exports/attendances.xlsx | operator, guru | Unduh Excel |
| GET | /audit-logs | operator | Log audit |

### Kode error absensi
`SESSION_CLOSED`, `NOT_IN_CLASS`, `DEVICE_MISMATCH`, `NONCE_INVALID`, `INTEGRITY_FAILED`, `MOCK_LOCATION`, `LOW_ACCURACY`, `OUT_OF_RADIUS`, `ALREADY_CHECKED_IN`, `INVALID_SELFIE`.

## 9. Format ekspor Excel

Satu baris = satu absensi. Kolom:
No | Tanggal | Hari | Kelas | Mapel | Guru | Nomor induk | Nama siswa | Status | Jam absen (WIB) | Nama lokasi | Latitude | Longitude | Jarak ke titik (m) | Akurasi GPS (m) | Tautan selfie | Catatan | Sumber (app/manual)

Ketentuan:
- Sheet 1 "Rekap" (data per baris), Sheet 2 "Ringkasan" (jumlah hadir/terlambat/izin/sakit/alpa per siswa).
- Judul/filter yang dipakai ditulis di baris awal. Header dibekukan (freeze pane), kolom otomatis lebar.
- Guru hanya mendapat data kelasnya (dipaksa di backend).

## 10. Struktur proyek

```
/
├── AGENTS.md
├── docs/RND.md
├── backend/
│   ├── prisma/schema.prisma
│   └── src/
│       ├── modules/{auth,users,classes,locations,schedules,sessions,attendance,leave,exports,audit}/
│       ├── middleware/{auth,rbac,rateLimit,error}.ts
│       ├── lib/{haversine,integrity,storage,excel}.ts
│       └── jobs/{openSessions,autoAlpa}.ts
└── android/app/src/main/java/.../
    ├── data/{remote,local,repository}
    ├── di/
    ├── domain/
    ├── security/{MockLocationChecker,RootChecker,IntegrityManager}.kt
    └── ui/{auth,siswa,guru,operator,common}/
```

## 11. Layar aplikasi per role
- Umum: Login, Profil/Ganti password.
- Siswa: Beranda (jadwal hari ini + sesi aktif), Layar Absen (status lokasi, kamera, kirim), Riwayat, Ajukan izin, Jadwal.
- Guru: Beranda (kelas hari ini), Kelola sesi (buka/tutup), Rekap kelas + filter + unduh Excel, Persetujuan izin, Koreksi status.
- Operator: Dashboard, Pengguna, Kelas dan anggota, Lokasi (peta + radius), Jadwal, Persetujuan perangkat, Rekap semua + unduh, Log audit.

## 12. Rencana pengerjaan dan kriteria selesai

| Fase | Isi | Kriteria selesai |
|---|---|---|
| F0 | Setup repo, docker-compose, skema Prisma, CI dasar | `docker compose up` jalan; migrasi sukses |
| F1 | Auth, RBAC, manajemen pengguna, ikat perangkat | Login 3 role; endpoint terlarang mengembalikan 403; tes RBAC lulus |
| F2 | Master data: kelas, mapel, lokasi, jadwal | CRUD lengkap + validasi; impor Excel pengguna |
| F3 | Sesi + absensi inti (lokasi, jarak, selfie, waktu server) | Absen sukses di dalam radius, ditolak di luar radius; tes unit Haversine |
| F4 | Anti-cheat: nonce, Play Integrity, deteksi mock/root, akurasi | Semua kode error absensi teruji; percobaan palsu tercatat |
| F5 | Izin/sakit + persetujuan + alpa otomatis | Alur izin end-to-end; cron alpa teruji |
| F6 | Ekspor Excel + filter | File terbuka di Excel, kolom sesuai bagian 9, guru terbatas kelasnya |
| F7 | FCM, audit log, polesan UI, penanganan error | Notifikasi sesi dibuka; audit tercatat |
| F8 | Pengujian menyeluruh, hardening, rilis APK | Daftar uji bagian 13 lulus |

## 13. Pengujian
- Backend: unit test (Haversine, penentuan status, RBAC), integration test endpoint absen (Supertest) dengan semua kode error.
- Android: unit test ViewModel; uji manual di perangkat nyata untuk lokasi, kamera, dan izin.
- Skenario wajib: di dalam radius, di luar radius, lokasi mati, izin ditolak, fake GPS aktif, perangkat berbeda, absen dua kali, sesi sudah ditutup, absen terlambat, akurasi buruk, unduh Excel oleh guru kelas lain (harus ditolak).

## 14. Risiko dan mitigasi
| Risiko | Mitigasi |
|---|---|
| Fake GPS dibypass di sisi klien | Play Integrity + validasi server + audit; jangan andalkan flag klien |
| GPS tidak akurat di dalam gedung | Batas akurasi dapat dikonfigurasi, radius realistis, izinkan koreksi manual guru |
| Titip absen dengan foto orang lain | Selfie langsung dari kamera, watermark, ikat perangkat; face matching sebagai pengembangan lanjutan |
| Ukuran Excel membengkak | Selfie berupa tautan, bukan gambar tertanam |
| Kebocoran data pribadi | Bucket privat, signed URL, retensi data, HTTPS, hak akses ketat |
| Perangkat siswa tanpa Google Play Services | Tetapkan kebijakan (tolak atau jalur manual oleh guru) dan dokumentasikan |

## 15. Prompt siap pakai untuk agen (per fase)

Gunakan satu prompt per fase, mulai dari F0. Setiap prompt diawali: "Baca AGENTS.md dan docs/RND.md terlebih dahulu."

- F0: "Buat kerangka monorepo sesuai bagian 10: backend Node+Express+TS+Prisma, docker-compose dengan PostgreSQL, skema Prisma setara DDL bagian 7, dan proyek Android Kotlin+Compose kosong dengan Hilt. Tambahkan README cara menjalankan."
- F1: "Implementasikan FR-AUTH-01 sampai 04 dan middleware RBAC sesuai matriks bagian 2, termasuk pengikatan perangkat. Sertakan tes untuk akses terlarang."
- F2: "Implementasikan FR-MST dan FR-JDW di backend beserta layar operator di Android."
- F3: "Implementasikan FR-SES dan FR-ABS-01 sampai 07 sesuai alur validasi bagian 6 (tanpa Play Integrity dulu). Android: layar absen dengan Fused Location dan CameraX."
- F4: "Tambahkan nonce/challenge, verifikasi Play Integrity di server, MockLocationChecker, RootChecker, dan batas akurasi. Semua kode error bagian 8 harus teruji."
- F5: "Implementasikan FR-IZN dan job alpa otomatis."
- F6: "Implementasikan FR-EXP dengan exceljs sesuai kolom bagian 9 dan pembatasan data untuk guru."
- F7: "Tambahkan FCM, audit log lengkap, dan polesan UI."
- F8: "Jalankan seluruh skenario bagian 13, perbaiki temuan, dan siapkan build rilis."
