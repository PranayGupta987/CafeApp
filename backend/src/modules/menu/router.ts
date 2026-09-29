import { Router } from 'express';
import { MenuService } from './service.js';
import {
  createCategorySchema,
  updateCategorySchema,
  createMenuItemSchema,
  updateMenuItemSchema,
  setItemAvailabilitySchema,
} from './types.js';
import { requireAuth, requireRoles } from '../../middleware/auth.js';
import { UserRole } from '@prisma/client';

const router = Router();

// --- Categories ---

// Public: Get all categories
router.get('/categories', async (req, res, next) => {
  try {
    const includeInactive = req.query.includeInactive === 'true';
    const categories = await MenuService.getAllCategories(includeInactive);
    res.json({ success: true, data: categories });
  } catch (err) {
    next(err);
  }
});

// Public: Get category by ID
router.get('/categories/:id', async (req, res, next) => {
  try {
    const category = await MenuService.getCategoryById(req.params.id);
    res.json({ success: true, data: category });
  } catch (err) {
    next(err);
  }
});

// Admin only: Create category
router.post('/categories', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = createCategorySchema.parse(req.body);
    const category = await MenuService.createCategory(data);
    res.status(201).json({ success: true, data: category });
  } catch (err) {
    next(err);
  }
});

// Admin only: Update category
router.put('/categories/:id', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = updateCategorySchema.parse(req.body);
    const category = await MenuService.updateCategory(req.params.id, data);
    res.json({ success: true, data: category });
  } catch (err) {
    next(err);
  }
});

// Admin only: Delete category
router.delete('/categories/:id', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const category = await MenuService.deleteCategory(req.params.id);
    res.json({ success: true, message: 'Category removed successfully', data: category });
  } catch (err) {
    next(err);
  }
});

// --- Menu Items ---

// Public: List items with optional filters
router.get('/items', async (req, res, next) => {
  try {
    const { categoryId, isVeg, isAvailable, search } = req.query;
    const items = await MenuService.getMenuItems({
      categoryId: typeof categoryId === 'string' ? categoryId : undefined,
      isVeg: isVeg !== undefined ? isVeg === 'true' : undefined,
      isAvailable: isAvailable !== undefined ? isAvailable === 'true' : undefined,
      search: typeof search === 'string' ? search : undefined,
    });
    res.json({ success: true, data: items });
  } catch (err) {
    next(err);
  }
});

// Public: Get menu item by ID
router.get('/items/:id', async (req, res, next) => {
  try {
    const item = await MenuService.getMenuItemById(req.params.id);
    res.json({ success: true, data: item });
  } catch (err) {
    next(err);
  }
});

// Admin only: Create menu item
router.post('/items', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = createMenuItemSchema.parse(req.body);
    const item = await MenuService.createMenuItem(data);
    res.status(201).json({ success: true, data: item });
  } catch (err) {
    next(err);
  }
});

// Admin only: Update menu item
router.put('/items/:id', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const data = updateMenuItemSchema.parse(req.body);
    const item = await MenuService.updateMenuItem(req.params.id, data);
    res.json({ success: true, data: item });
  } catch (err) {
    next(err);
  }
});

// Staff, Kitchen, Admin: Toggle item availability (86-ing an item)
router.patch(
  '/items/:id/availability',
  requireAuth,
  requireRoles(UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF, UserRole.KITCHEN),
  async (req, res, next) => {
    try {
      const { isAvailable } = setItemAvailabilitySchema.parse(req.body);
      const item = await MenuService.setMenuItemAvailability(req.params.id, isAvailable);
      res.json({ success: true, data: item });
    } catch (err) {
      next(err);
    }
  },
);

// Admin only: Delete item
router.delete('/items/:id', requireAuth, requireRoles(UserRole.CAFE_ADMIN), async (req, res, next) => {
  try {
    const item = await MenuService.deleteMenuItem(req.params.id);
    res.json({ success: true, message: 'Item deleted / archived', data: item });
  } catch (err) {
    next(err);
  }
});

export default router;
