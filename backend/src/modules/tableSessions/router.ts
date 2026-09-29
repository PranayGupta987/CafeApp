import { Router } from 'express';
import { TableSessionsService } from './service.js';
import { checkInSchema } from './types.js';
import { requireAuth } from '../../middleware/auth.js';

const router = Router();

// Authenticated: Get currently active session for user or specified table
router.get('/active', requireAuth, async (req, res, next) => {
  try {
    const tableId = typeof req.query.tableId === 'string' ? req.query.tableId : undefined;
    const session = await TableSessionsService.getActiveSession(req.user!.id, tableId);
    res.json({ success: true, data: session });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Get table session by ID
router.get('/:id', requireAuth, async (req, res, next) => {
  try {
    const session = await TableSessionsService.getSessionById(req.params.id, req.user!);
    res.json({ success: true, data: session });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Check in at a table (via QR token or tableId)
router.post('/check-in', requireAuth, async (req, res, next) => {
  try {
    const data = checkInSchema.parse(req.body);
    const session = await TableSessionsService.checkIn(data, req.user!.id);
    res.status(201).json({ success: true, data: session });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Check out from a table session
router.post(['/:id/check-out', '/check-out/:id'], requireAuth, async (req, res, next) => {
  try {
    const session = await TableSessionsService.checkOut(req.params.id, req.user!);
    res.json({ success: true, message: 'Checked out successfully', data: session });
  } catch (err) {
    next(err);
  }
});

export default router;
