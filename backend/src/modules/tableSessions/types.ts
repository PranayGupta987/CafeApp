import { z } from 'zod';

export const checkInSchema = z.object({
  qrToken: z.string().optional(),
  tableId: z.string().uuid().optional(),
  guestCount: z.number().int().min(1).max(20).optional().default(1),
}).refine((data) => data.qrToken || data.tableId, {
  message: 'Either qrToken or tableId must be provided',
});

export const checkOutSchema = z.object({
  notes: z.string().optional(),
});

export type CheckInInput = z.infer<typeof checkInSchema>;
export type CheckOutInput = z.infer<typeof checkOutSchema>;
