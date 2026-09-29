import { Router } from 'express';
import type { PrismaClient } from '@prisma/client';
import { getEnv } from '../../config/env.js';

export function createHealthRouter(prisma?: PrismaClient): Router {
  const router = Router();

  const handleHealth = async (_req: any, res: any) => {
    const env = getEnv();
    let dbStatus = 'disconnected';

    if (prisma) {
      try {
        await prisma.$queryRaw`SELECT 1`;
        dbStatus = 'connected';
      } catch {
        dbStatus = 'unreachable';
      }
    }

    res.json({
      status: 'ok',
      service: 'CafeApp Backend',
      timestamp: new Date().toISOString(),
      environment: env.NODE_ENV,
      database: dbStatus,
      uptime: process.uptime(),
    });
  };

  router.get('/', handleHealth);
  router.get('/health', handleHealth);

  return router;
}
