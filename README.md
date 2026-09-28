# CafeApp

Production dine-in cafe management + ordering system.

## Product (dine-in only)
- Single-cafe (no multi-tenant).
- Customer: browse, reserve, scan QR, join session, cart, order, track status, bill copy.
- Cafe: manage tables / QR, reservations, menu/offers, orders, staff, bills, ops.
- Kitchen: incoming orders, accept, preparing, ready (with table number on every ticket).
- No delivery. No delivery addresses, drivers, fees, or tracking.

## Architecture
Monorepo:

- `android/` — Kotlin, Jetpack Compose, Material 3, Clean Architecture (feature + core modules), Hilt, Retrofit + Socket.IO
- `backend/` — Node.js + TypeScript + Express + Prisma + PostgreSQL + Socket.IO
- `infra/` — Docker Compose (postgres 16 + adminer)

## Tech Stack
- Android: Kotlin, Compose, Material 3, Hilt, Retrofit/OkHttp, Coroutines/Flow, Navigation Compose, Room (cache), DataStore (tokens/session)
- Backend: TypeScript, Express, Prisma, PostgreSQL, Socket.IO, Zod validation, JWT auth, argon2id

## Quick Start

1. Start Docker services: `docker compose up --build`
2. Migrate + seed backend: `cd backend && npx prisma migrate dev && npx prisma db seed`
3. Start API: `npm run dev`
4. Health check: `curl http://localhost:3000/api/v1/health`
5. Android: open `android/` in Android Studio, build and run on emulator. Set `BASE_URL` in `local.properties` (e.g. `http://10.0.2.2:3000/api/v1` for emulator localhost).

## Emulator / local setup
- Docker Desktop must be running (Postgres + adminer).
- Android SDK + emulator (`platform-tools`, `build-tools`, `emulator`).
- `local.properties` for backend URL override.

## Security / Notes
- JWT access (15 min) + refresh (30 days). Refresh rotated on use; reuse revokes family.
- QR tokens: 32 bytes CSPRNG, rotatable, no PII embedded.
- Dine-in only. No delivery fields in schema or UI.
- Currency: INR, stored as integer paise. Timezone: Asia/Kolkata.
