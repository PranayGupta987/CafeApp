import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';
import { TableSessionStatus, TableStatus, UserRole } from '@prisma/client';
import type { AuthUser } from '../../middleware/auth.js';
import type { CheckInInput } from './types.js';

export class TableSessionsService {
  static async getActiveSession(userId?: string, tableId?: string) {
    if (tableId) {
      return prisma.tableSession.findFirst({
        where: {
          tableId,
          status: TableSessionStatus.ACTIVE,
        },
        include: {
          table: true,
          hostUser: {
            select: { id: true, name: true, email: true, phone: true },
          },
          orders: {
            include: {
              items: {
                include: { menuItem: true },
              },
              statusHistory: true,
            },
            orderBy: { createdAt: 'desc' },
          },
        },
      });
    }

    if (userId) {
      return prisma.tableSession.findFirst({
        where: {
          hostUserId: userId,
          status: TableSessionStatus.ACTIVE,
        },
        include: {
          table: true,
          hostUser: {
            select: { id: true, name: true, email: true, phone: true },
          },
          orders: {
            include: {
              items: {
                include: { menuItem: true },
              },
              statusHistory: true,
            },
            orderBy: { createdAt: 'desc' },
          },
        },
      });
    }

    return null;
  }

  static async getSessionById(id: string, user: AuthUser) {
    const session = await prisma.tableSession.findUnique({
      where: { id },
      include: {
        table: true,
        hostUser: {
          select: { id: true, name: true, email: true, phone: true },
        },
        orders: {
          include: {
            items: {
              include: { menuItem: true },
            },
            statusHistory: true,
          },
          orderBy: { createdAt: 'desc' },
        },
      },
    });

    if (!session) {
      throw AppError.notFound('Table Session');
    }

    const isStaffOrAdmin = (
      [UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF] as UserRole[]
    ).includes(user.role);
    if (!isStaffOrAdmin && session.hostUserId !== user.id) {
      throw AppError.forbidden('You can only access your own table session');
    }

    return session;
  }

  static async checkIn(data: CheckInInput, userId: string) {
    let table;
    if (data.qrToken) {
      table = await prisma.table.findUnique({
        where: { qrToken: data.qrToken },
      });
    } else if (data.tableId) {
      table = await prisma.table.findUnique({
        where: { id: data.tableId },
      });
    }

    if (!table || !table.isActive) {
      throw AppError.notFound('Table');
    }

    // Check if table currently has an active session
    const existingTableSession = await prisma.tableSession.findFirst({
      where: {
        tableId: table.id,
        status: TableSessionStatus.ACTIVE,
      },
      include: {
        table: true,
        hostUser: {
          select: { id: true, name: true, email: true },
        },
      },
    });

    if (existingTableSession) {
      // If the current user is already the host of this active session, return it
      if (existingTableSession.hostUserId === userId) {
        return existingTableSession;
      }
      // Table is occupied by another party
      throw AppError.conflict(
        'TABLE_OCCUPIED',
        `Table ${table.tableNumber} is currently occupied by another session`,
      );
    }

    // Check if the user already has an active session on another table
    const existingUserSession = await prisma.tableSession.findFirst({
      where: {
        hostUserId: userId,
        status: TableSessionStatus.ACTIVE,
      },
      include: { table: true },
    });

    if (existingUserSession) {
      throw AppError.conflict(
        'ACTIVE_SESSION_EXISTS',
        `You already have an active session at Table ${existingUserSession.table.tableNumber}. Please check out before scanning a new table.`,
      );
    }

    // Create active session and mark table OCCUPIED in transaction
    return prisma.$transaction(async (tx) => {
      const session = await tx.tableSession.create({
        data: {
          tableId: table.id,
          hostUserId: userId,
          guestCount: data.guestCount || 1,
          status: TableSessionStatus.ACTIVE,
        },
        include: {
          table: true,
          hostUser: {
            select: { id: true, name: true, email: true, phone: true },
          },
        },
      });

      await tx.table.update({
        where: { id: table.id },
        data: { status: TableStatus.OCCUPIED },
      });

      return session;
    });
  }

  static async checkOut(sessionId: string, user: AuthUser) {
    const session = await this.getSessionById(sessionId, user);

    if (session.status !== TableSessionStatus.ACTIVE) {
      throw AppError.badRequest('SESSION_INACTIVE', 'This session is already completed or cancelled');
    }

    return prisma.$transaction(async (tx) => {
      const updatedSession = await tx.tableSession.update({
        where: { id: sessionId },
        data: {
          status: TableSessionStatus.COMPLETED,
          checkedOutAt: new Date(),
        },
        include: {
          table: true,
          hostUser: {
            select: { id: true, name: true, email: true },
          },
        },
      });

      // Release table back to AVAILABLE
      await tx.table.update({
        where: { id: session.tableId },
        data: { status: TableStatus.AVAILABLE },
      });

      return updatedSession;
    });
  }
}
