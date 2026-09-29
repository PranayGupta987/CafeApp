import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';
import { NotificationType } from '@prisma/client';
import { getSocketServer } from '../../websocket/index.js';
import { logger } from '../../lib/logger.js';

export class NotificationsService {
  static async getUserNotifications(userId: string) {
    return prisma.notification.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' },
      take: 50,
    });
  }

  static async markAsRead(id: string, userId: string) {
    const notification = await prisma.notification.findUnique({
      where: { id },
    });

    if (!notification || notification.userId !== userId) {
      throw AppError.notFound('Notification');
    }

    return prisma.notification.update({
      where: { id },
      data: { isRead: true },
    });
  }

  static async markAllAsRead(userId: string) {
    return prisma.notification.updateMany({
      where: { userId, isRead: false },
      data: { isRead: true },
    });
  }

  static async createNotification(
    userId: string,
    type: NotificationType,
    title: string,
    message: string,
    data?: any,
  ) {
    const notification = await prisma.notification.create({
      data: {
        userId,
        type,
        title,
        message,
        data: data || undefined,
      },
    });

    try {
      const io = getSocketServer();
      io.to(`user:${userId}`).emit('notification:new', notification);
    } catch (wsErr) {
      logger.warn({ wsErr }, 'Failed to emit WebSocket notification event');
    }

    return notification;
  }
}
