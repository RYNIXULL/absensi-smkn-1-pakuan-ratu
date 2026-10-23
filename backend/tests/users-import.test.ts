import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import ExcelJS from 'exceljs';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findUnique: vi.fn(),
        findFirst: vi.fn(),
        create: vi.fn(),
      },
      auditLog: {
        create: vi.fn().mockResolvedValue({}),
      },
      $transaction: vi.fn((fns) => Promise.all(Array.isArray(fns) ? fns : [fns])),
    },
  };
});

describe('Users Import & Template Tests', () => {
  const operatorToken = signAccessToken({ sub: 'user-op', role: 'operator' });
  const guruToken = signAccessToken({ sub: 'user-guru', role: 'guru' });

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }: any) => {
      if (where.id === 'user-op') {
        return { id: 'user-op', nama: 'Operator Admin', role: 'operator', is_active: true } as any;
      }
      if (where.id === 'user-guru') {
        return { id: 'user-guru', nama: 'Guru', role: 'guru', is_active: true } as any;
      }
      return null;
    });
  });

  describe('GET /api/v1/users/import/template', () => {
    it('Operator: should download valid xlsx template', async () => {
      const res = await request(app)
        .get('/api/v1/users/import/template')
        .set('Authorization', `Bearer ${operatorToken}`)
        .buffer(true)
        .parse((res, callback) => {
          const chunks: Buffer[] = [];
          res.on('data', (chunk) => chunks.push(Buffer.from(chunk)));
          res.on('end', () => callback(null, Buffer.concat(chunks)));
        });

      expect(res.status).toBe(200);
      expect(res.headers['content-type']).toContain('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      expect(res.headers['content-disposition']).toContain('template_impor_pengguna.xlsx');

      const workbook = new ExcelJS.Workbook();
      await (workbook.xlsx.load as (b: Buffer) => Promise<ExcelJS.Workbook>)(res.body);
      const sheet = workbook.worksheets[0];
      expect(sheet).toBeDefined();
      expect(sheet.name).toBe('Template Pengguna');
      expect(sheet.getRow(1).getCell(1).text).toBe('role');
      expect(sheet.getRow(1).getCell(2).text).toBe('nama');
      expect(sheet.getRow(1).getCell(3).text).toBe('nomor_induk');
      expect(sheet.getRow(1).getCell(4).text).toBe('email');
    });

    it('Non-operator: should be rejected with 403', async () => {
      const res = await request(app)
        .get('/api/v1/users/import/template')
        .set('Authorization', `Bearer ${guruToken}`);

      expect(res.status).toBe(403);
    });
  });

  describe('POST /api/v1/users/import', () => {
    it('should parse excel and return row reports with success and failure reasons', async () => {
      const testWorkbook = new ExcelJS.Workbook();
      const testSheet = testWorkbook.addWorksheet('Sheet1');
      testSheet.columns = [
        { header: 'role', key: 'role' },
        { header: 'nama', key: 'nama' },
        { header: 'nomor_induk', key: 'nomor_induk' },
        { header: 'email', key: 'email' },
      ];

      // Row 2: valid guru
      testSheet.addRow({
        role: 'guru',
        nama: 'Asep Saepudin, S.Pd',
        nomor_induk: 'NIP001',
        email: 'asep@sekolah.sch.id',
      });
      // Row 3: valid siswa
      testSheet.addRow({
        role: 'siswa',
        nama: 'Bambang Pamungkas',
        nomor_induk: 'NIS001',
        email: 'bambang@sekolah.sch.id',
      });
      // Row 4: invalid role
      testSheet.addRow({
        role: 'walikelas',
        nama: 'Caca Handika',
        nomor_induk: 'NIP002',
        email: 'caca@sekolah.sch.id',
      });
      // Row 5: invalid email
      testSheet.addRow({
        role: 'siswa',
        nama: 'Dedi Kusnandar',
        nomor_induk: 'NIS002',
        email: 'dedi-bukan-email',
      });
      // Row 6: duplicate nomor induk in file
      testSheet.addRow({
        role: 'siswa',
        nama: 'Asep Kembaran',
        nomor_induk: 'NIP001',
        email: 'asep2@sekolah.sch.id',
      });

      const bufferRaw = await testWorkbook.xlsx.writeBuffer();
      const excelBuffer = Buffer.from(bufferRaw);

      // Mock DB: no conflicts for NIP001 and NIS001 initially
      vi.mocked(prisma.user.findFirst).mockResolvedValue(null);
      vi.mocked(prisma.user.create).mockResolvedValue({} as any);

      const res = await request(app)
        .post('/api/v1/users/import')
        .set('Authorization', `Bearer ${operatorToken}`)
        .attach('file', excelBuffer, 'data_pengguna.xlsx');

      expect(res.status).toBe(200);
      expect(res.body.summary.total).toBe(5);
      expect(res.body.summary.success).toBe(2);
      expect(res.body.summary.failed).toBe(3);

      const rows = res.body.rows;
      expect(rows[0].status).toBe('berhasil');
      expect(rows[0].initial_password).toBeDefined();
      expect(rows[1].status).toBe('berhasil');
      expect(rows[1].initial_password).toBeDefined();

      expect(rows[2].status).toBe('gagal');
      expect(rows[2].reason).toContain('Role');

      expect(rows[3].status).toBe('gagal');
      expect(rows[3].reason).toContain('Format email');

      expect(rows[4].status).toBe('gagal');
      expect(rows[4].reason).toContain('duplikat');

      expect(res.body.excel_base64).toBeDefined();
    });

    it('should return 400 when no file is uploaded', async () => {
      const res = await request(app)
        .post('/api/v1/users/import')
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('FILE_REQUIRED');
    });
  });
});
