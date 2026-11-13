import 'dotenv/config';
import http from 'http';
import { PrismaClient } from '@prisma/client';
import { createApp } from './app.js';
import { getEnv } from './config/env.js';
import { logger } from './lib/logger.js';
import { initSocketServer } from './websocket/index.js';
import { seedDatabase } from './lib/seed.js';

async function main() {
  const env = getEnv();
  const prisma = new PrismaClient();

  const app = createApp(prisma);
  const server = http.createServer(app);

  // Initialize Socket.IO
  initSocketServer(server);

  server.listen(env.PORT, '0.0.0.0', () => {
    logger.info(`API & WebSocket server running on port ${env.PORT}`);
    logger.info(`Health check: http://localhost:${env.PORT}/health or /api/v1/health`);
  });

  // Background DB connect and seed
  (async () => {
    try {
      logger.info('Connecting to database...');
      await prisma.$connect();
      logger.info('Connected to database successfully');

      // Auto-seed if database is fresh
      try {
        const cafeCount = await prisma.cafeProfile.count();
        if (cafeCount === 0) {
          logger.info('Fresh database detected. Seeding initial cafe profile, users, menu and tables...');
          await seedDatabase(prisma);
          logger.info('Database seeding completed successfully');
        } else {
          logger.info('Database already seeded (CafeProfile count > 0)');
        }
      } catch (seedErr) {
        logger.warn({ err: seedErr }, 'Database seeding check/execution skipped');
      }
    } catch (err) {
      logger.error(err, 'Database initialization error during startup');
    }
  })();

  // Graceful shutdown
  const shutdown = async (signal: string) => {
    logger.info(`${signal} received, shutting down`);
    server.close();
    await prisma.$disconnect();
    process.exit(0);
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));
}

main().catch((err) => {
  console.error('Fatal startup error:', err);
  process.exit(1);
});
