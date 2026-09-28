import { Router } from 'express';
import { PrismaClient } from '@prisma/client';

export function createHealthRouter(prisma: PrismaClient): Router {
  const router = Router();

  router.get('/health', async (_req, res) => {
    try {
      await prisma.$queryRaw`SELECT 1`;
      res.json({
        status: 'ok',
        timestamp: new Date().toISOString(),
        uptime: process.uptime(),
      });
    } catch {
      res.status(503).json({
        status: 'error',
        timestamp: new Date().toISOString(),
        message: 'Database connection failed',
      });
    }
  });

  return router;
}
