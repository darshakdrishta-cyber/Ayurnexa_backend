# AyurNexa Backend API

Spring Boot 4 REST API backed by MongoDB. Application endpoints require `X-API-Key`; routes marked with `@JWTAnnotation` also require a user Bearer JWT. The Razorpay webhook is excluded from the API-key check and authenticated with Razorpay's HMAC signature.

## Environment

Copy `.env.example` to `.env` for local development and replace the placeholders. The application imports `.env` from the project root. Never commit `.env` or put real credentials in README, source code, or frontend bundles. In Railway, configure the same names under the backend service's Variables.

| Variable | Required | Purpose |
| --- | --- | --- |
| `PORT` | Platform supplied; local default `8080` | HTTP listener port |
| `MONGODB_URI` | No; defaults to local MongoDB | MongoDB connection string |
| `JWT_SECRET` | Yes | HMAC signing and verification key; provide at least 32 random bytes |
| `JWT_EXPIRATION_MS` | No; defaults to `3600000` | JWT lifetime in milliseconds |
| `API_KEY` | Yes | Value expected in `X-API-Key` for `/api/**`, except the Razorpay webhook |
| `CORS_ALLOWED_ORIGINS` | No; defaults to `http://localhost:3000` | Comma-separated browser frontend origins permitted to call the API |
| `RAZORPAY_KEY_ID` | Yes | Razorpay API key ID |
| `RAZORPAY_KEY_SECRET` | Yes | Razorpay API key secret; used only by the backend |
| `RAZORPAY_WEBHOOK_SECRET` | Yes | Secret configured for the Razorpay webhook; distinct from the API key secret |
| `RAZORPAY_CALLBACK_URL` | No | Optional callback URL included with Payment Links |
| `RAZORPAY_LINK_EXPIRY_MINUTES` | No; defaults to `30` | Payment Link expiry duration |

Generate local secrets, for example:

```bash
openssl rand -base64 32
openssl rand -hex 32
```

Use separate values for `JWT_SECRET`, `API_KEY`, and `RAZORPAY_WEBHOOK_SECRET`. Set `CORS_ALLOWED_ORIGINS` to the exact frontend origin(s), without a path, for example `http://localhost:5173,https://your-frontend.example`.

## Important API-Key Note

`X-API-Key` is an additional shared request gate, not user authentication. Any key embedded in browser JavaScript or a mobile app can be extracted by users; it cannot protect a public frontend API from unauthorized callers. Keep user authentication and authorization on the JWT and backend. For production public clients, consider removing the static API-key requirement from browser-facing routes and using rate limits/origin controls instead. Never expose `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, or `JWT_SECRET` to the frontend.

## Base URL and Headers

Set the deployed public Railway domain as `BASE_URL`. Do not use a `railway.internal` domain from a browser.

```bash
export BASE_URL="http://localhost:8080"
export API_KEY="<same value as API_KEY in backend environment>"
```

Every application API call below sends:

```http
X-API-Key: <API_KEY>
Content-Type: application/json
```

Protected operations additionally send:

```http
Authorization: Bearer <JWT returned by login>
```

## Authentication

### Register

`POST /api/auth/register` (API key required; no JWT)

```bash
curl -i -X POST "$BASE_URL/api/auth/register" \
	-H "X-API-Key: $API_KEY" \
	-H "Content-Type: application/json" \
	-d '{"username":"demo-user","email":"demo@example.com","password":"change-this-password"}'
```

### Login

`POST /api/auth/login` (API key required; no JWT). Save `token` from the response for protected calls.

```bash
curl -i -X POST "$BASE_URL/api/auth/login" \
	-H "X-API-Key: $API_KEY" \
	-H "Content-Type: application/json" \
	-d '{"email":"demo@example.com","password":"change-this-password"}'
```

The response includes `token`, `tokenType` (`Bearer`), `expiresIn` (seconds), and safe user details.

### Current user

`GET /api/auth/me` (API key + JWT required)

```bash
curl -i "$BASE_URL/api/auth/me" \
	-H "X-API-Key: $API_KEY" \
	-H "Authorization: Bearer $TOKEN"
```

## Products

All Product routes require API key and JWT. Create, update, and delete additionally require `ROLE_ADMIN`.

```bash
# Create (ADMIN)
curl -i -X POST "$BASE_URL/api/products" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $ADMIN_TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"productName":"Herbal Oil","productDescription":"100 ml herbal oil","productPrice":499.00}'

