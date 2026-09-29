import { z } from 'zod';
import { TableStatus } from '@prisma/client';

export const createTableSchema = z.object({
  tableNumber: z.number().int().positive('Table number must be a positive integer'),
  capacity: z.number().int().min(1, 'Capacity must be at least 1 person'),
  qrToken: z.string().min(1).optional(),
  status: z.nativeEnum(TableStatus).optional().default(TableStatus.AVAILABLE),
  isActive: z.boolean().optional().default(true),
});

export const updateTableSchema = z.object({
  tableNumber: z.number().int().positive().optional(),
  capacity: z.number().int().min(1).optional(),
  qrToken: z.string().min(1).optional(),
  status: z.nativeEnum(TableStatus).optional(),
  isActive: z.boolean().optional(),
});

export const updateTableStatusSchema = z.object({
  status: z.nativeEnum(TableStatus),
});

export type CreateTableInput = z.infer<typeof createTableSchema>;
export type UpdateTableInput = z.infer<typeof updateTableSchema>;
export type UpdateTableStatusInput = z.infer<typeof updateTableStatusSchema>;
