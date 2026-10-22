import dotenv from 'dotenv';
import { z } from 'zod';

dotenv.config();

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.coerce.number().default(3000),
  DATABASE_URL: z.string().min(1, 'DATABASE_URL is required'),
  JWT_ACCESS_SECRET: z.string().min(1, 'JWT_ACCESS_SECRET is required'),
  JWT_REFRESH_SECRET: z.string().min(1, 'JWT_REFRESH_SECRET is required'),
  ACCESS_TTL: z.string().default('15m'),
  REFRESH_TTL: z.string().default('7d'),
  SEED_OPERATOR_ID: z.string().default('OP001'),
  SEED_OPERATOR_NAME: z.string().default('Administrator Operator'),
  SEED_OPERATOR_PASSWORD: z.string().default('OperatorPassword123!'),
  ATTENDANCE_MAX_ACCURACY_M: z.coerce.number().default(50),
  OPEN_BEFORE_MIN: z.coerce.number().default(10),
  LATE_AFTER_MIN: z.coerce.number().default(15),
  INTEGRITY_ENFORCE: z
    .preprocess((val) => val === 'true' || val === true, z.boolean())
    .default(false),
  PLAY_PACKAGE_NAME: z.string().default('com.absensi.app'),
  SPEED_CHECK_ENFORCE: z
    .preprocess((val) => val === 'true' || val === true, z.boolean())
    .default(false),
  STORAGE_DRIVER: z.enum(['local', 's3']).default('local'),
  STORAGE_LOCAL_PATH: z.string().default('./uploads'),
  S3_ENDPOINT: z.string().optional(),
  S3_REGION: z.string().optional(),
  S3_BUCKET: z.string().optional(),
  S3_ACCESS_KEY_ID: z.string().optional(),
  S3_SECRET_ACCESS_KEY: z.string().optional(),
  LEAVE_MAX_BACKDATE_DAYS: z.coerce.number().default(3),
  EXPORT_MAX_RANGE_DAYS: z.coerce.number().default(31),
  EXPORT_LINK_TTL_HOURS: z.coerce.number().default(168),
});

const parsedEnv = envSchema.safeParse(process.env);

if (!parsedEnv.success) {
  // eslint-disable-next-line no-console
  console.error('Invalid environment variables:\n', JSON.stringify(parsedEnv.error.format(), null, 2));
  process.exit(1);
}

export const env = parsedEnv.data;
