import ExcelJS from 'exceljs';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { UserRole, Prisma, AttendanceStatus } from '@prisma/client';
import { ExportAttendancesQuery } from './exports.types';
import { LocalStorageService } from '../../lib/storage';
import {
  getJakartaDateInfo,
  ASIA_JAKARTA_TZ,
} from '../../lib/dateUtils';

export class ExportsService {
  private storageService = new LocalStorageService();

  /**
   * Ekspor rekapitulasi kehadiran ke berkas Excel (.xlsx)
   */
  async exportAttendancesToExcel(
    userId: string,
    userRole: UserRole,
    query: ExportAttendancesQuery
  ): Promise<{ buffer: Buffer; filename: string }> {
    const where: Prisma.AttendanceWhereInput = {};

    // 1. Status Filter
    if (query.status) {
      where.status = query.status;
    }

    // 2. Student Filter
    if (query.student_id) {
      where.student_id = query.student_id;
    }

    // 3. Tanggal (Session) Filter
    const sessionWhere: Prisma.AttendanceSessionWhereInput = {};
    if (query.start_date || query.end_date) {
      const tanggalFilter: Prisma.DateTimeFilter = {};
      if (query.start_date) {
        tanggalFilter.gte = new Date(`${query.start_date}T00:00:00.000Z`);
      }
      if (query.end_date) {
        tanggalFilter.lte = new Date(`${query.end_date}T00:00:00.000Z`);
      }
      sessionWhere.tanggal = tanggalFilter;
    }

    // 4. Schedule Filters & Role Restrictions
    const scheduleWhere: Prisma.ScheduleWhereInput = {};
    if (query.class_id) {
      scheduleWhere.class_id = query.class_id;
    }
    if (query.subject_id) {
      scheduleWhere.subject_id = query.subject_id;
    }

    // Guru hanya boleh mengekspor rekap kelas yang diampunya
    if (userRole === UserRole.guru) {
      if (query.class_id) {
        const teachesClass = await prisma.schedule.findFirst({
          where: {
            teacher_id: userId,
            class_id: query.class_id,
          },
        });
        if (!teachesClass) {
          throw new AppError(
            'FORBIDDEN',
            'Guru hanya dapat mengekspor rekap absensi untuk kelas yang diampunya',
            403
          );
        }
      }
      scheduleWhere.teacher_id = userId;
    }

    if (Object.keys(scheduleWhere).length > 0) {
      sessionWhere.schedule = scheduleWhere;
    }
    if (Object.keys(sessionWhere).length > 0) {
      where.session = sessionWhere;
    }

    // 5. Query data dari database
    const attendances = await prisma.attendance.findMany({
      where,
      orderBy: [
        { session: { tanggal: 'asc' } },
        { student: { nama: 'asc' } },
        { checkin_at: 'asc' },
      ],
      include: {
        student: { select: { id: true, nama: true, nomor_induk: true } },
        session: {
          include: {
            schedule: {
              include: {
                class: true,
                subject: true,
                teacher: { select: { id: true, nama: true } },
                location: true,
              },
            },
          },
        },
      },
    });

    // 6. Buat berkas Excel dengan ExcelJS
    const workbook = new ExcelJS.Workbook();
    workbook.creator = 'Sistem Absensi Sekolah';
    workbook.lastModifiedBy = 'Sistem Absensi Sekolah';
    workbook.created = new Date();

    const nowInfo = getJakartaDateInfo();
    const cetakDateStr = `${nowInfo.dateString} ${new Intl.DateTimeFormat('id-ID', {
      timeZone: ASIA_JAKARTA_TZ,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(new Date())} WIB`;

    // ==========================================
    // SHEET 1: REKAPITULASI DETAIL
    // ==========================================
    const rekapSheet = workbook.addWorksheet('Rekap', {
      views: [{ state: 'frozen', xSplit: 0, ySplit: 6 }],
    });

    // Judul & Metadata Filter (Baris 1 - 4)
    rekapSheet.mergeCells('A1:R1');
    const titleCell = rekapSheet.getCell('A1');
    titleCell.value = 'REKAPITULASI PRESENSI SISWA';
    titleCell.font = { name: 'Arial', size: 16, bold: true, color: { argb: 'FFFFFFFF' } };
    titleCell.alignment = { vertical: 'middle', horizontal: 'center' };
    titleCell.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF0F172A' }, // Dark Slate 900
    };
    rekapSheet.getRow(1).height = 36;

