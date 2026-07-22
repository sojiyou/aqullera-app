# Aqua-llera Rewrite Plan

> Rewriting the Android app from scratch with Jetpack Compose + MVVM, using the PWA as design reference.

---

## Transfer Guide — Moving to Another Laptop

### Before Zipping (cleanup to shrink file size)

| Location | Action |
|---|---|
| `aquallera-pwa/node_modules/` | Delete (run `npm install` on new machine) |
| `aquallera_web/node_modules/` | Delete if exists |
| `*/build/` directories | Optional — Gradle rebuilds them anyway |
| `*.git/` | Keep — preserves commit history |

### After Unzipping on the New Laptop

| Step | Detail |
|---|---|
| **1.** Delete `aqullera-app/local.properties` | It has the old SDK path (`C:\Users\alumi\...`) — Android Studio recreates it on open |
| **2.** Open `aqullera-app` in Android Studio | It auto-creates `local.properties` pointing to the detected SDK |
| **3.** (PWA) Run `npm install` | Only if you deleted `node_modules` |
| **4.** First build | `gradlew.bat assembleDebug` — slow first time (downloads Gradle + deps) |
| **5.** Install on emulator | `gradlew.bat installDebug` (KB Emulator must be running first) |

### Build & Install Commands

All commands run from inside `aqullera-app/`.

| Action | Command | Notes |
|---|---|---|
| Build APK | `gradlew.bat assembleDebug` | Compiles everything |
| Build + install on emulator | `gradlew.bat installDebug` | Emulator must be running |
| Build without daemon | `gradlew.bat --no-daemon assembleDebug` | Use if you get lock file errors |
| Clean + rebuild | `gradlew.bat clean assembleDebug` | Fresh build from scratch |

First build takes ~2–3 min (cold start). Subsequent builds are faster (~30–60s).

### Troubleshooting Gradle Lock File Error

If you see `Could not create parent directory for lock file`:
- Run PowerShell as Administrator, OR
- Use `--no-daemon` flag

### Expected Folder Structure

```
<parent>/
└── Github/
    ├── aqullera-app/          ← Android project (open this in Android Studio)
    ├── aquallera-pwa/         ← PWA reference design
    ├── aquallera_web/         ← (optional)
    ├── KB Android Emulator/   ← Portable emulator (just run the .exe)
    ├── Portfolio/             ← (your other projects)
    └── ...
```

---

## Stack

| Layer | Choice |
|---|---|
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM + ViewModel + StateFlow |
| Navigation | Jetpack Navigation Compose (single-activity) |
| DI | Hilt |
| Async | Coroutines + Flow |
| Maps | Mapbox SDK v11 (Compose API) |
| Backend | Firebase Auth + Realtime Database |

## Project Structure

```
app/src/main/java/com/example/aquallera/
├── AqualleraApp.kt                    # @HiltAndroidApp
├── MainActivity.kt                    # Single Activity with NavHost
├── navigation/
│   └── NavGraph.kt                    # All route definitions
├── core/
│   ├── di/                            # Hilt modules (Firebase, Database)
│   ├── model/                         # Data models (User, WaterStation, Order)
│   ├── util/                          # Constants, TimeUtils, ErrorUtils
│   └── theme/                         # Compose theme (colors, typography, shapes)
├── auth/                              # Splash → Landing → Login → Signup → VerifyEmail
│   ├── data/                          # AuthRepository
│   └── ui/                            # Screens + AuthViewModel
├── stations/
│   ├── data/                          # StationRepository
│   └── ui/                            # MapScreen + MapViewModel (planned)
├── orders/
│   ├── data/                          # OrderRepository
│   └── ui/                            # OrdersScreen placeholder
├── store/                             # Store details (planned)
├── createorder/                       # Order creation + confirmation (planned)
├── profile/                           # Profile placeholder
├── about/                             # About placeholder
└── components/                        # Shared composables (BottomNav, cards, etc.)
```

