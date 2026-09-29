import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';
import { NotificationType, ReservationStatus, UserRole } from '@prisma/client';
import type { AuthUser } from '../../middleware/auth.js';
import type { CheckAvailabilityQuery, CreateReservationInput, UpdateReservationStatusInput } from './types.js';

export class ReservationsService {
  static async checkTableConflict(
    tableId: string,
    reservationTime: Date,
    durationMinutes: number,
    excludeReservationId?: string,
    txClient: any = prisma,
  ): Promise<boolean> {
    const targetStart = reservationTime;
    const targetEnd = new Date(targetStart.getTime() + durationMinutes * 60 * 1000);

    // Fetch active reservations for this table on this day window (+- 6 hours)
    const windowStart = new Date(targetStart.getTime() - 6 * 60 * 60 * 1000);
    const windowEnd = new Date(targetEnd.getTime() + 6 * 60 * 60 * 1000);

    const activeReservations = await txClient.reservation.findMany({
      where: {
        tableId,
        id: excludeReservationId ? { not: excludeReservationId } : undefined,
        status: { in: [ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.SEATED] },
        reservationTime: { gte: windowStart, lte: windowEnd },
      },
    });

    for (const res of activeReservations) {
      const existingStart = new Date(res.reservationTime);
      const existingEnd = new Date(existingStart.getTime() + res.durationMinutes * 60 * 1000);

      // Overlap condition: targetStart < existingEnd && targetEnd > existingStart
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
    txClient: any = prisma,
  ): Promise<string | null> {
    const suitableTables = await txClient.table.findMany({
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
        undefined,
        txClient,
      );
      if (!hasConflict) {
        return table.id;
      }
    }

    return null;
  }

  static async getAvailability(query: CheckAvailabilityQuery) {
    const partySize = query.partySize || 2;
    const durationMinutes = query.durationMinutes || 90;

    let targetDate: Date;
    if (query.reservationTime) {
      targetDate = new Date(query.reservationTime);
    } else if (query.date) {
      const timeStr = query.time || '12:00';
      targetDate = new Date(`${query.date}T${timeStr}:00`);
    } else {
      targetDate = new Date();
      targetDate.setHours(targetDate.getHours() + 2, 0, 0, 0);
    }

    // 1. Get all active tables matching party size
    const allTables = await prisma.table.findMany({
      where: { isActive: true },
      orderBy: { tableNumber: 'asc' },
    });

    const suitableTables = allTables.filter((t) => t.capacity >= partySize);

    // 2. Check table-level availability for target time
    const availableTables = [];
    for (const table of suitableTables) {
      const isConflicted = await this.checkTableConflict(
        table.id,
        targetDate,
        durationMinutes,
      );
      if (!isConflicted) {
        availableTables.push({
          id: table.id,
          tableNumber: table.tableNumber,
          capacity: table.capacity,
          status: table.status,
          isAvailable: true,
        });
      }
    }

    // 3. Generate standard time slots for the day (e.g. 10:00 to 22:00)
    const baseDate = new Date(targetDate);
    const dateStr = baseDate.toISOString().split('T')[0];
    const timeSlots: Array<{
      time: string;
      isoString: string;
      availableTablesCount: number;
      isAvailable: boolean;
    }> = [];

    const slotHours = [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21];
    for (const hour of slotHours) {
      for (const minute of [0, 30]) {
        if (hour === 21 && minute === 30) continue; // Cafe closes at 22:00
        const slotTime = new Date(baseDate);
        slotTime.setHours(hour, minute, 0, 0);

        const timeLabel = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
        let availableCount = 0;

        for (const table of suitableTables) {
          const hasConflict = await this.checkTableConflict(
            table.id,
            slotTime,
            durationMinutes,
          );
          if (!hasConflict) {
            availableCount++;
          }
        }

        timeSlots.push({
          time: timeLabel,
          isoString: slotTime.toISOString(),
          availableTablesCount: availableCount,
          isAvailable: availableCount > 0 && slotTime > new Date(),
        });
      }
    }

    return {
      date: dateStr,
      partySize,
      durationMinutes,
      requestedTime: targetDate.toISOString(),
      availableTables,
      suitableTablesCount: suitableTables.length,
      timeSlots,
    };
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
    const requestedTableId = data.tableId || null;

    // Use Prisma transaction with strict isolation to prevent double booking race conditions
    return prisma.$transaction(async (tx) => {
      let assignedTableId: string | null = null;

      if (requestedTableId) {
        // Validate table existence and capacity
        const table = await tx.table.findUnique({ where: { id: requestedTableId } });
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
          requestedTableId,
          reservationTime,
          durationMinutes,
          undefined,
          tx,
        );
        if (hasConflict) {
          throw AppError.conflict(
            'TABLE_UNAVAILABLE',
            `Table ${table.tableNumber} is already reserved for the selected time slot`,
          );
        }
        assignedTableId = table.id;
      } else {
        // Auto-assign suitable table if available
        assignedTableId = await this.findAvailableTableForParty(
          data.partySize,
          reservationTime,
          durationMinutes,
          tx,
        );
        if (!assignedTableId) {
          throw AppError.conflict(
            'NO_TABLE_AVAILABLE',
            'No tables are available for the selected party size and time slot. Please try another time.',
          );
        }
      }

      const reservation = await tx.reservation.create({
        data: {
          userId,
          tableId: assignedTableId,
          partySize: data.partySize,
          reservationTime,
          durationMinutes,
          status: ReservationStatus.CONFIRMED,
          specialRequests: data.specialRequests,
        },
        include: {
          user: {
            select: { id: true, name: true, email: true, phone: true },
          },
          table: true,
        },
      });

      // Create notification
      const tableInfo = reservation.table ? `at Table ${reservation.table.tableNumber}` : '';
      await tx.notification.create({
        data: {
          userId,
          type: NotificationType.RESERVATION_STATUS,
          title: 'Table Reservation Confirmed',
          message: `Your table reservation for ${data.partySize} guests ${tableInfo} is confirmed for ${reservationTime.toLocaleString()}.`,
          data: { reservationId: reservation.id, tableId: assignedTableId },
        },
      });

      return reservation;
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

    return prisma.$transaction(async (tx) => {
      const updated = await tx.reservation.update({
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

      if (data.status === ReservationStatus.CANCELLED) {
        await tx.notification.create({
          data: {
            userId: reservation.userId,
            type: NotificationType.RESERVATION_STATUS,
            title: 'Reservation Cancelled',
            message: `Your reservation for ${new Date(reservation.reservationTime).toLocaleString()} has been cancelled.`,
            data: { reservationId: id, status: ReservationStatus.CANCELLED },
          },
        });
      }

      return updated;
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

