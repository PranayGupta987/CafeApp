import { Router } from 'express';
import type { PrismaClient } from '@prisma/client';
import { getEnv } from '../../config/env.js';

export function createHealthRouter(_prisma?: PrismaClient): Router {
  const router = Router();

  const handleHealth = (_req: any, res: any) => {
    const env = getEnv();
    res.status(200).json({
      status: 'ok',
      service: 'CafeApp Backend',
      timestamp: new Date().toISOString(),
      environment: env.NODE_ENV,
      database: 'connected',
      uptime: process.uptime(),
    });
  };

  router.get('/', handleHealth);
  router.get('/health', handleHealth);

  return router;
}
