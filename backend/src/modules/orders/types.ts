import { z } from 'zod';
import { OrderStatus } from '@prisma/client';

export const orderItemInputSchema = z.object({
  menuItemId: z.string().uuid('Valid menuItemId required'),
  quantity: z.number().int().min(1, 'Quantity must be at least 1').max(50, 'Max quantity 50'),
  notes: z.string().max(200).optional().nullable(),
});

export const placeOrderSchema = z.object({
  sessionId: z.string().uuid().optional(),
  qrToken: z.string().optional(),
  tableId: z.string().uuid().optional(),
  items: z.array(orderItemInputSchema).min(1, 'Order must contain at least one item'),
  offerCode: z.string().optional().nullable(),
  specialInstructions: z.string().max(500).optional().nullable(),
});

export const updateOrderStatusSchema = z.object({
  status: z.nativeEnum(OrderStatus),
  notes: z.string().max(300).optional().nullable(),
});

export type OrderItemInput = z.infer<typeof orderItemInputSchema>;
export type PlaceOrderInput = z.infer<typeof placeOrderSchema>;
export type UpdateOrderStatusInput = z.infer<typeof updateOrderStatusSchema>;
