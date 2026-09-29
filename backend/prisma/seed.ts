import { PrismaClient, UserRole, DiscountType, TableStatus } from '@prisma/client';
import argon2 from 'argon2';
import bcrypt from 'bcrypt';

const prisma = new PrismaClient();

async function hashPassword(pw: string): Promise<string> {
  try {
    return await argon2.hash(pw, {
      type: argon2.argon2id,
      timeCost: 3,
      memoryCost: 65536,
      parallelism: 4,
    });
  } catch {
    return bcrypt.hashSync(pw, 10);
  }
}

async function main() {
  console.log('🌱 Starting database seed...');

  // 1. Cafe Profile (Singleton ID)
  const cafeId = '00000000-0000-4000-8000-000000000001';
  const cafe = await prisma.cafeProfile.upsert({
    where: { id: cafeId },
    update: {
      name: 'Cafe Rustica',
      description: 'Artisanal Coffee & Gourmet Bistro',
      address: '100 Feet Road, Indiranagar, Bangalore, Karnataka 560038',
      phone: '+919876543210',
      email: 'contact@caferustica.local',
      gstin: '29ABCDE1234F1Z5',
      gstPercent: 5.0,
      currency: 'INR',
      timezone: 'Asia/Kolkata',
      openingTime: '08:00',
      closingTime: '23:00',
      isAcceptingOrders: true,
    },
    create: {
      id: cafeId,
      name: 'Cafe Rustica',
      description: 'Artisanal Coffee & Gourmet Bistro',
      address: '100 Feet Road, Indiranagar, Bangalore, Karnataka 560038',
      phone: '+919876543210',
      email: 'contact@caferustica.local',
      gstin: '29ABCDE1234F1Z5',
      gstPercent: 5.0,
      currency: 'INR',
      timezone: 'Asia/Kolkata',
      openingTime: '08:00',
      closingTime: '23:00',
      isAcceptingOrders: true,
    },
  });
  console.log(`✅ Cafe profile configured: ${cafe.name} (${cafe.id})`);

  // 2. Development Users across all roles
  const usersToSeed = [
    {
      email: 'admin@cafeapp.local',
      name: 'Cafe Manager (Admin)',
      phone: '+919000000001',
      role: UserRole.CAFE_ADMIN,
      password: 'Admin@1234',
    },
    {
      email: 'staff@cafeapp.local',
      name: 'Floor Staff',
      phone: '+919000000002',
      role: UserRole.CAFE_STAFF,
      password: 'Staff@1234',
    },
    {
      email: 'kitchen@cafeapp.local',
      name: 'Head Chef',
      phone: '+919000000003',
      role: UserRole.KITCHEN,
      password: 'Kitchen@1234',
    },
    {
      email: 'customer@cafeapp.local',
      name: 'Rahul Sharma',
      phone: '+919000000004',
      role: UserRole.CUSTOMER,
      password: 'Customer@1234',
    },
    {
      email: 'host@cafeapp.local',
      name: 'Ananya Roy (Host)',
      phone: '+919000000005',
      role: UserRole.CUSTOMER,
      password: 'Host@1234',
    },
  ];

  for (const u of usersToSeed) {
    const passwordHash = await hashPassword(u.password);
    const user = await prisma.user.upsert({
      where: { email: u.email },
      update: {
        name: u.name,
        phone: u.phone,
        role: u.role,
        passwordHash,
        isActive: true,
      },
      create: {
        email: u.email,
        name: u.name,
        phone: u.phone,
        role: u.role,
        passwordHash,
        isActive: true,
      },
    });
    console.log(`👤 User ready: ${user.name} <${user.email}> [${user.role}]`);
  }

  // 3. Tables (1 to 10)
  const tablesData = [
    { tableNumber: 1, capacity: 2, qrToken: 'table_qr_01' },
    { tableNumber: 2, capacity: 2, qrToken: 'table_qr_02' },
    { tableNumber: 3, capacity: 4, qrToken: 'table_qr_03' },
    { tableNumber: 4, capacity: 4, qrToken: 'table_qr_04' },
    { tableNumber: 5, capacity: 4, qrToken: 'table_qr_05' },
    { tableNumber: 6, capacity: 6, qrToken: 'table_qr_06' },
    { tableNumber: 7, capacity: 6, qrToken: 'table_qr_07' },
    { tableNumber: 8, capacity: 8, qrToken: 'table_qr_08' },
    { tableNumber: 9, capacity: 2, qrToken: 'table_qr_09' },
    { tableNumber: 10, capacity: 4, qrToken: 'table_qr_10' },
  ];

  for (const t of tablesData) {
    await prisma.table.upsert({
      where: { tableNumber: t.tableNumber },
      update: {
        capacity: t.capacity,
        qrToken: t.qrToken,
        status: TableStatus.AVAILABLE,
        isActive: true,
      },
      create: {
        tableNumber: t.tableNumber,
        capacity: t.capacity,
        qrToken: t.qrToken,
        status: TableStatus.AVAILABLE,
        isActive: true,
      },
    });
  }
  console.log(`🪑 Seeded ${tablesData.length} tables with unique QR tokens`);

  // 4. Menu Categories & Items
  const menuStructure = [
    {
      category: 'Hot Brews',
      description: 'Single-origin espresso, pour-overs and artisanal milk coffees',
      displayOrder: 1,
      items: [
        {
          name: 'Classic Espresso',
          description: 'Intense double shot of house blend Arabica with rich crema',
          pricePaise: 14000, // ₹140.00
          preparationTimeMinutes: 5,
          isVeg: true,
          isSpicy: false,
          displayOrder: 1,
        },
        {
          name: 'Cafe Cappuccino',
          description: 'Velvety steamed micro-foam over a rich espresso base',
          pricePaise: 19000, // ₹190.00
          preparationTimeMinutes: 7,
          isVeg: true,
          isSpicy: false,
          displayOrder: 2,
        },
        {
          name: 'Cortado',
          description: 'Equal parts espresso and warm silky milk',
          pricePaise: 17000, // ₹170.00
          preparationTimeMinutes: 6,
          isVeg: true,
          isSpicy: false,
          displayOrder: 3,
        },
        {
          name: 'Flat White',
          description: 'Smooth ristretto with finely textured microfoam',
          pricePaise: 21000, // ₹210.00
          preparationTimeMinutes: 7,
          isVeg: true,
          isSpicy: false,
          displayOrder: 4,
        },
      ],
    },
    {
      category: 'Cold Beverages',
      description: 'Slow steeped cold brews, frappes, and refreshing coolers',
      displayOrder: 2,
      items: [
        {
          name: '18-Hour Cold Brew',
          description: 'Steeped for 18 hours, naturally sweet with chocolate notes',
          pricePaise: 22000, // ₹220.00
          preparationTimeMinutes: 5,
          isVeg: true,
          isSpicy: false,
          displayOrder: 1,
        },
        {
          name: 'Vietnamese Iced Coffee',
          description: 'Dark roast dripped over sweetened condensed milk with ice',
          pricePaise: 24000, // ₹240.00
          preparationTimeMinutes: 6,
          isVeg: true,
          isSpicy: false,
          displayOrder: 2,
        },
        {
          name: 'Mango Passion Cooler',
          description: 'Sparkling cooler with Alphonso mango pulp and passion fruit',
          pricePaise: 21000, // ₹210.00
          preparationTimeMinutes: 5,
          isVeg: true,
          isSpicy: false,
          displayOrder: 3,
        },
      ],
    },
    {
      category: 'All-Day Breakfast',
      description: 'Artisanal toasts, eggs made to order, and wholesome bowls',
      displayOrder: 3,
      items: [
        {
          name: 'Sourdough Avocado Toast',
          description: 'Hass avocado, cherry tomatoes, pumpkin seeds on toasted sourdough',
          pricePaise: 32000, // ₹320.00
          preparationTimeMinutes: 12,
          isVeg: true,
          isSpicy: false,
          displayOrder: 1,
        },
        {
          name: 'Truffle Scrambled Eggs',
          description: 'Creamy scrambled eggs infused with white truffle oil on brioche',
          pricePaise: 36000, // ₹360.00
          preparationTimeMinutes: 15,
          isVeg: false,
          isSpicy: false,
          displayOrder: 2,
        },
        {
          name: 'Mediterranean Shakshuka',
          description: 'Poached eggs in spiced tomato bell pepper sauce with Greek feta',
          pricePaise: 34000, // ₹340.00
          preparationTimeMinutes: 18,
          isVeg: false,
          isSpicy: true,
          displayOrder: 3,
        },
      ],
    },
    {
      category: 'Artisanal Pizzas & Pastas',
      description: 'Hand-stretched sourdough pizzas and fresh hand-rolled pastas',
      displayOrder: 4,
      items: [
        {
          name: 'Margherita Burrata Pizza',
          description: 'San Marzano tomatoes, artisanal burrata, fresh basil, extra virgin olive oil',
          pricePaise: 48000, // ₹480.00
          preparationTimeMinutes: 20,
          isVeg: true,
          isSpicy: false,
          displayOrder: 1,
        },
        {
          name: 'Pepperoni & Hot Honey Pizza',
          description: 'Smoked chicken pepperoni with organic hot chili honey drizzle',
          pricePaise: 54000, // ₹540.00
          preparationTimeMinutes: 20,
          isVeg: false,
          isSpicy: true,
          displayOrder: 2,
        },
        {
          name: 'Wild Mushroom Truffle Penne',
          description: 'Shiitake and button mushrooms in creamy truffle parmesan sauce',
          pricePaise: 46000, // ₹460.00
          preparationTimeMinutes: 18,
          isVeg: true,
          isSpicy: false,
          displayOrder: 3,
        },
        {
          name: 'Spicy Arrabiata Spaghetti',
          description: 'San Marzano tomatoes, fresh red chilies, garlic, and fresh basil',
          pricePaise: 42000, // ₹420.00
          preparationTimeMinutes: 15,
          isVeg: true,
          isSpicy: true,
          displayOrder: 4,
        },
      ],
    },
    {
      category: 'Small Plates & Sides',
      description: 'Gourmet starters, loaded fries and crispy bites',
      displayOrder: 5,
      items: [
        {
          name: 'Truffle Parmesan Fries',
          description: 'Hand-cut fries tossed in white truffle oil and aged parmesan',
          pricePaise: 26000, // ₹260.00
          preparationTimeMinutes: 10,
          isVeg: true,
          isSpicy: false,
          displayOrder: 1,
        },
        {
          name: 'Peri Peri Chicken Skewers',
          description: 'Grilled tender chicken skewers with spicy peri peri dip',
          pricePaise: 38000, // ₹380.00
          preparationTimeMinutes: 15,
          isVeg: false,
          isSpicy: true,
          displayOrder: 2,
        },
        {
          name: 'Garlic Butter Crostini',
          description: 'Toasted baguette slices with herb garlic butter and parmesan',
          pricePaise: 22000, // ₹220.00
          preparationTimeMinutes: 8,
          isVeg: true,
          isSpicy: false,
          displayOrder: 3,
        },
      ],
    },
    {
      category: 'Bakery & Desserts',
      description: 'Decadent European desserts and freshly baked treats',
      displayOrder: 6,
      items: [
        {
          name: 'Classic Tiramisu',
          description: 'Savoiardi ladyfingers soaked in espresso and mascarpone cream',
          pricePaise: 29000, // ₹290.00
          preparationTimeMinutes: 5,
          isVeg: true,
          isSpicy: false,
          displayOrder: 1,
        },
        {
          name: 'Belgian Chocolate Fondant',
          description: 'Warm molten dark chocolate cake served with vanilla gelato',
          pricePaise: 32000, // ₹320.00
          preparationTimeMinutes: 12,
          isVeg: true,
          isSpicy: false,
          displayOrder: 2,
        },
        {
          name: 'Blueberry Almond Tart',
          description: 'Crisp pastry shell filled with frangipane and fresh blueberries',
          pricePaise: 26000, // ₹260.00
          preparationTimeMinutes: 5,
          isVeg: true,
          isSpicy: false,
          displayOrder: 3,
        },
      ],
    },
  ];

  for (const catData of menuStructure) {
    const category = await prisma.menuCategory.upsert({
      where: { name: catData.category },
      update: {
        description: catData.description,
        displayOrder: catData.displayOrder,
        isActive: true,
      },
      create: {
        name: catData.category,
        description: catData.description,
        displayOrder: catData.displayOrder,
        isActive: true,
      },
    });

    for (const item of catData.items) {
      const existingItem = await prisma.menuItem.findFirst({
        where: { name: item.name, categoryId: category.id },
      });

      if (existingItem) {
        await prisma.menuItem.update({
          where: { id: existingItem.id },
          data: {
            description: item.description,
            pricePaise: item.pricePaise,
            preparationTimeMinutes: item.preparationTimeMinutes,
            isVeg: item.isVeg,
            isSpicy: item.isSpicy,
            displayOrder: item.displayOrder,
            isAvailable: true,
          },
        });
      } else {
        await prisma.menuItem.create({
          data: {
            categoryId: category.id,
            name: item.name,
            description: item.description,
            pricePaise: item.pricePaise,
            preparationTimeMinutes: item.preparationTimeMinutes,
            isVeg: item.isVeg,
            isSpicy: item.isSpicy,
            displayOrder: item.displayOrder,
            isAvailable: true,
          },
        });
      }
    }
  }
  console.log(`📋 Seeded ${menuStructure.length} categories with all menu items`);

  // 5. Promotional Offers
  const now = new Date();
  const oneYearFromNow = new Date(now.getTime() + 365 * 24 * 60 * 60 * 1000);

  const offersData = [
    {
      code: 'WELCOME50',
      title: '50% Welcome Discount',
      description: 'Get 50% discount up to ₹100 on your order of ₹200 or more',
      discountType: DiscountType.PERCENTAGE,
      discountValue: 50,
      minOrderAmountPaise: 20000,
      maxDiscountAmountPaise: 10000,
      validFrom: now,
      validTo: oneYearFromNow,
    },
    {
      code: 'FLAT100',
      title: 'Flat ₹100 Off',
      description: 'Get flat ₹100 off on minimum order value of ₹500',
      discountType: DiscountType.FLAT_AMOUNT,
      discountValue: 10000,
      minOrderAmountPaise: 50000,
      maxDiscountAmountPaise: null,
      validFrom: now,
      validTo: oneYearFromNow,
    },
    {
      code: 'CHEF20',
      title: "Chef's Special 20%",
      description: '20% off on all bistro mains and pizzas on orders above ₹400',
      discountType: DiscountType.PERCENTAGE,
      discountValue: 20,
      minOrderAmountPaise: 40000,
      maxDiscountAmountPaise: 15000,
      validFrom: now,
      validTo: oneYearFromNow,
    },
    {
      code: 'FESTIVE15',
      title: 'Festive Delight 15%',
      description: '15% discount on total food bill with no cap',
      discountType: DiscountType.PERCENTAGE,
      discountValue: 15,
      minOrderAmountPaise: 30000,
      maxDiscountAmountPaise: null,
      validFrom: now,
      validTo: oneYearFromNow,
    },
  ];

  for (const off of offersData) {
    await prisma.offer.upsert({
      where: { code: off.code },
      update: {
        title: off.title,
        description: off.description,
        discountType: off.discountType,
        discountValue: off.discountValue,
        minOrderAmountPaise: off.minOrderAmountPaise,
        maxDiscountAmountPaise: off.maxDiscountAmountPaise,
        validFrom: off.validFrom,
        validTo: off.validTo,
        isActive: true,
      },
      create: {
        code: off.code,
        title: off.title,
        description: off.description,
        discountType: off.discountType,
        discountValue: off.discountValue,
        minOrderAmountPaise: off.minOrderAmountPaise,
        maxDiscountAmountPaise: off.maxDiscountAmountPaise,
        validFrom: off.validFrom,
        validTo: off.validTo,
        isActive: true,
      },
    });
  }
  console.log(`🎟️ Seeded ${offersData.length} active promotional offers`);

  console.log('✨ Database seed completed successfully!');
}

main()
  .catch((e) => {
    console.error('❌ Seed error:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
