import { Router } from 'express';
import { CafeService } from './service.js';
import { updateCafeProfileSchema } from './types.js';
import { requireAuth, requireRoles } from '../../middleware/auth.js';
import { UserRole } from '@prisma/client';

const router = Router();

// Public: view cafe profile (name, timings, address, phone, GST, status)
router.get('/', async (_req, res, next) => {
  try {
    const profile = await CafeService.getProfile();
    res.json({ success: true, data: profile });
  } catch (err) {
    next(err);
  }
});

// Admin only: update cafe profile
router.put('/', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = updateCafeProfileSchema.parse(req.body);
    const updated = await CafeService.updateProfile(data);
    res.json({ success: true, data: updated });
  } catch (err) {
    next(err);
  }
});

export default router;