## Phases

### Phase 0 — Prep (safe checkpoint)
- [x] `git checkout -b rewrite/compose`
- [x] ~~Verify `gradlew assembleDebug` works from CLI~~ *(skipped — old XML app had pre-existing bugs)*
- [x] Confirm emulator is reachable

### Phase 1 — Scaffolding
- [x] Update `gradle/libs.versions.toml` — Compose BOM, Navigation Compose, Hilt
- [x] Update `build.gradle.kts` files for Compose + Hilt + ksp
- [x] Create `AqualleraApp.kt` (`@HiltAndroidApp`)
- [x] Create `MainActivity.kt` (single activity with `setContent`)
- [x] Set up `NavGraph.kt` with all 14 routes (initially placeholders)
- [x] Set up Hilt DI modules (Firebase, Database refs)
- [x] Set up Material3 theme matching PWA colors

### Phase 2 — Shared Foundation
- [x] Data models: `User`, `WaterStation`, `Order`
- [x] Firebase repository layer: `AuthRepository`, `StationRepository`, `OrderRepository`
- [x] Shared composables: `BottomNavBar`, `LoadingIndicator`, `ErrorDialog`, `AqualleraInput`, `PrimaryButton`, `LinkButton`, `WaveBackground`, `WaterStationCard`, `OrderTicketItem`, `ReceiptCard`
- [x] Utilities: error mapping, time formatting, constants

### Phase 3 — Auth Flow (5 screens)
- [x] `SplashScreen` + `AuthViewModel`
- [x] `LandingScreen`
- [x] `LoginScreen`
- [x] `SignupScreen`
- [x] `VerifyEmailScreen`

##### Phase 4 — Core Features (5 screens)
- [x] `MapScreen` + `MapViewModel` — map + draggable station list
- [x] `StoreDetailsScreen` + `StoreViewModel`
- [x] `CreateOrderScreen` + `CreateOrderViewModel`
- [x] `OrderConfirmationScreen` + `OrderConfirmationViewModel`
- [x] `OrderSuccessScreen`
- [x] `MapScreen` + `MapViewModel`
- [x] `StoreDetailsScreen` + `StoreViewModel`
- [x] `CreateOrderScreen` + `CreateOrderViewModel`
- [x] `OrderConfirmationScreen` + `OrderConfirmationViewModel`
- [x] `OrderSuccessScreen`

### Phase 5 — Remaining Screens (4 screens)
- [x] OrdersScreen — implemented with order list, expandable cards, status badges
- [x] ProfileScreen — implemented with avatar, user info, action rows
- [x] `EditProfileScreen` + `EditProfileViewModel`
- [x] AboutScreen — full PWA design with logo, tagline, version, info sections, link to map

### Phase 6 — Services & Polish
- [x] NotificationListener as Compose side-effect (order status toasts)
- [x] EmailService integration (EmailJS order confirmation)
- [x] Error handling, loading states, empty states
- [ ] End-to-end testing with emulator

## API / Screens Reference

| Screen | PWA File | Compose Screen | Status |
|---|---|---|---|---|
| Splash | `Splash.jsx` | `SplashScreen` | ✅ Done |
| Landing | `Landing.jsx` | `LandingScreen` | ✅ Done |
| Login | `Login.jsx` | `LoginScreen` | ✅ Done |
| Signup | `Signup.jsx` | `SignupScreen` | ✅ Done |
| Verify Email | `VerifyEmail.jsx` | `VerifyEmailScreen` | ✅ Done |
| About | `About.jsx` | `AboutScreen` | ✅ Done |
| Maps | `Maps.jsx` | `MapScreen` | ✅ Done |
| Store Details | `StoreDetails.jsx` | `StoreDetailsScreen` | ✅ Done |
| Create Order | `CreateOrder.jsx` | `CreateOrderScreen` | ✅ Done |
| Order Confirmation | `OrderConfirmation.jsx` | `OrderConfirmationScreen` | ✅ Done |
| Order Success | `OrderSuccess.jsx` | `OrderSuccessScreen` | ✅ Done |
| Orders | `Orders.jsx` | `OrdersScreen` | ✅ Done |
| Profile | `Profile.jsx` | `ProfileScreen` | ✅ Done |
| Edit Profile | `EditProfile.jsx` | `EditProfileScreen` | ✅ Done |

