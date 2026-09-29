# CafeApp — Database Design & Schema Specification

## 1. Relational Database Overview

CafeApp uses **PostgreSQL 16** managed via **Prisma ORM**.
- **Case-Insensitive Identifiers**: Utilizes PostgreSQL's `citext` extension for unique email addresses and phone numbers.
- **Financial Precision**: All monetary values are stored as integers representing **paise** (1 INR = 100 paise) to eliminate floating-point rounding errors.
- **Single-Cafe Architecture**: A singleton `CafeProfile` holds global cafe settings. Individual operational records (tables, menu items, orders) do not include redundant `cafeId` columns.

---

## 2. Entity-Relationship Diagram (ERD)

```
       +--------------------+
       |    CafeProfile     |
       |  (Singleton Row)   |
       +--------------------+

       +--------------------+       1:N       +--------------------+
       |        User        |---------------->|    RefreshToken    |
       +--------------------+                 +--------------------+
         | 1:N          | 1:N
         |              |
         v              v
+------------------+  +--------------------+
|   Reservation    |  |    TableSession    |
+------------------+  +--------------------+
         ^                      | 1:N
         |                      |
         v                      v
+------------------+  +--------------------+       1:N       +--------------------+
|      Table       |  |       Order        |---------------->|     OrderItem      |
+------------------+  +--------------------+                 +--------------------+
                        | 1:N                                           | N:1
                        v                                               v
                      +--------------------+                 +--------------------+
                      | OrderStatusHistory |                 |      MenuItem      |
                      +--------------------+                 +--------------------+
                                                                        ^
                                                                        | N:1
                                                             +--------------------+
                                                             |    MenuCategory    |
                                                             +--------------------+

+--------------------+
|       Offer        |
+--------------------+
```

---

## 3. Detailed Data Models & Specifications

### 3.1. Enums
```prisma
enum UserRole {
  CUSTOMER
  CAFE_STAFF
  KITCHEN
  CAFE_ADMIN
}

enum TableStatus {
  AVAILABLE
  OCCUPIED
  RESERVED
  OUT_OF_SERVICE
}

enum TableSessionStatus {
  ACTIVE
  COMPLETED
  CANCELLED
}

enum ReservationStatus {
  PENDING
  CONFIRMED
  SEATED
  CANCELLED
  NO_SHOW
}

enum OrderStatus {
  PLACED
  ACCEPTED
  PREPARING
  READY
  SERVED
  CANCELLED
}

enum DiscountType {
  PERCENTAGE
  FLAT_AMOUNT
}

enum NotificationType {
  ORDER_STATUS
  RESERVATION_STATUS
  PROMOTIONAL
  SYSTEM
}
```

---

### 3.2. Table Definitions

#### `User`
Stores authenticated users across all roles.
- `id` (`UUID`, PK): Unique identifier.
- `email` (`citext`, Unique): Normalized lowercase email address.
- `passwordHash` (`VARCHAR`): Argon2id / bcrypt hash.
- `name` (`VARCHAR`): Full name of the user.
- `phone` (`citext`, Unique, Nullable): Mobile contact number.
- `role` (`UserRole`, Default: `CUSTOMER`): Role for RBAC.
- `isActive` (`BOOLEAN`, Default: `true`): Soft-delete flag / account status.
- `createdAt`, `updatedAt` (`TIMESTAMP`): Audit timestamps.

#### `RefreshToken`
Manages rotating cryptographically secure refresh tokens.
- `id` (`UUID`, PK): Unique token ID.
- `userId` (`UUID`, FK -> `User.id` ON DELETE CASCADE): Token owner.
- `tokenHash` (`VARCHAR(64)`, Unique): SHA-256 hash of the random refresh string.
- `expiresAt` (`TIMESTAMP`): Expiration datetime (30 days from issue).
- `revokedAt` (`TIMESTAMP`, Nullable): Revocation timestamp if consumed or invalidated.
- `replacedById` (`UUID`, Nullable): Points to replacement token ID in rotation chain.
- `createdAt` (`TIMESTAMP`): Audit creation timestamp.

#### `CafeProfile`
Singleton cafe configuration record.
- `id` (`UUID`, PK, Default: `"00000000-0000-4000-8000-000000000001"`): Constant UUID.
- `name` (`VARCHAR`): Cafe business name.
- `address` (`TEXT`): Physical location description.
- `phone` (`VARCHAR`): Cafe telephone.
- `email` (`VARCHAR`): Cafe official email.
- `gstin` (`VARCHAR`, Nullable): Indian GST Registration Number.
- `gstPercent` (`DECIMAL(5,2)`, Default: `5.00`): Standard restaurant GST rate.
- `currency` (`VARCHAR(3)`, Default: `"INR"`): Currency ISO code.
- `timezone` (`VARCHAR`, Default: `"Asia/Kolkata"`): Operational timezone.
- `openingTime` (`VARCHAR(5)`): Opening time format "HH:mm" (e.g. "08:00").
- `closingTime` (`VARCHAR(5)`): Closing time format "HH:mm" (e.g. "23:00").
- `isAcceptingOrders` (`BOOLEAN`, Default: `true`): Global ordering kill-switch.

