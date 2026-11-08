# Aqua-llera — Android App

Native Android app for Baguio City residents to locate nearby water refilling stations and order
clean water for delivery or pickup. Built with Jetpack Compose, using the
[Aqua-llera PWA](https://github.com/anomalyco/aquallera-pwa) as the design reference.
Part of a capstone project.

| Component | Purpose |
|---|---|
| **Android App (this)** | Customer-facing native app |
| **Customer PWA** (`aquallera_pwa`) | Cross-platform customer-facing app (React) |
| **Admin Dashboard** (`aquallera_web`) | Dashboard for water station owners |

All three share the same Firebase project and Realtime Database.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 (Compose BOM 2024.12.01) — no XML layouts |
| Architecture | MVVM — single Activity, `ViewModel` + `StateFlow` |
| Navigation | Navigation Compose (`NavGraph.kt`), one `NavHost` |
| Dependency injection | Hilt 2.51.1 (with KSP) |
| Async | Coroutines + Flow |
| Maps | Mapbox Maps SDK v11.0.0 |
| Location | Google Play Services Location 21.0.1 |
| Backend | Firebase Auth + Realtime Database (BOM 32.7.0) |
| Email | EmailJS REST API (verification codes only) |
| Font | Nunito |

`minSdk` 24 · `targetSdk` 34 · `compileSdk` 36 · applicationId `com.example.aquallera`

> **Note:** this app was originally written with XML layouts and 14 separate Activities. That version
> was fully replaced by the Compose rewrite documented in `PLAN.md`. There are no XML layouts or
> per-screen Activities left — only `MainActivity` hosting composable screens.

---

## Screens (14)

All screens are composables under a single `MainActivity` + `NavHost`.

| Screen | Composable | Key features |
|---|---|---|
| Splash | `SplashScreen` | Logo, 1.5s delay, routes by auth + verification state |
| Landing | `LandingScreen` | Logo, Log In / Sign Up, tagline, footer |
| Login | `LoginScreen` | Wave gradient, floating inputs, Firebase Auth |
| Signup | `SignupScreen` | Wave gradient, duplicate email/phone checks, sends verification code |
| Verify Email | `VerifyEmailScreen` | 6-digit OTP entry, 30s resend cooldown, data privacy notice on success |
| About | `AboutScreen` | App info + 6 data privacy sections |
| Maps | `MapScreen` | Full-screen Mapbox, custom markers, draggable station panel, status filter |
| Store Details | `StoreDetailsScreen` | Verified documents, business hours, services, delivery schedule, price list |
| Create Order | `CreateOrderScreen` | Quantities, Delivery/Pickup, Mapbox address search, GPS, date/time |
| Order Confirmation | `OrderConfirmationScreen` | Receipt with fee breakdown and grand total |
| Order Success | `OrderSuccessScreen` | Confirmation, order details, next actions |
| Orders | `OrdersScreen` | Order list, status filter, receipt view, cancel with confirmation |
| Profile | `ProfileScreen` | Avatar, details, verification badge, action rows |
| Edit Profile | `EditProfileScreen` | Name/email/phone, delete account |

---

## Project Structure

```
app/src/main/java/com/example/aquallera/
├── AqualleraApp.kt                 # @HiltAndroidApp
├── MainActivity.kt                 # Single Activity hosting the NavHost
├── navigation/NavGraph.kt          # All routes + verification gating
├── core/
│   ├── di/                         # Hilt modules (Firebase, Database)
│   ├── model/                      # User, WaterStation, Order
│   ├── theme/                      # Colors, typography, shapes
│   └── util/                       # Constants, TimeUtils, ErrorUtils, EmailService
├── auth/         data/ + ui/       # AuthRepository, AuthViewModel, 5 auth screens
├── stations/     data/             # StationRepository (real-time flows)
├── maps/         ui/               # MapScreen + MapViewModel
├── store/        ui/               # StoreDetailsScreen + StoreViewModel
├── createorder/  data/ + ui/       # PendingOrderStore, order creation + confirmation
├── orders/       data/             # OrderRepository (real-time flows, auto-cancel)
├── ui/
│   ├── about/                      # AboutScreen
│   ├── orders/                     # OrdersScreen + OrdersViewModel
│   └── profile/                    # Profile + EditProfile screens and ViewModels
└── components/                     # Shared composables (see below)
```

**Shared composables:** `BottomNavBar`, `WaterStationCard`, `OrderTicketItem`, `ReceiptCard`,
`AqualleraInput`, `PrimaryButton`, `WaveBackground`, `Footer`, `LoadingIndicator`, `ErrorDialog`,
`DataPrivacyDialog`, `GallonSwapNote`, `NotificationListener`.

---

## Firebase Data Model

| Path | Purpose |
|---|---|
| `/users/{uid}` | Profiles: `{ uid, fullName, email, number, createdAt }` |
| `/waterStations/{stationId}` | Station profiles: pricing, hours, services, `waterTypes`, `businessPermitDocuments`, status |
| `/orders/{orderId}` | Orders with line items and fee breakdown |
| `/orderCounter/{yyyyMMdd}` | Daily auto-incrementing counter for reference numbers |
| `/emailVerification/{uid}` | `{ code, email, createdAt, verified }` for the OTP flow |

**Reference number format:** `{stationPrefix}-{yyyyMMdd}-{counter}` — e.g. `AQU-20260813-0042`.
Generated by an atomic `runTransaction` on `/orderCounter`, matching the PWA exactly.

**Fees:** 2% transaction fee on (subtotal + delivery fee); delivery fee defaults to ₱50 when the
station has not set one.

Rules live in `database.rules.json`. `/emailVerification` is scoped per user
(`auth.uid === $uid`) — it holds verification codes, so it must never be world-readable.

---

## Email Verification (6-digit OTP)

Firebase's built-in email verification is **not** used. Signup writes a 6-digit code to
`/emailVerification/{uid}` and emails it via EmailJS; the code expires after 10 minutes. Every
protected route is gated on `/emailVerification/{uid}/verified`, the same flag the PWA reads, so a
user verified in one client is verified in both.

Leaving the verify screen without entering a code (either the "Back to Signup" link or the system
back button) deletes the unverified account, so the email address stays available for another
attempt.

### EmailJS configuration gotcha

The Android app is a **non-browser** client. In the EmailJS dashboard under
**Account → Security**, both of these must be set or sending fails with HTTP 403 and no email
arrives:

1. **API access from non-browser applications** — enabled
2. **Use Private Key** — disabled (otherwise requests must carry the private key)

Credentials live in `core/util/Constants.kt`.

> Order confirmation emails were removed — the app no longer emails customers about orders. Status
> changes surface in-app via `NotificationListener` and the Orders tab.

---

## Setup

1. Open the project in Android Studio.
2. Ensure `google-services.json` is present in `app/`.
3. Confirm the Mapbox token in `core/util/Constants.kt` and
   `app/src/main/res/values/strings.xml` (`mapbox_access_token`).
4. A debug keystore is read from `~/.android/debug.keystore` and is created automatically by
   Android Studio. See the caveat below if you hit a signing error.
5. Build and run on a device or emulator (API 24+).

### Build commands

Run from the project root.

| Action | Command |
|---|---|
| Build debug APK | `gradlew.bat assembleDebug` |
| Build + install on a running device | `gradlew.bat installDebug` |
| Clean rebuild | `gradlew.bat clean assembleDebug` |
| Avoid Gradle lock errors | add `--no-daemon` |

First build takes ~2–3 minutes; later builds are 30–60s.

### Signing

The `debug` signing config points at `~/.android/debug.keystore`. Do not point it back at `%TEMP%`
— Windows wipes it and every existing install then fails to update
(`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), requiring an uninstall.

The `release` build type is signed from a gitignored `keystore.properties` at the project root
(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Without that file, `assembleRelease`
produces an **unsigned APK that cannot be installed**. When it is present, `assembleRelease`
emits a signed, installable APK at `app/build/outputs/apk/release/app-release.apk`; the packaged
release artifact is `dist/apk/aquallera-v1.1.apk`. Rebuild it after a version bump with
`gradlew.bat --no-daemon assembleRelease`.

---

## Known Limitations

1. **GPS unverified on real hardware.** The location fix logic (`getUserLocation` in `MapScreen.kt`)
   is verified against a simulated position only. Emulators have no GPS and report whatever is set
   under Extended Controls → Location, so they can never show a real position. See `PLAN.md`.
2. **Coarse fixes are shown without warning.** Accuracy filters were removed because they discarded
   fixes silently; a low-accuracy fix now renders as a confident marker.
3. **Payment is Cash on Delivery only.** `paymentMethod` exists on every order but is not selectable.
4. **API keys are compiled into the APK** (`Constants.kt`), so they are recoverable by anyone who
   unpacks it.
5. **PWA orders can carry mismatched field types** (e.g. `createdAt` as an ISO string). `OrderRepository`
   parses each child in its own try/catch and skips unreadable orders rather than crashing.

---

## Documentation

- **`PLAN.md`** — rewrite plan, phase-by-phase history, per-session change logs, and resolved issues.
- **`database.rules.json`** — Realtime Database security rules.
