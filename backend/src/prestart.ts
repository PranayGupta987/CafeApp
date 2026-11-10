import { PrismaClient } from '@prisma/client';

async function main() {
  const prisma = new PrismaClient();
  try {
    console.log('Running prestart database verification...');
    await prisma.$connect();

    // Check if initial User table exists
    const tables: Array<{ table_name: string }> = await prisma.$queryRawUnsafe(`
      SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'User';
    `);

    if (!tables || tables.length === 0) {
      console.log('User table not found. Resetting public schema to ensure clean initial migration...');
      await prisma.$executeRawUnsafe('DROP SCHEMA public CASCADE;');
      await prisma.$executeRawUnsafe('CREATE SCHEMA public;');
      await prisma.$executeRawUnsafe('GRANT ALL ON SCHEMA public TO cafe;');
      await prisma.$executeRawUnsafe('GRANT ALL ON SCHEMA public TO public;');
      console.log('✅ Clean public schema prepared');
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