    rekapSheet.getCell('A2').value = `Waktu Cetak: ${cetakDateStr}`;
    rekapSheet.getCell('A2').font = { name: 'Arial', size: 10, italic: true };

    const periodeStr =
      query.start_date || query.end_date
        ? `${query.start_date || 'Awal'} s/d ${query.end_date || 'Akhir'}`
        : 'Semua Periode';
    rekapSheet.getCell('A3').value = `Periode: ${periodeStr}`;
    rekapSheet.getCell('A3').font = { name: 'Arial', size: 10, bold: true };

    const statusFilterText = query.status ? `Status: ${query.status.toUpperCase()}` : 'Status: Semua';
    rekapSheet.getCell('A4').value = `Filter: ${statusFilterText} | Peran Pengunduh: ${userRole.toUpperCase()}`;
    rekapSheet.getCell('A4').font = { name: 'Arial', size: 10 };

    rekapSheet.getRow(5).height = 8; // Spacer

    // Header Tabel (Baris 6)
    const headers = [
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
    ];

    const headerRow = rekapSheet.getRow(6);
    headerRow.values = headers;
    headerRow.height = 26;
    headerRow.eachCell((cell) => {
      cell.font = { name: 'Arial', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FF1E293B' }, // Slate 800
      };
      cell.alignment = { vertical: 'middle', horizontal: 'center' };
      cell.border = {
        top: { style: 'thin', color: { argb: 'FFCBD5E1' } },
        left: { style: 'thin', color: { argb: 'FFCBD5E1' } },
        bottom: { style: 'medium', color: { argb: 'FF94A3B8' } },
        right: { style: 'thin', color: { argb: 'FFCBD5E1' } },
      };
    });

