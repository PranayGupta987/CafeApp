import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';
import { OrderStatus, TableSessionStatus, UserRole, NotificationType } from '@prisma/client';
import { CafeService } from '../cafe/service.js';
import { OffersService } from '../offers/service.js';
import { getSocketServer } from '../../websocket/index.js';
import { logger } from '../../lib/logger.js';
import type { AuthUser } from '../../middleware/auth.js';
import type { PlaceOrderInput, UpdateOrderStatusInput } from './types.js';

export class OrdersService {
  private static async getNextOrderNumber(): Promise<number> {
    const lastOrder = await prisma.order.findFirst({
      orderBy: { orderNumber: 'desc' },
      select: { orderNumber: true },
    });
    return lastOrder ? lastOrder.orderNumber + 1 : 1001;
  }

  static async getOrders(
    user: AuthUser,
    filters?: {
      status?: OrderStatus;
      tableId?: string;
      sessionId?: string;
      limit?: number;
    },
  ) {
    const isStaffOrKitchenOrAdmin = (
      [UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF, UserRole.KITCHEN] as UserRole[]
    ).includes(user.role);

    const where: any = {};

    if (!isStaffOrKitchenOrAdmin) {
      where.userId = user.id;
    }

    if (filters?.status) {
      where.status = filters.status;
    }
    if (filters?.tableId) {
      where.tableId = filters.tableId;
    }
    if (filters?.sessionId) {
      where.sessionId = filters.sessionId;
    }

    return prisma.order.findMany({
      where,
      include: {
        table: true,
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
        items: {
          include: {
            menuItem: {
              select: { id: true, name: true, isVeg: true, isSpicy: true, imageUrl: true },
            },
          },
        },
        statusHistory: {
          include: {
            changedByUser: {
              select: { id: true, name: true, role: true },
            },
          },
          orderBy: { createdAt: 'asc' },
        },
      },
      orderBy: { createdAt: 'desc' },
      take: filters?.limit || 100,
    });
  }

  static async getOrderById(id: string, user: AuthUser) {
    const order = await prisma.order.findUnique({
      where: { id },
      include: {
        table: true,
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
        items: {
          include: {
            menuItem: true,
          },
        },
        statusHistory: {
          include: {
            changedByUser: {
              select: { id: true, name: true, role: true },
            },
          },
          orderBy: { createdAt: 'asc' },
        },
      },
    });

    if (!order) {
      throw AppError.notFound('Order');
    }

    const isStaffOrKitchenOrAdmin = (
      [UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF, UserRole.KITCHEN] as UserRole[]
    ).includes(user.role);

    if (!isStaffOrKitchenOrAdmin && order.userId !== user.id) {
      throw AppError.forbidden('You can only access your own orders');
    }

    return order;
  }

