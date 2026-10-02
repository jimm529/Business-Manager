# API_NOTES.md — Marketplace (LeadTrack Industrial) backend

Single source of truth for every API call in the Android app.
Extracted by reading `./backend` (Express + MongoDB, ES modules). Nothing here is invented; where I did not read a response shape in full, it is marked **(not fully verified)**.

---

## 1. Server basics

| Item | Value |
|---|---|
| Entry file | `backend/src/server.js` |
| Default port | `5000` (`process.env.PORT || 5000`) |
| API prefix | `/api` |
| Health check | `GET /api/health` → `{ "status":"ok", "message":"Market API is operational", "database":"connected" }` |
| Body format | JSON (`Content-Type: application/json`) except uploads (multipart) |
| Realtime | Socket.io on the same host/port (see §9) |
| Currency | INR. Prices are **₹ per kg**; quantities are **MT** (1 MT = 1000 kg). `amount = quantityMT * 1000 * unitPricePerKg` (server recalculates) |

### Android base URL

| Where the app runs | Base URL |
|---|---|
| Emulator | `http://10.0.2.2:5000/api/` |
| Real phone on same Wi-Fi | `http://<PC LAN IP>:5000/api/` (the PC firewall must allow port 5000) |
| Deployed | the deployed host (`https://…/api/`) — ask the backend owner |

- Plain `http://` needs a `network_security_config` (cleartext allowed for debug only). Put the base URL in `BuildConfig` per build type, never hardcode in code.
- Retrofit base URL must end with `/`; endpoint paths below are written **without** the leading `/api/` slash issue: use `auth/login`, not `/api/auth/login`.
- **CORS does not affect the app.** Requests without an `Origin` header (native apps) are allowed.

### Android data rules

- Dates: Mongo timestamps come as ISO-8601 UTC strings (`2026-10-02T10:49:00.000Z`). Exception: purchase-history `date` is `YYYY-MM-DD`.
- Almost every field can be `null`, `""` or missing (`lastLogin`, `expectedDate`, `lot`, `bid`, `rating`…). Use nullable wrappers/defaults in every model.
- IDs are 24-char Mongo ObjectId strings. Documents have `_id`; users and some mapped objects have `id`. Accept both.
- Response envelope is **not uniform**: lists come back under different keys (`data`, `orders`, `lots`, `requests`). The key is listed per endpoint below.
- Error body shape is `{ "message": "…" }` or `{ "success": false, "message": "…" }`. Always read `message`. In non-production the generic error handler also adds `stack` (ignore it).

---

## 2. Authentication

### Roles
Two roles only: `operator` (buyer — "Operator Portal") and `vendor` (seller — "Vendor Portal"). (`admin` exists in permissions config but cannot be registered.)

### Token
- Format: JWT, header `Authorization: Bearer <token>`.
- Payload: `{ id, role, iat, exp }`. Lifetime: `JWT_EXPIRES_IN` env, default **7d**.
- **There is no refresh endpoint.** When the token expires (or the user is deleted / access revoked) the server answers **401**; the app must clear the token and go to Login.
- 401 messages seen: `Not authorized, no token provided`, `…token has expired`, `…invalid token`, `…user belonging to this token no longer exists`, `…access to this account has been revoked`.
- Store the token in `EncryptedSharedPreferences` (Jetpack Security) and add it with one OkHttp interceptor; one authenticator/interceptor handles 401 → logout → Login.

### Endpoints

| Screen | Method & path | Auth | Body | Success | Errors |
|---|---|---|---|---|---|
| Login | `POST auth/login` | none | `{ email, password, role }` — `role` = `"vendor"` or `"operator"` (the portal selector; `portal` also accepted; optional but send it) | `200` | `400` missing fields, `401` wrong email/password, `403` portal/role mismatch or access revoked |
| Login (2FA step) | `POST auth/2fa/login-verify` | none | `{ pendingToken, code }` (6-digit) | `200` | `400` bad code, `401` pending token expired (5 min) |
| Register | `POST auth/register` | none | `{ fullName, jobTitle, email, password, confirmPassword, role }` — all required | `201` | `400` validation, `409` email exists |
| Profile | `GET auth/me` | Bearer | – | `200` `{ success, user }` | `401` |
| Change password | `POST auth/change-password` | Bearer | `{ currentPassword, newPassword, confirmNewPassword }` | `200` `{ success, message }` | `400`, `401` current password wrong |
| 2FA setup | `POST auth/2fa/setup` | Bearer | – | `200` (secret/QR data, **not fully verified**) | `401` |
| 2FA enable | `POST auth/2fa/verify` | Bearer | `{ code }` | `200` | `400` |
| 2FA disable | `POST auth/2fa/disable` | Bearer | `{ password, code }` | `200` | `400`, `401` |

