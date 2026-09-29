import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';
import { DiscountType } from '@prisma/client';
import type { CreateOfferInput, UpdateOfferInput } from './types.js';

export interface DiscountCalculationResult {
  valid: boolean;
  offer?: any;
  discountPaise: number;
  reason?: string;
}

export class OffersService {
  static async getActiveOffers() {
    const now = new Date();
    return prisma.offer.findMany({
      where: {
        isActive: true,
        validFrom: { lte: now },
        validTo: { gte: now },
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  static async getAllOffers() {
    return prisma.offer.findMany({
      orderBy: { createdAt: 'desc' },
    });
  }

  static async getOfferById(id: string) {
    const offer = await prisma.offer.findUnique({
      where: { id },
    });
    if (!offer) {
      throw AppError.notFound('Offer');
    }
    return offer;
  }

  static async getOfferByCode(code: string) {
    const offer = await prisma.offer.findUnique({
      where: { code: code.toUpperCase().trim() },
    });
    if (!offer) {
      throw AppError.notFound(`Offer code "${code}"`);
    }
    return offer;
  }

  static calculateDiscount(offer: any, subtotalPaise: number): DiscountCalculationResult {
    const now = new Date();

    if (!offer.isActive) {
      return { valid: false, discountPaise: 0, reason: 'Offer is inactive' };
    }

    if (new Date(offer.validFrom) > now) {
      return { valid: false, discountPaise: 0, reason: 'Offer has not started yet' };
    }

    if (new Date(offer.validTo) < now) {
      return { valid: false, discountPaise: 0, reason: 'Offer has expired' };
    }

    if (subtotalPaise < offer.minOrderAmountPaise) {
      return {
        valid: false,
        discountPaise: 0,
        reason: `Minimum order amount of ₹${(offer.minOrderAmountPaise / 100).toFixed(2)} required`,
      };
    }

    let discountPaise = 0;
    if (offer.discountType === DiscountType.PERCENTAGE) {
      discountPaise = Math.floor((subtotalPaise * offer.discountValue) / 100);
      if (offer.maxDiscountAmountPaise && discountPaise > offer.maxDiscountAmountPaise) {
        discountPaise = offer.maxDiscountAmountPaise;
      }
    } else if (offer.discountType === DiscountType.FLAT_AMOUNT) {
      discountPaise = Math.min(offer.discountValue, subtotalPaise);
    }

    return {
      valid: true,
      offer,
      discountPaise,
    };
  }

  static async validateOffer(code: string, subtotalPaise: number): Promise<DiscountCalculationResult> {
    const offer = await prisma.offer.findUnique({
      where: { code: code.toUpperCase().trim() },
    });

    if (!offer) {
      return { valid: false, discountPaise: 0, reason: `Invalid promo code "${code}"` };
    }

    return this.calculateDiscount(offer, subtotalPaise);
  }

  static async createOffer(data: CreateOfferInput) {
    const code = data.code.toUpperCase().trim();
    const existing = await prisma.offer.findUnique({
      where: { code },
    });
    if (existing) {
      throw AppError.conflict('OFFER_EXISTS', `Offer code "${code}" already exists`);
    }

    return prisma.offer.create({
      data: {
        code,
        title: data.title,
        description: data.description,
        discountType: data.discountType,
        discountValue: data.discountValue,
        minOrderAmountPaise: data.minOrderAmountPaise || 0,
        maxDiscountAmountPaise: data.maxDiscountAmountPaise,
        validFrom: new Date(data.validFrom),
        validTo: new Date(data.validTo),
        isActive: data.isActive !== undefined ? data.isActive : true,
      },
    });
  }

  static async updateOffer(id: string, data: UpdateOfferInput) {
    await this.getOfferById(id);

    if (data.code) {
      const code = data.code.toUpperCase().trim();
      const existing = await prisma.offer.findFirst({
        where: { code, NOT: { id } },
      });
      if (existing) {
        throw AppError.conflict('OFFER_EXISTS', `Offer code "${code}" already in use`);
      }
    }

    return prisma.offer.update({
      where: { id },
      data: {
        ...(data.code ? { code: data.code.toUpperCase().trim() } : {}),
        ...(data.title !== undefined ? { title: data.title } : {}),
        ...(data.description !== undefined ? { description: data.description } : {}),
        ...(data.discountType !== undefined ? { discountType: data.discountType } : {}),
        ...(data.discountValue !== undefined ? { discountValue: data.discountValue } : {}),
        ...(data.minOrderAmountPaise !== undefined
          ? { minOrderAmountPaise: data.minOrderAmountPaise }
          : {}),
        ...(data.maxDiscountAmountPaise !== undefined
          ? { maxDiscountAmountPaise: data.maxDiscountAmountPaise }
          : {}),
        ...(data.validFrom ? { validFrom: new Date(data.validFrom) } : {}),
        ...(data.validTo ? { validTo: new Date(data.validTo) } : {}),
        ...(data.isActive !== undefined ? { isActive: data.isActive } : {}),
      },
    });
  }

  static async deleteOffer(id: string) {
    await this.getOfferById(id);
    return prisma.offer.delete({
      where: { id },
    });
  }
}
