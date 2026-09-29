import { Router } from 'express';
import { TablesService } from './service.js';
import { createTableSchema, updateTableSchema, updateTableStatusSchema } from './types.js';
import { requireAuth, requireRoles } from '../../middleware/auth.js';
import { UserRole } from '@prisma/client';

const router = Router();

// Public: Get all tables
router.get('/', async (req, res, next) => {
  try {
    const includeInactive = req.query.includeInactive === 'true';
    const tables = await TablesService.getAllTables(includeInactive);
    res.json({ success: true, data: tables });
  } catch (err) {
    next(err);
  }
});

// Public / Authenticated: Look up table by QR Token
router.get('/qr/:qrToken', async (req, res, next) => {
  try {
    const table = await TablesService.getTableByQrToken(req.params.qrToken);
    res.json({ success: true, data: table });
  } catch (err) {
    next(err);
  }
});

// Public / Authenticated: Get table by ID
router.get('/:id', async (req, res, next) => {
  try {
    const table = await TablesService.getTableById(req.params.id);
    res.json({ success: true, data: table });
  } catch (err) {
    next(err);
  }
});

// Admin only: Create table
router.post('/', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = createTableSchema.parse(req.body);
    const table = await TablesService.createTable(data);
    res.status(201).json({ success: true, data: table });
  } catch (err) {
    next(err);
  }
});

// Admin only: Update table details
router.put('/:id', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = updateTableSchema.parse(req.body);
    const table = await TablesService.updateTable(req.params.id, data);
    res.json({ success: true, data: table });
  } catch (err) {
    next(err);
  }
});

// Admin, Staff & Kitchen: Update table occupancy/status
router.put(
  '/:id/status',
  requireAuth,
  requireRoles(UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF, UserRole.KITCHEN),
  async (req, res, next) => {
    try {
      const { status } = updateTableStatusSchema.parse(req.body);
      const table = await TablesService.updateTableStatus(req.params.id, status);
      res.json({ success: true, data: table });
    } catch (err) {
      next(err);
    }
  },
);

// Admin only: Delete table (soft delete)
router.delete('/:id', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const table = await TablesService.deleteTable(req.params.id);
    res.json({ success: true, message: 'Table deactivated successfully', data: table });
  } catch (err) {
    next(err);
  }
});

export default router;