Password rule (server enforced): ≥ 8 chars, 1 uppercase, 1 number, 1 symbol. Email must be a valid format.

**Login success (no 2FA)**
```json
{
  "success": true,
  "message": "Login successful",
  "token": "<jwt>",
  "user": {
    "id": "665f…", "fullName": "…", "jobTitle": "…", "email": "…",
    "role": "operator", "vendor": null, "twoFactorEnabled": false,
    "createdAt": "2026-…Z", "updatedAt": "2026-…Z"
  }
}
```
**Login when 2FA is on** → `200`, **no `token`**:
```json
{ "success": true, "requires2FA": true, "pendingToken": "<5-min jwt>",
  "message": "Two-factor authentication code required",
  "user": { "id": "…", "fullName": "…", "email": "…", "role": "vendor" } }
```
Then call `auth/2fa/login-verify`, which returns the same shape as a normal login success.

`user.vendor` is the linked Vendor id for vendor-role users.

### Things on the login mock-up the backend does NOT support
| UI element | Backend |
|---|---|
| Forgot password | **No endpoint** |
| Continue with Google / SSO | **No endpoint** |
| "Keep me signed in for 30 days" | **Not supported** (token is a fixed 7d). Store token either way; the checkbox can only control whether the app keeps it across restarts |
| Support link | none |

---

## 3. Role permissions (who may call what)

`protect` = valid Bearer token. Then either a role check (`authorize`) or a permission check. Wrong role → **403** `Forbidden: …`.

| Permission group | operator | vendor |
|---|---|---|
| Vendors CRUD (`/vendors`) | ✔ | ✘ |
| Orders read / update status / upload doc / export | ✔ | ✔ (scoped to own orders) |
| Orders create | ✔ | ✘ |
| Lots read / bookmark / place offer | ✔ read, offer | ✔ read |
| Lots create / update / delete / photos | ✘ | ✔ |
| Messages read / send | ✔ | ✔ |
| Bids: counter (create) | ✔ | ✘ |
| Bids: accept / decline via `/bids` | permission yes, **but controller requires the bid's operator** | permission yes, but returns **403** (not the owner). Vendors respond via `vendor-portal/order-approvals/:id/respond` |
| `/settings` (org settings, 2FA, members) | ✔ | ✘ |
| `/vendor-portal/*` | mostly ✘ | ✔ |

Data is multi-tenant: operators only see their own vendors/bids/orders/conversations; vendors only see rows linked to their Vendor record.

---

## 4. Screen → route map

