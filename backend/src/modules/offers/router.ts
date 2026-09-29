import { Router } from 'express';
import { OffersService } from './service.js';
import { createOfferSchema, updateOfferSchema, validateOfferQuerySchema } from './types.js';
import { requireAuth, requireRoles } from '../../middleware/auth.js';
import { UserRole } from '@prisma/client';

const router = Router();

// Public: Get all active offers
router.get('/', async (req, res, next) => {
  try {
    const all = req.query.all === 'true';
    const offers = all ? await OffersService.getAllOffers() : await OffersService.getActiveOffers();
    res.json({ success: true, data: offers });
  } catch (err) {
    next(err);
  }
});

// Public / Authenticated: Validate promo code for cart/order subtotal
router.get(['/:code/validate', '/validate/:code'], async (req, res, next) => {
  try {
    const { subtotalPaise } = validateOfferQuerySchema.parse(req.query);
    const result = await OffersService.validateOffer(req.params.code, subtotalPaise);
    if (!result.valid) {
      res.status(400).json({
        success: false,
        valid: false,
        discountPaise: 0,
        message: result.reason,
      });
      return;
    }
    res.json({
      success: true,
      valid: true,
      data: {
        code: result.offer.code,
        title: result.offer.title,
        discountType: result.offer.discountType,
        discountValue: result.offer.discountValue,
        discountPaise: result.discountPaise,
      },
    });
  } catch (err) {
    next(err);
  }
});

// Public / Authenticated: Get offer by ID
router.get('/:id', async (req, res, next) => {
  try {
    const offer = await OffersService.getOfferById(req.params.id);
    res.json({ success: true, data: offer });
  } catch (err) {
    next(err);
  }
});

// Admin only: Create offer
router.post('/', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = createOfferSchema.parse(req.body);
    const offer = await OffersService.createOffer(data);
    res.status(201).json({ success: true, data: offer });
  } catch (err) {
    next(err);
  }
});

// Admin only: Update offer
router.put('/:id', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = updateOfferSchema.parse(req.body);
    const offer = await OffersService.updateOffer(req.params.id, data);
    res.json({ success: true, data: offer });
  } catch (err) {
    next(err);
  }
});

// Admin only: Delete offer
router.delete('/:id', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const offer = await OffersService.deleteOffer(req.params.id);
    res.json({ success: true, message: 'Offer deleted successfully', data: offer });
  } catch (err) {
    next(err);
  }
});

export default router;
