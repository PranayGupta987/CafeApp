import { Router } from 'express';
import { ReservationsService } from './service.js';
import { createReservationSchema, updateReservationStatusSchema } from './types.js';
import { requireAuth } from '../../middleware/auth.js';
import { ReservationStatus } from '@prisma/client';

const router = Router();

// Authenticated: Get reservations (Users get their own; Staff/Admin get all)
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const status = req.query.status as ReservationStatus | undefined;
    const reservations = await ReservationsService.getReservations(req.user!, status);
    res.json({ success: true, data: reservations });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Get reservation by ID
router.get('/:id', requireAuth, async (req, res, next) => {
  try {
    const reservation = await ReservationsService.getReservationById(req.params.id, req.user!);
    res.json({ success: true, data: reservation });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Create reservation
router.post('/', requireAuth, async (req, res, next) => {
  try {
    const data = createReservationSchema.parse(req.body);
    const reservation = await ReservationsService.createReservation(data, req.user!.id);
    res.status(201).json({ success: true, data: reservation });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Update reservation status / assign table
router.put('/:id/status', requireAuth, async (req, res, next) => {
  try {
    const data = updateReservationStatusSchema.parse(req.body);
    const reservation = await ReservationsService.updateReservationStatus(
      req.params.id,
      data,
      req.user!,
    );
    res.json({ success: true, data: reservation });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Cancel reservation
router.delete('/:id', requireAuth, async (req, res, next) => {
  try {
    const reservation = await ReservationsService.cancelReservation(req.params.id, req.user!);
    res.json({ success: true, message: 'Reservation cancelled successfully', data: reservation });
  } catch (err) {
    next(err);
  }
});

export default router;
