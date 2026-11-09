# AyurNexa Backend API

Spring Boot 4 REST API backed by MongoDB. Application endpoints require `X-API-Key`; routes marked with `@JWTAnnotation` also require a user JWT.

## Environment

Copy `.env.example` to `.env` for local development and replace the placeholders. The application imports `.env` from the project root. Never commit `.env` or put real credentials in README, source code, or frontend bundles. In Railway, configure the same names under the backend service's Variables.

| Variable | Required | Purpose |
| --- | --- | --- |
| `PORT` | Platform supplied; local default `8080` | HTTP listener port |
| `MONGODB_URI` | No; defaults to local MongoDB | MongoDB connection string |
| `JWT_SECRET` | Yes | HMAC signing and verification key; provide at least 32 random bytes |
| `JWT_EXPIRATION_MS` | No; defaults to `3600000` | JWT lifetime in milliseconds |
| `API_KEY` | Yes | Value expected in `X-API-Key` for `/api/**` |
| `CORS_ALLOWED_ORIGINS` | No; defaults to `*` | Comma-separated browser frontend origins permitted to call the API; `*` permits all origins |
| `PAYU_CLIENT_ID` | Required for online payments | PayU Payment Links OAuth client ID; backend only |
| `PAYU_CLIENT_SECRET` | Required for online payments | PayU Payment Links OAuth client secret; backend only |
| `PAYU_MERCHANT_ID` | Required for online payments | PayU merchant ID used in the Payment Links API header |
| `PAYU_ENVIRONMENT` | No; defaults to `uat` | `uat` for testing or `production` for live payments |
| `SUPABASE_URL` | Required for uploads | Supabase project URL; also used to derive the S3-compatible endpoint |
| `SUPABASE_S3_ACCESS_KEY_ID` | Required for uploads | Supabase Storage S3 access key ID; backend only |
| `SUPABASE_S3_SECRET_ACCESS_KEY` | Required for uploads | Supabase Storage S3 secret access key; backend only |
| `SUPABASE_S3_REGION` | Required for uploads | Supabase project Storage region |
| `SUPABASE_S3_ENDPOINT` | No | Optional S3 endpoint override; defaults to `${SUPABASE_URL}/storage/v1/s3` |
| `SUPABASE_BUCKET` | No; defaults to `products` | Supabase Storage bucket used for uploads |

Generate local secrets, for example:

```bash
openssl rand -base64 32
openssl rand -hex 32
```

Use separate values for `JWT_SECRET` and `API_KEY`. `CORS_ALLOWED_ORIGINS=*` permits requests from every origin. The API does not enable cookie credentials; browser clients should send the API key and bearer JWT headers without `credentials: "include"`. Restrict this setting to trusted frontend origins in production. Provide comma-separated exact origins (scheme, host, and optional port; no paths), for example `https://shop.example.com,http://localhost:5173`.

## Important API-Key Note

`X-API-Key` is an additional shared request gate, not user authentication. Any key embedded in browser JavaScript or a mobile app can be extracted by users; it cannot protect a public frontend API from unauthorized callers. Keep user authentication and authorization on the JWT and backend. For production public clients, consider removing the static API-key requirement from browser-facing routes and using rate limits/origin controls instead. Never expose `PAYU_CLIENT_SECRET` or `JWT_SECRET` to the frontend.

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

After Google sign-in, protected operations additionally send:

```http
Authorization: Bearer <JWT returned by login>
```

## Authentication

### Google sign in or create an account

The backend currently supports user sign-in/sign-up through Google. There are no email/password `/register` or `/login` endpoints.

The frontend obtains a Google ID token, then sends it to `POST /api/auth/google` (API key required; no JWT):

```bash
curl -i -X POST "$BASE_URL/api/auth/google" \
	-H "X-API-Key: $API_KEY" \
	-H "Content-Type: application/json" \
	-d '{"credential":"<Google ID token>"}'
```

