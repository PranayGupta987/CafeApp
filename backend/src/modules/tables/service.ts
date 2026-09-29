import crypto from 'crypto';
import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';
import { TableStatus } from '@prisma/client';
import type { CreateTableInput, UpdateTableInput } from './types.js';

export class TablesService {
  static async getAllTables(includeInactive = false) {
    return prisma.table.findMany({
      where: includeInactive ? undefined : { isActive: true },
      orderBy: { tableNumber: 'asc' },
    });
  }

  static async getTableById(id: string) {
    const table = await prisma.table.findUnique({
      where: { id },
      include: {
        tableSessions: {
          where: { status: 'ACTIVE' },
          take: 1,
        },
      },
    });

    if (!table) {
      throw AppError.notFound('Table');
    }
    return table;
  }

  static async getTableByQrToken(qrToken: string) {
    const table = await prisma.table.findUnique({
      where: { qrToken },
      include: {
        tableSessions: {
          where: { status: 'ACTIVE' },
          include: {
            hostUser: {
              select: { id: true, name: true, email: true },
            },
          },
          take: 1,
        },
      },
    });

    if (!table || !table.isActive) {
      throw AppError.notFound('Table with provided QR token');
    }
    return table;
  }

  static async createTable(data: CreateTableInput) {
    const existingNumber = await prisma.table.findUnique({
      where: { tableNumber: data.tableNumber },
    });
    if (existingNumber) {
      throw AppError.conflict('TABLE_EXISTS', `Table number ${data.tableNumber} already exists`);
    }

    const qrToken = data.qrToken || `table_qr_${data.tableNumber}_${crypto.randomBytes(4).toString('hex')}`;
    const existingQr = await prisma.table.findUnique({
      where: { qrToken },
    });
    if (existingQr) {
      throw AppError.conflict('QR_EXISTS', 'QR token already assigned to another table');
    }

    return prisma.table.create({
      data: {
        tableNumber: data.tableNumber,
        capacity: data.capacity,
        qrToken,
        status: data.status || TableStatus.AVAILABLE,
        isActive: data.isActive !== undefined ? data.isActive : true,
      },
    });
  }

  static async updateTable(id: string, data: UpdateTableInput) {
    await this.getTableById(id);

    if (data.tableNumber) {
      const existing = await prisma.table.findFirst({
        where: { tableNumber: data.tableNumber, NOT: { id } },
      });
      if (existing) {
        throw AppError.conflict('TABLE_EXISTS', `Table number ${data.tableNumber} already in use`);
      }
    }

    if (data.qrToken) {
      const existingQr = await prisma.table.findFirst({
        where: { qrToken: data.qrToken, NOT: { id } },
      });
      if (existingQr) {
        throw AppError.conflict('QR_EXISTS', 'QR token already assigned to another table');
      }
    }

    return prisma.table.update({
      where: { id },
      data: {
        ...(data.tableNumber !== undefined ? { tableNumber: data.tableNumber } : {}),
        ...(data.capacity !== undefined ? { capacity: data.capacity } : {}),
        ...(data.qrToken !== undefined ? { qrToken: data.qrToken } : {}),
        ...(data.status !== undefined ? { status: data.status } : {}),
        ...(data.isActive !== undefined ? { isActive: data.isActive } : {}),
      },
    });
  }

  static async updateTableStatus(id: string, status: TableStatus) {
    await this.getTableById(id);
    return prisma.table.update({
      where: { id },
      data: { status },
    });
  }

  static async deleteTable(id: string) {
    await this.getTableById(id);

    // Check if table has active sessions or placed orders
    const activeSession = await prisma.tableSession.findFirst({
      where: { tableId: id, status: 'ACTIVE' },
    });
    if (activeSession) {
      throw AppError.badRequest('TABLE_IN_USE', 'Cannot delete table with active customer session');
    }

    // Soft delete table by marking inactive
    return prisma.table.update({
      where: { id },
      data: { isActive: false },
    });
  }
}
