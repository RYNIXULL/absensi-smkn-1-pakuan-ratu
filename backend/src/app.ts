import express, { Application, Request, Response } from 'express';
import helmet from 'helmet';
import cors from 'cors';
import { errorHandler } from './middleware/error';

import { authRouter } from './modules/auth/auth.routes';
import { devicesRouter } from './modules/devices/devices.routes';
import { usersRouter } from './modules/users/users.routes';

export const app: Application = express();

app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.get('/api/v1/health', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'ok',
    timestamp: new Date().toISOString(),
  });
});

app.use('/api/v1/auth', authRouter);
app.use('/api/v1/devices', devicesRouter);
app.use('/api/v1', usersRouter);

app.use(errorHandler);