| App screen | Operator routes | Vendor routes |
|---|---|---|
| Auth | `POST auth/login`, `POST auth/register`, `POST auth/2fa/login-verify` | same |
| Home / Dashboard | `GET orders/stats`, `GET bids/active-deals`, `GET orders?sort=…` (recent), `GET lots` (market). **No dedicated operator dashboard endpoint exists** | `GET vendor-portal/overview` (one call), `GET vendor-portal/order-approvals` |
| Marketplace lots | `GET lots`, `GET lots/:id`, `GET lots/:id/related`, `POST lots/:id/bookmark`, `POST lots/:id/offers`, `POST lots/:id/messages` | `GET lots/my-lots`, `POST lots`, `PATCH lots/:id/status`, `DELETE lots/:id`, photos routes |
| Bids / negotiations | `GET bids`, `GET bids/active-deals`, `POST bids/:id/counter|accept|decline` | `GET vendor-portal/orders`, `…/orders/stats`, `POST vendor-portal/order-approvals/:id/respond`, `POST vendor-portal/orders/:id/accept|reject` |
| Orders list & detail | `GET orders`, `GET orders/stats`, `GET orders/:id` | `GET orders`, `GET vendor-portal/orders/:id/detail`, `GET vendor-portal/sales-history` |
| Order tracking | `POST orders/:id/verify-purity|schedule-pickup|update-location|mark-delivered`, `PATCH orders/:id/status` | `PATCH orders/:id/status` |
| Messaging | `GET conversations`, `GET conversations/:id`, `GET/POST conversations/:id/messages`, `POST conversations/vendor/:vendorId`, `POST conversations/:id/documents` | same (scoped to vendor) |
| Vendors (suppliers) | `GET/POST vendors`, `GET/PUT/PATCH/DELETE vendors/:id`, reviews | – |
| Purchase history | `GET purchase-history` | `GET vendor-portal/sales-history` |
| Profile / settings | `GET auth/me`, `GET/PUT settings`, `PUT settings/password`, 2FA routes | `GET vendor-portal/settings`, `PUT vendor-portal/settings/profile|account|notifications|operational-preferences` |

All paths below are relative to `/api/`. Every route except the 4 public auth routes needs `Authorization: Bearer <token>`.

---

## 5. Bids / negotiations

A "bid" is an order request negotiated between operator and vendor.

| Field | Notes |
|---|---|
| `status` | `open`, `pending`, `countered`, `accepted`, `declined` |
| `currentTurn` | `operator`, `vendor` or `null` (closed). Only the side whose turn it is may act |
| `lastActionBy` | `operator` / `vendor` |
| other | `lotId` (string like `#VN-1234`), `lot` (id or populated object), `batchTitle`, `material`, `quantityMT`, `unitPricePerKg`, `amount`, `currency`, `notes`, `declineReason`, `history[]` (`proposedBy, unitPricePerKg, quantityMT, amount, notes, createdAt`), `vendor`, `operator`, `createdAt`, `updatedAt`, virtual `resolvedTurn` |

Lifecycle: operator places order (`POST lots/:id/offers`) → `pending`, turn=vendor → vendor `approve` (→ `accepted` + order created) / `counter` (→ `countered`, turn=operator) / `reject` (→ `declined`) → operator `accept` / `counter` / `decline`.

