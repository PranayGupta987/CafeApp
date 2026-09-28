import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { PrismaClient } from '@prisma/client';
import { getEnv } from './config/env.js';
import { errorHandler } from './middleware/errorHandler.js';
import { createHealthRouter } from './modules/health/router.js';

export function createApp(prisma: PrismaClient) {
  const env = getEnv();
  const app = express();

  // Security
  app.use(helmet());
  app.use(
    cors({
      origin: env.CORS_ORIGINS === '*' ? '*' : env.CORS_ORIGINS.split(','),
      credentials: true,
    }),
  );

  // Body parsing
  app.use(express.json({ limit: '1mb' }));

  // Routes
  app.use('/api/v1', createHealthRouter(prisma));

  // Error handling
  app.use(errorHandler);

  return app;
}
