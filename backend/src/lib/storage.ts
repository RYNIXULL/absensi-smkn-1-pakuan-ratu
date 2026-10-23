import fs from 'fs';
import path from 'path';
import crypto from 'crypto';
import { logger } from './logger';

export interface StorageService {
  upload(key: string, buffer: Buffer, mimeType: string): Promise<string>;
  getSignedUrl(key: string, expiresInSeconds: number): Promise<string>;
  delete(key: string): Promise<void>;
}

export class LocalStorageService implements StorageService {
  private baseDir: string;
  private appSecret: string;
  private baseUrl: string;

  constructor() {
    this.baseDir = path.resolve(process.cwd(), process.env.STORAGE_LOCAL_DIR || 'uploads');
    this.appSecret = process.env.JWT_SECRET || 'absensi-secure-local-storage-secret-key-2026';
    this.baseUrl = process.env.APP_BASE_URL || 'http://localhost:3000';

    if (!fs.existsSync(this.baseDir)) {
      fs.mkdirSync(this.baseDir, { recursive: true });
    }
  }

  async upload(key: string, buffer: Buffer, mimeType: string): Promise<string> {
    const fullPath = path.join(this.baseDir, key);
    const dir = path.dirname(fullPath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    await fs.promises.writeFile(fullPath, buffer);
    logger.debug({ key, mimeType }, 'File uploaded to local storage');
    return key;
  }

  async getSignedUrl(key: string, expiresInSeconds = 900): Promise<string> {
    const expiresAt = Math.floor(Date.now() / 1000) + expiresInSeconds;
    const signature = this.generateSignature(key, expiresAt);
    return `${this.baseUrl}/api/v1/files/download?key=${encodeURIComponent(key)}&expires=${expiresAt}&sig=${signature}`;
  }

  async delete(key: string): Promise<void> {
    const fullPath = path.join(this.baseDir, key);
    if (fs.existsSync(fullPath)) {
      await fs.promises.unlink(fullPath);
    }
  }

  verifySignedUrl(key: string, expiresAt: number, signature: string): boolean {
    const now = Math.floor(Date.now() / 1000);
    if (now > expiresAt) {
      return false;
    }
    const expected = this.generateSignature(key, expiresAt);
    try {
      return crypto.timingSafeEqual(Buffer.from(signature, 'hex'), Buffer.from(expected, 'hex'));
    } catch {
      return false;
    }
  }

  getFilePath(key: string): string {
    return path.join(this.baseDir, key);
  }

  private generateSignature(key: string, expiresAt: number): string {
    return crypto
      .createHmac('sha256', this.appSecret)
      .update(`${key}:${expiresAt}`)
      .digest('hex');
  }
}

let storageInstance: StorageService | null = null;

export function getStorageService(): StorageService {
  if (!storageInstance) {
    // In dev and local, default to LocalStorageService
    storageInstance = new LocalStorageService();
  }
  return storageInstance;
}
