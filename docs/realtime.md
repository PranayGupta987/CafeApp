# CafeApp — Real-Time WebSocket & Event Protocol

## 1. Real-Time Architecture Overview

CafeApp uses **Socket.IO (v4.8)** to provide low-latency, bidirectional state synchronization between Customers, Staff, and Kitchen Displays.
- **WebSocket Transport**: WebSockets with fallback to HTTP long-polling.
- **Database-First Invariant**: WebSocket events are NEVER the source of truth. State is committed to PostgreSQL in an ACID transaction FIRST, and the resulting snapshot is emitted to relevant Socket.IO rooms.
- **Resilience Invariant**: Clients handle disconnects and reconnects seamlessly. Upon reconnecting, the client automatically executes an authoritative REST sync to reconcile any dropped frames.

---

## 2. Connection Handshake & Authentication

When establishing a Socket.IO connection, the client provides the active JWT access token in the connection handshake:

```typescript
// Android / Kotlin Socket.IO Client handshake
val options = IO.Options.builder()
    .setAuth(mapOf("token" to "Bearer $accessToken"))
    .setTransports(arrayOf(WebSocket.NAME))
    .setReconnection(true)
    .setReconnectionAttempts(Int.MAX_VALUE)
    .setReconnectionDelay(1000)
    .setReconnectionDelayMax(5000)
    .build()

val socket = IO.socket("http://10.0.2.2:3000", options)
socket.connect()
```

### Server Handshake Verification
```typescript
io.use((socket, next) => {
  const token = socket.handshake.auth.token?.replace("Bearer ", "");
  if (!token) return next(new Error("Authentication token missing"));
  try {
    const payload = jwt.verify(token, ACCESS_SECRET) as JwtPayload;
    socket.data.user = payload;
    next();
  } catch (err) {
    next(new Error("Invalid or expired token"));
  }
});
```

---

## 3. Scoped Rooms & Subscriptions

To prevent data leaks and maintain strict tenant/role boundaries, connections join targeted rooms based on role and active sessions:

| Room Name | Target Audience | Joined When |
| :--- | :--- | :--- |
| `user:{userId}` | Specific User | Automatically on authenticated connection |
| `kitchen` | Kitchen Displays, Staff, Admins | Role in (`KITCHEN`, `CAFE_STAFF`, `CAFE_ADMIN`) |
| `session:{sessionId}` | Table Session Host & Guests | Joined when user enters active table session |
| `order:{orderId}` | Order Creator & Observers | Joined when customer navigates to Order Tracking |
| `table:{tableId}` | Floor Staff & Table Observers | Table status changes and call-waiter alerts |

---

## 4. Event Lifecycle & Catalog

### 4.1. Customer Order Flow
```
[ Customer App ]              [ Express API ]           [ PostgreSQL ]          [ Socket.IO Server ]          [ Kitchen KDS ]
       |                             |                         |                         |                           |
       |--- POST /api/v1/orders ---->|                         |                         |                           |
       |                             |--- Save Order & Items ->|                         |                           |
       |                             |<-- Order Persisted -----|                         |                           |
       |                             |                                                   |                           |
       |                             |--- Broadcast ORDER_CREATED to 'kitchen' room ---->|                           |
       |<-- 201 Created (Order) -----|                                                   |--- 'ORDER_CREATED' ------>|
       |                             |                                                   |    (Rings KDS Ticket)     |
```

### 4.2. Kitchen Status Transition Flow
```
[ Kitchen KDS ]               [ Express API ]           [ PostgreSQL ]          [ Socket.IO Server ]          [ Customer App ]
       |                             |                         |                         |                           |
       |-- PATCH /orders/:id/status->|                         |                         |                           |
       |   (status: 'PREPARING')     |--- Update Order State ->|                         |                           |
       |                             |<-- State Persisted -----|                         |                           |
       |                             |                                                   |                           |
       |                             |--- Broadcast ORDER_PREPARING to 'order:{id}' ---->|                           |
       |<-- 200 OK ------------------|                                                   |-- 'ORDER_PREPARING' ----->|
       |                             |                                                   |   (Timeline updates)      |
```

---

## 5. Event Specifications & Payloads

### 5.1. `ORDER_CREATED`
- **Emitted To**: `kitchen`, `session:{sessionId}`
- **Payload**:
  ```json
  {
    "orderId": "uuid",
    "orderNumber": 1042,
    "tableNumber": 4,
    "guestName": "Alex Doe",
    "items": [
      { "name": "Cappuccino", "quantity": 2, "notes": "Oat milk" },
      { "name": "Avocado Toast", "quantity": 1, "notes": "Extra crispy" }
    ],
    "specialInstructions": "Serve coffee first",
    "createdAt": "2026-09-29T12:35:00.000Z"
  }
  ```

### 5.2. `ORDER_STATUS_CHANGED`
- **Emitted To**: `kitchen`, `session:{sessionId}`, `order:{orderId}`
- **Payload**:
  ```json
  {
    "orderId": "uuid",
    "orderNumber": 1042,
    "tableNumber": 4,
    "previousStatus": "PLACED",
    "currentStatus": "PREPARING",
    "updatedAt": "2026-09-29T12:37:00.000Z"
  }
  ```

### 5.3. `TABLE_STATUS_CHANGED`
- **Emitted To**: `kitchen`, staff floor managers
- **Payload**:
  ```json
  {
    "tableId": "uuid",
    "tableNumber": 4,
    "status": "OCCUPIED",
    "activeSessionId": "uuid",
    "updatedAt": "2026-09-29T12:30:00.000Z"
  }
  ```

---

## 6. Stale Event Handling & Monotonic Ordering

To guard against out-of-order network frames on unstable mobile connections:
1. Every event includes an ISO timestamp (`updatedAt`) and monotonic state sequence value.
2. The Android client discards incoming status updates whose sequence rank is less than or equal to the current local state:
   - `PLACED (1)` -> `ACCEPTED (2)` -> `PREPARING (3)` -> `READY (4)` -> `SERVED (5)`
3. If an invalid or skipping transition is received (e.g. going straight from `PLACED` to `SERVED`), the client triggers an immediate REST query (`GET /api/v1/orders/:id`) to retrieve the authoritative state history.
