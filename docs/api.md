# CafeApp — REST API Architecture & Specification

## 1. REST Conventions & Standards

- **Base URL**: `/api/v1`
- **Authentication**: `Authorization: Bearer <accessToken>` header.
- **Content-Type**: `application/json` (UTF-8).
- **Date Format**: ISO-8601 Strings (`YYYY-MM-DDTHH:mm:ss.sssZ`).
- **Currency/Amount**: All monetary figures represent **integer paise** (e.g., ₹150.00 = `15000`).

### 1.1. Standard Success Response Envelope
```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "timestamp": "2026-09-29T12:00:00.000Z"
  }
}
```

### 1.2. Standard Error Response Envelope
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | CONFLICT | INTERNAL_SERVER_ERROR",
    "message": "Human-readable explanation of error",
    "details": [
      { "field": "email", "issue": "Invalid email address format" }
    ]
  }
}
```

---

## 2. API Endpoints by Domain

### 2.1. Authentication (`/api/v1/auth`)

#### `POST /api/v1/auth/register`
- **Purpose**: Register a new customer account.
- **Auth**: Public.
- **Request Body**:
  ```json
  {
    "email": "customer@example.com",
    "password": "SecurePassword123!",
    "name": "Alex Doe",
    "phone": "+919876543210"
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "user": {
      "id": "uuid",
      "email": "customer@example.com",
      "name": "Alex Doe",
      "phone": "+919876543210",
      "role": "CUSTOMER"
    }
  }
  ```

#### `POST /api/v1/auth/login`
- **Purpose**: Authenticate user credentials and issue token pair.
- **Auth**: Public.
- **Request Body**:
  ```json
  {
    "email": "customer@example.com",
    "password": "SecurePassword123!"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "7f8b9c0d1e2f3a4b...",
    "user": {
      "id": "uuid",
      "email": "customer@example.com",
      "name": "Alex Doe",
      "role": "CUSTOMER"
    }
  }
  ```

#### `POST /api/v1/auth/refresh`
- **Purpose**: Rotate refresh token and issue new 15-minute access token.
- **Auth**: Public (Requires raw refresh token).
- **Request Body**: `{ "refreshToken": "7f8b9c0d1e2f3a4b..." }`
- **Response `200 OK`**: `{ "accessToken": "...", "refreshToken": "..." }`

#### `POST /api/v1/auth/logout`
- **Purpose**: Revoke active refresh tokens for the authenticated user session.
- **Auth**: Authenticated (`CUSTOMER`, `CAFE_STAFF`, `KITCHEN`, `CAFE_ADMIN`).
- **Response `200 OK`**: `{ "ok": true }`

#### `GET /api/v1/auth/me`
- **Purpose**: Fetch profile data for the current authenticated user.
- **Auth**: Authenticated (`CUSTOMER`, `CAFE_STAFF`, `KITCHEN`, `CAFE_ADMIN`).
- **Response `200 OK`**: `{ "user": { "id": "...", "email": "...", "name": "...", "role": "..." } }`

---

### 2.2. Cafe Profile (`/api/v1/cafe`)

#### `GET /api/v1/cafe/profile`
- **Purpose**: Retrieve global cafe profile, business hours, and operational status.
- **Auth**: Public / Authenticated.
- **Response `200 OK`**:
  ```json
  {
    "id": "00000000-0000-4000-8000-000000000001",
    "name": "CafeApp Roasters",
    "address": "123 Indiranagar, Bengaluru, Karnataka",
    "phone": "+918012345678",
    "email": "hello@cafeapp.local",
    "gstPercent": "5.00",
    "currency": "INR",
    "timezone": "Asia/Kolkata",
    "openingTime": "08:00",
    "closingTime": "23:00",
    "isAcceptingOrders": true
  }
  ```

#### `PATCH /api/v1/cafe/profile`
- **Purpose**: Update cafe operating hours or operational flags.
- **Auth**: `CAFE_ADMIN`.

---

### 2.3. Menu & Categories (`/api/v1/menu`)

#### `GET /api/v1/menu/categories`
- **Purpose**: List all active menu categories ordered by `displayOrder`.
- **Auth**: Public / Authenticated.
- **Response `200 OK`**:
  ```json
  [
    { "id": "uuid", "name": "Coffee & Espresso", "displayOrder": 1, "isActive": true },
    { "id": "uuid", "name": "Artisanal Breads", "displayOrder": 2, "isActive": true }
  ]
  ```

#### `GET /api/v1/menu/items`
- **Purpose**: Fetch menu items. Filterable by `categoryId`, `isVeg`, `search`.
- **Auth**: Public / Authenticated.
- **Query Params**: `?categoryId=uuid&isVeg=true&search=latte`
- **Response `200 OK`**:
  ```json
  [
    {
      "id": "uuid",
      "categoryId": "uuid",
      "name": "Cappuccino",
      "description": "Double espresso with velvety steamed whole milk",
      "pricePaise": 22000,
      "preparationTimeMinutes": 5,
      "isAvailable": true,
      "isVeg": true,
      "isSpicy": false,
      "imageUrl": "https://..."
    }
  ]
  ```

#### `POST /api/v1/menu/items` / `PATCH /api/v1/menu/items/:id`
- **Purpose**: Create or update menu item details and availability toggles.
- **Auth**: `CAFE_ADMIN`, `CAFE_STAFF`.

---

### 2.4. Offers & Discounts (`/api/v1/offers`)

#### `GET /api/v1/offers/active`
- **Purpose**: List active public promotional offers and coupons.
- **Auth**: Authenticated (`CUSTOMER`).

#### `POST /api/v1/offers/validate`
- **Purpose**: Validate a coupon code against a prospective order subtotal.
- **Auth**: Authenticated (`CUSTOMER`).
- **Request Body**: `{ "code": "FLAT50", "subtotalPaise": 50000 }`
- **Response `200 OK`**:
  ```json
  {
    "valid": true,
    "discountPaise": 5000,
    "offer": { "code": "FLAT50", "description": "Flat ₹50 off on orders above ₹400" }
  }
  ```

---

### 2.5. Tables & QR Verification (`/api/v1/tables`)

#### `GET /api/v1/tables`
- **Purpose**: View real-time list of all physical tables and current occupancy states.
- **Auth**: `CAFE_STAFF`, `CAFE_ADMIN`.

#### `POST /api/v1/tables/verify-qr`
- **Purpose**: Decode and cryptographically verify scanned table QR token.
- **Auth**: Authenticated (`CUSTOMER`, `CAFE_STAFF`).
- **Request Body**: `{ "qrToken": "a3f5c9e2b1d4087..." }`
- **Response `200 OK`**:
  ```json
  {
    "table": {
      "id": "uuid",
      "tableNumber": 4,
      "capacity": 4,
      "status": "AVAILABLE"
    },
    "activeSession": null
  }
  ```

---

### 2.6. Table Sessions (`/api/v1/table-sessions`)

#### `POST /api/v1/table-sessions/check-in`
- **Purpose**: Establish an active dining session at a table using verified QR token.
- **Auth**: Authenticated (`CUSTOMER`, `CAFE_STAFF`).
- **Request Body**: `{ "tableId": "uuid", "qrToken": "...", "guestCount": 2 }`
- **Response `201 Created`**:
  ```json
  {
    "sessionId": "uuid",
    "tableNumber": 4,
    "status": "ACTIVE",
    "checkedInAt": "2026-09-29T12:30:00.000Z"
  }
  ```

#### `GET /api/v1/table-sessions/:id/bill`
- **Purpose**: Generate consolidated bill summary for all served/placed orders in session.
- **Auth**: Authenticated (`CUSTOMER` [host only], `CAFE_STAFF`, `CAFE_ADMIN`).

#### `POST /api/v1/table-sessions/:id/complete`
- **Purpose**: Settle payment at counter and close table session.
- **Auth**: `CAFE_STAFF`, `CAFE_ADMIN`.

---

### 2.7. Reservations (`/api/v1/reservations`)

#### `POST /api/v1/reservations`
- **Purpose**: Create a table reservation request.
- **Auth**: Authenticated (`CUSTOMER`).
- **Request Body**:
  ```json
  {
    "partySize": 4,
    "reservationTime": "2026-09-29T19:00:00.000Z",
    "durationMinutes": 90,
    "specialRequests": "Window seating preferred"
  }
  ```

#### `GET /api/v1/reservations`
- **Purpose**: View all incoming reservations. Filterable by date and status.
- **Auth**: `CAFE_STAFF`, `CAFE_ADMIN`.

---

### 2.8. Orders & Kitchen Operations (`/api/v1/orders`)

#### `POST /api/v1/orders`
- **Purpose**: Place a dine-in order against an active table session.
- **Auth**: Authenticated (`CUSTOMER`, `CAFE_STAFF`).
- **Request Body**:
  ```json
  {
    "sessionId": "uuid",
    "tableId": "uuid",
    "items": [
      { "menuItemId": "uuid-1", "quantity": 2, "notes": "Oat milk" },
      { "menuItemId": "uuid-2", "quantity": 1, "notes": null }
    ],
    "offerCode": "WELCOME50",
    "specialInstructions": "Please serve coffee first"
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "id": "uuid",
    "orderNumber": 1042,
    "status": "PLACED",
    "subtotalPaise": 54000,
    "discountPaise": 5000,
    "taxPaise": 2450,
    "totalPaise": 51450,
    "createdAt": "2026-09-29T12:35:00.000Z"
  }
  ```

#### `PATCH /api/v1/orders/:id/status`
- **Purpose**: Update order lifecycle state (triggers real-time WebSocket broadcast).
- **Auth**: `KITCHEN`, `CAFE_STAFF`, `CAFE_ADMIN`.
- **Request Body**:
  ```json
  {
    "status": "PREPARING",
    "notes": "Order accepted and on espresso machine"
  }
  ```

#### `GET /api/v1/kitchen/queue`
- **Purpose**: Fetch live KDS queue of all active (`PLACED`, `ACCEPTED`, `PREPARING`, `READY`) orders.
- **Auth**: `KITCHEN`, `CAFE_STAFF`, `CAFE_ADMIN`.
