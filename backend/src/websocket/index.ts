import type { Server as HttpServer } from 'http';
import { Server, Socket } from 'socket.io';
import jwt from 'jsonwebtoken';
import { logger } from '../lib/logger.js';
import { getEnv } from '../config/env.js';

export interface AuthenticatedSocket extends Socket {
  data: {
    user?: {
      id: string;
      email: string;
      role: string;
    };
  };
}

let _io: Server | null = null;

export function initSocketServer(httpServer: HttpServer): Server {
  const env = getEnv();

  _io = new Server(httpServer, {
    cors: {
      origin: env.CORS_ORIGINS === '*' ? '*' : env.CORS_ORIGINS.split(','),
      methods: ['GET', 'POST'],
      credentials: true,
    },
    pingInterval: 25000,
    pingTimeout: 20000,
  });

  // Authentication hook foundation
  _io.use((socket: AuthenticatedSocket, next) => {
    const rawAuth = socket.handshake.auth?.token || socket.handshake.headers?.authorization;

    if (!rawAuth) {
      logger.debug({ socketId: socket.id }, 'Socket connection established without token (guest/unauthenticated)');
      return next();
    }

    const token = typeof rawAuth === 'string' && rawAuth.startsWith('Bearer ')
      ? rawAuth.slice(7)
      : rawAuth;

    try {
      const decoded = jwt.verify(token, env.JWT_ACCESS_SECRET) as any;
      socket.data.user = {
        id: decoded.sub,
        email: decoded.email,
        role: decoded.role,
      };
      logger.info({ socketId: socket.id, userId: decoded.sub, role: decoded.role }, 'Socket authenticated successfully');
      next();
    } catch (err: any) {
      logger.warn({ socketId: socket.id, error: err.message }, 'Socket authentication failed');
      next(new Error('Authentication failed'));
    }
  });

  // Connection handling foundation
  _io.on('connection', (socket: AuthenticatedSocket) => {
    const user = socket.data.user;
    logger.info({ socketId: socket.id, user: user ? `${user.email} (${user.role})` : 'anonymous' }, 'Client connected to WebSocket');

    // Auto-join personal room if authenticated
    if (user?.id) {
      const userRoom = `user:${user.id}`;
      socket.join(userRoom);
      logger.debug({ socketId: socket.id, room: userRoom }, 'Socket joined personal room');

      // Auto-join kitchen/staff rooms if staff/kitchen/admin
      if (['CAFE_ADMIN', 'CAFE_STAFF', 'KITCHEN'].includes(user.role)) {
        socket.join('kitchen');
        logger.debug({ socketId: socket.id, room: 'kitchen' }, 'Socket joined kitchen room');
      }
    }

    // Ping / Pong diagnostics
    socket.on('ping', () => {
      socket.emit('pong', { timestamp: new Date().toISOString() });
    });

    // Disconnect handling
    socket.on('disconnect', (reason) => {
      logger.info({ socketId: socket.id, reason }, 'Client disconnected from WebSocket');
    });

    socket.on('error', (err) => {
      logger.error({ socketId: socket.id, err }, 'Socket error encountered');
    });
  });

  logger.info('WebSocket / Socket.IO server initialized');
  return _io;
}

export function getSocketServer(): Server {
  if (!_io) {
    throw new Error('Socket.IO server has not been initialized');
  }
  return _io;
}
