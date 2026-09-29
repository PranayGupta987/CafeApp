import 'dotenv/config';
import http from 'http';
import { PrismaClient } from '@prisma/client';
import { createApp } from './app.js';
import { getEnv } from './config/env.js';
import { logger } from './lib/logger.js';
import { initSocketServer } from './websocket/index.js';

async function main() {
  const env = getEnv();
  const prisma = new PrismaClient();

  try {
    await prisma.$connect();
    logger.info('Connected to database');
  } catch (err) {
    if (env.NODE_ENV === 'development') {
      logger.error(err, 'Failed to connect to database, but continuing in development mode');
    } else {
      logger.fatal(err, 'Failed to connect to database');
      process.exit(1);
    }
  }

  const app = createApp(prisma);
  const server = http.createServer(app);

  // Initialize Socket.IO
  initSocketServer(server);

  server.listen(env.PORT, () => {
    logger.info(`API & WebSocket server running on port ${env.PORT}`);
    logger.info(`Health check: http://localhost:${env.PORT}/health or /api/v1/health`);
  });

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