| Method & path | Role | Body / query | Success | Errors |
|---|---|---|---|---|
| `GET bids` | operator | `?status=pending|countered|accepted|declined|all` `&vendorId=` | `200` `{ success, data:[Bid], count }` | `401`, `403` |
| `GET bids/active-deals` | operator | – | `200` `{ success, data:[deal], totalCount }`; deal = bid fields + `id`, `pricePerMT`, `currentTurn`, `lot`, `vendor` | `401`, `403` |
| `POST bids/:id/counter` | operator | `{ proposedRate (₹/kg), revisedQuantity (MT), notes?≤500, expectedUpdatedAt? }` (aliases `unitPricePerKg`, `quantityMT`) | `200` `{ success, message, data: Bid }` | `400` bad values / not your turn / closed / qty > lot qty, `403` not owner, `404`, `409` stale (`expectedUpdatedAt` differs by >1s) |
| `POST bids/:id/accept` | operator | none | `200` `{ success, message, data: Bid, order, transaction }` | `400` closed / vendor's turn, `403`, `404` |
| `POST bids/:id/decline` | operator | `{ reason?≤500, expectedUpdatedAt? }` | `200` `{ success, message, data: Bid }` | `400`, `403`, `404`, `409` |
| `POST lots/:id/offers` | operator | `{ proposedRate?, proposedQuantity?, notes? }` (defaults to the lot's price/qty) | `201` `{ success, message, bid, conversationId }` | `400` rate/qty ≤ 0, `404` lot |
| `POST vendor-portal/order-approvals/:id/respond` | vendor | `{ action: "approve"|"counter"|"reject", counterPrice?, counterQuantity? (≤ requested), notes? }` | `200` approve → `{ order, transaction }`; counter → `{ bid }`; reject → `{ message }` | `400` invalid action / not vendor's turn / closed, `403`, `404` |
| `GET vendor-portal/order-approvals` | vendor | – | `200` `{ success, data:{ pendingBids:[Bid], awaitingDocOrders:[Order] } }` | `403` |
| `GET vendor-portal/orders` | vendor | `?page=1&limit=4&status=all&search=` | `200` `{ success, count, totalRequests, totalPages, currentPage, limit, requests:[…] }` (rows mix bids and orders; status is an uppercase label like `PENDING REVIEW`, `ACCEPTED`) | `403` |
| `GET vendor-portal/orders/stats` | vendor | – | `200` `{ success, stats:{ activeRequests, activeRequestsTrend, totalQuantityMT, totalQuantityMTRaw, dominantMaterial, averageRate, averageRateRaw, averageRateSubtext, pendingApprovalCount, pendingApprovalSubtext } }` | `403` |
| `GET vendor-portal/orders/:id/detail` | vendor | – | `200` (**not fully verified**) | `403`, `404` |
| `POST vendor-portal/orders/:id/accept` | vendor | none | `200` `{ success, message, data:{ id, status:"ACCEPTED", order } }` | `403` |
| `POST vendor-portal/orders/:id/reject` | vendor | none (**not fully verified**) | `200` | `403` |
| `GET vendor-portal/insights` | vendor | – | `200` `{ success, data:{ procurementStrategy, marketplaceInsights } }` | `403` |

`/api/vendor/...` is an alias of `/api/vendor-portal/...` (same router).

> Quote text/strings the server sends (`message`) may be shown in a snackbar, but app labels must still come from `strings.xml`.

---

## 6. Orders & tracking

### Order object
`_id, orderId ("#ORD-1234"), operator, vendor (populated: companyName, vendorName, location, …), commodity, quantity, unit ("MT"), purity, buyerName, status, expectedDate, value, currency, bid, manifestDocUrl, uploadedDocUrl, notes, purityVerifiedAt, pickupScheduledAt, pickupDate, inTransitAt, currentLocation, deliveredAt, trackingHistory[{status,note,location,timestamp}], batchId, grade, packingDetails, originFacility, carrierName, driverName, vehicleId, destinationFacilityName, destinationAddress, arrivalWindowStart, arrivalWindowEnd, paymentTerms, specifications[{requirement,targetMetric,tolerance}], timeline[{title,date,location,carrier,status}], buyerProfile{name,hq,rating,bio}, createdAt, updatedAt`

### Statuses
`Processing`, `Awaiting Docs`, `Dispatched`, `On the Way`, `In-Transit`, `Delivered`, `Completed`, `Cancelled` (exact strings, with spaces/capitals).

`PATCH orders/:id/status` only accepts: `Processing`, `Awaiting Docs`, `On the Way`, `Delivered`.

### 5-step logistics chain (operator only, strict order)
1. Order placed (auto)
2. `POST orders/:id/verify-purity` — no body. Vendor → `403`.
3. `POST orders/:id/schedule-pickup` — `{ pickupDate }` (ISO). Needs step 2 first, else `400 Sequence Error…`.
4. `POST orders/:id/update-location` — `{ location }` (alias `currentLocation`). Needs step 3; sets status `On the Way`.
5. `POST orders/:id/mark-delivered` — no body. Needs in-transit; sets `Delivered`.

All return `200 { success, message, order }`; `400` for cancelled / out-of-sequence / already delivered; `404 Order not found or unauthorized`.

### Endpoints

| Method & path | Role | Params / body | Success | Errors |
|---|---|---|---|---|
| `GET orders` | both | `?status=` (`all`, `processing`, `in-transit`/`on the way`, `delivered`, `awaiting-docs`, or exact status) `&search=` (order id, commodity, vendor name/location, status) `&sort=` (`oldest`, `value-high`, `value-low`, `quantity-high`, `quantity-low`; default newest). **No pagination** | `200` `{ success, count, orders:[Order] }` | `401`, `403` |
| `GET orders/stats` | both | – | `200` `{ success, stats:{ activeShipments, activeShipmentsGrowth, pendingVerification, estimatedCompletionTime, totalProcurementMT, totalProcurementValue, totalProcurementFormattedValue, processingCount, deliveredCount, totalOrdersCount } }` | `401` |
| `GET orders/:id` | both | – | `200` `{ success, order, manifest }`; `manifest` = `{ status, specs, carrierLogistics, route, milestone, chainOfCustody[{key,title,handler,description,timestamp,location,…}], … }` | `404` |
| `POST orders` | operator | `{ vendorId, commodity, quantity, unit?, status?, expectedDate?, value, currency?, notes?, orderId?, manifestDocUrl? }`; if user has 2FA on and value is above threshold: header `X-2FA-Code` or body `totpCode` | `201` `{ success, message, order }` | `400`, `403` with `requires2FA:true` (prompt for code), `404` vendor not yours, `409` duplicate orderId |
| `PATCH orders/:id/status` | both | `{ status, note?, location?, expectedDate? }` | `200` `{ success, message, order }` | `400` invalid status, `404` |
| `POST orders/:id/upload-doc` | both | `{ documentUrl, docType: "manifest"|"compliance" }` (URL comes from the upload flow, §8) | `200` `{ success, message, order }` | `400`, `404` |
| `GET orders/export` | both | `?status=&search=` | `200` CSV text | – |
| `GET purchase-history` | operator | `?page=&limit=&status=completed|dispatched|cancelled|processing&vendorId=&startDate=YYYY-MM-DD&endDate=` | `200` `{ success, metrics:{ totalOrders,totalSpend,materialsProcuredMT }, orders:[{ orderId, date, materialType, quantityMT, unit, purity, vendorName, valueINR, currency, status (label), documentUrl }], pagination:{ totalOrders,totalPages,currentPage,pageSize } }` | `400` bad filter, `403` vendor |
| `GET vendor-portal/sales-history` | vendor | `?page=1&limit=10&status&material&minPurity&maxPurity&minValue&maxValue&startDate&endDate&search&sortBy=createdAt&sortOrder=desc` | `200` `{ success, orders[], pagination{ page,limit,totalOrders,totalPages,hasNextPage,hasPrevPage,showingStart,showingEnd }, filterOptions{ materials[], statuses[] } }` | `403` |
| `GET vendor-portal/orders/:id/document` | vendor | `?download=true|false` | `200` `{ success, orderId, documentUrl, downloadUrl, isDownload }` | `400`, `403`, `404` |

---

## 7. Lots / listings (marketplace inventory) and vendors

### Lot object (main fields)
`_id, lotId ("#VN-1234"), title, material, description, vendor (populated: companyName, vendorName, location, complianceLabel, rating, brandLogoUrl…), photos[] (URLs), pricePerKg, priceHistory[], quantity, unit, leadPurity, grade, region, location, pickupLocation, packagingDetails[], shippingTerms{ termType, description, availablePickupDate }, complianceCertifications[], status, isDraft, createdAt` — plus `myLatestBid{unitPricePerKg,status,createdAt}|null` and `isBookmarked` on list items.

Lot `status`: `Available`, `Ready for Pickup`, `In Extraction`, `Reserved`, `Sold`, `draft`, `published`.

| Method & path | Role | Params / body | Success | Errors |
|---|---|---|---|---|
| `GET lots` | both | `?search=&materials=a,b&region=&purityGrade=&minPrice=&maxPrice=&status=&sort=price-asc|price-desc|quantity-desc|purity-desc` (price in ₹/kg). **No pagination** | `200` `{ success, count, lots[], categories[], filters:{ materials[], regions[{value,label}], purityGrades[{value,label,minPurity}] } }` | `400` bad purity grade / price range |
| `GET lots/:id` | both | – | `200` `{ success, lot, sellerSummary, reviewsSummary{ averageRating,totalReviews,reviews[],canReview }, relatedLots[] }` | `404` |
| `GET lots/:id/related` | both | – | `200` (related lots) | `404` |
| `POST lots/:id/bookmark` | both | – | `200` `{ success, bookmarked, lotId }` | `400` bad id, `404` |
| `POST lots/:id/messages` | operator | `{ initialMessage }` | `200` (conversation, **not fully verified**) | `404` |
| `GET lots/my-lots` | vendor | `?status=&search=` | `200` `{ success, count, lots[] }` | `403` no vendor profile |
| `POST lots` | vendor | `{ title?, material, description?, pricePerKg, quantity, unit?, leadPurity?, grade?, location?, packagingDetails?[], shippingTerms?, complianceCertifications?[], photos?[], status? }` | `201` `{ success, message, … }` | `403` |
| `PATCH lots/:id/status` | vendor | `{ status?, pricePerKg?, quantity? }` | `200` | `403`, `404` |
| `DELETE lots/:id` | vendor | – | `200` | `403`, `404` |
| `POST lots/:id/photos` | vendor | multipart, field **`photos`** (≤ 6 files per request, ≤ 5 MB each, JPEG/PNG/WEBP) | `200` `{ success, message, uploadedCount, … photos }` | `400` too many/invalid, `403` not owner, `502/503` storage |
| `DELETE lots/:id/photos/:photoId` | vendor | `photoId` = file name of the photo URL | `200` | `404` |

### Vendor listing wizard (vendor only)
| Method & path | Notes |
|---|---|
| `GET vendor-portal/listings/options` | `200` `{ success, data:{ materialTypes, packagingTypes, incoterms, regions } }` (dropdown values come from the DB) |
| `GET vendor-portal/listings/locations` | `200` `{ success, data:[string] }` |
| `GET vendor-portal/listings/templates` | `?search=` → `200` `{ success, data[] }` |
| `GET vendor-portal/listings/draft` | `200` `{ success, data: draft|null }` |
| `POST vendor-portal/listings/draft` | save draft (`materialType, availabilityDate, shippingTerms, askingPricePerKg, certificationAccepted, draftStep, title, description, region, …`) → `200` `{ data: draft }` |
| `DELETE vendor-portal/listings/draft/:id` | `200` |
| `POST vendor-portal/listings/publish` | same fields as draft; validated → `201` `{ data: lot }`; `400` with per-field `errors` |

### Operator's supplier records — `vendors` (operator only)
| Method & path | Notes |
|---|---|
| `GET vendors` | `?status=active…&complianceStatus=&search=` → `200` `{ success, count, data:[Vendor] }` |
| `POST vendors` | `{ companyName|vendorName, licenseNumber, hqLocation|location, materialType|primaryMaterial, monthlyCapacity|volumeTons, complianceCertifications[], complianceStatus, complianceLabel, certificationPdfUrl, lastAudit, leadManagerName, secureEmail, status }` → `201` |
| `GET/PUT/PATCH/DELETE vendors/:id` | single vendor; `403` if not the owner |
| `POST vendors/bulk-delete` | `{ ids:[…] }` |
| `GET vendors/user-groups/suggestions` | suggestions list |

### Reviews
| Method & path | Notes |
|---|---|
| `GET reviews/vendor/:vendorId` | `200` reviews + summary (**not fully verified**), `404` |
| `POST reviews/vendor/:vendorId` | operator only. `{ rating, title, content, lotId? }` → `201`; `400` missing title/content/rating, `403` (not eligible) |

### Products (legacy, unused by the plan)
`GET products`, `GET products/:id` (any login), `POST products`, `DELETE products/:id` (operator). Responses `{ success, count, data }`.

### Vendor dashboard — `GET vendor-portal/overview` (vendor)
`200 { success, data:{ vendor:{ id, companyName, materialType, location, status, leadManagerName, secureEmail }, stats:{ totalRevenue, totalRevenueRaw, revenueGrowthPct, revenueGrowthYoY, activeBatchesCount, activeBatchesMT, extractionEfficiency, extractionEfficiencyValue, complianceHealth }, marketPerformance[], activeNegotiations[], logisticsPipeline[], inventoryStatus[] } }`. `403` no vendor profile, `404` vendor not found. Many values can be `null` (e.g. `revenueGrowthPct`). The server also sends preformatted strings (`totalRevenue`); prefer the `*Raw` numbers and format on the device.

---

## 8. Messaging

Conversation = one operator ↔ one vendor (unique pair). Messages belong to a conversation. A bid card is a message with `type` `bid_received`/`counter_offer` and a populated `bid`.

Message `type`: `text`, `bid_received`, `counter_offer`, `order_received`, `system`.

| Method & path | Role | Body | Success | Errors |
|---|---|---|---|---|
| `GET conversations` | both | – | `200` `{ success, data:[Conversation], count }` sorted by `lastMessageAt` desc. Conversation: `_id, operator{fullName,jobTitle,email,role}, vendor{companyName,vendorName,location,…,brandLogoUrl}, lastMessage (with bid summary), lastMessageText, lastMessageType, lastMessageAt, lotTitle, documents[], unreadCountOperator, unreadCountVendor` | `401`, `403` |
| `GET conversations/:id` | participant | – | `200` `{ success, data }` | `403`, `404` |
| `POST conversations/vendor/:vendorId` | operator | – (get or create thread) | `200` `{ success, data }` | `403` vendor not yours, `404` |
| `GET conversations/:id/messages` | participant | – | `200` `{ success, data:[Message], count }` oldest → newest, **whole history, no paging, no `since`** | `403`, `404` |
| `POST conversations/:id/messages` | participant | `{ content (required, non-empty), type?: "text"|"bid_received", bidId? }` | `201` `{ success, data: Message }` | `400` empty, `403`, `404` |
| `POST conversations/:id/documents` | participant | multipart, field **`document`** (PDF/JPG/PNG, ≤ 10 MB) | `201` `{ success, message, data: documents[] }` (each `{ name, url, contentType, size, uploadedByName, uploadedAt }`) | `400` no file, `403`, `404` |

Message: `{ _id, conversation, sender:{ fullName,email,role,jobTitle }, senderRole, senderName, type, content, bid|null, readBy[], createdAt, updatedAt }`.

**Chat refresh:** there is no incremental endpoint. Either poll `GET …/messages` (e.g. every few seconds while the chat screen is open) or use Socket.io (§9). There is **no "mark as read" endpoint** and no unread-reset route.

---

## 9. Realtime (Socket.io, optional)

Same host/port. Auth: handshake `auth: { token: "<jwt>" }` (or `Authorization: Bearer` header).
Client → server: `join_conversation(conversationId)`, `leave_conversation`, `typing({conversationId})`, `stop_typing`.
Server → client: `new_message` (Message), `bid_updated` (Bid), `conversation_updated` `{ conversationId, lastMessage, lastMessageAt }`, `user_typing`, `user_stop_typing`.
Polling is enough for step one; sockets can be added later.

---

## 10. File uploads

| Purpose | How |
|---|---|
| Lot photos | multipart `POST lots/:id/photos`, field `photos` (see §7) |
| Chat document | multipart `POST conversations/:id/documents`, field `document` (see §8) |
| Vendor facility images | multipart `POST vendor-portal/settings/facility-images`, up to 3 images, ≤ 5 MB, JPG/PNG/WEBP/SVG, any field name → `200` `{ success, facilityImages[], uploadedCount }` |
| Vendor certification | multipart `POST vendor-portal/certifications`, one file (PDF/JPG/PNG ≤ 10 MB) + text fields `name`, `expiryDate` (field name of the file is not restricted) |
| Any other file (S3 direct) | 1) `POST uploads/presign` body `{ fileName, fileType, fileSize?, folder? }` — default folder accepts **PDF only** (≤ 10 MB); `folder:"logos"` or `"brand-logos"` accepts PNG/JPG/WEBP/SVG (≤ 5 MB). Returns `200 { success, message, presignedUrl, fileUrl, key, bucket }` (valid 5 min). 2) `PUT` the raw bytes to `presignedUrl` with header `Content-Type` = the same `fileType` (no Bearer header on this S3 call). 3) Use `fileUrl` (e.g. send it as `documentUrl` to `orders/:id/upload-doc`) |