---

## Session Summary (Jul 15, 2026)

### Map
- Fixed `isOnline` Firebase annotation (`@get:@set:PropertyName`)
- Added custom water drop marker icon (`ic_station_marker.png`, 512×512 → 96×96)
- Added zoom-based icon scaling (16–48px) and station name labels (visible at zoom ≥13)
- Added runtime contrast adjustment for marker bitmap

### Orders Tab
- Created `OrdersViewModel` — fetches orders via `OrderRepository.getOrdersByUser()`
- Built `OrdersScreen` — `LazyColumn` of expandable cards with color-coded status badges, item details, delivery address
- Added per-child try-catch in `getOrdersByUser()` to handle PWA orders with type-mismatched fields (e.g. `createdAt` as ISO string vs Long) — skips bad orders instead of crashing

### Profile Tab
- Created `ProfileViewModel` — loads user from Firebase RTDB `users/{uid}`, handles sign-out
- Built `ProfileScreen` — avatar circle with initials, name/email/phone, action rows (Edit Profile, My Orders, About, Sign Out)
- Created `EditProfileScreen` + `EditProfileViewModel` — matches PWA layout (Name, Email, Phone cards + Save Changes)
- Fixed `AuthRepository.updateUserProfile()` to also call `FirebaseUser.updateProfile()` so new name propagates to Firebase Auth displayName (used in order creation)

### Auth Bug Fix
- Replaced one-shot `checkAuthState()` in `AuthViewModel` with Firebase `onAuthStateChanged` listener
- Sign-out from ProfileScreen now correctly sets `Unauthenticated`; re-login works without stale state / premature redirect

### Notifications & Email
- Created `NotificationListener` composable — watches `/orders` via `ChildEventListener`, shows Android Toast on status change (e.g. "Order AQ-1234 is now On Delivery")
- Created `EmailService` — calls EmailJS REST API to send order confirmation emails (fire-and-forget after `confirmOrder()`)
- Mounted `NotificationListener` in `MainActivity.kt` at the app level

### Firebase Rules
- Created `database.rules.json` with `.indexOn` for: `orders/userId`, `users/{number,email}`, `waterStations/{email,isOnline}`
- User's existing rules updated: added `"orders": { ".indexOn": "userId" }`

### UI Polish
- Added `Footer` composable (brand, contact info, copyright) — matches PWA design
- Added footer to `LandingScreen`
- Fixed empty/error states: `StoreDetailsScreen` ("Station not found"), `OrderConfirmationScreen` ("No order data available"), `ProfileScreen` (error shown inline)

### Files Changed
- **New files:** `OrdersViewModel.kt`, `OrdersScreen.kt`, `ProfileViewModel.kt`, `ProfileScreen.kt`, `EditProfileViewModel.kt`, `EditProfileScreen.kt`, `NotificationListener.kt`, `Footer.kt`, `EmailService.kt`, `database.rules.json`
- **Modified files:** `MapScreen.kt`, `AuthRepository.kt`, `AuthViewModel.kt`, `OrderRepository.kt`, `OrderConfirmationViewModel.kt`, `OrderConfirmationScreen.kt`, `StoreDetailsScreen.kt`, `ProfileScreen.kt`, `LandingScreen.kt`, `NavGraph.kt`, `MainActivity.kt`, `Constants.kt`, `PLAN.md`

---

## Part 2 — PWA Design Alignment

Polish the Android UI to match the PWA reference designs.

