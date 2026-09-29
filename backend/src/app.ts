import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { PrismaClient } from '@prisma/client';
import { getEnv } from './config/env.js';
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js';
import { authenticateToken } from './middleware/auth.js';
import { createHealthRouter } from './modules/health/router.js';
import authRouter from './modules/auth/router.js';
import cafeRouter from './modules/cafe/router.js';
import tablesRouter from './modules/tables/router.js';
import menuRouter from './modules/menu/router.js';
import offersRouter from './modules/offers/router.js';
import reservationsRouter from './modules/reservations/router.js';
import tableSessionsRouter from './modules/tableSessions/router.js';
import ordersRouter from './modules/orders/router.js';
import notificationsRouter from './modules/notifications/router.js';

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

  // Global Auth Token Extractor
  app.use(authenticateToken);

  // Health
  const healthRouter = createHealthRouter(prisma);
  app.use('/health', healthRouter);
  app.use('/api/v1/health', healthRouter);

  // Phase 3 Modules
  app.use('/api/v1/auth', authRouter);
  app.use('/api/v1/cafe', cafeRouter);
  app.use('/api/v1/tables', tablesRouter);
  app.use('/api/v1/menu', menuRouter);
  app.use('/api/v1/offers', offersRouter);
  app.use('/api/v1/reservations', reservationsRouter);
  app.use('/api/v1/table-sessions', tableSessionsRouter);
  app.use('/api/v1/orders', ordersRouter);
  app.use('/api/v1/notifications', notificationsRouter);

  // 404 handler
  app.use(notFoundHandler);

  // Error handling
  app.use(errorHandler);

  return app;
}
