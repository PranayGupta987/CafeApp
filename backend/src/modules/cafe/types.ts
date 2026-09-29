import { z } from 'zod';

export const updateCafeProfileSchema = z.object({
  name: z.string().min(1).optional(),
  description: z.string().optional().nullable(),
  address: z.string().min(1).optional(),
  phone: z.string().min(5).optional(),
  email: z.string().email().optional(),
  gstin: z.string().optional().nullable(),
  gstPercent: z.number().min(0).max(100).optional(),
  currency: z.string().length(3).optional(),
  timezone: z.string().optional(),
  logoUrl: z.string().url().optional().nullable(),
  openingTime: z.string().regex(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/, 'Invalid time format (HH:MM)').optional(),
  closingTime: z.string().regex(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/, 'Invalid time format (HH:MM)').optional(),
  isAcceptingOrders: z.boolean().optional(),
});

export type UpdateCafeProfileInput = z.infer<typeof updateCafeProfileSchema>;
