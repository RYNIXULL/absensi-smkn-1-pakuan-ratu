import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import ExcelJS from 'exceljs';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';
import { AttendanceStatus } from '@prisma/client';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findUnique: vi.fn(),
        findFirst: vi.fn(),
      },
      schedule: {
        findFirst: vi.fn(),
      },
      attendance: {
        findMany: vi.fn(),
      },
    },
  };
});

describe('Export Attendance Excel Tests (FR-EXP-01..03)', () => {
  const operatorToken = signAccessToken({ sub: 'user-operator', role: 'operator' });
  const guru1Token = signAccessToken({ sub: 'user-guru-1', role: 'guru' });
  const guru2Token = signAccessToken({ sub: 'user-guru-2', role: 'guru' });
  const siswaToken = signAccessToken({ sub: 'user-siswa', role: 'siswa' });

  const mockAttendances = [
    {
      id: 'att-1',
      session_id: 'sess-1',
      student_id: 'student-1',
      status: AttendanceStatus.hadir,
      checkin_at: new Date('2026-10-24T00:05:00.000Z'),
      latitude: -6.2088,
      longitude: 106.8456,
      distance_m: 15.2,
      accuracy_m: 8.0,
      selfie_key: 'selfies/student-1.jpg',
      catatan: 'Hadir tepat waktu',
      source: 'app',
      student: {
        id: 'student-1',
        nama: 'Ahmad Siswa',
        nomor_induk: '1001',
      },
      session: {
        id: 'sess-1',
        tanggal: new Date('2026-10-24T00:00:00.000Z'),
        status: 'ditutup',
        schedule: {
          id: 'sched-1',
          teacher_id: 'user-guru-1',
          class: { id: 'class-1', name: 'X-IPA-1' },
          subject: { id: 'subj-1', name: 'Matematika Wajib' },
          teacher: { id: 'user-guru-1', nama: 'Budi Guru' },
          location: { id: 'loc-1', name: 'Kampus Utama' },
        },
      },
    },
    {
      id: 'att-2',
      session_id: 'sess-1',
      student_id: 'student-2',
      status: AttendanceStatus.alpa,
      checkin_at: null,
      latitude: null,
      longitude: null,
      distance_m: null,
      accuracy_m: null,
      selfie_key: null,
      catatan: 'Tidak hadir tanpa keterangan',
      source: 'system',
      student: {
        id: 'student-2',
        nama: 'Budi Santoso',
        nomor_induk: '1002',
      },
      session: {
        id: 'sess-1',
        tanggal: new Date('2026-10-24T00:00:00.000Z'),
        status: 'ditutup',
        schedule: {
          id: 'sched-1',
          teacher_id: 'user-guru-1',
          class: { id: 'class-1', name: 'X-IPA-1' },
          subject: { id: 'subj-1', name: 'Matematika Wajib' },
          teacher: { id: 'user-guru-1', nama: 'Budi Guru' },
          location: { id: 'loc-1', name: 'Kampus Utama' },
        },
      },
    },
    {
      id: 'att-3',
      session_id: 'sess-1',
      student_id: 'student-3',
      status: AttendanceStatus.izin,
      checkin_at: null,
      latitude: null,
      longitude: null,
      distance_m: null,
      accuracy_m: null,
      selfie_key: null,
      catatan: 'Izin acara keluarga',
      source: 'izin',
      student: {
        id: 'student-3',
        nama: 'Citra Dewi',
        nomor_induk: '1003',
      },
      session: {
        id: 'sess-1',
        tanggal: new Date('2026-10-24T00:00:00.000Z'),
        status: 'ditutup',
        schedule: {
          id: 'sched-1',
          teacher_id: 'user-guru-1',
          class: { id: 'class-1', name: 'X-IPA-1' },
          subject: { id: 'subj-1', name: 'Matematika Wajib' },
          teacher: { id: 'user-guru-1', nama: 'Budi Guru' },
          location: { id: 'loc-1', name: 'Kampus Utama' },
        },
      },
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }: any) => {
      if (where.id === 'user-operator') {
        return { id: 'user-operator', nama: 'Operator Utama', role: 'operator', is_active: true } as any;
      }
      if (where.id === 'user-guru-1') {
        return { id: 'user-guru-1', nama: 'Budi Guru', role: 'guru', is_active: true } as any;
      }
      if (where.id === 'user-guru-2') {
        return { id: 'user-guru-2', nama: 'Siti Guru', role: 'guru', is_active: true } as any;
      }
      if (where.id === 'user-siswa') {
        return { id: 'user-siswa', nama: 'Ahmad Siswa', role: 'siswa', is_active: true } as any;
      }
      return null;
    });

    vi.mocked(prisma.attendance.findMany).mockResolvedValue(mockAttendances as any);
  });

  it('Siswa: should be rejected with 403 FORBIDDEN', async () => {
    const res = await request(app)
      .get('/api/v1/exports/attendances.xlsx')
      .set('Authorization', `Bearer ${siswaToken}`);

    expect(res.status).toBe(403);
    expect(res.body.error.code).toBe('FORBIDDEN');
  });

  it('Unauthenticated: should be rejected with 401 UNAUTHORIZED', async () => {
    const res = await request(app).get('/api/v1/exports/attendances.xlsx');

    expect(res.status).toBe(401);
  });

  it('Operator: should download valid Excel workbook with Rekap and Ringkasan sheets', async () => {
    const res = await request(app)
      .get('/api/v1/exports/attendances.xlsx')
      .set('Authorization', `Bearer ${operatorToken}`)
      .buffer(true)
      .parse((res, callback) => {
        const chunks: Buffer[] = [];
        res.on('data', (chunk) => chunks.push(Buffer.from(chunk)));
        res.on('end', () => callback(null, Buffer.concat(chunks)));
      });

    expect(res.status).toBe(200);
    expect(res.headers['content-type']).toContain(
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    );
    expect(res.headers['content-disposition']).toMatch(/attachment; filename="rekap-absensi-\d+\.xlsx"/);

    // Verifikasi isi Excel dengan ExcelJS
    const workbook = new ExcelJS.Workbook();
    await (workbook.xlsx.load as (b: Buffer) => Promise<ExcelJS.Workbook>)(res.body);

    expect(workbook.worksheets.length).toBe(2);

    // Sheet 1: Rekap
    const rekapSheet = workbook.getWorksheet('Rekap');
    expect(rekapSheet).toBeDefined();
    expect(rekapSheet!.getCell('A1').value).toBe('REKAPITULASI PRESENSI SISWA');

    // Kolom Header di baris 6
    const row6Values = (rekapSheet!.getRow(6).values as any[]).slice(1);
    expect(row6Values).toEqual([
      'No',
      'Tanggal',
      'Hari',
      'Kelas',
      'Mapel',
      'Guru',
      'Nomor Induk',
      'Nama Siswa',
      'Status',
      'Jam Absen (WIB)',
      'Nama Lokasi',
      'Latitude',
      'Longitude',
      'Jarak ke Titik (m)',
      'Akurasi GPS (m)',
      'Tautan Selfie',
      'Catatan',
      'Sumber',
    ]);

    // Baris Data Pertama (Ahmad Siswa - Hadir)
    const row7 = rekapSheet!.getRow(7);
    expect(row7.getCell(1).value).toBe(1);
    expect(row7.getCell(7).value).toBe('1001');
    expect(row7.getCell(8).value).toBe('Ahmad Siswa');
    expect(row7.getCell(9).value).toBe('HADIR');
    expect(row7.getCell(14).value).toBe(15);
    expect(row7.getCell(18).value).toBe('app');

    // Tautan selfie untuk siswa yang hadir
    const selfieCell = row7.getCell(16);
    expect(typeof selfieCell.value).toBe('object');
    expect((selfieCell.value as any).text).toBe('Buka Foto Selfie');
    expect((selfieCell.value as any).hyperlink).toContain('/api/v1/files/download?key=selfies%2Fstudent-1.jpg');

    // Baris Data Kedua (Budi - Alpa, tidak ada selfie)
    const row8 = rekapSheet!.getRow(8);
    expect(row8.getCell(8).value).toBe('Budi Santoso');
    expect(row8.getCell(9).value).toBe('ALPA');
    expect(row8.getCell(16).value).toBe('-');

    // Sheet 2: Ringkasan
    const summarySheet = workbook.getWorksheet('Ringkasan');
    expect(summarySheet).toBeDefined();
    expect(summarySheet!.getCell('A1').value).toBe('RINGKASAN KEHADIRAN PER SISWA');

    // Kolom Header Ringkasan di baris 5
    const sumHeaders = (summarySheet!.getRow(5).values as any[]).slice(1);
    expect(sumHeaders).toEqual([
      'No',
      'Nomor Induk',
      'Nama Siswa',
      'Kelas Terdata',
      'Hadir',
      'Terlambat',
      'Izin',
      'Sakit',
      'Alpa',
      'Total Pertemuan',
      'Persentase Hadir (%)',
    ]);

    // Data Baris Ringkasan
    const sumRow1 = summarySheet!.getRow(6);
    expect(sumRow1.getCell(3).value).toBe('Ahmad Siswa');
    expect(sumRow1.getCell(5).value).toBe(1); // Hadir
    expect(sumRow1.getCell(10).value).toBe(1); // Total Pertemuan
    expect(sumRow1.getCell(11).value).toBe('100.0%');
  });

  it('Guru: should be restricted to classes they teach', async () => {
    // Guru 1 mengajar class-1
    vi.mocked(prisma.schedule.findFirst).mockResolvedValue({
      id: 'sched-1',
      teacher_id: 'user-guru-1',
      class_id: '11111111-1111-1111-1111-111111111111',
    } as any);

    const res = await request(app)
      .get('/api/v1/exports/attendances.xlsx?class_id=11111111-1111-1111-1111-111111111111')
      .set('Authorization', `Bearer ${guru1Token}`)
      .buffer(true)
      .parse((res, callback) => {
        const chunks: Buffer[] = [];
        res.on('data', (chunk) => chunks.push(Buffer.from(chunk)));
        res.on('end', () => callback(null, Buffer.concat(chunks)));
      });

    expect(res.status).toBe(200);
    expect(prisma.attendance.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining({
          session: expect.objectContaining({
            schedule: expect.objectContaining({
              teacher_id: 'user-guru-1',
              class_id: '11111111-1111-1111-1111-111111111111',
            }),
          }),
        }),
      })
    );
  });

  it('Guru: attempting to export class not taught should return 403 FORBIDDEN', async () => {
    // Guru 2 tidak mengajar class-999
    vi.mocked(prisma.schedule.findFirst).mockResolvedValue(null);

    const res = await request(app)
      .get('/api/v1/exports/attendances.xlsx?class_id=99999999-9999-9999-9999-999999999999')
      .set('Authorization', `Bearer ${guru2Token}`);

    expect(res.status).toBe(403);
    expect(res.body.error.code).toBe('FORBIDDEN');
    expect(res.body.error.message).toContain('Guru hanya dapat mengekspor rekap absensi untuk kelas yang diampunya');
  });

  it('Filters: should apply date range and status filters to Prisma query', async () => {
    await request(app)
      .get('/api/v1/exports/attendances.xlsx?start_date=2026-10-01&end_date=2026-10-31&status=hadir')
      .set('Authorization', `Bearer ${operatorToken}`);

    expect(prisma.attendance.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining({
          status: 'hadir',
          session: expect.objectContaining({
            tanggal: {
              gte: new Date('2026-10-01T00:00:00.000Z'),
              lte: new Date('2026-10-31T00:00:00.000Z'),
            },
          }),
        }),
      })
    );
  });
});
