import { z } from 'zod';
import { ReservationStatus } from '@prisma/client';

export const createReservationSchema = z.object({
  tableId: z.string().uuid().optional().nullable(),
  partySize: z.number().int().min(1, 'Party size must be at least 1 person').max(20, 'Max party size is 20'),
  reservationTime: z.string().datetime('Valid ISO-8601 reservation datetime required'),
  durationMinutes: z.number().int().min(30).max(240).optional().default(90),
  specialRequests: z.string().optional().nullable(),
});

export const updateReservationStatusSchema = z.object({
  status: z.nativeEnum(ReservationStatus),
  tableId: z.string().uuid().optional().nullable(),
});

export type CreateReservationInput = z.infer<typeof createReservationSchema>;
export type UpdateReservationStatusInput = z.infer<typeof updateReservationStatusSchema>;
