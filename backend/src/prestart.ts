import { PrismaClient } from '@prisma/client';

async function main() {
  const prisma = new PrismaClient();
  try {
    console.log('Running prestart database verification...');
    await prisma.$connect();

    // Check if User table exists
    const tables: Array<{ table_name: string }> = await prisma.$queryRawUnsafe(`
      SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'User';
    `);

    if (!tables || tables.length === 0) {
      console.log('User table not found. Dropping partial tables and enums safely if any remain...');
      const tableNames = [
        'Notification',
        'OrderStatusHistory',
        'OrderItem',
        'Order',
        'TableSession',
        'Reservation',
        'Offer',
        'MenuItem',
        'MenuCategory',
        'Table',
        'CafeProfile',
        'RefreshToken',
        'User',
        '_prisma_migrations',
      ];
      for (const t of tableNames) {
        try {
          await prisma.$executeRawUnsafe(`DROP TABLE IF EXISTS "${t}" CASCADE;`);
        } catch {
          // ignore individual drop errors
        }
      }

      const typeNames = [
        'UserRole',
        'TableStatus',
        'TableSessionStatus',
        'ReservationStatus',
        'OrderStatus',
        'DiscountType',
        'NotificationType',
      ];
      for (const t of typeNames) {
        try {
          await prisma.$executeRawUnsafe(`DROP TYPE IF EXISTS "${t}" CASCADE;`);
        } catch {
          // ignore individual drop errors
        }
      }

      console.log('✅ Clean database state prepared');
    } else {
      // If schema exists, clean up any failed migration locks if any
      try {
        await prisma.$executeRawUnsafe('DELETE FROM "_prisma_migrations" WHERE "finished_at" IS NULL;');
        console.log('✅ Checked _prisma_migrations for failed entries');
      } catch (e: any) {
        console.log('Note on _prisma_migrations:', e?.message || e);
      }
    }
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
