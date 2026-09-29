# CafeApp — Engineering Standards, Invariants & Development Rules

## 1. Absolute Product & Architecture Invariants

1. **DINE-IN ONLY (Zero Delivery Code)**:
   - NEVER add delivery addresses, delivery driver tracking, delivery fees, or external logistics to database schemas, REST APIs, or Android UI screens.
   - All orders must be tied to an active physical table session or counter takeaway.
2. **Single Cafe Tenancy**:
   - The system is engineered for a single physical cafe location.
   - Singleton `CafeProfile` ID is fixed: `00000000-0000-4000-8000-000000000001`.
   - Never add `cafeId` columns to `Table`, `Order`, `MenuItem`, `Reservation`, or `TableSession`.
3. **No Plaintext Secrets or Passwords**:
   - Passwords must be hashed using Argon2id (with bcrypt fallback).
   - Refresh tokens must be stored strictly as SHA-256 hashes in PostgreSQL.
   - Secrets must never be committed to git repositories.
4. **Server-Side Authorization Invariant**:
   - Never trust client-supplied roles. Every protected API endpoint must execute server-side JWT authentication and role verification middleware (`requireRole(...)`).
5. **Monetary Value Representation**:
   - All currency values are stored and transmitted as **integer paise** (₹1.00 = 100 paise).
   - Never use floating-point types (`Float`, `Double`, `number`) for storing or calculating monetary totals.
6. **Timezone & Regional Defaults**:
   - Business operational timezone is strictly `Asia/Kolkata`.
   - Currency display symbol is `₹` (INR).

---

## 2. Android Kotlin & Compose Development Rules

1. **Clean Architecture Module Boundaries**:
   - Feature modules (`:feature:*`) must never depend on each other.
   - Domain layer (`domain/`) must remain pure Kotlin with zero Android framework dependencies.
   - Business logic must reside in UseCases or ViewModels, never in Composable UI functions.
2. **Jetpack Compose Guidelines**:
   - **State Hoisting**: Screen composables should collect `StateFlow` from ViewModels and pass immutable state objects down to stateless UI components.
   - **Events**: UI components must bubble user interactions up via lambda callbacks (`onEvent: (ScreenEvent) -> Unit`).
   - **Previews**: Every leaf UI component and screen content composable must include at least one `@Preview` with light and dark mode variants.
   - **Modifiers**: Always accept an optional `modifier: Modifier = Modifier` parameter as the first optional argument in composables.
3. **Coroutines & Flow**:
   - Never use `GlobalScope`. Always launch coroutines within `viewModelScope` or inject structured `CoroutineScope`.
   - Use injected `CoroutineDispatchers` (`Dispatchers.IO`, `Dispatchers.Default`) for testability.
4. **Dependency Injection**:
   - Use Hilt exclusively for dependency injection (`@HiltViewModel`, `@Inject constructor`, `@Module`, `@InstallIn(SingletonComponent::class)`).

---

## 3. Backend TypeScript & Express Development Rules

1. **Strict ESM & TypeScript Standards**:
   - NodeNext ESM resolution is active. All relative file imports MUST include the `.js` extension (e.g., `import { prisma } from "./service.js";`).
   - Strict type checking is mandatory. No usage of `any` except in catch blocks where safely cast.
2. **Request Validation**:
   - Every route that accepts request bodies, query parameters, or route parameters must validate them using **Zod** schemas before invoking service methods.
3. **Controller & Route Handlers**:
   - Express handlers must explicitly return after sending responses (`res.json(...); return;`) to avoid double-response execution.
4. **Database Transactions**:
   - Composite operations (e.g. creating an order and line items, updating table status and session status) must execute within `prisma.$transaction([...])`.
5. **Logging & Error Handling**:
   - Use Pino logger (`logger.info`, `logger.error`) for all server events. Never use `console.log` in production code.
   - Errors must be passed to `next(error)` to be formatted uniformly by the global `errorHandler`.

---

## 4. Local Development & Emulator QA Workflow

### 4.1. Prerequisites
- Node.js 20+ and npm 10+
- JDK 17 (Microsoft Build of OpenJDK 17 or equivalent)
- Android SDK with `platform-tools`, `build-tools`, and Android Emulator (API 34+)
- Docker Desktop (for local PostgreSQL 16)

### 4.2. Local Environment Setup
1. **Start PostgreSQL**:
   ```bash
   docker compose -f infra/docker-compose.yml up -d
   ```
2. **Backend Setup & Migrations**:
   ```bash
   cd backend
   cp .env.example .env
   npm install
   npx prisma migrate dev
   npx prisma db seed
   npm run dev
   ```
3. **Android Build & Run**:
   ```bash
   # In Git Bash on Windows:
   export TEMP="C:/tmp" TMP="C:/tmp"
   cd android
   ./gradlew :app:assembleDebug --no-daemon
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### 4.3. Android Emulator Network Mapping
- To connect the Android Emulator to the local backend, use the Android loopback alias:
  `http://10.0.2.2:3000/api/v1`