    const dayNames = ['', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu', 'Minggu'];

    // Isi Baris Data Rekap
    for (let i = 0; i < attendances.length; i++) {
      const att = attendances[i];
      const sessionDate = att.session.tanggal;
      const dayInfo = getJakartaDateInfo(sessionDate);
      const dayName = dayNames[dayInfo.dayOfWeek] || '-';
      const dateFormatted = dayInfo.dateString;

      const checkinFormatted = att.checkin_at
        ? new Intl.DateTimeFormat('id-ID', {
            timeZone: ASIA_JAKARTA_TZ,
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false,
          }).format(att.checkin_at) + ' WIB'
        : '-';

      const rowNum = 7 + i;
      const row = rekapSheet.getRow(rowNum);

      row.getCell(1).value = i + 1;
      row.getCell(2).value = dateFormatted;
      row.getCell(3).value = dayName;
      row.getCell(4).value = att.session.schedule?.class?.nama || '-';
      row.getCell(5).value = att.session.schedule?.subject?.nama || '-';
      row.getCell(6).value = att.session.schedule?.teacher?.nama || '-';
      row.getCell(7).value = att.student.nomor_induk;
      row.getCell(8).value = att.student.nama;
      row.getCell(9).value = att.status.toUpperCase();
      row.getCell(10).value = checkinFormatted;
      row.getCell(11).value = att.session.schedule?.location?.nama || '-';
      row.getCell(12).value = att.latitude != null ? Number(att.latitude) : '-';
      row.getCell(13).value = att.longitude != null ? Number(att.longitude) : '-';
      row.getCell(14).value = att.distance_m != null ? Math.round(att.distance_m) : '-';
      row.getCell(15).value = att.accuracy_m != null ? Math.round(att.accuracy_m) : '-';

      // Tautan Selfie berumur panjang (7 hari = 604800 detik)
      if (att.selfie_key) {
        const signedUrl = await this.storageService.getSignedUrl(att.selfie_key, 604800);
        row.getCell(16).value = {
          text: 'Buka Foto Selfie',
          hyperlink: signedUrl,
        };
        row.getCell(16).font = { color: { argb: 'FF0284C7' }, underline: true };
      } else {
        row.getCell(16).value = '-';
      }

      row.getCell(17).value = att.catatan || '-';
      row.getCell(18).value = att.source;

      // Styling Zebra & Status Color
      const isEven = i % 2 === 0;
      row.eachCell((cell, colNumber) => {
        cell.font = cell.font || { name: 'Arial', size: 10 };
        if (!cell.fill) {
          cell.fill = {
            type: 'pattern',
            pattern: 'solid',
            fgColor: { argb: isEven ? 'FFFFFFFF' : 'FFF8FAFC' },
          };
        }
        cell.border = {
          top: { style: 'thin', color: { argb: 'FFE2E8F0' } },
          left: { style: 'thin', color: { argb: 'FFE2E8F0' } },
          bottom: { style: 'thin', color: { argb: 'FFE2E8F0' } },
          right: { style: 'thin', color: { argb: 'FFE2E8F0' } },
        };
        if ([1, 2, 3, 7, 9, 10, 14, 15, 18].includes(colNumber)) {
          cell.alignment = { vertical: 'middle', horizontal: 'center' };
        } else {
          cell.alignment = { vertical: 'middle', horizontal: 'left' };
        }
      });

      // Highlight warna status
      const statusCell = row.getCell(9);
      if (att.status === AttendanceStatus.hadir) {
        statusCell.font = { bold: true, color: { argb: 'FF065F46' } }; // Dark Green
      } else if (att.status === AttendanceStatus.terlambat) {
        statusCell.font = { bold: true, color: { argb: 'FF92400E' } }; // Dark Amber
      } else if (att.status === AttendanceStatus.izin || att.status === AttendanceStatus.sakit) {
        statusCell.font = { bold: true, color: { argb: 'FF1E40AF' } }; // Dark Blue
      } else if (att.status === AttendanceStatus.alpa) {
        statusCell.font = { bold: true, color: { argb: 'FF991B1B' } }; // Dark Red
      }
    }

    // Auto-adjust lebar kolom Sheet Rekap
    rekapSheet.columns.forEach((col, idx) => {
      let maxLen = headers[idx] ? headers[idx].length : 10;
      col.eachCell?.({ includeEmpty: false }, (cell, rowNumber) => {
        if (rowNumber >= 6) {
          const valStr = cell.value ? (typeof cell.value === 'object' && 'text' in cell.value ? cell.value.text : cell.value.toString()) : '';
          if (valStr.length > maxLen) {
            maxLen = valStr.length;
          }
        }
      });
      col.width = Math.min(Math.max(maxLen + 4, 12), 40);
    });

    // ==========================================
    // SHEET 2: RINGKASAN REKAPITULASI SISWA
    // ==========================================
    const summarySheet = workbook.addWorksheet('Ringkasan', {
      views: [{ state: 'frozen', xSplit: 0, ySplit: 5 }],
    });

    summarySheet.mergeCells('A1:K1');
    const sumTitle = summarySheet.getCell('A1');
    sumTitle.value = 'RINGKASAN KEHADIRAN PER SISWA';
    sumTitle.font = { name: 'Arial', size: 16, bold: true, color: { argb: 'FFFFFFFF' } };
    sumTitle.alignment = { vertical: 'middle', horizontal: 'center' };
    sumTitle.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF0F172A' },
    };
    summarySheet.getRow(1).height = 36;

    summarySheet.getCell('A2').value = `Waktu Cetak: ${cetakDateStr}`;
    summarySheet.getCell('A2').font = { name: 'Arial', size: 10, italic: true };
    summarySheet.getCell('A3').value = `Periode: ${periodeStr}`;
    summarySheet.getCell('A3').font = { name: 'Arial', size: 10, bold: true };
    summarySheet.getRow(4).height = 8;

    const summaryHeaders = [
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
    ];