### Map Screen — Station List (`WaterStationCard`)
- [x] Show ₱ prices per water type (Pure/Spring/Mineral) with colored badges on each station card
- [x] Change status label from "Open"/"Closed" to "Online"/"Offline" with green/red dot indicator
- [x] Display distance from user on each station card
- [x] Add filter bar (All/Online/Offline/Pending) above the station list
- [x] Add "How to Order" dialog
- [x] Add user location button + blue dot marker on map
- [x] Show mini info card overlay at top when a station is selected on the map

### Store Details
- [x] Header badges: show Online/Offline + Approved/Pending statuses
- [x] Service types: "Pickup & Delivery" subtitle + individual Capability chips
- [x] Pricing in 3-column grid layout (Pure / Spring / Mineral)
- [x] Delivery Days section + Delivery Hours time-slot chips
- [x] "Return" + "Order Now" buttons at bottom

### Create Order
- [x] Address search with Mapbox geocoding (text input + suggestions)
- [x] GPS location button to auto-fill address
- [x] Preferred date selector: calendar for pickup, day grid for delivery
- [x] Delivery time slot selection
- [x] Order preview screen before submitting

### Orders / Profile
- [ ] Auto-cancel orders when a station is revoked
- [ ] Email verification status badge + resend button on Profile

### Phase 7 — Part 2 Implementation
- [x] WaterStationCard: add pricing display with colored badges
- [x] WaterStationCard: status → Online/Offline with dot
- [x] WaterStationCard: distance display
- [x] MapScreen: filter bar
- [x] MapScreen: "How to Order" dialog
- [x] MapScreen: user location button + blue dot
- [x] MapScreen: selected-station mini info card
- [x] StoreDetailsScreen: dual status badges (Online/Offline + Approved/Pending)
- [x] StoreDetailsScreen: service type chips (Pickup & Delivery subtitle + OrderListBg chips)
- [x] StoreDetailsScreen: 3-column pricing grid
- [x] StoreDetailsScreen: delivery days + hours chips
- [x] StoreDetailsScreen: action buttons layout (Return + Order Now)
- [x] StoreDetailsScreen: BusinessHoursCard open/close format support
- [x] WaterStationCard: add "View on Map" + "Order Now" buttons
- [x] MapScreen: fly-to-station on "View on Map" button
- [x] CreateOrderScreen: address search with Mapbox
- [x] CreateOrderScreen: GPS location button
- [x] CreateOrderScreen: date selector
- [x] CreateOrderScreen: time slot picker
- [x] CreateOrderScreen: order preview
- [ ] OrdersScreen: auto-cancel revoked stations
- [ ] ProfileScreen: email verification badge + resend

---

## Session Summary (Jul 22, 2026) — Part 2: Map Screen Polish

### What Changed
- **WaterStationCard**: Shows ₱ prices per water type in a `OrderListBg`-colored badge row. Status label changed from "Open"/"Closed" to "● Online" (green dot) / "● Offline" (orange dot) / "Pending" (red dot). Accepts `userLat`/`userLng` to calculate and display distance (e.g. "1.2 km away").
- **MapViewModel**: Added `StationFilter` enum (All/Online/Offline/Pending), `selectedStation`, `userLat`/`userLng` to `MapUiState`. Added computed `filteredStations`. New methods: `setFilter()`, `selectStation()`, `setUserLocation()`.
- **MapScreen — Header**: "How to Order" button at top-right opens a `HowToOrderDialog` (5-step guide, midnight-blue theme matching PWA).
- **MapScreen — Location**: Crosshair location button (top-right overlay) requests `ACCESS_FINE_LOCATION` permission at runtime, then fetches last-known location via `LocationManager` — sets blue dot not yet implemented (user marker pending).
- **MapScreen — Filter**: `FilterChipsRow` composable above the station list with pill-style chips (MidnightBlue selected, MistGray unselected).
- **MapScreen — Mini Card**: When a map marker is tapped, a white info card appears at the top showing station name, address, "View" button (→ StoreDetails), and ✕ dismiss.
- **MapScreen — Markers**: Marker `clickListener` now selects the station (via `PointAnnotationManager.addClickListener`).

