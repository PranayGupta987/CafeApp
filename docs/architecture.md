# CafeApp — System Architecture Specification

## 1. System Overview & Core Invariants

CafeApp is a high-performance, real-time dine-in cafe management and ordering platform. It provides an end-to-end digital experience for dine-in customers, table reservation management, table QR code scanning and session synchronization, kitchen order display (KDS), and administrative operations.

### Non-Negotiable Core Invariants
1. **DINE-IN ONLY**: Under no circumstances does the system support food delivery, delivery addresses, delivery driver tracking, delivery fees, or external logistics. Every order is tied strictly to a physical table session or counter takeaway within the cafe.
2. **Single Cafe Tenancy**: The platform operates as a dedicated single-cafe instance. A singleton `CafeProfile` entity exists with fixed UUID `00000000-0000-4000-8000-000000000001`. Tables, orders, menu items, and sessions do NOT maintain redundant `cafeId` foreign keys.
3. **Strict Server-Side RBAC**: Client-side role assertions (from Android or web) are never trusted. All access control is enforced at the backend API gateway and middleware level via cryptographically signed JWT claims verified against the database.
4. **Security & Cryptography Invariants**: Plaintext passwords are never persisted. Passwords use Argon2id hashing with bcrypt fallback. Refresh tokens are stored strictly as SHA-256 hashes with reuse detection and token family revocation.
5. **Single Source of Truth**: PostgreSQL is the definitive source of truth for all order, table, and payment states. Real-time WebSockets (Socket.IO) act as ephemeral event dispatchers; clients always verify state via authoritative REST endpoints.
6. **Localization & Financials**: Currency is strictly **INR** (stored in integer paise to avoid IEEE-754 floating-point inaccuracies), and the business timezone is fixed to `Asia/Kolkata`.

---

## 2. System Topology

```
+-----------------------------------------------------------------------------------+
|                                  CLIENT LAYER                                     |
|                                                                                   |
|   +---------------------------------------------------------------------------+   |
|   |                        CafeApp Android Application                        |   |
|   |   (Single APK with Dynamic Role-Based Presentation & Navigation Graphs)   |   |
|   |                                                                           |   |
|   |   [ Customer Experience ]      [ Staff / Waiter ]      [ Kitchen Display ]|   |
|   |   - Menu & Offers Browsing     - Table Management      - Live KDS Tickets |   |
|   |   - Table Booking / QR Scan    - Order Dispatch        - Status Toggles   |   |
|   |   - Host Cart & Dine-in Order  - Bill Settlement       - Preparation Flow |   |
|   |   - Live Order Tracking        - Active Sessions                          |   |
|   +---------------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------------+
                                  |                 ^
                     HTTPS / REST |                 | WSS / Socket.IO
                      (Port 3000) |                 | (Bidirectional)
                                  v                 |
+-----------------------------------------------------------------------------------+
|                                 BACKEND LAYER                                     |
|                                                                                   |
|   +---------------------------------------------------------------------------+   |
|   |                     Express.js HTTP & Real-time Server                    |   |
|   |                                                                           |   |
|   |   [ API Gateway / Security ]                                              |   |
|   |   - Helmet Security Headers, CORS, Rate Limiting, Request ID Tracking     |   |
|   |   - JWT Authentication & Role-Based Access Control (RBAC) Middleware      |   |
|   |   - Zod Schema Validation & Centralized Error Handler                     |   |
|   |                                                                           |   |
|   |   [ Domain Modules ]                                                      |   |
|   |   - Auth & Session Service (Argon2id, Refresh Token Rotation)             |   |
|   |   - Table & QR Validation Service (Cryptographic Token Verification)      |   |
|   |   - Reservation & Occupancy Engine (Double-booking Prevention)            |   |
|   |   - Menu, Pricing & Offers Engine (Server-side Discount Calculation)      |   |
|   |   - Order State Machine & Kitchen Dispatch Engine                         |   |
|   |   - Real-time Socket.IO Gateway (Scoped Rooms & State Broadcasting)       |   |
|   +---------------------------------------------------------------------------+   |
|                                  |                                                |
|                     Prisma ORM   |                                                |
|                                  v                                                |
|   +---------------------------------------------------------------------------+   |
|   |                          PostgreSQL 16 Database                           |   |
|   |   - citext extension for case-insensitive emails and phone numbers        |   |
|   |   - ACID Transactions for Order Placement and Table Session Management   |   |
|   |   - Strict Foreign Keys, Check Constraints, and Partial Unique Indexes    |   |
|   +---------------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------------+
```

