import { prisma } from '../../lib/prisma';

export const DEFAULT_SETTINGS: Record<string, string> = {
  teacher_can_edit_schedule: 'false',
};

export class SettingsService {
  static async get(key: string): Promise<string> {
    const setting = await prisma.setting.findUnique({
      where: { key },
    });
    if (!setting) {
      return DEFAULT_SETTINGS[key] ?? '';
    }
    return setting.value;
  }

  static async getAll(): Promise<Record<string, string>> {
    const settings = await prisma.setting.findMany();
    const result: Record<string, string> = { ...DEFAULT_SETTINGS };
    for (const item of settings) {
      result[item.key] = item.value;
    }
    return result;
  }

  static async set(key: string, value: string): Promise<{ key: string; value: string }> {
    const updated = await prisma.setting.upsert({
      where: { key },
      create: { key, value },
      update: { value, updated_at: new Date() },
    });
    return { key: updated.key, value: updated.value };
  }
}