### Files Changed
- **Modified:** `MapScreen.kt`, `MapViewModel.kt`, `WaterStationCard.kt`, `PLAN.md`

---

## Session Summary (Jul 22, 2026) — Part 2: Store Details, Real-Time & Card Buttons

### StoreDetailsScreen — PWA Alignment
- **HeaderSection**: Midnight-blue background with dual status pill badges — Online/Offline (green/orange) + Approved/Pending (green/orange)
- **ServicesCard**: Added subtitle ("Pickup & Delivery" / "Delivery Only" / "Pickup Only"). Chips changed to `OrderListBg` bg with `MidnightBlue` text
- **PricingCard**: Title → "Price List". 3-column grid (Pure/Spring/Mineral) using `OrderListBg` rounded cells. Spring falls back to `pricing_liter_spring`
- **DeliveryDaysCard** + **DeliveryHoursCard**: Split from single card, chips with `OrderListBg` bg
- **ActionButtons**: "Return" (OutlinedButton) + "Order Now" (Button, 50% opacity if not approved)
- **BusinessHoursCard**: Added `open`/`close` key detection — single row format when present, per-day fallback

### Real-Time Status Updates
- **StationRepository**: Added `getStationByIdFlow()` and `getAllStationsFlow()` using `callbackFlow` (Firebase `addValueEventListener`) replacing one-shot `.get().await()`
- **StoreViewModel**: Collects from flow — website changes (online/offline) reflect instantly
- **MapViewModel**: Collects from flow — station list auto-updates
- **TimeUtils**: Added `to12Hour()` matching PWA format (e.g. "09:00" → "9:00 AM")

### WaterStationCard — Action Buttons
- Added `onViewOnMap` (flies camera) and `onOrderNow` (→ CreateOrder) buttons
- "Order Now" dimmed at 50% opacity when not approved

### MapScreen — Fly-To
- `MapUiState` gains `flyToStationId`; `MapboxMapView` flies camera to station coordinates on change

### Files Changed
- **Modified:** `WaterStationCard.kt`, `StoreDetailsScreen.kt`, `StoreViewModel.kt`, `MapViewModel.kt`, `MapScreen.kt`, `StationRepository.kt`, `TimeUtils.kt`, `NavGraph.kt`, `PLAN.md`

---

## Session Summary (Jul 22, 2026) — Part 2: CreateOrderScreen, Reference ID, Install

### CreateOrderScreen — Complete PWA Alignment
- **Address Search**: Mapbox geocoding text input with debounced suggestions dropdown
- **GPS Location**: Button requests location permission, fetches location, reverse-geocodes via Mapbox → fills address
- **Delivery Day Grid**: 7-day grid starting from today, day name/number chips, next-day minimum
- **Pickup Date Picker**: DatePicker dialog for pickup orders
- **Time Slot Picker**: For delivery — time chips generated from station business hours (1h slots). For pickup — same hours as chips
- **Order Preview**: Full preview screen with Edit Order / Proceed buttons, fee breakdown (water subtotal, delivery fee, 2% transaction fee, grand total)

### Reference Number — Fixed
- Removed random `AQ-{timestamp}-{random}` client-side generation
- `OrderRepository.createOrder()` now atomically increments `/orderCounter/{yyyyMMdd}` via Firebase `runTransaction`
- Format: `{stationPrefix}-{yyyyMMdd}-{counter}` (e.g. `SOJ-20260713-0003`) matching PWA backend

### Files Changed
- **Modified:** `CreateOrderScreen.kt`, `CreateOrderViewModel.kt`, `OrderConfirmationScreen.kt`, `OrderConfirmationViewModel.kt`, `PendingOrderStore.kt`, `OrderRepository.kt`, `PLAN.md`