---

## 3. Layered Android Architecture (Clean Architecture + MVVM)

The Android client is organized as a modular Gradle project adhering to Clean Architecture principles:

```
android/
├── app/                        # Main Application, Navigation Host & DI Root
├── core/
│   ├── common/                 # Base Result types, Coroutine dispatchers, Extensions
│   ├── ui/                     # Material 3 Design System, Themes, Shared Components
│   ├── network/                # Retrofit, OkHttp, Auth Interceptors, Network DTOs
│   ├── database/               # Room DB, Offline Caches, Room Entities & DAOs
│   ├── datastore/              # Proto/Preferences DataStore (Tokens, Active Session)
│   └── realtime/               # Socket.IO Client wrapper & event flow emissions
└── feature/
    ├── auth/                   # Login, Registration, Splash, Password Management
    ├── cafe/                   # Cafe Profile, Operational Hours, Admin Dashboard
    ├── menu/                   # Category tabs, Menu items, Dietary badges, Search
    ├── offers/                 # Active promo banners, Coupon code application
    ├── reservations/           # Table pre-booking calendar, Time slots, Party size
    ├── session/                # CameraX QR Scanner, QR Token Validation, Table Lock
    ├── cart/                   # Host Cart, Item modifiers, Special cooking notes
    ├── orders/                 # Customer Order History, Live Timeline Tracking
    └── kitchen/                # Kitchen Display System (KDS), Order Status Controls
```

### Module Responsibilities & Boundary Invariants
1. **Feature to Core Dependency**: Features may depend on `:core:*` modules, but features NEVER depend directly on other feature modules.
2. **Navigation**: Feature interactions and navigation between screens are decoupled through navigation interfaces and lambdas aggregated in `:app`.
3. **Separation of Concerns**:
   - **UI Layer (`:feature:*:ui`)**: Jetpack Compose Composable functions observing `StateFlow` from ViewModels. Zero business logic or database access.
   - **Presentation Layer (`ViewModel`)**: Manages UI state as immutable data classes, handles UI events, interacts with UseCases/Repositories within `viewModelScope`.
   - **Domain Layer (`:feature:*:domain`)**: Pure Kotlin business models and UseCases containing business logic (e.g., cart total calculations, validation).
   - **Data Layer (`:feature:*:data`)**: Repository implementations coordinating between Local (`Room`, `DataStore`) and Remote (`Retrofit`, `Socket.IO`) data sources.

---

## 4. Layered Backend Architecture

The backend is built with TypeScript on Node.js using Express.js and Prisma ORM:

```
backend/
├── src/
│   ├── config/                 # Environment variables parsing & validation (Zod)
│   ├── lib/                    # Shared utilities: Logger (Pino), Errors, Crypto
│   ├── middleware/             # Auth, RBAC, Request Logger, Error Handler
│   ├── modules/
│   │   ├── auth/               # Login, Register, Token Refresh, Logout, /me
│   │   ├── users/              # User profile & staff management
│   │   ├── cafe/               # Cafe Profile, opening hours, GST configuration
│   │   ├── tables/             # Tables, QR code generation, Table verification
│   │   ├── reservations/       # Reservation booking, validation, status updates
│   │   ├── menu/               # Categories, Menu Items, Variations, Availability
│   │   ├── offers/             # Discounts, Coupon validation, Pricing rules
│   │   ├── table-sessions/     # QR check-in, Active dining sessions, Bill summary
│   │   ├── orders/             # Order creation, Price calculation, Status changes
│   │   ├── kitchen/            # KDS live queue, ticket status transitions
│   │   └── realtime/           # Socket.IO server, Room management, Event emitters
│   ├── app.ts                  # Express application configuration & router mounts
│   └── index.ts                # HTTP + WebSocket server initialization
└── prisma/
    ├── schema.prisma           # Prisma Database schema definition
    └── seed.ts                 # Database seed script for dev/testing
```

### Module Boundary Invariants
1. **Controller Layer**: Handles HTTP requests, parses headers and query parameters, delegates to Services.
2. **Validation Layer**: Every request payload is strictly validated using Zod schemas before touching any service logic.
3. **Service Layer**: Pure business logic, transaction boundaries (`prisma.$transaction`), domain state machines, and real-time event broadcasting.
4. **Data Access Layer**: Exclusively managed through Prisma Client, ensuring type-safe SQL queries and referential integrity.
