import { app } from './app';
import { env } from './config/env';
import { logger } from './lib/logger';
import { startSessionCronJob, stopSessionCronJob } from './jobs/sessionManager';

const server = app.listen(env.PORT, () => {
  logger.info(`Server running in ${env.NODE_ENV} mode on port ${env.PORT}`);
  startSessionCronJob();
});

process.on('SIGTERM', () => {
  logger.info('SIGTERM signal received: closing HTTP server');
  stopSessionCronJob();
  server.close(() => {
    logger.info('HTTP server closed');
  });
});