The backend verifies the Google token and its email. If that email already exists, it signs in that user; otherwise, it creates a user account. The response includes the backend's own `token`, `tokenType` (`Bearer`), `expiresIn` (seconds), and safe user details. Use this JWT for subsequent protected requests.

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

# Create PayU hosted payment link
curl -i -X POST "$BASE_URL/api/payments" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"amount":499.00,"paymentMethod":"UPI","idempotencyKey":"checkout-<unique-request-id>"}'

# List current user's payments / get one / update eligible pending payment / delete eligible pending payment
curl -i "$BASE_URL/api/payments" -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
curl -i "$BASE_URL/api/payments/$PAYMENT_RECORD_ID" -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
curl -i -X PUT "$BASE_URL/api/payments/$PAYMENT_RECORD_ID" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" -d '{"amount":499.00,"paymentMethod":"COD"}'
curl -i -X DELETE "$BASE_URL/api/payments/$PAYMENT_RECORD_ID" \
	-H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
```

The online payment response contains `paymentLinkUrl`; redirect the customer to it for PayU-hosted checkout. After PayU redirects the customer back to your frontend, verify the local payment:

```bash
curl -i -X POST "$BASE_URL/api/payments/$PAYMENT_RECORD_ID/verify" \
  -H "X-API-Key: $API_KEY" \
  -H "Authorization: Bearer $JWT"
```

The backend checks the link status with PayU and only marks the record `SUCCESS` when the amount collected matches the saved amount. Do not mark a payment successful from the frontend. A payment with a PayU payment link or a terminal status cannot be edited/deleted.

The OAuth Payment Links API uses the client ID, client secret, and merchant ID. The classic PayU merchant key and salt are not used by this OAuth flow. Keep PayU credentials server-side, and rotate any credentials pasted into chat or otherwise exposed.

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

## Other Routes

`GET /api/test` and `GET /api/mongo/test-mongo` require `X-API-Key`. The Mongo ping route reports database connectivity; it is not a general DB health endpoint.

## Supabase Storage Uploads

Uploads go from the frontend to this backend, then from the backend to Supabase's S3-compatible API. The browser never calls Supabase Storage directly, so Supabase bucket CORS settings are not needed for this upload path. S3 credentials stay on the backend; upload metadata is saved in MongoDB's `uploads` collection.

The upload endpoint requires `X-API-Key` and a user JWT. Send multipart form data (do not set `Content-Type` manually; curl supplies the multipart boundary):

```bash
curl -i -X POST "$BASE_URL/api/uploads" \
  -H "X-API-Key: $API_KEY" \
  -H "Authorization: Bearer $JWT" \
  -F "file=@./image.jpg"
```

The `201` response includes an absolute `url` for displaying the uploaded image. It is a random, unguessable capability URL, so anyone who has the link can view the file; do not treat it as private. Use the URL directly as an image `src` or fetch it without Supabase credentials:

```bash
curl -i "$UPLOAD_URL" -o downloaded-file
```

For private authenticated downloads, use the returned `id` instead; that route requires the uploader's API key and JWT:

```bash
curl -i "$BASE_URL/api/uploads/$UPLOAD_ID" \
  -H "X-API-Key: $API_KEY" \
  -H "Authorization: Bearer $JWT" \
  -o downloaded-file
```

For browser API calls, configure `CORS_ALLOWED_ORIGINS` to include the exact frontend origin (for example `https://shop.example.com` or `http://localhost:5173`). Do not enable `credentials: "include"`; authentication is sent through the allowed `X-API-Key` and `Authorization` headers.

## Common Responses

- `400`: invalid request body or amount mismatch
- `401`: missing/invalid API key or missing/invalid/expired JWT
- `403`: authenticated user lacks the required admin role
- `404`: requested resource does not exist or does not belong to the caller
- `409`: duplicate resource or payment conflict
- `201`: resource created
- `204`: resource deleted