# Controller request-object refactor

## Status

Source refactor implemented; **build and runtime verification are still pending**.
All 16 controllers and the supporting Java backend were inspected. The 27 ordinary JSON-body endpoints in 13 controllers now use named request classes in `com.worksure.web.request`.

No frontend, schema, dependency, security configuration, storage, or JWT changes. Nothing was pushed to GitHub.

## Why request objects?

A request object describes the data an endpoint accepts. For example, `CreateBookingRequest` exposes `serviceId`, `scheduledAt`, `address`, and `notes` through getters/setters. `@JsonProperty("service_id")` preserves the existing JSON name. The controller still performs authorization, validation, and database operations.

These are simple Java classes, not database entities. Fields are private, getters expose typed values, and setters receive JSON values. No service/repository redesign or new framework was introduced.

## Controllers inspected and changed

All filenames below are under `backend/src/main/java/com/worksure/web/`. All request filenames below are under its `request/` subdirectory, with `.java` appended.

| Controller changed | Method and route | Request class created |
| --- | --- | --- |
| AuthController.java | POST /api/auth/register | RegisterRequest |
| AuthController.java | POST /api/auth/login | LoginRequest |
| AuthController.java | POST /api/auth/forgot-password | ForgotPasswordRequest |
| AuthController.java | POST /api/auth/reset-password | ResetPasswordRequest |
| AuthController.java | PATCH /api/auth/fcm-token | UpdateFcmTokenRequest |
| BookingController.java | POST /api/bookings | CreateBookingRequest |
| BookingController.java | PATCH /api/bookings/{id}/status | UpdateBookingStatusRequest |
| BookingController.java | PATCH /api/bookings/{id}/tracking | UpdateBookingTrackingRequest |
| CartController.java | POST /api/cart/items | AddCartItemRequest |
| CartController.java | PATCH /api/cart/items/{itemId} | UpdateCartItemRequest |
| CartController.java | POST /api/cart/checkout | CheckoutRequest |
| ChatController.java | POST /api/chat/{bookingId} | SendChatMessageRequest |
| ComplaintController.java | POST /api/complaints | CreateComplaintRequest |
| ContactController.java | POST /api/contact | ContactRequest |
| WishlistController.java | POST /api/wishlist | AddWishlistItemRequest |
| ReviewController.java | POST /api/reviews | CreateReviewRequest |
| ServiceController.java | POST /api/services | CreateServiceRequest |
| ServiceController.java | PATCH /api/services/{id} | UpdateServiceRequest |
| UserController.java | PATCH /api/users/profile | UpdateProfileRequest |
| WorkerController.java | PATCH /api/workers/me | UpdateWorkerProfileRequest |
| AdminController.java | PATCH /api/admin/documents/{id} | ReviewDocumentRequest |
| AdminController.java | PATCH /api/admin/users/{id}/ban | UpdateUserBanRequest |
| AdminController.java | PATCH /api/admin/users/{id}/suspend | SuspendUserRequest |
| AdminController.java | PATCH /api/admin/complaints/{id} | UpdateComplaintRequest |
| PaymentController.java | POST /api/payments/mock | MockPaymentRequest |
| PaymentController.java | POST /api/payments/stripe/create-intent | CreatePaymentIntentRequest |
| PaymentController.java | POST /api/payments/stripe/confirm | ConfirmPaymentRequest |

Also inspected, unchanged:

- `HealthController.java`
- `NotificationController.java`
- `VerificationDocumentController.java`

## Compatibility choices

- Existing snake_case API fields and controller response structures remain.
- Existing controller validation, role checks, SQL ownership checks, booking transition checks, and the verification transaction annotation remain.
- No validation annotations were added: moving checks ahead of controller authorization or replacing messages would alter existing behavior.
- `UpdateProfileRequest`, `UpdateWorkerProfileRequest`, and `UpdateServiceRequest` track whether each field was supplied. Omission leaves it unchanged; explicit null still reaches the existing update logic.
- Empty profile updates still produce `No fields to update`; empty worker/service updates still return the current record.
- Service updates still do not accept `category_id` or ownership fields. Extra unsupported fields remain ignored.
- Nullable wrapper types avoid inventing zero/false values when inputs are absent.
- Default-initialized string fields retain the distinction between missing values and explicit null used by the old `getOrDefault` calls.
- `RequestDeserializers.java` preserves existing flexible boolean inputs, defaulted quantity/duration parsing, and review-rating parsing (fractional ratings remain invalid). Identifiers accept integral numeric values/strings without silently truncating a fractional ID into another record.
- `RequestJson.java` preserves structured JSON, already-encoded JSON strings, and null for the existing `images` and `availability` JSON fields. These fields use `JsonNode` because the current API does not enforce a fixed nested schema; no new nested fields were invented.
- Contact submissions still print their supported fields; logging a bean's default object identity would have lost that information.

### Input-type limitation

