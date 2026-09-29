import { Router } from 'express';
import { OrdersService } from './service.js';
import { placeOrderSchema, updateOrderStatusSchema } from './types.js';
import { requireAuth } from '../../middleware/auth.js';
import { OrderStatus } from '@prisma/client';

const router = Router();

// Authenticated: List orders (Customer gets their own; Staff/Kitchen/Admin get full queue)
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const { status, tableId, sessionId, limit } = req.query;
    const orders = await OrdersService.getOrders(req.user!, {
      status: status as OrderStatus | undefined,
      tableId: typeof tableId === 'string' ? tableId : undefined,
      sessionId: typeof sessionId === 'string' ? sessionId : undefined,
      limit: limit ? parseInt(limit as string, 10) : undefined,
    });
    res.json({ success: true, data: orders });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Get order by ID
router.get('/:id', requireAuth, async (req, res, next) => {
  try {
    const order = await OrdersService.getOrderById(req.params.id, req.user!);
    res.json({ success: true, data: order });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Place a new dine-in order
router.post('/', requireAuth, async (req, res, next) => {
  try {
    const data = placeOrderSchema.parse(req.body);
    const order = await OrdersService.placeOrder(data, req.user!);
    res.status(201).json({ success: true, data: order });
  } catch (err) {
    next(err);
  }
});

// Authenticated: Update order status (Staff / Kitchen / Admin, or cancel if placed)
const updateStatusHandler = async (req: any, res: any, next: any) => {
  try {
    const data = updateOrderStatusSchema.parse(req.body);
    const order = await OrdersService.updateOrderStatus(req.params.id, data, req.user!);
    res.json({ success: true, data: order });
  } catch (err) {
    next(err);
  }
};

router.put('/:id/status', requireAuth, updateStatusHandler);
router.patch('/:id/status', requireAuth, updateStatusHandler);

export default router;
