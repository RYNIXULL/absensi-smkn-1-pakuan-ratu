import { Router, Request, Response, NextFunction } from 'express';
import fs from 'fs';
import { LocalStorageService, getStorageService } from '../../lib/storage';
import { AppError } from '../../errors/appError';

const router = Router();

// GET /files/download - Verifikasi tanda tangan HMAC dan kedaluwarsa signed URL
router.get('/download', (req: Request, res: Response, next: NextFunction) => {
  try {
    const key = req.query.key as string;
    const expires = parseInt(req.query.expires as string, 10);
    const sig = req.query.sig as string;

    if (!key || !expires || !sig) {
      throw new AppError('INVALID_INPUT', 'Parameter tautan signed URL tidak lengkap', 400);
    }

    const storage = getStorageService();
    if (storage instanceof LocalStorageService) {
      const isValid = storage.verifySignedUrl(key, expires, sig);
      if (!isValid) {
        throw new AppError(
          'FORBIDDEN',
          'Tautan tanda tangan digital tidak valid atau telah kedaluwarsa',
          403
        );
      }

      const filePath = storage.getFilePath(key);
      if (!fs.existsSync(filePath)) {
        throw new AppError('NOT_FOUND', 'File yang diminta tidak ditemukan di server', 404);
      }

      // Stream / send file
      res.setHeader('Content-Type', 'image/jpeg');
      return res.sendFile(filePath);
    }

    // In non-local storage, client should be redirected to pre-signed S3 URL
    res.redirect(302, key);
  } catch (err) {
    next(err);
  }
});

export default router;
