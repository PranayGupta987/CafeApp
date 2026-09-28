import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  console.log('🌱 Seeding database...');

  // CafeProfile singleton
  await prisma.cafeProfile.upsert({
    where: { id: '00000000-0000-4000-8000-000000000001' },
    update: {},
    create: {
      name: 'The Daily Cup',
      address: '42 Brew Street, Bangalore 560001',
      phone: '+91-80-1234-5678',
      email: 'hello@thedailycup.in',
      gstin: '29AABCT1234A1ZH',
      gstPercent: 5.0,
      openingTime: '08:00',
      closingTime: '22:00',
    },
  });

  console.log('✅ Seed complete');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
