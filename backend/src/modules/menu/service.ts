import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';
import type {
  CreateCategoryInput,
  UpdateCategoryInput,
  CreateMenuItemInput,
  UpdateMenuItemInput,
} from './types.js';

export class MenuService {
  // Categories
  static async getAllCategories(includeInactive = false) {
    return prisma.menuCategory.findMany({
      where: includeInactive ? undefined : { isActive: true },
      include: {
        _count: {
          select: { menuItems: { where: { isAvailable: true } } },
        },
      },
      orderBy: { displayOrder: 'asc' },
    });
  }

  static async getCategoryById(id: string) {
    const category = await prisma.menuCategory.findUnique({
      where: { id },
      include: {
        menuItems: {
          orderBy: { displayOrder: 'asc' },
        },
      },
    });

    if (!category) {
      throw AppError.notFound('Menu Category');
    }
    return category;
  }

  static async createCategory(data: CreateCategoryInput) {
    const existing = await prisma.menuCategory.findUnique({
      where: { name: data.name },
    });
    if (existing) {
      throw AppError.conflict('CATEGORY_EXISTS', `Category "${data.name}" already exists`);
    }

    return prisma.menuCategory.create({
      data: {
        name: data.name,
        description: data.description,
        displayOrder: data.displayOrder || 0,
        isActive: data.isActive !== undefined ? data.isActive : true,
      },
    });
  }

  static async updateCategory(id: string, data: UpdateCategoryInput) {
    await this.getCategoryById(id);

    if (data.name) {
      const existing = await prisma.menuCategory.findFirst({
        where: { name: data.name, NOT: { id } },
      });
      if (existing) {
        throw AppError.conflict('CATEGORY_EXISTS', `Category "${data.name}" already exists`);
      }
    }

    return prisma.menuCategory.update({
      where: { id },
      data: {
        ...(data.name !== undefined ? { name: data.name } : {}),
        ...(data.description !== undefined ? { description: data.description } : {}),
        ...(data.displayOrder !== undefined ? { displayOrder: data.displayOrder } : {}),
        ...(data.isActive !== undefined ? { isActive: data.isActive } : {}),
      },
    });
  }

  static async deleteCategory(id: string) {
    await this.getCategoryById(id);

    const itemCount = await prisma.menuItem.count({
      where: { categoryId: id },
    });
    if (itemCount > 0) {
      // Soft delete category if it has items
      return prisma.menuCategory.update({
        where: { id },
        data: { isActive: false },
      });
    }

    return prisma.menuCategory.delete({
      where: { id },
    });
  }

  // Menu Items
  static async getMenuItems(filters?: {
    categoryId?: string;
    isVeg?: boolean;
    isAvailable?: boolean;
    search?: string;
  }) {
    const where: any = {};

    if (filters?.categoryId) {
      where.categoryId = filters.categoryId;
    }
    if (filters?.isVeg !== undefined) {
      where.isVeg = filters.isVeg;
    }
    if (filters?.isAvailable !== undefined) {
      where.isAvailable = filters.isAvailable;
    }
    if (filters?.search) {
      where.OR = [
        { name: { contains: filters.search, mode: 'insensitive' } },
        { description: { contains: filters.search, mode: 'insensitive' } },
      ];
    }

    return prisma.menuItem.findMany({
      where,
      include: {
        category: {
          select: { id: true, name: true },
        },
      },
      orderBy: [{ category: { displayOrder: 'asc' } }, { displayOrder: 'asc' }],
    });
  }

  static async getMenuItemById(id: string) {
    const item = await prisma.menuItem.findUnique({
      where: { id },
      include: {
        category: true,
      },
    });

    if (!item) {
      throw AppError.notFound('Menu Item');
    }
    return item;
  }

  static async createMenuItem(data: CreateMenuItemInput) {
    // Validate category exists
    await this.getCategoryById(data.categoryId);

    return prisma.menuItem.create({
      data: {
        categoryId: data.categoryId,
        name: data.name,
        description: data.description,
        pricePaise: data.pricePaise,
        preparationTimeMinutes: data.preparationTimeMinutes || 15,
        isAvailable: data.isAvailable !== undefined ? data.isAvailable : true,
        isVeg: data.isVeg !== undefined ? data.isVeg : true,
        isSpicy: data.isSpicy !== undefined ? data.isSpicy : false,
        imageUrl: data.imageUrl,
        displayOrder: data.displayOrder || 0,
      },
      include: {
        category: true,
      },
    });
  }

  static async updateMenuItem(id: string, data: UpdateMenuItemInput) {
    await this.getMenuItemById(id);

    if (data.categoryId) {
      await this.getCategoryById(data.categoryId);
    }

    return prisma.menuItem.update({
      where: { id },
      data: {
        ...(data.categoryId !== undefined ? { categoryId: data.categoryId } : {}),
        ...(data.name !== undefined ? { name: data.name } : {}),
        ...(data.description !== undefined ? { description: data.description } : {}),
        ...(data.pricePaise !== undefined ? { pricePaise: data.pricePaise } : {}),
        ...(data.preparationTimeMinutes !== undefined
          ? { preparationTimeMinutes: data.preparationTimeMinutes }
          : {}),
        ...(data.isAvailable !== undefined ? { isAvailable: data.isAvailable } : {}),
        ...(data.isVeg !== undefined ? { isVeg: data.isVeg } : {}),
        ...(data.isSpicy !== undefined ? { isSpicy: data.isSpicy } : {}),
        ...(data.imageUrl !== undefined ? { imageUrl: data.imageUrl } : {}),
        ...(data.displayOrder !== undefined ? { displayOrder: data.displayOrder } : {}),
      },
      include: {
        category: true,
      },
    });
  }

  static async setMenuItemAvailability(id: string, isAvailable: boolean) {
    await this.getMenuItemById(id);
    return prisma.menuItem.update({
      where: { id },
      data: { isAvailable },
      include: { category: true },
    });
  }

  static async deleteMenuItem(id: string) {
    await this.getMenuItemById(id);

    // Check if used in historical orders
    const orderItemCount = await prisma.orderItem.count({
      where: { menuItemId: id },
    });

    if (orderItemCount > 0) {
      // Cannot delete completely if order history references it; soft-delete by marking unavailable
      return prisma.menuItem.update({
        where: { id },
        data: { isAvailable: false },
      });
    }

    return prisma.menuItem.delete({
      where: { id },
    });
  }
}
