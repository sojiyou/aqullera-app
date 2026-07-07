# Aqua-llera — Android App

Native Android app (Kotlin + XML) for Baguio City residents to locate nearby water refilling stations and order clean water for delivery or pickup. Pixel-replica conversion of the [Aqua-llera PWA](https://github.com/anomalyco/aquallera-pwa). Part of a capstone project.

| Component | Purpose |
|---|---|
| **Android App (this)** | Customer-facing native app |
| **PWA** | Cross-platform customer-facing app (React) |
| **Admin Website** | Dashboard for water station owners |

All three share the same Firebase project + Realtime Database.

---

## Tech Stack

- **Kotlin** — language
- **XML layouts** — no Compose
- **Firebase Auth** — email/password authentication
- **Firebase Realtime Database** — users, orders, water stations, order counter
- **Mapbox Maps SDK v11** — interactive maps with station markers
- **Google Play Services Location** — user location
- **Material Components** — TextInputLayout (floating labels), MaterialCardView
- **Coroutines** — async operations
- **Nunito** — font (matching PWA)

---

## Screens (14 Activities)

| Screen | Activity | Key Features |
|---|---|---|
| Splash | `SplashActivity` | Logo + tagline + 1.5s delay + auth check |
| Landing | `MainActivity` | Logo, Login/Signup buttons, tagline, footer |
| Login | `LoginActivity` | Wave gradient bg, floating inputs, Firebase Auth |
| Signup | `SignupActivity` | Wave gradient bg, floating inputs, duplicate checks |
| Verify Email | `VerifyEmailActivity` | Resend/refresh, auto-redirect |
| About | `AboutActivity` | Sections + BottomNav |
| Maps | `MapActivity` | Full-screen MapView, draggable bottom sheet, custom markers |
| Store Details | `StoreDetailsActivity` | Hours, services, prices grid, about, badges |
| Create Order | `CreateOrderActivity` | Water type qty, Delivery/Pickup, Mapbox Geocoding, date/time |
| Order Confirmation | `OrderConfirmationActivity` | Receipt card, fee breakdown, grand total |
| Order Success | `OrderSuccessActivity` | Green checkmark, order details, action buttons |
| Orders | `OrdersActivity` | Ticket list, 7 status colors, click to details |
| Profile | `ProfileActivity` | Avatar, account card, email verification, action buttons |
| Edit Profile | `EditProfileActivity` | Card inputs, save changes |

---

## Firebase Data Model

Same as the PWA. Root paths:

| Path | Purpose |
|---|---|
| `/users/{uid}` | User profiles: `{ uid, fullName, email, number, createdAt }` |
| `/waterStations/{stationId}` | Station profiles with pricing, hours, status, `about` |
| `/orders/{orderId}` | Orders with full line-item breakdown + fees |
| `/orderCounter/{yyyyMMdd}` | Daily auto-incrementing counter |

Order ID format: `{stationPrefix}-{yyyyMMdd}-{counter}` (e.g. `AQU-20260427-0042`).

Order includes `transactionFee` (2% of subtotal + delivery fee).

---

## PWA → Android Comparison

### Pages (14/14)

| PWA Page | Android Activity | Status |
|---|---|---|
| `Splash.jsx` | `SplashActivity` | ✅ logo without text |
| `Landing.jsx` | `MainActivity` | ✅ |
| `Login.jsx` | `LoginActivity` | ✅ wave gradient + floating inputs |
| `Signup.jsx` | `SignupActivity` | ✅ wave gradient + floating inputs |
| `VerifyEmail.jsx` | `VerifyEmailActivity` | ✅ |
| `About.jsx` | `AboutActivity` | ✅ |
| `Maps.jsx` | `MapActivity` | ✅ pixel-match |
| `StoreDetails.jsx` | `StoreDetailsActivity` | ✅ |
| `CreateOrder.jsx` | `CreateOrderActivity` | ✅ pixel-match |
| `OrderConfirmation.jsx` | `OrderConfirmationActivity` | ✅ pixel-match |
| `OrderSuccess.jsx` | `OrderSuccessActivity` | ✅ |
| `Orders.jsx` | `OrdersActivity` | ✅ pixel-match |
| `Profile.jsx` | `ProfileActivity` | ✅ pixel-match |
| `EditProfile.jsx` | `EditProfileActivity` | ✅ |

### Components (7/7)

| PWA Component | Android Layout | Status |
|---|---|---|
| `BottomNav.jsx` | `bottom_navigation.xml` | ✅ |
| `Footer.jsx` | `footer.xml` | ✅ |
| `FloatingInput.jsx` | `TextInputLayout` + `TextInputEditText` | ✅ |
| `HowToOrderDialog.jsx` | `dialog_how_to_order.xml` | ✅ |
| `OrderReceipt.jsx` | `receipt_layout.xml` | ✅ |
| `OrderTicketItem.jsx` | `order_ticket_item.xml` | ✅ |
| `WaterStationCard.jsx` | `item_water_station.xml` | ✅ |

### Services & Utils (5/5)

| PWA File | Android File | Status |
|---|---|---|
| `firebase.js` | `FirebaseConfig.kt` | ✅ |
| `emailjs.js` | `EmailService.kt` | ✅ |
| `haversine.js` | `WaterStation.kt` (inline) | ✅ |
| `errors.js` | `ErrorUtils.kt` | ✅ |
| `formatTime.js` | `TimeUtils.kt` | ✅ |

### Assets & Design

| PWA Asset | Android Equivalent | Status |
|---|---|---|
| Tailwind color palette | `res/values/colors.xml` | ✅ |
| `logo.png` | `aqua_llera_logo.png` | ✅ |
| `logo-no-name.png` | `aqua_llera_logo_no_name.png` | ✅ |
| `ic_water_drop.svg` | `ic_water_drop.xml` | ✅ |
| Nunito font | `res/font/nunito.ttf` | ✅ |
| Wave SVG (Login/Signup) | `ic_wave.xml` + `login_bg.xml` | ✅ |
| button/input backgrounds | 55+ XML drawables | ✅ |

---

## Conversion Phases

### Phase 1 — Foundation
- Kotlin + XML project scaffolded, all dependencies configured (Firebase, Mapbox, Material Components, Coroutines)
- Firebase Auth + Realtime Database integration (`FirebaseConfig.kt`)
- Data models: `User.kt`, `WaterStation.kt`, `Order.kt` (with `transactionFee`)
- Utility classes: `TimeUtils.kt`, `ErrorUtils.kt`, `Constants.kt`, `EmailService.kt`
- Navigation: `AndroidManifest.xml` with all 14 activities

### Phase 2a — Design System
- **Colors** — `midnight_blue` (#191970), `blue` (#015084), `light_yellow` (#E5C95F), `app_bg` (#FFFCF2), status colors (Pending → Cancelled)
- **Font** — Nunito via `res/font/nunito.ttf`
- **Drawables (55+ XML)** — buttons (`btn_primary`, `btn_secondary`, `btn_logout_bg`), cards (`card_bg`, `rounded_corner_white`, `text_card_background`), input fields (`gray_input_field_bg`), status badges (`badge_bg`, `status_pending_bg`, `status_confirmed_bg`, etc.), status pills (`status_pill_*`), toggle buttons (`toggle_delivery_bg`, `toggle_pickup_bg`), backgrounds (`amber_warning_bg`, `green_verified_bg`, `green_circle_bg`, `circle_avatar_bg`), icons (`ic_water_drop`, `ic_my_location`, `ic_back_arrow`, `ic_clear`, `ic_location`, `ic_phone`, `ic_time`, `ic_service`), wave gradient (`ic_wave`, `login_bg`)
- **Shared layouts** — `bottom_navigation.xml` (3-tab: Map/Orders/Profile), `footer.xml`, `dialog_how_to_order.xml`, `item_water_station.xml`, `order_ticket_item.xml`, `receipt_layout.xml`
- **Introduced PWA wave gradient + floating label inputs** on Login/Signup screens

### Phase 2b — Core Screen Redesigns

| Screen | Changes |
|---|---|
| **MapActivity** | Full-screen MapView, HowToOrder header, user-location FAB, custom water-drop markers with zoom-scaled labels, draggable bottom sheet (15%/55%/85% snap), station flyTo, marker-to-sheet interaction |
| **CreateOrderActivity** | Midnight-blue header, card-based form, Mapbox Geocoding autocomplete, Delivery/Pickup toggle buttons, +/- quantity controls, date picker, time slot buttons, preview overlay with fee breakdown + 2% transaction fee, email verification warning |
| **OrderConfirmationActivity** | Midnight-blue header, receipt-style card (AQUA-LLERA title, order type badge, line-item columns, fee breakdown, grand total, customer details) |
| **OrdersActivity** | Midnight-blue header, progress bar, empty state, scrollable order ticket rows with 7 colored status pills, items summary via `buildItemsSummary()`, click-to-navigate to OrderSuccess |
| **ProfileActivity** | Avatar initial circle (80dp midnight-blue), Account Details card with justify-between layout, Email Verification card (green verified or amber warning w/ Resend + Refresh), 5 action buttons (Edit/Maps/Orders/About/Log Out red) |

### Phase 3 — Remaining Screens & Polish

| Screen | Changes |
|---|---|
| **EditProfileActivity** | Midnight-blue header with back button, 3 card-form inputs (Name/Email/Phone), Save Changes button (`btn_primary`); uses Firebase Auth `uid`; removed password fields |
| **OrderSuccessActivity** | Green checkmark circle, "Order Placed!" heading, simplified Order Details card (Reference/Station/Type/Date/divider/Total), "You can track your order status" note, "Back to Maps" + "View My Orders" buttons; removed Cancel Order + real-time listener |
| **StoreDetailsActivity** | Midnight-blue header + back, station name/address/status badge, Business Hours card (7 days, today bold), Offered Services chips, Delivery Hours chips (conditional), Price List 3-column grid (Pure/Spring/Mineral), About card (conditional), Return + Order Now buttons; `WaterStation.kt` updated with `about` field |
| **VerifyEmailActivity** | Already matched PWA — no changes needed |
| **Login & Signup** | Wave gradient background (`login_bg` layer-list), decorative wave SVG overlay (`ic_wave`), floating label inputs (`TextInputLayout` + `TextInputEditText`), back buttons, color-matched to PWA |

---

## Design System

- **Colors** — `midnight_blue` (#191970), `blue` (#015084), `light_yellow` (#E5C95F), `app_bg` (#FFFCF2)
- **Font** — Nunito via `res/font/nunito.ttf`
- **Drawables** — 55+ XML drawables: buttons, cards, badges, status pills, icons, wave background
- **Status colors** — 7 distinct status pills (Pending → Cancelled)

---

## Project Structure

```
app/src/main/
├── java/com/example/aquallera/
│   ├── SplashActivity.kt
│   ├── MainActivity.kt
│   ├── LoginActivity.kt
│   ├── SignupActivity.kt
│   ├── VerifyEmailActivity.kt
│   ├── AboutActivity.kt
│   ├── MapActivity.kt
│   ├── StoreDetailsActivity.kt
│   ├── CreateOrderActivity.kt
│   ├── OrderConfirmationActivity.kt
│   ├── OrderSuccessActivity.kt
│   ├── OrdersActivity.kt
│   ├── ProfileActivity.kt
│   ├── EditProfileActivity.kt
│   ├── FirebaseConfig.kt
│   ├── EmailService.kt
│   ├── ErrorUtils.kt
│   ├── TimeUtils.kt
│   ├── Constants.kt
│   ├── User.kt
│   ├── WaterStation.kt
│   ├── Order.kt
│   └── WaterStationAdapter.kt
├── res/
│   ├── drawable/          (55+ XML drawables + PNGs)
│   ├── font/              (Nunito.ttf)
│   ├── layout/            (13 XML layouts)
│   └── values/
│       ├── colors.xml
│       ├── strings.xml
│       └── themes.xml
└── AndroidManifest.xml
```

---

## Setup

1. Open in Android Studio
2. Place `debug.keystore` at `%USERPROFILE%/AppData/Local/Temp/`
3. Ensure `google-services.json` is in `app/` (Firebase)
4. Add Mapbox token to `res/values/strings.xml`
5. Build & run on device/emulator (API 24+)