Typed binding necessarily changes handling of some malformed input that previously flowed through an unrestricted map. For example, objects supplied as text and nonnumeric/oversized IDs fail during JSON binding instead of reaching SQL. A fractional ID is rejected rather than silently truncated by Jackson. The existing global exception handler is unchanged, so some binding failures can still produce its generic 500 error envelope. Existing controller validation messages remain for correctly shaped requests, including missing required values and invalid statuses. Live compatibility testing is still required before claiming all behavior verified.

## Endpoints that do not need a request DTO

- Every GET endpoint: these have no JSON body and already use typed query/path parameters, or no input at all. This includes public service/worker browsing, authenticated profiles/history, admin lists/counts, notifications, health, and protected document download.
- DELETE `/api/services/{id}`, `/api/cart/items/{itemId}`, `/api/wishlist/{serviceId}`: the typed path identifier is the entire input.
- PATCH `/api/notifications/{id}/read` and POST `/api/notifications/read-all`: no request body is used.
- POST `/api/users/avatar`: already typed `MultipartFile` named `avatar`.
- POST `/api/services/{id}/images`: already typed `MultipartFile[]` named `images`, plus the service path ID.
- POST `/api/workers/me/documents`: already typed `MultipartFile` named `file` and optional String `doc_type`. Keeping these preserves the existing multipart contract and required-file handling.
- POST `/api/payments/stripe/webhook`: intentionally retains `byte[]` and the `Stripe-Signature` header. Stripe verifies the original payload; deserializing and reserializing it would invalidate signatures.

**No controller still accepts a Map JSON request body.** Response/DB-row maps and internal audit/realtime metadata remain unchanged. The compatibility deserializers inspect individual legacy scalar values internally; they are not unstructured endpoint bodies.

## Files added besides the 27 request classes

- `backend/src/main/java/com/worksure/web/request/RequestDeserializers.java`
- `backend/src/main/java/com/worksure/web/request/RequestJson.java`
- `backend/src/test/java/com/worksure/web/request/RequestCompatibilityTest.java`
- `backend/src/test/java/com/worksure/web/ControllerRequestTest.java`
- `docs/controller-request-refactor.md` (this report)

No files deleted.

## Checks actually performed

Source inspection and automated source comparisons passed:

1. All route annotations match the pre-refactor source.
2. All explicit `ApiException` validation/error statements match.
3. All `SecurityUtils` authentication/authorization calls match.
4. Every controller request getter/presence method exists in its request class.
5. No Map request bodies remain; 27 request DTOs are referenced.
6. Checksums confirm only the 13 existing controller files changed. Frontend, schema, security, storage, configuration, dependencies, and other pre-existing files are unchanged.

Source snapshot: `/tmp/worksure-dto-before.tar.gz`; original-file checksum manifest: `/tmp/worksure-dto-before.json`.

### Build outcome

`./gradlew build --offline` could not start Gradle because its cache lock is outside the writable sandbox. The elevated build was rejected by automatic approval review because the approval service returned an authentication/API-key error. This is an environment block, **not a successful build or an observed Java compilation failure**.

The local server/process check was separately blocked by the same approval-service failure. No live requests or database mutations were performed during this task. No test accounts, bookings, reviews, payments, or documents were created.

### Tests added, not yet executed

20 JUnit tests across the two new test files cover:

- Snake_case fields and numeric strings; identifier precision.
- Omitted versus null PATCH fields and default values.
- Legacy boolean, quantity/duration, and rating conventions.
- JSON-column values and ignored unsupported fields.
- MVC login binding/response and registration validation/role restriction.
- Customer-only booking creation and anonymous rejection at the controller layer.
- Worker transition validation and assigned-worker authorization.
- Customer/worker profile and service partial updates.
- Review validation messages.
- Document approval-count decision and preservation of the transaction annotation.
- Admin ban/suspend request fields.
- Optional checkout body.

The MVC tests use mocked database/storage/realtime dependencies and a supplied security principal. They do **not** prove MariaDB integration, the actual Spring Security filter chain, JWT validity, transaction rollback, document download privacy, or Stripe connectivity.

## Remaining verification

After build access is restored:

```bash
cd /home/samir/Documents/worksure/backend
./gradlew build
./gradlew bootRun
```

Run the existing real API workflows against `http://localhost:5000`:

1. Customer and worker registration; valid/invalid login and `/api/auth/me`.
2. Customer booking creation; worker accept/start/complete; invalid transition and admin/customer/other-worker rejection.
3. Worker service creation/edit/deletion of an unbooked test service, ownership rejection, and both profile updates (including omission/null cases).
4. Review of a completed booking; premature/duplicate review rejection.
5. Admin ban/unban, suspend/clear suspension, and complaint decisions using synthetic accounts/records.
6. Worker multipart upload; admin approve/reject ordering; private document access; transaction/rollback regression.
7. Remaining cart/chat/contact/wishlist/password reset/FCM request endpoints and mock payments. Stripe success paths need configured test keys; do not make real charges.

Do not mark this refactor build-verified or submission-ready until these checks complete. UI work and GitHub publishing remain outside this task.
