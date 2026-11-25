# Business Manager (Android)

A native Android app that helps an industrial scrap business run its buying and selling from a phone. Buyers find scrap lots, negotiate prices in chat, place orders and track deliveries. Sellers list their material, respond to order requests and keep an eye on sales. Everything happens in one app, and each person only sees the screens that belong to their role.

All data comes from the backend API. The app contains no hard-coded or sample data.

## Who uses it

**Buyers** source scrap material for the business. They search the market, talk to vendors, agree on a price, place an order and follow it until it arrives.

**Sellers** supply the material. They publish listings, accept or reject incoming order requests and review what they have sold.

## What you can do

### As a Buyer
- **Home dashboard:** total spend, active orders, monthly purchases, active deals, incoming shipments and regular suppliers at a glance
- **Market:** search and filter lots, then place an order from the lot page
- **Messages:** live two-way chat with vendors, including bid cards, counter prices and offer accepted
- **Registered Vendors:** add a vendor and browse that vendor's inventory
- **Orders and Tracking / Purchase History:** status filters and pagination
- **Settings:** procurement rules, two-factor authentication, password

### As a Seller
- **Home dashboard:** a summary of listings, requests and sales
- **Requests:** a ledger of incoming order requests to accept or reject
- **Listings:** create a new listing and manage existing ones
- **Sales History:** filters and pagination
- **Settings:** company profile, certifications, notifications, two-factor authentication, password

### For everyone
- Notifications and unread counts
- Pull-to-refresh
- Loading, empty and error states on every screen
- Safe handling of rotation and the back button

## Tech stack

| Area | Choice |
|---|---|
| Language / UI | Java, XML layouts, ViewBinding, Material 3 |
| Architecture | MVVM (ViewModel, LiveData), single Activity with Navigation Component |
| Networking | Retrofit 2, Gson, OkHttp (auth interceptor; a 401 sends the user back to Login) |
| Live chat | Socket.io client |
| Sign-in | Email and password, optional 2FA, Google sign-in (Credential Manager) |
| Images | Glide |
| Storage | Encrypted session (token, role, server address) |
| SDK | minSdk 24, targetSdk 34, compileSdk 36 |

## Project structure

```
app/src/main/java/com/example/marketplace/
  MainActivity.java     bottom navigation per role, access guard
  data/                 api (Retrofit), model, repository, socket
  ui/                   one package per screen area (home, market, messages, orders, ...)
  utils/                Resource/Event helpers, SessionManager, RoleAccess, formatters
app/src/main/res/       layouts, navigation graph, strings, colors, drawables
backend/                Node/Express/MongoDB API (separate repository)
```

## Getting started

### Requirements
- Android Studio (or the Android SDK and JDK 17) and an emulator or a phone
- The backend running (Node 22, MongoDB)

### 1. Run the backend
```bash
cd backend
npm install
npm run dev        # http://localhost:5000
```
`backend/` is its own repository and is not part of this one. It needs its own `.env` (MongoDB, JWT secret and so on).

### 2. Build and install the app
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
On a low-memory PC, use a smaller heap:
```bash
./gradlew assembleDebug "-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8"
```

### 3. Connect the app to the backend
Change the server address on the Login screen (Server connection sheet):

| Device | Address |
|---|---|
| Emulator | `http://10.0.2.2:5000/` |
| Phone on USB | `http://localhost:5000/` and run `adb reverse tcp:5000 tcp:5000` |
| Phone on Wi-Fi | `http://<your PC's LAN IP>:5000/` |

Do not add `/api`. If you restart the emulator, run `adb reverse` again.

## Conventions
- Prices are INR per kg; quantities are in metric tons (MT)
- No hard-coded text or colors: strings go in `strings.xml`, colors in `colors.xml`
- Every screen handles loading, empty and error states, rotation and the back button

## Contributing
- Push only to `https://github.com/jimm529/Business-Manager.git`, branch `main`
- Stage with `git add app` (never `git add -A`) so `backend/` is not included
- See [CLAUDE.md](https://github.com/jimm529/Business-Manager/blob/main/CLAUDE.md) for the full rules, including keeping AWS usage low