    const sumHeaderRow = summarySheet.getRow(5);
    sumHeaderRow.values = summaryHeaders;
    sumHeaderRow.height = 26;
    sumHeaderRow.eachCell((cell) => {
      cell.font = { name: 'Arial', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FF1E293B' },
      };
      cell.alignment = { vertical: 'middle', horizontal: 'center' };
      cell.border = {
        top: { style: 'thin', color: { argb: 'FFCBD5E1' } },
        left: { style: 'thin', color: { argb: 'FFCBD5E1' } },
        bottom: { style: 'medium', color: { argb: 'FF94A3B8' } },
        right: { style: 'thin', color: { argb: 'FFCBD5E1' } },
      };
    });

    // Agregasi kehadiran per siswa
    interface StudentSummary {
      nomor_induk: string;
      nama: string;
      className: string;
      hadir: number;
      terlambat: number;
      izin: number;
      sakit: number;
      alpa: number;
    }

    const studentMap = new Map<string, StudentSummary>();

    for (const att of attendances) {
      const studentId = att.student.id;
      if (!studentMap.has(studentId)) {
        studentMap.set(studentId, {
          nomor_induk: att.student.nomor_induk,
          nama: att.student.nama,
          className: att.session.schedule?.class?.nama || '-',
          hadir: 0,
          terlambat: 0,
          izin: 0,
          sakit: 0,
          alpa: 0,
        });
      }

      const current = studentMap.get(studentId)!;
      if (att.status === AttendanceStatus.hadir) current.hadir++;
      else if (att.status === AttendanceStatus.terlambat) current.terlambat++;
      else if (att.status === AttendanceStatus.izin) current.izin++;
      else if (att.status === AttendanceStatus.sakit) current.sakit++;
      else if (att.status === AttendanceStatus.alpa) current.alpa++;
    }

    const studentSummaries = Array.from(studentMap.values()).sort((a, b) =>
      a.nama.localeCompare(b.nama)
    );

    studentSummaries.forEach((sum, idx) => {
      const rowIdx = 6 + idx;
      const row = summarySheet.getRow(rowIdx);
      const total = sum.hadir + sum.terlambat + sum.izin + sum.sakit + sum.alpa;
      const percentage =
        total > 0 ? `${(((sum.hadir + sum.terlambat) / total) * 100).toFixed(1)}%` : '0.0%';

      row.values = [
        idx + 1,
        sum.nomor_induk,
        sum.nama,
        sum.className,
        sum.hadir,
        sum.terlambat,
        sum.izin,
        sum.sakit,
        sum.alpa,
        total,
        percentage,
      ];

      const isEven = idx % 2 === 0;
      row.eachCell((cell, colNum) => {
        cell.font = { name: 'Arial', size: 10 };
        cell.fill = {
          type: 'pattern',
          pattern: 'solid',
          fgColor: { argb: isEven ? 'FFFFFFFF' : 'FFF8FAFC' },
        };
        cell.border = {
          top: { style: 'thin', color: { argb: 'FFE2E8F0' } },
          left: { style: 'thin', color: { argb: 'FFE2E8F0' } },
          bottom: { style: 'thin', color: { argb: 'FFE2E8F0' } },
          right: { style: 'thin', color: { argb: 'FFE2E8F0' } },
        };
        if ([1, 2, 5, 6, 7, 8, 9, 10, 11].includes(colNum)) {
          cell.alignment = { vertical: 'middle', horizontal: 'center' };
        } else {
          cell.alignment = { vertical: 'middle', horizontal: 'left' };
        }
      });
    });

    summarySheet.columns.forEach((col, idx) => {
      let maxLen = summaryHeaders[idx] ? summaryHeaders[idx].length : 10;
      col.eachCell?.({ includeEmpty: false }, (cell, rowNumber) => {
        if (rowNumber >= 5) {
          const valStr = cell.value ? cell.value.toString() : '';
          if (valStr.length > maxLen) {
            maxLen = valStr.length;
          }
        }
      });
      col.width = Math.min(Math.max(maxLen + 4, 12), 35);
    });

    // 7. Render Buffer
    const buffer = Buffer.from(await workbook.xlsx.writeBuffer());
    const cleanDate = nowInfo.dateString.replace(/-/g, '');
    const filename = `rekap-absensi-${cleanDate}.xlsx`;

    return { buffer, filename };
  }
}