  static async placeOrder(data: PlaceOrderInput, user: AuthUser) {
    // 1. Resolve active session and table
    let session;
    if (data.sessionId) {
      session = await prisma.tableSession.findUnique({
        where: { id: data.sessionId },
        include: { table: true },
      });
    } else if (data.qrToken) {
      const table = await prisma.table.findUnique({ where: { qrToken: data.qrToken } });
      if (table) {
        session = await prisma.tableSession.findFirst({
          where: { tableId: table.id, status: TableSessionStatus.ACTIVE },
          include: { table: true },
        });
      }
    } else if (data.tableId) {
      session = await prisma.tableSession.findFirst({
        where: { tableId: data.tableId, status: TableSessionStatus.ACTIVE },
        include: { table: true },
      });
    } else {
      // Look up current user's active session
      session = await prisma.tableSession.findFirst({
        where: { hostUserId: user.id, status: TableSessionStatus.ACTIVE },
        include: { table: true },
      });
    }

    if (!session || session.status !== TableSessionStatus.ACTIVE) {
      throw AppError.badRequest(
        'NO_ACTIVE_SESSION',
        'An active table session is required to place a dine-in order. Please check in at a table first.',
      );
    }

    // 2. Fetch Cafe Profile for ordering status and GST rate
    const cafe = await CafeService.getProfile();
    if (!cafe.isAcceptingOrders) {
      throw AppError.badRequest('CAFE_CLOSED', 'Cafe is currently not accepting new orders');
    }

    // 3. Fetch menu items and compute authoritative prices
    const itemIds = data.items.map((i) => i.menuItemId);
    const dbMenuItems = await prisma.menuItem.findMany({
      where: { id: { in: itemIds } },
    });

    if (dbMenuItems.length !== itemIds.length) {
      throw AppError.badRequest('INVALID_ITEMS', 'One or more menu items do not exist');
    }

    const itemMap = new Map(dbMenuItems.map((item) => [item.id, item]));

    let subtotalPaise = 0;
    const resolvedItems: Array<{
      menuItemId: string;
      quantity: number;
      unitPricePaise: number;
      subtotalPaise: number;
      notes?: string | null;
    }> = [];

    for (const itemInput of data.items) {
      const dbItem = itemMap.get(itemInput.menuItemId)!;
      if (!dbItem.isAvailable) {
        throw AppError.badRequest(
          'ITEM_UNAVAILABLE',
          `"${dbItem.name}" is currently marked unavailable (86-ed)`,
        );
      }

      const itemSubtotal = dbItem.pricePaise * itemInput.quantity;
      subtotalPaise += itemSubtotal;

      resolvedItems.push({
        menuItemId: dbItem.id,
        quantity: itemInput.quantity,
        unitPricePaise: dbItem.pricePaise, // Immutable snapshot
        subtotalPaise: itemSubtotal,
        notes: itemInput.notes,
      });
    }

    // 4. Calculate discount if offer code provided
    let discountPaise = 0;
    if (data.offerCode) {
      const discountResult = await OffersService.validateOffer(data.offerCode, subtotalPaise);
      if (discountResult.valid) {
        discountPaise = discountResult.discountPaise;
      } else {
        throw AppError.badRequest('INVALID_PROMO', discountResult.reason || 'Invalid promo code');
      }
    }

    // 5. Calculate Tax and Total (Integer Paise)
    const discountedSubtotal = Math.max(0, subtotalPaise - discountPaise);
    const gstRate = Number(cafe.gstPercent) || 5.0;
    const taxPaise = Math.round((discountedSubtotal * gstRate) / 100);
    const totalPaise = discountedSubtotal + taxPaise;

    // 6. Execute atomic creation
    const orderNumber = await this.getNextOrderNumber();

    const createdOrder = await prisma.$transaction(async (tx) => {
      const order = await tx.order.create({
        data: {
          orderNumber,
          sessionId: session.id,
          userId: user.id,
          tableId: session.tableId,
          status: OrderStatus.PLACED,
          subtotalPaise,
          discountPaise,
          taxPaise,
          totalPaise,
          specialInstructions: data.specialInstructions,
          items: {
            create: resolvedItems.map((item) => ({
              menuItemId: item.menuItemId,
              quantity: item.quantity,
              unitPricePaise: item.unitPricePaise,
              subtotalPaise: item.subtotalPaise,
              notes: item.notes,
            })),
          },
          statusHistory: {
            create: {
              status: OrderStatus.PLACED,
              changedByUserId: user.id,
              notes: 'Order placed by customer',
            },
          },
        },
        include: {
          table: true,
          user: {
            select: { id: true, name: true, email: true },
          },
          items: {
            include: { menuItem: true },
          },
          statusHistory: true,
        },
      });

      // Update session total amount
      await tx.tableSession.update({
        where: { id: session.id },
        data: {
          totalAmountPaise: { increment: totalPaise },
        },
      });

      // Create notification for customer
      await tx.notification.create({
        data: {
          userId: user.id,
          type: NotificationType.ORDER_STATUS,
          title: `Order #${orderNumber} Placed`,
          message: `Your order of ₹${(totalPaise / 100).toFixed(2)} at Table ${session.table.tableNumber} has been sent to the kitchen.`,
          data: { orderId: order.id, orderNumber, status: OrderStatus.PLACED },
        },
      });

      return order;
    });

    // 7. Emit WebSocket events
    try {
      const io = getSocketServer();
      io.to('kitchen').emit('order:new', {
        orderId: createdOrder.id,
        orderNumber: createdOrder.orderNumber,
        tableNumber: session.table.tableNumber,
        itemsCount: resolvedItems.length,
        status: OrderStatus.PLACED,
        createdAt: createdOrder.createdAt,
      });

      io.to(`user:${user.id}`).emit('order:status_update', {
        orderId: createdOrder.id,
        orderNumber: createdOrder.orderNumber,
        status: OrderStatus.PLACED,
      });
    } catch (wsErr) {
      logger.warn({ wsErr }, 'Failed to emit WebSocket event for placed order');
    }

    return createdOrder;
  }

  static async updateOrderStatus(
    orderId: string,
    data: UpdateOrderStatusInput,
    user: AuthUser,
  ) {
    const order = await this.getOrderById(orderId, user);

    const isStaffOrKitchenOrAdmin = (
      [UserRole.CAFE_ADMIN, UserRole.CAFE_STAFF, UserRole.KITCHEN] as UserRole[]
    ).includes(user.role);

    if (!isStaffOrKitchenOrAdmin) {
      // Customers can only request cancellation if order is still PLACED
      if (data.status !== OrderStatus.CANCELLED || order.status !== OrderStatus.PLACED) {
        throw AppError.forbidden('Only staff and kitchen can update order workflow status');
      }
    }

    const updatedOrder = await prisma.$transaction(async (tx) => {
      const res = await tx.order.update({
        where: { id: orderId },
        data: {
          status: data.status,
          statusHistory: {
            create: {
              status: data.status,
              changedByUserId: user.id,
              notes: data.notes || `Status updated to ${data.status} by ${user.name || user.email}`,
            },
          },
        },
        include: {
          table: true,
          user: {
            select: { id: true, name: true, email: true },
          },
          items: {
            include: { menuItem: true },
          },
          statusHistory: {
            include: {
              changedByUser: {
                select: { id: true, name: true, role: true },
              },
            },
            orderBy: { createdAt: 'asc' },
          },
        },
      });

      // Notify customer
      await tx.notification.create({
        data: {
          userId: order.userId,
          type: NotificationType.ORDER_STATUS,
          title: `Order #${order.orderNumber} ${data.status}`,
          message: `Your order at Table ${order.table.tableNumber} is now ${data.status.toLowerCase()}.`,
          data: { orderId: order.id, orderNumber: order.orderNumber, status: data.status },
        },
      });

      return res;
    });

    // Broadcast WebSocket updates
    try {
      const io = getSocketServer();
      io.to('kitchen').emit('order:status_update', {
        orderId: updatedOrder.id,
        orderNumber: updatedOrder.orderNumber,
        status: data.status,
      });

      io.to(`user:${order.userId}`).emit('order:status_update', {
        orderId: updatedOrder.id,
        orderNumber: updatedOrder.orderNumber,
        status: data.status,
      });
    } catch (wsErr) {
      logger.warn({ wsErr }, 'Failed to emit WebSocket update for order status change');
    }

    return updatedOrder;
  }
}
