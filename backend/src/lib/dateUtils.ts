/**
 * Timezone utilities for Asia/Jakarta (WIB = UTC+7)
 */

export const ASIA_JAKARTA_TZ = 'Asia/Jakarta';
export const JAKARTA_OFFSET_HOURS = 7;

export interface JakartaDateInfo {
  year: number;
  month: number; // 1-12
  day: number; // 1-31
  dayOfWeek: number; // 1=Senin..7=Minggu
  dateString: string; // YYYY-MM-DD
  dateOnly: Date; // Date object representing the midnight UTC of the date (for Prisma @db.Date)
}

/**
 * Gets date and day of week information in Asia/Jakarta timezone.
 */
export function getJakartaDateInfo(date = new Date()): JakartaDateInfo {
  const dateStr = new Intl.DateTimeFormat('en-CA', {
    timeZone: ASIA_JAKARTA_TZ,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date); // YYYY-MM-DD

  const [yearStr, monthStr, dayStr] = dateStr.split('-');
  const year = parseInt(yearStr, 10);
  const month = parseInt(monthStr, 10);
  const day = parseInt(dayStr, 10);

  const weekdayStr = new Intl.DateTimeFormat('en-US', {
    timeZone: ASIA_JAKARTA_TZ,
    weekday: 'short',
  }).format(date);

  const dayMap: Record<string, number> = {
    Mon: 1,
    Tue: 2,
    Wed: 3,
    Thu: 4,
    Fri: 5,
    Sat: 6,
    Sun: 7,
  };
  const dayOfWeek = dayMap[weekdayStr] || 1;

  // DB @db.Date expects UTC midnight representing the date YYYY-MM-DD
  const dateOnly = new Date(`${dateStr}T00:00:00.000Z`);

  return {
    year,
    month,
    day,
    dayOfWeek,
    dateString: dateStr,
    dateOnly,
  };
}

/**
 * Parses time string (e.g. "07:30" or Date object from Prisma @db.Time)
 * and returns minutes from midnight (0..1439).
 */
export function parseTimeToMinutes(time: string | Date): number {
  if (typeof time === 'string') {
    const parts = time.split(':');
    const h = parseInt(parts[0], 10) || 0;
    const m = parseInt(parts[1], 10) || 0;
    return h * 60 + m;
  }
  // If Prisma Date object for @db.Time
  const h = time.getUTCHours();
  const m = time.getUTCMinutes();
  return h * 60 + m;
}

/**
 * Converts a time (e.g. 07:30 WIB) on a specific Jakarta date into a UTC Date object.
 *
 * @param dateOnlyString Date string in "YYYY-MM-DD" in Jakarta
 * @param timeMinutes Minutes of day in WIB (0..1439)
 */
export function createUtcFromJakartaTime(dateOnlyString: string, timeMinutes: number): Date {
  const [yStr, mStr, dStr] = dateOnlyString.split('-');
  const y = parseInt(yStr, 10);
  const m = parseInt(mStr, 10);
  const d = parseInt(dStr, 10);

  // Midnight WIB in UTC is (midnight - 7 hours)
  const midnightWibInUtcMs = Date.UTC(y, m - 1, d, 0, 0, 0) - JAKARTA_OFFSET_HOURS * 60 * 60 * 1000;
  return new Date(midnightWibInUtcMs + timeMinutes * 60 * 1000);
}
