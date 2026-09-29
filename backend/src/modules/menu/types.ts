import { z } from 'zod';

export const createCategorySchema = z.object({
  name: z.string().min(1, 'Category name is required'),
  description: z.string().optional().nullable(),
  displayOrder: z.number().int().optional().default(0),
  isActive: z.boolean().optional().default(true),
});

export const updateCategorySchema = z.object({
  name: z.string().min(1).optional(),
  description: z.string().optional().nullable(),
  displayOrder: z.number().int().optional(),
  isActive: z.boolean().optional(),
});

export const createMenuItemSchema = z.object({
  categoryId: z.string().uuid('Valid category ID required'),
  name: z.string().min(1, 'Item name is required'),
  description: z.string().optional().nullable(),
  pricePaise: z.number().int().positive('Price must be a positive integer in paise'),
  preparationTimeMinutes: z.number().int().positive().optional().default(15),
  isAvailable: z.boolean().optional().default(true),
  isVeg: z.boolean().optional().default(true),
  isSpicy: z.boolean().optional().default(false),
  imageUrl: z.string().url().optional().nullable(),
  displayOrder: z.number().int().optional().default(0),
});

export const updateMenuItemSchema = z.object({
  categoryId: z.string().uuid().optional(),
  name: z.string().min(1).optional(),
  description: z.string().optional().nullable(),
  pricePaise: z.number().int().positive().optional(),
  preparationTimeMinutes: z.number().int().positive().optional(),
  isAvailable: z.boolean().optional(),
  isVeg: z.boolean().optional(),
  isSpicy: z.boolean().optional(),
  imageUrl: z.string().url().optional().nullable(),
  displayOrder: z.number().int().optional(),
});

export const setItemAvailabilitySchema = z.object({
  isAvailable: z.boolean(),
});

export type CreateCategoryInput = z.infer<typeof createCategorySchema>;
export type UpdateCategoryInput = z.infer<typeof updateCategorySchema>;
export type CreateMenuItemInput = z.infer<typeof createMenuItemSchema>;
export type UpdateMenuItemInput = z.infer<typeof updateMenuItemSchema>;
export type SetItemAvailabilityInput = z.infer<typeof setItemAvailabilitySchema>;
