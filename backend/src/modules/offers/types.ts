import { z } from 'zod';
import { DiscountType } from '@prisma/client';

export const createOfferSchema = z.object({
  code: z
    .string()
    .min(3, 'Code must be at least 3 characters')
    .max(20, 'Code max 20 characters')
    .toUpperCase(),
  title: z.string().optional().nullable(),
  description: z.string().min(1, 'Description is required'),
  discountType: z.nativeEnum(DiscountType),
  discountValue: z.number().int().positive('Discount value must be a positive integer'),
  minOrderAmountPaise: z.number().int().min(0).optional().default(0),
  maxDiscountAmountPaise: z.number().int().positive().optional().nullable(),
  validFrom: z.string().datetime(),
  validTo: z.string().datetime(),
  isActive: z.boolean().optional().default(true),
});

export const updateOfferSchema = z.object({
  code: z.string().min(3).max(20).toUpperCase().optional(),
  title: z.string().optional().nullable(),
  description: z.string().min(1).optional(),
  discountType: z.nativeEnum(DiscountType).optional(),
  discountValue: z.number().int().positive().optional(),
  minOrderAmountPaise: z.number().int().min(0).optional(),
  maxDiscountAmountPaise: z.number().int().positive().optional().nullable(),
  validFrom: z.string().datetime().optional(),
  validTo: z.string().datetime().optional(),
  isActive: z.boolean().optional(),
});

export const validateOfferQuerySchema = z.object({
  subtotalPaise: z.coerce.number().int().min(0, 'Subtotal in paise must be non-negative'),
});

export type CreateOfferInput = z.infer<typeof createOfferSchema>;
export type UpdateOfferInput = z.infer<typeof updateOfferSchema>;
