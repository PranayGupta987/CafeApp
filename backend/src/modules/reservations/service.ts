import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';
import { ReservationStatus, UserRole } from '@prisma/client';
import type { AuthUser } from '../../middleware/auth.js';
import type { CreateReservationInput, UpdateReservationStatusInput } from './types.js';

export class ReservationsService {
  static async checkTableConflict(
    tableId: string,
    reservationTime: Date,
    durationMinutes: number,
    excludeReservationId?: string,
  ): Promise<boolean> {
    const targetStart = reservationTime;
    const targetEnd = new Date(targetStart.getTime() + durationMinutes * 60 * 1000);

    // Fetch active reservations for this table on this day window (+- 6 hours)
    const windowStart = new Date(targetStart.getTime() - 6 * 60 * 60 * 1000);
    const windowEnd = new Date(targetEnd.getTime() + 6 * 60 * 60 * 1000);

    const activeReservations = await prisma.reservation.findMany({
      where: {
        tableId,
        id: excludeReservationId ? { not: excludeReservationId } : undefined,
        status: { in: [ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.SEATED] },
        reservationTime: { gte: windowStart, lte: windowEnd },
      },
    });

    for (const res of activeReservations) {
      const existingStart = res.reservationTime;
      const existingEnd = new Date(existingStart.getTime() + res.durationMinutes * 60 * 1000);

      // Overlap condition: startA < endB && endA > startB
      if (targetStart < existingEnd && targetEnd > existingStart) {
        return true; // Conflict found
      }
    }

    return false;
  }

  static async findAvailableTableForParty(
    partySize: number,
    reservationTime: Date,
    durationMinutes: number,
  ): Promise<string | null> {
    const suitableTables = await prisma.table.findMany({
      where: {
        isActive: true,
        capacity: { gte: partySize },
      },
      orderBy: { capacity: 'asc' }, // Smallest suitable table first
    });

    for (const table of suitableTables) {
      const hasConflict = await this.checkTableConflict(
        table.id,
        reservationTime,
        durationMinutes,
      );
      if (!hasConflict) {
        return table.id;
      }
    }

    return null;
  }

  static async getReservations(user: AuthUser, status?: ReservationStatus) {
    const isStaffOrAdmin = (
      [UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF] as UserRole[]
    ).includes(user.role);

    return prisma.reservation.findMany({
      where: {
        userId: isStaffOrAdmin ? undefined : user.id,
        status: status || undefined,
      },
      include: {
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
        table: {
          select: { id: true, tableNumber: true, capacity: true },
        },
      },
      orderBy: { reservationTime: 'desc' },
    });
  }

  static async getReservationById(id: string, user: AuthUser) {
    const reservation = await prisma.reservation.findUnique({
      where: { id },
      include: {
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
        table: true,
      },
    });

    if (!reservation) {
      throw AppError.notFound('Reservation');
    }

    const isStaffOrAdmin = (
      [UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF] as UserRole[]
    ).includes(user.role);
    if (!isStaffOrAdmin && reservation.userId !== user.id) {
      throw AppError.forbidden('You can only access your own reservations');
    }

    return reservation;
  }

  static async createReservation(data: CreateReservationInput, userId: string) {
    const reservationTime = new Date(data.reservationTime);
    const now = new Date();

    if (reservationTime < now) {
      throw AppError.badRequest('INVALID_TIME', 'Reservation time must be in the future');
    }

    const durationMinutes = data.durationMinutes || 90;
    let assignedTableId = data.tableId || null;

    if (assignedTableId) {
      // Validate table
      const table = await prisma.table.findUnique({ where: { id: assignedTableId } });
      if (!table || !table.isActive) {
        throw AppError.notFound('Selected table');
      }
      if (table.capacity < data.partySize) {
        throw AppError.badRequest(
          'INSUFFICIENT_CAPACITY',
          `Table ${table.tableNumber} capacity (${table.capacity}) is less than party size (${data.partySize})`,
        );
      }

      const hasConflict = await this.checkTableConflict(
        assignedTableId,
        reservationTime,
        durationMinutes,
      );
      if (hasConflict) {
        throw AppError.conflict(
          'TABLE_UNAVAILABLE',
          `Table ${table.tableNumber} is already reserved for the selected time slot`,
        );
      }
    } else {
      // Auto-assign suitable table if available
      assignedTableId = await this.findAvailableTableForParty(
        data.partySize,
        reservationTime,
        durationMinutes,
      );
    }

    return prisma.reservation.create({
      data: {
        userId,
        tableId: assignedTableId,
        partySize: data.partySize,
        reservationTime,
        durationMinutes,
        status: assignedTableId ? ReservationStatus.CONFIRMED : ReservationStatus.PENDING,
        specialRequests: data.specialRequests,
      },
      include: {
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
        table: true,
      },
    });
  }

  static async updateReservationStatus(
    id: string,
    data: UpdateReservationStatusInput,
    user: AuthUser,
  ) {
    const reservation = await this.getReservationById(id, user);
    const isStaffOrAdmin = (
      [UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF] as UserRole[]
    ).includes(user.role);

    if (!isStaffOrAdmin) {
      // Customers can only cancel their own pending/confirmed reservation
      if (data.status !== ReservationStatus.CANCELLED) {
        throw AppError.forbidden('Customers can only cancel reservations');
      }
    }

    let tableId = data.tableId !== undefined ? data.tableId : reservation.tableId;

    if (
      tableId &&
      ([ReservationStatus.CONFIRMED, ReservationStatus.SEATED] as ReservationStatus[]).includes(data.status)
    ) {
      const hasConflict = await this.checkTableConflict(
        tableId,
        reservation.reservationTime,
        reservation.durationMinutes,
        reservation.id,
      );
      if (hasConflict) {
        throw AppError.conflict(
          'TABLE_UNAVAILABLE',
          'The specified table is already reserved during this time slot',
        );
      }
    }

    return prisma.reservation.update({
      where: { id },
      data: {
        status: data.status,
        ...(tableId !== undefined ? { tableId } : {}),
      },
      include: {
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
        table: true,
      },
    });
  }

  static async cancelReservation(id: string, user: AuthUser) {
    return this.updateReservationStatus(
      id,
      { status: ReservationStatus.CANCELLED },
      user,
    );
  }
}
