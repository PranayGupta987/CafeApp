import { z } from 'zod';
import { NotificationType } from '@prisma/client';

export const createNotificationSchema = z.object({
  userId: z.string().uuid(),
  type: z.nativeEnum(NotificationType),
  title: z.string().min(1),
  message: z.string().min(1),
  data: z.record(z.any()).optional().nullable(),
});

export type CreateNotificationInput = z.infer<typeof createNotificationSchema>;
