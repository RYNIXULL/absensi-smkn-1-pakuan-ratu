import { PrismaClient, UserRole } from '@prisma/client';
import argon2 from 'argon2';
import dotenv from 'dotenv';

dotenv.config();

const prisma = new PrismaClient();

async function main(): Promise<void> {
  const operatorId = process.env.SEED_OPERATOR_ID || 'OP001';
  const operatorName = process.env.SEED_OPERATOR_NAME || 'Administrator Operator';
  const operatorPassword = process.env.SEED_OPERATOR_PASSWORD || 'OperatorPassword123!';

  const passwordHash = await argon2.hash(operatorPassword, {
    type: argon2.argon2id,
  });

  // Seed Operator
  const operator = await prisma.user.upsert({
    where: { nomor_induk: operatorId },
    update: {},
    create: {
      role: UserRole.operator,
      nama: operatorName,
      nomor_induk: operatorId,
      email: 'operator@absensi.local',
      password_hash: passwordHash,
      is_active: true,
    },
  });

  // eslint-disable-next-line no-console
  console.log(`Operator seeded: ${operator.nama} (${operator.nomor_induk})`);

  // Seed Sample Location
  const location = await prisma.location.upsert({
    where: { id: '00000000-0000-0000-0000-000000000001' },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000001',
      nama: 'Gedung Utama Sekolah',
      latitude: -6.200000,
      longitude: 106.816666,
      radius_m: 100,
      is_active: true,
    },
  });

  // eslint-disable-next-line no-console
  console.log(`Location seeded: ${location.nama}`);

  // Seed Sample Class
  const sampleClass = await prisma.class.upsert({
    where: { id: '00000000-0000-0000-0000-000000000002' },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000002',
      nama: 'XII IPA 1',
      tahun_ajaran: '2026/2027',
    },
  });

  // eslint-disable-next-line no-console
  console.log(`Class seeded: ${sampleClass.nama}`);

  // Seed Sample Subject
  const subject = await prisma.subject.upsert({
    where: { id: '00000000-0000-0000-0000-000000000003' },
    update: {},
    create: {
      id: '00000000-0000-0000-0000-000000000003',
      nama: 'Matematika Wajib',
    },
  });

  // eslint-disable-next-line no-console
  console.log(`Subject seeded: ${subject.nama}`);
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
