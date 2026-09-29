# CafeApp — Android Application Structure & Guidelines

## 1. Multi-Module Architecture

The Android codebase is structured into feature and core modules to enforce strict separation of concerns, improve build parallelization, and enable isolated testing.

```
                      +-------------------+
                      |       :app        |
                      +-------------------+
                        /        |        \
         +-------------+         |         +-------------+
         |                       |                       |
         v                       v                       v
+-----------------+     +-----------------+     +-----------------+
|  :feature:auth  |     |  :feature:menu  |     | :feature:orders |  ... (all features)
+-----------------+     +-----------------+     +-----------------+
         \                       |                       /
          +----------------------+----------------------+
                                 |
                                 v
+-----------------------------------------------------------------+
|                          :core:ui                               |
|            (Material 3 Design System, Themes, Icons)            |
+-----------------------------------------------------------------+
                                 |
  +--------------+---------------+--------------+--------------+
  |              |               |              |              |
  v              v               v              v              v
:core:common   :core:network   :core:database  :core:datastore :core:realtime
```

---

## 2. Module Directory Layout & Invariants

Every `:feature:*` module follows a strict 3-tier internal layout:

```
feature/<feature_name>/src/main/java/com/cafeapp/feature/<feature_name>/
├── data/
│   ├── api/                    # Retrofit service interface
│   ├── dto/                    # Network DTO models with @JsonClass(generateAdapter=true)
│   ├── mapper/                 # Extension functions mapping DTOs <-> Domain models
│   ├── repository/             # Repository implementation (coordinates Network + DB)
│   └── di/                     # Hilt @Module binding Repository interfaces
├── domain/
│   ├── model/                  # Pure Kotlin domain entities
│   ├── repository/             # Pure Kotlin repository interface contracts
│   └── usecase/                # Single-responsibility business logic interactors
└── ui/
    ├── <ScreenName>Screen.kt   # Jetpack Compose UI layout + Previews
    ├── <ScreenName>ViewModel.kt# Hilt ViewModel emitting StateFlow<ScreenState>
    └── <ScreenName>Contract.kt # Sealed interfaces for ScreenState and ScreenEvent
```

### Module Boundary Rules
1. **Zero Feature-to-Feature Dependencies**: `:feature:cart` cannot import `:feature:menu`. Inter-feature navigation and data passing are orchestrated at the `:app` level or through shared core domain repositories.
2. **Pure Domain Layer**: `domain/` packages must never import Android framework classes (`android.os.*`, `android.content.*`, `androidx.compose.*`).
3. **No Business Logic in UI**: Compose composable functions must only render given state objects and pass user interactions upstream as events to the ViewModel.

---

## 3. Navigation Architecture & Role Graphs

CafeApp uses **Jetpack Navigation Compose** with type-safe route definitions.

```
                           [ Splash Screen ]
                                  |
                   (Inspects SessionPreferences)
                                  |
           +----------------------+----------------------+
           |                                             |
   [ Unauthenticated ]                            [ Authenticated ]
           |                                             |
     [ Auth Graph ]                         (Checks Stored User Role)
     ├── LoginScreen                                     |
     └── RegisterScreen                +-----------------+-----------------+
                                       |                 |                 |
                                       v                 v                 v
                                [ Customer Graph ] [ Staff Graph ]  [ Kitchen Graph ]
```

### 3.1. Customer Navigation Destinations
- `CustomerRoot` (Bottom Navigation scaffold):
  - `MenuHomeScreen`: Categories, bestsellers, search, dietary filters.
  - `OffersScreen`: Active promos, coupon code cards.
  - `ReservationsScreen`: Table booking calendar, party size picker.
  - `ProfileScreen`: Customer details, order history, active session status.
- Nested / Modal Flows:
  - `ItemDetailsScreen(itemId)`: Quantity selection, item customization notes.
  - `CartScreen`: Subtotal, coupon input, GST calculation, special cooking notes.
  - `QRScannerScreen`: CameraX scanner targeting table QR codes.
  - `ActiveSessionScreen`: Active table dining session, participants, order button.
  - `OrderTrackingScreen(orderId)`: Real-time progress timeline (`PLACED` -> `SERVED`).

### 3.2. Staff & Admin Navigation Destinations
- `StaffDashboard`: Real-time floor overview, table occupancy grid.
- `TableManagementScreen`: Create/edit tables, regenerate QR tokens.
- `ReservationsAdminScreen`: Accept/reject table reservation requests.
- `LiveOrdersScreen`: All active dining orders across tables.
- `MenuAdminScreen`: Edit items, pricing, toggle stock availability.

### 3.3. Kitchen Navigation Destinations
- `KitchenDisplayScreen`: Live multi-column KDS ticket grid (`PLACED`, `ACCEPTED`, `PREPARING`, `READY`).
- `TicketDetailsDialog`: Special cooking instructions, prep timers, item checklist.

---

## 4. State Management (MVI / Unidirectional Data Flow)

Every feature screen follows a strict Contract-ViewModel-Composable pattern:

```kotlin
// Contract Definition
sealed interface MenuUiState {
    object Loading : MenuUiState
    data class Success(
        val categories: List<MenuCategory>,
        val selectedCategoryId: String?,
        val items: List<MenuItem>,
        val searchQuery: String
    ) : MenuUiState
    data class Error(val message: String) : MenuUiState
}

sealed interface MenuUiEvent {
    data class CategorySelected(val categoryId: String) : MenuUiEvent
    data class SearchQueryChanged(val query: String) : MenuUiEvent
    data class AddToCartClicked(val item: MenuItem) : MenuUiEvent
    object RetryClicked : MenuUiEvent
}

// Composable Signature
@Composable
fun MenuScreen(
    viewModel: MenuViewModel = hiltViewModel(),
    onNavigateToDetails: (String) -> Unit,
    onNavigateToCart: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MenuContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToDetails = onNavigateToDetails,
        onNavigateToCart = onNavigateToCart
    )
}
```

---

## 5. CameraX & ML Kit QR Scanner Architecture

The `:feature:session` module hosts the QR scanning engine:
- **CameraX 1.4.1**: Provides lifecycle-aware camera preview and image analysis pipeline.
- **Google ML Kit Barcode Scanning 17.3.0**: Runs on background coroutines processing camera frames to detect QR barcodes.
- **Validation**: Scanned payload is parsed and immediately verified against `POST /api/v1/tables/verify-qr` before opening the table session.