#### `Table`
Physical dining tables located in the cafe.
- `id` (`UUID`, PK): Unique table ID.
- `tableNumber` (`INTEGER`, Unique): Visible table number (e.g., Table 1, 2, 3...).
- `capacity` (`INTEGER`): Seating capacity (e.g., 2, 4, 6, 8).
- `qrToken` (`VARCHAR(64)`, Unique): 32-byte CSPRNG hex string for QR scanning.
- `status` (`TableStatus`, Default: `AVAILABLE`): Current live table state.
- `isActive` (`BOOLEAN`, Default: `true`): Table availability flag.
- `createdAt`, `updatedAt` (`TIMESTAMP`).

#### `TableSession`
Represents an active or past physical dining session at a table.
- `id` (`UUID`, PK): Unique session ID.
- `tableId` (`UUID`, FK -> `Table.id` ON DELETE RESTRICT): Associated table.
- `hostUserId` (`UUID`, FK -> `User.id` ON DELETE RESTRICT): Customer who opened session.
- `guestCount` (`INTEGER`, Default: `1`): Number of guests at table.
- `status` (`TableSessionStatus`, Default: `ACTIVE`): `ACTIVE` | `COMPLETED` | `CANCELLED`.
- `checkedInAt` (`TIMESTAMP`, Default: `NOW()`): Check-in timestamp.
- `checkedOutAt` (`TIMESTAMP`, Nullable): Checkout / bill settlement timestamp.
- `totalAmountPaise` (`INTEGER`, Default: `0`): Total settled amount in paise.

#### `MenuCategory`
Categorical hierarchy for menu organization.
- `id` (`UUID`, PK): Unique category ID.
- `name` (`VARCHAR`): Category name (e.g., "Beverages", "Starters", "Desserts").
- `displayOrder` (`INTEGER`, Default: `0`): UI ordering rank.
- `isActive` (`BOOLEAN`, Default: `true`): Visibility toggle.

#### `MenuItem`
Individual food and beverage items available for ordering.
- `id` (`UUID`, PK): Unique item ID.
- `categoryId` (`UUID`, FK -> `MenuCategory.id` ON DELETE RESTRICT): Category reference.
- `name` (`VARCHAR`): Dish or drink title.
- `description` (`TEXT`, Nullable): Description / ingredients.
- `pricePaise` (`INTEGER`): Base unit price in paise (e.g., ₹250.00 = 25000 paise).
- `preparationTimeMinutes` (`INTEGER`, Default: `15`): Estimated kitchen prep time.
- `isAvailable` (`BOOLEAN`, Default: `true`): Stock availability toggle.
- `isVeg` (`BOOLEAN`, Default: `true`): Vegetarian dietary indicator.
- `isSpicy` (`BOOLEAN`, Default: `false`): Spicy flavor indicator.
- `imageUrl` (`VARCHAR`, Nullable): CDN asset URL.

#### `Offer`
Promotional codes and discount rules.
- `id` (`UUID`, PK): Unique offer ID.
- `code` (`VARCHAR`, Unique): Case-insensitive coupon code (e.g., "WELCOME50").
- `description` (`TEXT`): User-facing promo details.
- `discountType` (`DiscountType`): `PERCENTAGE` or `FLAT_AMOUNT`.
- `discountValue` (`INTEGER`): Percentage points (e.g. 20) or flat amount in paise.
- `minOrderAmountPaise` (`INTEGER`, Default: `0`): Minimum subtotal required.
- `maxDiscountAmountPaise` (`INTEGER`, Nullable): Cap for percentage discounts in paise.
- `validFrom` (`TIMESTAMP`): Offer start datetime.
- `validTo` (`TIMESTAMP`): Offer expiration datetime.
- `isActive` (`BOOLEAN`, Default: `true`): Administrative toggle.

#### `Reservation`
Customer table pre-booking requests.
- `id` (`UUID`, PK): Unique reservation ID.
- `userId` (`UUID`, FK -> `User.id` ON DELETE RESTRICT): Booking customer.
- `tableId` (`UUID`, FK -> `Table.id`, Nullable): Assigned table.
- `partySize` (`INTEGER`): Number of expected guests.
- `reservationTime` (`TIMESTAMP`): Scheduled arrival time.
- `durationMinutes` (`INTEGER`, Default: `90`): Expected duration.
- `status` (`ReservationStatus`, Default: `PENDING`): Booking lifecycle state.
- `specialRequests` (`TEXT`, Nullable): Customer requests (e.g., quiet booth).
- `createdAt`, `updatedAt` (`TIMESTAMP`).

