import { prisma } from '../../lib/prisma.js';
import type { UpdateCafeProfileInput } from './types.js';

export class CafeService {
  static async getProfile() {
    let profile = await prisma.cafeProfile.findFirst();

    if (!profile) {
      // Fallback auto-creation if not found
      profile = await prisma.cafeProfile.create({
        data: {
          name: 'Cafe Rustica',
          description: 'Artisanal Coffee & Gourmet Bistro',
          address: '100 Feet Road, Indiranagar, Bangalore, Karnataka 560038',
          phone: '+919876543210',
          email: 'contact@caferustica.local',
          gstPercent: 5.0,
          currency: 'INR',
          timezone: 'Asia/Kolkata',
          openingTime: '08:00',
          closingTime: '23:00',
          isAcceptingOrders: true,
        },
      });
    }

    return profile;
  }

  static async updateProfile(data: UpdateCafeProfileInput) {
    const profile = await this.getProfile(); // Ensure exists

    const updated = await prisma.cafeProfile.update({
      where: { id: profile.id },
      data: {
        ...(data.name !== undefined ? { name: data.name } : {}),
        ...(data.description !== undefined ? { description: data.description } : {}),
        ...(data.address !== undefined ? { address: data.address } : {}),
        ...(data.phone !== undefined ? { phone: data.phone } : {}),
        ...(data.email !== undefined ? { email: data.email } : {}),
        ...(data.gstin !== undefined ? { gstin: data.gstin } : {}),
        ...(data.gstPercent !== undefined ? { gstPercent: data.gstPercent } : {}),
        ...(data.currency !== undefined ? { currency: data.currency } : {}),
        ...(data.timezone !== undefined ? { timezone: data.timezone } : {}),
        ...(data.logoUrl !== undefined ? { logoUrl: data.logoUrl } : {}),
        ...(data.openingTime !== undefined ? { openingTime: data.openingTime } : {}),
        ...(data.closingTime !== undefined ? { closingTime: data.closingTime } : {}),
        ...(data.isAcceptingOrders !== undefined ? { isAcceptingOrders: data.isAcceptingOrders } : {}),
      },
    });

    return updated;
  }
}