Stored files are public/secure S3 URLs returned as plain strings; load them with Coil/Glide. `400` for wrong type/size, `500` if AWS is not configured, `502/503` for storage failures.

---

## 11. Settings & profile

### Operator (`/settings`, operator only)
| Method & path | Notes |
|---|---|
| `GET settings` | `200` `{ success, …, settings }` — sections: `procurementProfile`, `approvalWorkflows`, `materialPreferences`, `logisticsPartners`, `securityAccess` (includes `twoFactorThreshold`), `priceFixing`, `rbac`, team members (**inner shapes not fully verified**) |
| `PUT settings` | body = any of those sections; `400` on invalid amounts; `200` `{ …, settings }` |
| `PUT settings/password` | `{ currentPassword, newPassword, confirmPassword }` (any signed-in user) |
| `PUT settings/security-questions` | `{ questions }` |
| `POST settings/2fa/setup|verify|disable` | like auth 2FA |
| `POST settings/members`, `PATCH settings/members/:id/permissions|revoke|restore` | team management (owner only; `403` for members) |

**There is no operator notification-preferences endpoint.**

### Vendor (`/vendor-portal/settings…`, vendor only)
| Method & path | Body |
|---|---|
| `GET vendor-portal/settings` | → `200` `{ success, settings, user }` |
| `PUT vendor-portal/settings/profile` | `{ entityName, taxId, headquartersAddress }` |
| `PUT vendor-portal/settings/operational-preferences` | `{ materialYieldTargetPercent, plantCapacityMT, typicalLeadPurity, listingDefaults }` |
| `PUT vendor-portal/settings/notifications` | `{ newOrderRequests, successfulSales, logisticsUpdates, priceAlerts }` (booleans) |
| `PUT vendor-portal/settings/account` | `{ fullName, jobTitle, email }` |
| `GET vendor-portal/certifications` | → `{ success, certifications[] }` |
| `POST vendor-portal/certifications` | multipart (see §10) |

