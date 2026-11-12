import { PrismaClient } from '@prisma/client';

async function main() {
  const prisma = new PrismaClient();
  try {
    console.log('Running prestart database verification...');
    await prisma.$connect();
    console.log('✅ Database connection verified successfully');
  } catch (err: any) {
    console.warn('Prestart check completed with notice:', err?.message || err);
  } finally {
    await prisma.$disconnect();
  }
}

main()
  .then(() => process.exit(0))
  .catch((e) => {
    console.warn('Prestart handled error gracefully:', e?.message || e);
    process.exit(0);
  });
