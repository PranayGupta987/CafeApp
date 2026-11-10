import { PrismaClient } from '@prisma/client';
import { seedDatabase } from '../src/lib/seed.js';

const prisma = new PrismaClient();

seedDatabase(prisma)
  .then(() => {
    console.log('✅ Seed finished');
  })
  .catch((e) => {
    console.error('❌ Seed error:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