# List
curl -i "$BASE_URL/api/products" -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"

# Get, update, delete by business productId
curl -i "$BASE_URL/api/products/$PRODUCT_ID" -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
curl -i -X PUT "$BASE_URL/api/products/$PRODUCT_ID" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $ADMIN_TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"productName":"Herbal Oil","productDescription":"Updated description","productPrice":525.00}'
curl -i -X DELETE "$BASE_URL/api/products/$PRODUCT_ID" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $ADMIN_TOKEN"
```

Products referenced by existing orders cannot be deleted.

## Payments

All Payment CRUD routes require API key + JWT and are scoped to the logged-in user. `idempotencyKey` is required when creating a payment. Reuse the same key when retrying the same request; use a new key for a genuinely new attempt.

```bash
# Create COD payment
curl -i -X POST "$BASE_URL/api/payments" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"amount":499.00,"paymentMethod":"COD","idempotencyKey":"checkout-<unique-request-id>"}'

# Create Razorpay UPI Payment Link
curl -i -X POST "$BASE_URL/api/payments" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"amount":499.00,"paymentMethod":"UPI","idempotencyKey":"checkout-<unique-request-id>","customerName":"Demo User","customerEmail":"demo@example.com"}'

# List current user's payments / get one / update eligible pending payment / delete eligible pending payment
curl -i "$BASE_URL/api/payments" -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
curl -i "$BASE_URL/api/payments/$PAYMENT_RECORD_ID" -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
curl -i -X PUT "$BASE_URL/api/payments/$PAYMENT_RECORD_ID" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" -d '{"amount":499.00,"paymentMethod":"COD"}'
curl -i -X DELETE "$BASE_URL/api/payments/$PAYMENT_RECORD_ID" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
```

Do not mark a payment successful from the frontend. Razorpay payment status is changed only by its verified webhook. A payment with a Payment Link or a terminal status cannot be edited/deleted.

## Orders

All Order routes require API key + JWT and operate on the logged-in user's orders. Create requires a saved Payment record: payment method must be COD or status must already be `SUCCESS`. Item prices and order total are computed from the Product catalog; the submitted total is never trusted.

```bash
# Create; paymentId is the local Payment response's `id`, item productId is a Product business ID.
curl -i -X POST "$BASE_URL/api/orders" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"paymentId":"<payment-record-id>","expectedDeliveryDate":"2026-10-30","items":[{"productId":"<product-business-id>","quantity":2}]}'

# List/get/update/delete by business orderId
curl -i "$BASE_URL/api/orders" -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
curl -i "$BASE_URL/api/orders/$ORDER_ID" -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
curl -i -X PUT "$BASE_URL/api/orders/$ORDER_ID" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" -d '{"orderProgress":"PACKED","expectedDeliveryDate":"2026-10-31"}'
curl -i -X DELETE "$BASE_URL/api/orders/$ORDER_ID" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
```

## Razorpay Webhook

Configure this public URL in the Razorpay dashboard:

```text
POST https://<public-railway-domain>/api/payments/razorpay/webhook
```

Subscribe to `payment_link.paid`. The endpoint deliberately does not require `X-API-Key` or a user JWT. It validates `X-Razorpay-Signature` against the exact raw body using `RAZORPAY_WEBHOOK_SECRET`. It then checks the saved Payment reference, Payment Link ID, and exact amount before marking the Payment `SUCCESS` and storing the gateway payment ID. Repeated paid events are safe. Invalid signatures receive `401`; malformed/mismatched payment events receive `400`; success receives `200`. After payment is confirmed, the frontend can create the Order using the local payment record `id`.

Do not fake a Razorpay webhook with curl in production. For local integration testing, use Razorpay's test mode and configure a reachable webhook tunnel; manually posting a request without a valid HMAC signature is expected to return `401`.

## Other Routes

`GET /api/test` and `GET /api/mongo/test-mongo` require `X-API-Key`. The Mongo ping route reports database connectivity; it is not a general DB health endpoint.

## Common Responses

- `400`: invalid request body, amount mismatch, or malformed webhook
- `401`: missing/invalid API key, missing/invalid/expired JWT, or invalid webhook signature
- `403`: authenticated user lacks the required admin role
- `404`: requested resource does not exist or does not belong to the caller
- `409`: duplicate resource or payment conflict
- `201`: resource created
- `204`: resource deleted