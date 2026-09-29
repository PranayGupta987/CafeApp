import { Router } from 'express';
import { NotificationsService } from './service.js';
import { requireAuth } from '../../middleware/auth.js';

const router = Router();

// Authenticated: Get current user notifications
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const notifications = await NotificationsService.getUserNotifications(req.user!.id);
    res.json({ success: true, data: notifications });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Mark single notification as read
router.patch('/:id/read', requireAuth, async (req, res, next) => {
  try {
    const notification = await NotificationsService.markAsRead(req.params.id, req.user!.id);
    res.json({ success: true, data: notification });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Mark all notifications as read
router.patch('/read-all', requireAuth, async (req, res, next) => {
  try {
    await NotificationsService.markAllAsRead(req.user!.id);
    res.json({ success: true, message: 'All notifications marked as read' });
  } catch (err) {
    next(err);
  }
});

export default router;
