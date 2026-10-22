import pino from 'pino';
import { env } from '../config/env';

export const logger = pino({
  level: env.NODE_ENV === 'test' ? 'silent' : 'info',
  redact: {
    paths: ['req.headers.authorization', 'password', 'token', 'refreshToken', 'password_hash'],
    censor: '[REDACTED]',
  },
});
