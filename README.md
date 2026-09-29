# CafeApp — Production Dine-in Cafe Management System

CafeApp is a full-stack, real-time dine-in ordering and management platform built with modern Android (Kotlin 2.1.0, Jetpack Compose, Material 3, Clean Architecture) and Backend (Node.js, TypeScript, Express, PostgreSQL 16, Prisma, Socket.IO).

---

## 📌 Core Product Invariants

- **DINE-IN ONLY**: Exclusively designed for in-cafe dining and table management. Strictly zero food delivery, delivery addresses, driver tracking, or delivery fees.
- **Single Cafe Tenancy**: Singleton `CafeProfile` (`00000000-0000-4000-8000-000000000001`). No redundant `cafeId` columns in tables.
- **Single Android APK with Dynamic Role Navigation**: Dynamically renders Customer, Cafe Staff, Kitchen KDS, and Admin experiences based on verified server-side JWT claims.
- **Pay at Counter**: Customer places dine-in orders at their table; payment settlement occurs at the counter.
- **Financial Precision**: All monetary values are integer paise in **INR (₹)** with business operations anchored to **Asia/Kolkata** timezone.

---

## 📚 Technical Documentation Suite

Comprehensive architectural blueprints and specifications are documented in [`docs/`](docs/):

- 🏛️ [**System Architecture Specification**](docs/architecture.md): System overview, module boundaries, Android Clean Architecture + MVVM, backend layered design, and topology.
- 🗄️ [**Database Schema & Design**](docs/database.md): PostgreSQL 16 schema, Prisma definitions, ERD, indexes, state machines, and financial integrity rules.
- 🔌 [**REST API Contracts**](docs/api.md): Complete REST endpoint documentation (`/auth`, `/cafe`, `/menu`, `/offers`, `/tables`, `/table-sessions`, `/reservations`, `/orders`, `/kitchen`).
- ⚡ [**Real-Time WebSocket Protocol**](docs/realtime.md): Socket.IO events, room scoping (`kitchen`, `session:{id}`, `order:{id}`), connection lifecycle, and stale event handling.
- 🔒 [**Authentication & Security**](docs/authentication.md): 15-minute JWT access tokens, 30-day rotating refresh tokens with SHA-256 hashing & reuse family revocation, Argon2id passwords, and server-side RBAC.
- 📱 [**Android Application Structure**](docs/android-structure.md): Feature/core module layout, presentation/domain/data separation, Hilt DI, Navigation Compose graphs, and CameraX QR scanning.
- 📏 [**Development Rules & Standards**](docs/development-rules.md): Coding conventions, testing strategies, invariant checklists, and emulator QA workflow.

---

## 🛠️ Tech Stack Matrix

| Layer | Technologies |
| :--- | :--- |
| **Android Application** | Kotlin 2.1.0, Jetpack Compose (Material 3), Hilt 2.52, Navigation Compose, Coroutines & StateFlow, Retrofit 2.11, OkHttp 4.12, Room 2.6.1, DataStore Preferences 1.1.1, Socket.IO Client 2.1.1, Coil 2.7.0, CameraX 1.4.1 + ML Kit 17.3.0 |
| **Backend API & Real-time** | Node.js 20+, TypeScript 5.6 (ESM NodeNext), Express 4.21, PostgreSQL 16 (citext), Prisma ORM 6.0, Socket.IO 4.8, Zod 3.23, Pino 9.5, Argon2 0.41, jsonwebtoken 9.0 |
| **Infrastructure & Tooling** | Docker Compose (PostgreSQL 16, Adminer), Gradle 8.10.2 (AGP 8.7.3), KSP 2.1.0-1.0.29 |

---

## 🚀 Quick Start Guide

### 1. Start Infrastructure (PostgreSQL)
```bash
docker compose -f infra/docker-compose.yml up -d
```

### 2. Setup & Start Backend Server
```bash
cd backend
cp .env.example .env
npm install
npx prisma migrate dev
npx prisma db seed
npm run dev
```
*The backend API starts on `http://localhost:3000` (`http://localhost:3000/api/v1/health` for health checks).*

### 3. Build & Run Android Application
```bash
# Set temporary directory for Windows Gradle loopback compatibility
export TEMP="C:/tmp" TMP="C:/tmp"

cd android
./gradlew :app:assembleDebug --no-daemon

# Install onto connected Android Emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*Inside the Android emulator, the app connects to the local backend using `http://10.0.2.2:3000/api/v1`.*

---

## 📂 Repository Layout

```
cafeapp/
├── android/                    # Android multi-module project (Kotlin & Compose)
│   ├── app/                    # Application root & Navigation Host
│   ├── core/                   # Common, UI, Network, Database, DataStore, Realtime
│   └── feature/                # Auth, Cafe, Menu, Offers, Reservations, Session, Cart, Orders, Kitchen
├── backend/                    # Node.js + TypeScript Express REST & WebSocket server
│   ├── src/                    # Controllers, Services, Middleware, Routes, Real-time
│   └── prisma/                 # Database schema & Seed scripts
├── docs/                       # Architecture, Database, API, Real-time, Auth, Android specs
├── infra/                      # Docker Compose & container configurations
└── scripts/                    # Build and utility automation scripts
```
