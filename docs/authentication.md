# CafeApp — Authentication, Authorization & Security Architecture

## 1. Authentication Architecture Overview

CafeApp implements a zero-trust, stateless token-based authentication architecture backed by cryptographically secure token rotation and multi-tier role-based access control (RBAC).

```
+-----------------------------------------------------------------------------------+
|                              AUTHENTICATION SYSTEM                                |
|                                                                                   |
|   +--------------------------+             +----------------------------------+   |
|   |   Short-Lived Access     |             |      Long-Lived Refresh          |   |
|   |   Token (JWT)            |             |      Token (Opaque)              |   |
|   |   - Lifespan: 15 minutes |             |      - Lifespan: 30 days         |   |
|   |   - Cryptographic claims |             |      - 32-byte CSPRNG Hex        |   |
|   |   - Verified via secret  |             |      - SHA-256 Hashed in DB      |   |
|   |   - Never hits database  |             |      - Rotated on every use      |   |
|   +--------------------------+             +----------------------------------+   |
+-----------------------------------------------------------------------------------+
```

---

## 2. Password Security & Hashing

Passwords must never be stored in plaintext. CafeApp implements **Argon2id** (OWASP recommended) with an automated **bcrypt** fallback for legacy and memory-constrained environments:

```typescript
// Argon2id Configuration Parameters
const ARGON2_CONFIG = {
  type: argon2.argon2id,
  timeCost: 3,        // 3 iterations
  memoryCost: 65536,  // 64 MB memory cost
  parallelism: 4      // 4 parallel threads
};

// Automatic Hash Verification Dispatcher
export async function verifyPassword(password: string, storedHash: string): Promise<boolean> {
  if (storedHash.startsWith("$argon2")) {
    return argon2.verify(storedHash, password);
  }
  return bcrypt.compare(password, storedHash);
}
```

---

## 3. Token Rotation & Reuse Detection (Family Revocation)

To protect against token theft, refresh tokens are **rotated on every single use**.

### 3.1. Database Schema Representation
```
RefreshToken Table:
- id: UUID (PK)
- userId: UUID (FK)
- tokenHash: VARCHAR(64) (SHA-256 unique hash of raw token)
- expiresAt: TIMESTAMP
- revokedAt: TIMESTAMP (Nullable)
- replacedById: UUID (Nullable, points to newly issued token)
```

### 3.2. Rotation & Reuse Detection Algorithm
1. **Client Request**: Client sends raw `refreshToken` to `POST /api/v1/auth/refresh`.
2. **Hash Lookup**: Server calculates `tokenHash = SHA256(refreshToken)` and queries the database.
3. **Validation Checks**:
   - If record not found, expired, or `revokedAt != null` without replacement -> **Reject (401 Unauthorized)**.
   - **Reuse Detection Trigger**: If `replacedById != null`, the token has already been consumed once. This indicates a potential replay attack or stolen credential. The server immediately **revokes all refresh tokens** for that `userId` (family revocation), invalidating the entire user session.
4. **Successful Rotation**:
   - Generate new CSPRNG raw refresh token (32 bytes hex).
   - Insert new `RefreshToken` record.
   - Update original `RefreshToken` record: `revokedAt = NOW()`, `replacedById = newToken.id`.
   - Sign and issue new 15-minute JWT Access Token.
   - Return `{ accessToken, refreshToken }` to client.

---

## 4. Role-Based Access Control (RBAC)

### 4.1. Role Definitions & Hierarchy

```
                   [ CAFE_ADMIN ]
                         |
           +-------------+-------------+
           |                           |
     [ CAFE_STAFF ]               [ KITCHEN ]
           |
     [ CUSTOMER ]
```

| Feature / Domain | `CUSTOMER` | `CAFE_STAFF` | `KITCHEN` | `CAFE_ADMIN` |
| :--- | :---: | :---: | :---: | :---: |
| Browse Menu & View Offers | YES | YES | YES | YES |
| Book Table Reservations | Self Only | All | Read Only | All |
| Scan QR & Open Table Session | YES | YES | NO | YES |
| Place Dine-in Orders | Active Session | Active Session | NO | Active Session |
| View Customer Order History | Self Only | All Tables | KDS Queue Only | All |
| Update Order Status (Prep/Ready)| NO | YES | YES | YES |
| Cancel Orders / Issue Refunds | NO | With Reason | NO | YES |
| Manage Menu, Categories, Items | NO | Availability Only| Availability Only| Full CRUD |
| Manage Offers & Discounts | NO | View Only | NO | Full CRUD |
| Table Management & QR Tokens | NO | View / Clean | NO | Full CRUD |
| Staff Management & Role Assign | NO | NO | NO | YES |
| Edit Cafe Operational Profile | NO | NO | NO | YES |

### 4.2. Express Server-Side RBAC Middleware
```typescript
export function requireRole(...allowedRoles: UserRole[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    const user = req.user; // Populated by JWT authentication middleware
    if (!user) {
      return res.status(401).json({ error: "Authentication required" });
    }
    if (!allowedRoles.includes(user.role)) {
      return res.status(403).json({ error: "Access denied: insufficient permissions" });
    }
    next();
  };
}
```

---

## 5. Android Client-Side Security & Token Storage

1. **Encrypted DataStore**: Access tokens, refresh tokens, and basic user claims are stored using Jetpack DataStore Preferences (`SessionPreferences`).
2. **OkHttp Authenticator & Interceptor**:
   - `AuthInterceptor`: Attaches `Authorization: Bearer <accessToken>` to outgoing requests.
   - `TokenAuthenticator`: Intercepts `401 Unauthorized` responses, executes a synchronous token refresh flow, updates DataStore, and retries the failed request transparently.
   - If refresh fails, `SessionPreferences.clearAll()` is invoked, redirecting user to Login.
3. **Role-Based Navigation Guard**: The top-level Compose `NavHost` inspects the authenticated role from `SessionPreferences` to mount the appropriate presentation graph (`CustomerRoot` vs `StaffRoot` vs `KitchenRoot`).