For vendor password change use `POST auth/change-password` or `PUT settings/password`.

---

## 12. Status codes (what the app should handle)

| Code | Meaning in this API | App behaviour |
|---|---|---|
| `200` / `201` | OK / created. A `200` with an empty array/null is the **empty state** | show data or empty view |
| `400` | validation, wrong turn, out-of-sequence, bad file | show `message` |
| `401` | missing/invalid/expired token, deleted or revoked user; also wrong login credentials and wrong current password | on any call except login → clear token → Login screen. On login/change-password → show message |
| `403` | wrong role/permission, not owner, wrong portal at login, 2FA needed (`requires2FA:true` on order create) | show `message` |
| `404` | not found (also "not found or unauthorized") | show message / empty |
| `409` | duplicate email/order id, or stale bid (`expectedUpdatedAt`) | refetch and show message |
| `500` / `502` / `503` | server/DB/storage error | generic error + retry |
| network failure | no response | offline/error state with retry |

Every screen needs three states: loading, empty (e.g. `count: 0`, `data: []`, `null`), error (+ retry).

---

## 13. Gaps, risks and questions for the backend owner

1. **No operator dashboard endpoint** — compose from `orders/stats` + `bids/active-deals` + `orders`.
2. **No refresh token / logout endpoint** — expiry = re-login. (Logout = delete token locally.)
3. **No forgot-password, Google or SSO endpoints**, so those buttons on the login design cannot work yet.
4. **No operator notification settings** (only vendor).
5. **No unread/mark-read endpoint**; `unreadCountOperator/Vendor` is only incremented by the server.
6. **No pagination** on `orders`, `lots`, `bids`, `conversations`, `messages`; pagination exists only on `purchase-history`, `vendor-portal/orders`, `vendor-portal/sales-history`.
7. **Bid accept/decline for vendors** is a separate route (`order-approvals/:id/respond`), not `/bids/:id/*`.
8. `GET lots` calls `seedLotsIfEmpty` on the backend, so a fresh database can return seeded demo lots. That is server behaviour, not app data.
9. Some `GET` responses contain server-formatted display strings (₹ text, "Est. 24h completion"). Prefer raw numeric fields and build text from `strings.xml`.
10. `vendor-portal/orders/:id/reject`, `…/orders/:id/detail`, `lots/:id/messages`, `reviews GET`, `settings GET` inner shapes were not read in full — confirm by calling them once with a real token before writing the models.
11. Production base URL and the real `PORT` (set in `backend/.env`, which I deliberately did not read) are unknown — please confirm.
12. The backend has Jest-less Node test scripts (`backend/test-*.js`) and seed scripts (`npm run seed:orders`, `seed:deals`, `seed:options`, `seed:operator-settings`) to create realistic data for testing the app.
