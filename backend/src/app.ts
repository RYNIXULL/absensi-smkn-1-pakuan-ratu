import express, { Application, Request, Response } from 'express';
import helmet from 'helmet';
import cors from 'cors';
import { errorHandler } from './middleware/error';

import { authRouter } from './modules/auth/auth.routes';
import { devicesRouter } from './modules/devices/devices.routes';
import { usersRouter } from './modules/users/users.routes';
import { classesRouter } from './modules/classes/classes.routes';
import { subjectsRouter } from './modules/subjects/subjects.routes';
import { locationsRouter } from './modules/locations/locations.routes';
import { schedulesRouter } from './modules/schedules/schedules.routes';
import { settingsRouter } from './modules/settings/settings.routes';
import sessionsRouter from './modules/sessions/sessions.routes';
import attendanceRouter, { myAttendanceRouter } from './modules/attendance/attendance.routes';
import filesRouter from './modules/files/files.routes';
import leaveRouter from './modules/leave/leave.routes';
import { exportsRouter } from './modules/exports/exports.routes';

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
app.use('/api/v1/classes', classesRouter);
app.use('/api/v1/subjects', subjectsRouter);
app.use('/api/v1/locations', locationsRouter);
app.use('/api/v1', schedulesRouter);
app.use('/api/v1/settings', settingsRouter);

// Sesi dan Absensi (Fase F3 & F4)
app.use('/api/v1/sessions', sessionsRouter);
app.use('/api/v1/attendances', attendanceRouter);
app.use('/api/v1/me/attendances', myAttendanceRouter);
app.use('/api/v1/files', filesRouter);

// Izin dan Sakit (Fase F5)
app.use('/api/v1/leave-requests', leaveRouter);

// Ekspor Rekap Excel (Fase F6)
app.use('/api/v1/exports', exportsRouter);

app.use(errorHandler);
