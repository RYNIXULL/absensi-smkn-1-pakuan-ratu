import { PrismaClient, UserRole } from '@prisma/client';
import argon2 from 'argon2';
import dotenv from 'dotenv';

dotenv.config();

const prisma = new PrismaClient();

async function main(): Promise<void> {
  // 1. Password hashing
  const defaultPasswordOptions = { type: argon2.argon2id };
  const operatorPassword = process.env.SEED_OPERATOR_PASSWORD || 'OperatorPassword123!';
  const guruPassword = 'GuruPassword123!';
  const siswaPassword = 'SiswaPassword123!';

  const [operatorHash, guruHash, siswaHash] = await Promise.all([
    argon2.hash(operatorPassword, defaultPasswordOptions),
    argon2.hash(guruPassword, defaultPasswordOptions),
    argon2.hash(siswaPassword, defaultPasswordOptions),
  ]);

  // 2. Seed Operator
  const operatorId = process.env.SEED_OPERATOR_ID || 'OP001';
  const operatorName = process.env.SEED_OPERATOR_NAME || 'Administrator Operator';

  const operator = await prisma.user.upsert({
    where: { nomor_induk: operatorId },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000001',
      role: UserRole.operator,
      nama: operatorName,
      nomor_induk: operatorId,
      email: 'operator@absensi.local',
      password_hash: operatorHash,
      is_active: true,
    },
  });
  // eslint-disable-next-line no-console
  console.log(`[Seed] Operator: ${operator.nama} (${operator.nomor_induk})`);

  // 3. Seed Guru
  const guru = await prisma.user.upsert({
    where: { nomor_induk: 'GURU001' },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000002',
      role: UserRole.guru,
      nama: 'Budi Hartono, S.Pd.',
      nomor_induk: 'GURU001',
      email: 'guru@absensi.local',
      password_hash: guruHash,
      is_active: true,
    },
  });
  // eslint-disable-next-line no-console
  console.log(`[Seed] Guru: ${guru.nama} (${guru.nomor_induk})`);

  // 4. Seed Siswa
  const siswa = await prisma.user.upsert({
    where: { nomor_induk: 'SISWA001' },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000003',
      role: UserRole.siswa,
      nama: 'Ahmad Zaki',
      nomor_induk: 'SISWA001',
      email: 'siswa@absensi.local',
      password_hash: siswaHash,
      is_active: true,
    },
  });
  // eslint-disable-next-line no-console
  console.log(`[Seed] Siswa: ${siswa.nama} (${siswa.nomor_induk})`);

  // 5. Seed Sample Location
  const location = await prisma.location.upsert({
    where: { id: '00000000-0000-0000-0000-000000000010' },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000010',
      nama: 'Gedung Utama Sekolah',
      latitude: -6.200000,
      longitude: 106.816666,
      radius_m: 100,
      is_active: true,
    },
  });
  // eslint-disable-next-line no-console
  console.log(`[Seed] Location: ${location.nama}`);

  // 6. Seed Sample Class
  const sampleClass = await prisma.class.upsert({
    where: { id: '00000000-0000-0000-0000-000000000020' },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000020',
      nama: 'XII IPA 1',
      tahun_ajaran: '2026/2027',
    },
  });
  // eslint-disable-next-line no-console
  console.log(`[Seed] Class: ${sampleClass.nama}`);

  // 7. Seed Sample Subject
  const subject = await prisma.subject.upsert({
    where: { id: '00000000-0000-0000-0000-000000000030' },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000030',
      nama: 'Matematika Wajib',
    },
  });
  // eslint-disable-next-line no-console
  console.log(`[Seed] Subject: ${subject.nama}`);

  // 8. Enroll Siswa into Class
  await prisma.classMember.upsert({
    where: {
      class_id_user_id: {
        class_id: sampleClass.id,
        user_id: siswa.id,
      },
    },
    update: {},
    create: {
      class_id: sampleClass.id,
      user_id: siswa.id,
    },
  });
  // eslint-disable-next-line no-console
  console.log(`[Seed] Enrolled ${siswa.nama} into ${sampleClass.nama}`);

  // 9. Seed Schedule
  const today = new Date();
  const currentDayOfWeek = today.getDay() === 0 ? 7 : today.getDay(); // 1=Senin..7=Minggu

  const startTime = new Date('1970-01-01T07:00:00Z');
  const endTime = new Date('1970-01-01T15:00:00Z');

  const schedule = await prisma.schedule.upsert({
    where: { id: '00000000-0000-0000-0000-000000000040' },
    update: {
      day_of_week: currentDayOfWeek,
    },
    create: {
      id: '00000000-0000-0000-0000-000000000040',
      class_id: sampleClass.id,
      subject_id: subject.id,
      teacher_id: guru.id,
      location_id: location.id,
      day_of_week: currentDayOfWeek,
      start_time: startTime,
      end_time: endTime,
    },
  });
  // eslint-disable-next-line no-console
  console.log(`[Seed] Schedule created: ${subject.nama} (Day: ${schedule.day_of_week})`);
}

main()
  .catch((e) => {
    // eslint-disable-next-line no-console
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