#### `Order`
Dine-in orders placed against an active table session.
- `id` (`UUID`, PK): Unique order ID.
- `orderNumber` (`INTEGER`, Unique): Sequential human-friendly order number (e.g., 1001).
- `sessionId` (`UUID`, FK -> `TableSession.id` ON DELETE RESTRICT): Dining session.
- `userId` (`UUID`, FK -> `User.id` ON DELETE RESTRICT): Ordering customer.
- `tableId` (`UUID`, FK -> `Table.id` ON DELETE RESTRICT): Physical table ID.
- `status` (`OrderStatus`, Default: `PLACED`): `PLACED` -> `ACCEPTED` -> `PREPARING` -> `READY` -> `SERVED` (or `CANCELLED`).
- `subtotalPaise` (`INTEGER`): Sum of line items in paise.
- `discountPaise` (`INTEGER`, Default: `0`): Applied discount in paise.
- `taxPaise` (`INTEGER`, Default: `0`): Calculated GST in paise.
- `totalPaise` (`INTEGER`): Final payable amount in paise.
- `specialInstructions` (`TEXT`, Nullable): Kitchen cooking instructions.
- `createdAt`, `updatedAt` (`TIMESTAMP`).

#### `OrderItem`
Line items within an order with immutable historical pricing.
- `id` (`UUID`, PK): Unique line item ID.
- `orderId` (`UUID`, FK -> `Order.id` ON DELETE CASCADE): Parent order.
- `menuItemId` (`UUID`, FK -> `MenuItem.id` ON DELETE RESTRICT): Referenced menu item.
- `quantity` (`INTEGER`, Default: `1`): Number of portions.
- `unitPricePaise` (`INTEGER`): Snapshot of item price at time of order in paise.
- `subtotalPaise` (`INTEGER`): `quantity * unitPricePaise`.
- `notes` (`VARCHAR`, Nullable): Special item instructions (e.g. "no onions").

#### `OrderStatusHistory`
Audit trail of order lifecycle transitions.
- `id` (`UUID`, PK): Unique history record ID.
- `orderId` (`UUID`, FK -> `Order.id` ON DELETE CASCADE): Associated order.
- `status` (`OrderStatus`): Recorded status.
- `changedByUserId` (`UUID`, FK -> `User.id` ON DELETE SET NULL, Nullable): Staff/Kitchen user.
- `notes` (`VARCHAR`, Nullable): Reason for transition or cancellation note.
- `createdAt` (`TIMESTAMP`, Default: `NOW()`).

---

## 4. State Machines & Transition Rules

### 4.1. Table Lifecycle
```
[ AVAILABLE ] ---> (Customer Scans QR / Staff Checks in) ---> [ OCCUPIED ]
      ^                                                            |
      |                                                            |
(Staff Marks Available / Checkout) <-------------------------------+
      |
(Staff Schedules Maintenance) ---> [ OUT_OF_SERVICE ]
```

### 4.2. Reservation Lifecycle
```
[ PENDING ] ---> (Staff Approves) ---> [ CONFIRMED ] ---> (Customer Arrives) ---> [ SEATED ]
     |                                       |
     +------------> [ CANCELLED ] <----------+
     |                                       |
     +------------> [ NO_SHOW ] <------------+
```

### 4.3. Order Lifecycle
```
[ PLACED ] ---> [ ACCEPTED ] ---> [ PREPARING ] ---> [ READY ] ---> [ SERVED ]
    |                |                 |
    +----------------+-----------------+-------> [ CANCELLED ] (Staff/Admin only)
```
- **Guarantees**: Status transitions are monotonic. Once `SERVED`, an order cannot transition back to `PREPARING`. Only unserved orders can be cancelled with recorded audit notes.

---

## 5. Security & Data Integrity Protections

1. **Double-Booking Prevention**:
   Reservations check for overlapping time windows on the assigned table using transactional queries (`reservationTime < existing.end AND reservationEnd > existing.start`).
2. **Session Tamper Resistance**:
   A table can have at most **one** `ACTIVE` `TableSession` at any point in time. When a session is active, new QR scans attach to the existing active session if authorized, or reject conflicting check-ins.
3. **Price Manipulation Prevention**:
   Client order payloads supply ONLY `menuItemId` and `quantity`. The backend fetches active `pricePaise` from the database, applies server-verified active `Offer` rules, calculates 5% GST, and creates the order within an ACID transaction.
4. **Historical Price Immutability**:
   `OrderItem.unitPricePaise` captures the exact price at order creation. Future price changes to `MenuItem` do not alter historical orders or financial reporting.
