# KeepAlive - Backend Component Working

This document explains what every backend file does and how components interact.

---

## Directory Structure

```
backend/src/main/java/akakash/backend/
├── KeepaliveApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   ├── ThreadConfig.java
│   ├── WorkerStartupRunner.java
│   └── SecretEncryptionService.java
├── user/
│   ├── User.java
│   ├── Plan.java
│   ├── UserService.java
│   └── UserRepository.java
├── monitor/
│   ├── Monitor.java
│   ├── MonitorController.java
│   ├── MonitorService.java
│   └── MonitorRepository.java
├── check/
│   ├── CheckJob.java
│   ├── CheckResult.java
│   ├── CheckEnqueuer.java
│   ├── CheckScheduler.java
│   ├── CheckWorker.java
│   ├── CheckResultWriter.java
│   ├── HttpChecker.java
│   ├── KeywordChecker.java
│   ├── SslChecker.java
│   ├── CronChecker.java
│   ├── CronMissedScanner.java
│   ├── SslExpiryScanner.java
│   ├── CheckRetentionJob.java
│   ├── PingController.java
│   ├── MonitorCheck.java
│   └── MonitorCheckRepository.java
├── alert/
│   ├── AlertDispatcher.java
│   ├── AlertWorker.java
│   ├── AlertJob.java
│   ├── AlertLog.java
│   ├── AlertLogRepository.java
│   ├── AlertLogController.java
│   ├── SslAlertSent.java
│   └── SslAlertSentRepository.java
├── incident/
│   ├── Incident.java
│   ├── IncidentController.java
│   ├── IncidentService.java
│   └── IncidentRepository.java
├── notification/
│   ├── Notification.java
│   ├── NotificationController.java
│   ├── NotificationService.java
│   ├── NotificationRepository.java
│   ├── NotificationPreferences.java
│   ├── NotificationPreferencesController.java
│   ├── NotificationPreferencesService.java
│   └── NotificationPreferencesRepository.java
├── statuspage/
│   ├── StatusPage.java
│   ├── StatusPageController.java
│   ├── StatusPageService.java
│   ├── StatusPageRepository.java
│   ├── PublicStatusPageController.java
│   └── UptimeCalculator.java
├── dashboard/
│   ├── DashboardController.java
│   ├── DashboardService.java
│   └── DashboardSummary.java
├── webhook/
│   ├── ClerkWebhookController.java
│   └── StripeWebhookController.java
└── common/
    ├── dto/ (13 files)
    ├── enums/
    │   ├── MonitorStatus.java
    │   └── MonitorType.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── ResourceNotFoundException.java
    │   └── PlanLimitException.java
    └── validation/
        └── MonitorValidator.java
```

---

## Application Entry Point

### `KeepaliveApplication.java`
- Main Spring Boot class with `@SpringBootApplication`
- Enables `@EnableScheduling` so scheduled tasks (`@Scheduled`) can run
- This is the entry point - `main()` method starts the Spring context

---

## Config Package

### `SecurityConfig.java`
- Configures Spring Security as a **stateless OAuth2 Resource Server**
- Validates JWTs from **Clerk** using JWKS URI and issuer URL
- Defines CORS policy: allows `localhost:3000` and `keepalive.akakash.me`
- Marks these endpoints as **public** (no auth needed):
  - `/api/ping/**` - heartbeat pings
  - `/api/v1/heartbeat/**` - heartbeat pings (v1)
  - `/api/status/**` - public status pages
  - `/api/webhooks/**` - webhook receivers
  - `/actuator/**` - health checks
- All other endpoints require a valid Bearer token

### `RedisConfig.java`
- Configures `RedisTemplate<String, Object>` bean
- Uses `StringRedisSerializer` for keys
- Registers `JavaTimeModule` on the primary `ObjectMapper` so `LocalDateTime` and other Java time types serialize correctly to/from Redis

### `ThreadConfig.java`
- Creates a `virtualThreadExecutor` bean using `Executors.newVirtualThreadPerTaskExecutor()`
- Enables `@EnableAsync` for async method execution
- All background workers (check workers, alert workers) run on Java 25 virtual threads

### `WorkerStartupRunner.java`
- Implements `CommandLineRunner` - runs once on app startup
- Spins up **5 virtual threads** for `CheckWorker.processQueue()` (check job consumers)
- Spins up **2 virtual threads** for `AlertWorker.processQueue()` (alert job consumers)
- These threads run indefinitely, consuming jobs from Redis queues

### `SecretEncryptionService.java`
- AES/GCM/256-bit encryption for sensitive data at rest
- Takes the `keepalive.encryption-secret` env var, derives a 256-bit key via SHA-256
- `encrypt(plaintext)` - encrypts with a random IV, returns Base64-encoded ciphertext
- `decrypt(ciphertext)` - extracts IV, decrypts, returns plaintext
- Used to encrypt Basic Auth passwords and Bearer tokens stored in the database

---

## User Package

### `User.java`
- JPA entity mapped to `users` table
- Fields:
  - `id` (UUID, primary key)
  - `clerkUserId` (unique, from Clerk JWT)
  - `email` (unique)
  - `name`
  - `plan` (enum: FREE or PRO)
  - `stripeCustomerId` (for Stripe billing)
  - `stripeSubscriptionId` (for Stripe billing)
  - `createdAt`, `updatedAt`

### `Plan.java`
- Simple enum: `FREE`, `PRO`
- Used to enforce plan limits on monitors and status pages

### `UserService.java`
- `getOrCreateUser(clerUserId, email, name)` - idempotent. If user exists, returns them. If not, creates a new FREE plan user. Uses `@Lazy` self-injection to avoid infinite recursion on `@Transactional` proxy
- `createUser()`, `updateUser()`, `deleteUser()` - basic CRUD

### `UserRepository.java`
- JPA repository
- `findByClerkUserId()` - used by SecurityConfig to load user from JWT
- `findByStripeCustomerId()` - used by Stripe webhook to find user by Stripe customer

---

## Monitor Package

### `Monitor.java`
- JPA entity mapped to `monitors` table - the **central entity** of the app
- Key fields:
  - `id` (UUID), `userId` (FK to users)
  - `name`, `type` (HTTP/SSL/HEARTBEAT/KEYWORD), `url`
  - `pingKey` (unique key for heartbeat pings)
  - `checkIntervalMin`, `httpMethod`, `expectedStatus`, `timeoutSeconds`
  - `status` (UP/DOWN/DEGRADED/UNKNOWN/PAUSED)
  - `requestHeaders`, `requestBody` (for custom HTTP requests)
  - `basicAuthUsername`, `basicAuthPasswordEnc` (encrypted)
  - `bearerTokenEnc` (encrypted)
  - `expectedKeyword` (for keyword monitors)
  - `sslDaysRemaining`, `sslExpiresAt`, `sslIssuer` (SSL monitor data)
  - `consecutiveFailures`, `alertAfterFailures` (alerting config)
  - `lastResponseTimeMs`, `lastCheckedAt`, `lastPingedAt`
  - `expectedNextHeartbeatAt`, `nextCheckAt`
  - `isActive`, timestamps

### `MonitorController.java`
- REST controller at `/api/monitors`
- Extracts user ID from the Clerk JWT (`@AuthenticationPrincipal`)
- Endpoints:
  - `POST /` - create monitor
  - `GET /` - list all user's monitors
  - `GET /{id}` - get single monitor
  - `PUT /{id}` - update monitor
  - `DELETE /{id}` - delete monitor
  - `POST /{id}/pause` - pause monitoring (sets `isActive=false`)
  - `POST /{id}/resume` - resume monitoring (sets `isActive=true`)
  - `GET /{id}/checks` - get check history for this monitor
  - `GET /{id}/incidents` - get incidents for this monitor

### `MonitorService.java`
- Business logic layer
- Sets defaults for new monitors (interval=2, timeout=10, method=GET, expectedStatus=200)
- Generates unique `pingKey` for heartbeat monitors
- Encrypts sensitive fields (basicAuthPassword, bearerToken) before saving
- Invalidates Redis cache on updates

### `MonitorRepository.java`
- JPA repository with custom queries:
  - `findDueForCheck(now)` - finds active monitors where `nextCheckAt <= now` (up to 200)
  - `updateStatus()` - updates monitor status
  - `updateNextCheckAt()` - advances next check time
  - `updateSslDetails()` - updates SSL cert info
  - `updateConsecutiveFailures()` - increments/resets failure counter
  - `updateHeartbeatPing()` - updates last ping time and expected next heartbeat

---

## Check Package (The Check Pipeline)

### `CheckJob.java`
- Simple Java record: `monitorId` (UUID) + `type` (MonitorType)
- This is the message pushed to the Redis queue

### `CheckResult.java`
- Record: `monitorId`, `status`, `responseTimeMs`, `httpStatusCode`, `errorMessage`
- Returned by each checker after performing the check

### `CheckEnqueuer.java`
- Takes a `Monitor` object
- Creates a `CheckJob` record
- Serializes it to JSON and pushes to Redis list `queue:checks`
- Updates the monitor's `nextCheckAt` to `now + checkIntervalMin`

### `CheckScheduler.java`
- Runs `@Scheduled(fixedDelay = 10000)` - every 10 seconds
- Queries PostgreSQL for monitors where `nextCheckAt <= now` (max 200)
- For each due monitor, calls `CheckEnqueuer` to push to Redis queue

### `CheckWorker.java`
- Runs in an infinite loop on virtual threads (5 workers)
- `processQueue()` - pops from Redis `queue:checks` with 5-second timeout (BLPOP)
- Deserializes the `CheckJob`
- Dispatches to the correct checker based on type:
  - `HTTP` -> `HttpChecker`
  - `KEYWORD` -> `KeywordChecker`
  - `SSL` -> `SslChecker`
  - `HEARTBEAT` -> `CronChecker`
- Passes the `CheckResult` to `CheckResultWriter`

### `CheckResultWriter.java`
- Receives a `CheckResult` after a check completes
- Persists a `MonitorCheck` record to the database
- Updates `lastCheckedAt` on the monitor
- Calls `IncidentService.handleCheckResult()` for incident/alert management
- Invalidates the monitor's status cache in Redis

### `HttpChecker.java`
- Makes an HTTP request to the monitor's URL
- Supports: custom headers, request body (POST/PUT/PATCH), Basic Auth (decrypts password), Bearer token (decrypts token)
- Uses Java's `HttpClient` with configurable timeout
- Returns "up" if response status matches `expectedStatus`, "down" otherwise
- Records response time in milliseconds

### `KeywordChecker.java`
- Makes an HTTP GET request to the monitor's URL
- Reads the response body as a string
- Checks if `expectedKeyword` is present in the body (case-sensitive)
- Returns "up" if keyword found, "down" if not found

### `SslChecker.java`
- Opens an SSL socket to the host on port 443
- Reads the X.509 certificate
- Calculates days until expiry
- Extracts the issuer name
- Status logic:
  - **UP**: >30 days remaining
  - **DEGRADED**: 1-30 days remaining
  - **DOWN**: expired
- Updates SSL details on the monitor entity

### `CronChecker.java`
- For heartbeat monitors
- Checks if `lastPingedAt` is within `checkIntervalMin * 2` (grace period)
- If the time since last ping exceeds the threshold, returns "down"
- If never pinged, also returns "down"

### `CronMissedScanner.java`
- Runs `@Scheduled(fixedDelay = 60000)` - every 60 seconds
- Finds all active heartbeat-type monitors
- For each: if `expectedNextHeartbeatAt` has passed and monitor is not already "down", writes a "down" check result via `CheckResultWriter`
- This is the proactive scanner that catches missed heartbeats

### `SslExpiryScanner.java`
- Runs `@Scheduled(cron = "0 0 2 * * *")` - daily at 2:00 AM
- Iterates all active SSL monitors
- Re-checks each certificate
- Sends alerts at thresholds: 30, 15, 7, 3, 1 day(s) before expiry
- Uses `SslAlertSent` table for deduplication (same threshold won't alert twice)

### `CheckRetentionJob.java`
- Runs `@Scheduled(cron = "0 0 3 * * *")` - daily at 3:00 AM
- Deletes `monitor_checks` records older than 90 days
- Keeps the database from growing indefinitely

### `PingController.java`
- Public REST endpoint (no auth)
- `GET /api/ping/{pingKey}` and `GET /api/v1/heartbeat/{pingKey}`
- Rate-limited to 1 request per second per key using Redis
- Finds the monitor by `pingKey`
- Updates `lastPingedAt` to now
- Calculates `expectedNextHeartbeatAt` = now + `checkIntervalMin`
- Returns "pong"
- This is how external cron jobs report they are alive

### `MonitorCheck.java`
- JPA entity mapped to `monitor_checks` table
- Fields: `id`, `monitorId`, `status`, `responseTimeMs`, `httpStatusCode`, `errorMessage`, `createdAt`
- Each row represents one check result

### `MonitorCheckRepository.java`
- JPA repository
- `findByMonitorId()` - check history for a monitor
- `findRecentByMonitorId()` - latest N checks
- `countFailuresInRange()` - count failures in a date range (for uptime calc)
- `countTotalInRange()` - total checks in a date range
- `deleteByCreatedAtBefore()` - retention cleanup

---

## Alert Package (The Alert Pipeline)

### `AlertDispatcher.java`
- The brain of the alerting system
- Called by `IncidentService` when an incident is created or resolved
- Steps:
  1. Checks `NotificationPreferencesService.shouldSendEmail(userId, alertType)` - respects user preferences
  2. Deduplicates via Redis `SETNX` with 1-hour TTL (7 days for SSL alerts)
  3. Saves a `Notification` record for in-app display
  4. Creates an `AlertJob` and enqueues it to Redis `queue:alerts` **after the transaction commits** (via `TransactionSynchronization.afterCommit()`) - prevents sending alerts for rolled-back transactions
- Also handles SSL expiry alerts with separate dedup logic

### `AlertWorker.java`
- Runs on 2 virtual threads (started by `WorkerStartupRunner`)
- `processQueue()` - infinite loop, pops from Redis `queue:alerts` with 5-second timeout
- Deserializes the `AlertJob`
- Sends email via **Resend API**:
  - POST to `https://api.resend.com/emails`
  - Builds styled HTML email with color coding:
    - Red: down/cron_missed alerts
    - Green: recovery alerts
    - Yellow: SSL expiry alerts
    - Blue: default
  - Validates email format before sending
- Saves an `AlertLog` record (sent/failed) for audit trail

### `AlertJob.java`
- Record: `monitorId`, `incidentId`, `alertType`, `recipientEmail`, `monitorName`
- The message pushed to the Redis alert queue

### `AlertLog.java`
- JPA entity mapped to `alert_logs` table
- Audit trail of every email sent or failed
- Fields: `id`, `monitorId`, `incidentId`, `alertType`, `channel` (email), `sentTo`, `status` (sent/failed/pending), `errorMessage`, `sentAt`

### `AlertLogRepository.java`
- JPA repository for querying alert logs by monitor

### `AlertLogController.java`
- REST: `GET /api/alert-logs` - returns all alert logs for the authenticated user's monitors

### `SslAlertSent.java`
- JPA entity mapped to `ssl_alert_sent` table
- Tracks which SSL threshold alerts have been sent per monitor
- Unique constraint on `(monitor_id, threshold_days)` - prevents duplicate SSL alerts

### `SslAlertSentRepository.java`
- JPA repository
- `existsByMonitorIdAndThresholdDays()` - checks if an SSL alert was already sent for a specific threshold

---

## Incident Package

### `Incident.java`
- JPA entity mapped to `incidents` table
- Fields:
  - `id` (UUID), `monitorId`
  - `status` (open/resolved)
  - `startedAt`, `resolvedAt`
  - `durationSeconds` (calculated on resolution)
  - `cause` (e.g., "HTTP 500", "SSL expired in 5 days")

### `IncidentService.java`
- Core incident lifecycle management
- Called by `CheckResultWriter` after every check
- Logic:
  1. Updates `lastResponseTimeMs` on the monitor
  2. Tracks `consecutiveFailures`:
     - If status is DOWN: increments counter
     - If status is UP: resets counter to 0
  3. Updates monitor status (up/down/degraded)
  4. **Incident creation** (when DOWN):
     - If `consecutiveFailures >= alertAfterFailures`:
       - Uses pessimistic lock to check for existing open incident
       - Creates new `Incident` with status="open"
       - Calls `AlertDispatcher.dispatch(monitor, incident, "down")`
  5. **Incident resolution** (when recovering):
     - If previous status was down/degraded and new status is UP:
       - Finds the open incident
       - Sets `resolvedAt`, calculates `durationSeconds`
       - Calls `AlertDispatcher.dispatch(monitor, incident, "recovery")`
  6. Invalidates dashboard cache

### `IncidentController.java`
- REST: `GET /api/incidents?limit=50` - returns all incidents for the user's monitors, capped at 100

### `IncidentRepository.java`
- JPA repository
- `findOpenByMonitorId()` - uses **pessimistic write lock** (`PESSIMISTIC_WRITE`) with 3-second timeout to prevent duplicate incident creation under concurrent checks
- `findByMonitorId()` - all incidents for a monitor
- `findRecentByMonitorIds()` - recent incidents across multiple monitors

---

## Notification Package

### `Notification.java`
- JPA entity mapped to `notifications` table
- In-app notification (not email)
- Fields: `id`, `userId`, `monitorId`, `incidentId`, `title`, `body`, `isRead`, `createdAt`
- Indexed on `(user_id, created_at)` and `(user_id, is_read)` for fast queries

### `NotificationService.java`
- `create()` - creates a notification
- `getNotifications(userId)` - returns all notifications, sorted: unread first, then by newest
- `markAllAsRead(userId)` - marks all as read
- `markAsRead(notificationId, userId)` - marks one as read

### `NotificationController.java`
- REST endpoints:
  - `GET /api/notifications` - list all notifications
  - `POST /api/notifications/read-all` - mark all as read
  - `POST /api/notifications/{id}/read` - mark one as read

### `NotificationPreferences.java`
- JPA entity mapped to `notification_preferences` table
- Per-user email alert toggles:
  - `emailOnDown` (default: true)
  - `emailOnUp` (default: true)
  - `emailOnSslExpiry` (default: true)

### `NotificationPreferencesService.java`
- `shouldSendEmail(userId, alertType)` - maps alert types to preferences:
  - `down` / `cron_missed` -> `emailOnDown`
  - `recovery` -> `emailOnUp`
  - `ssl_expiry` -> `emailOnSslExpiry`
- Auto-creates default preferences if none exist for the user

### `NotificationPreferencesController.java`
- REST:
  - `GET /api/notification-preferences` - get current preferences
  - `PUT /api/notification-preferences` - update preferences

---

## Status Page Package

### `StatusPage.java`
- JPA entity mapped to `status_pages` table
- Fields: `id`, `userId`, `name`, `slug` (unique), `monitorIds` (UUID array), `isPublic`, `customDomain`, `description`, `logoUrl`, `primaryColor` (default: #06b6d4)

### `StatusPageService.java`
- Validates that all monitor IDs belong to the user before creating/updating
- Invalidates Redis cache on changes

### `StatusPageController.java`
- REST (authenticated):
  - `POST /` - create status page
  - `GET /` - list all
  - `GET /{id}` - get by ID
  - `PUT /{id}` - update
  - `DELETE /{id}` - delete

### `PublicStatusPageController.java`
- REST (public, no auth):
  - `GET /api/status/{slug}`
- Returns combined response:
  - Page config (name, slug, description, logo, color)
  - All monitors with their current status
  - 30-day uptime percentages for each monitor
  - Recent incidents across all monitors
- **Cached in Redis for 60 seconds** to handle high traffic

### `UptimeCalculator.java`
- Calculates uptime percentage over N days
- Queries `monitor_checks` in the date range
- Counts "up" + "degraded" as healthy
- Returns percentage rounded to 2 decimal places (e.g., 99.95)

---

## Dashboard Package

### `DashboardSummary.java`
- Java record: `totalMonitors`, `activeMonitors`, `upMonitors`, `downMonitors`, `unknownMonitors`, `sslWarnings`, `openIncidents`

### `DashboardService.java`
- `getSummary()`:
  - Queries all monitors for the user
  - Counts by status (up/down/unknown/active)
  - Counts SSL warnings (<=30 days remaining)
  - Counts open incidents
  - **Cached in Redis for 30 seconds**
- `getActivity(limit)`:
  - Returns recent checks, recent incidents, recent failures
  - Configurable limit (max 50)

### `DashboardController.java`
- REST:
  - `GET /api/dashboard/summary` - aggregate metrics
  - `GET /api/dashboard/activity?limit=10` - activity feed

---

## Webhook Package

### `ClerkWebhookController.java`
- `POST /api/webhooks/clerk`
- Receives Clerk user lifecycle events
- Verifies webhook signature using **Svix** library
- Handles:
  - `user.created` -> creates user in DB via `UserService.getOrCreateUser()`
  - `user.updated` -> updates user email/name
  - `user.deleted` -> deletes user and all associated data

### `StripeWebhookController.java`
- `POST /api/webhooks/stripe`
- Receives Stripe billing events
- Verifies webhook signature using Stripe SDK
- Handles:
  - `checkout.session.completed` -> links `stripeCustomerId` to user, upgrades to PRO
  - `customer.subscription.created` / `updated` -> upgrades to PRO if subscription is active
  - `customer.subscription.deleted` -> downgrades to FREE

---

## Common Package

### DTOs (`common/dto/`)
13 Java records used as API request/response bodies:
- `CreateMonitorRequest`, `UpdateMonitorRequest`, `MonitorResponse`
- `MonitorCheckResponse`
- `IncidentResponse`
- `CreateStatusPageRequest`, `UpdateStatusPageRequest`, `StatusPageResponse`
- `NotificationResponse`
- `NotificationPreferencesResponse`, `UpdateNotificationPreferencesRequest`
- `DashboardActivityResponse`
- `AlertLogResponse`

All use `fromEntity()` static factory methods for entity-to-DTO conversion.

### Enums (`common/enums/`)
- `MonitorStatus`: UP, DOWN, DEGRADED, UNKNOWN, PAUSED
- `MonitorType`: HTTP, SSL, HEARTBEAT (cron), KEYWORD
  - Each type has allowed intervals, timeouts, and alert-after-failure values

### Exceptions (`common/exception/`)
- `GlobalExceptionHandler` (`@RestControllerAdvice`) - catches all exceptions and returns structured JSON:
  - `ResourceNotFoundException` -> 404
  - `PlanLimitException` -> 403
  - Validation errors -> 400
  - Generic exceptions -> 500
- `ResourceNotFoundException` - thrown when entity not found
- `PlanLimitException` - thrown when user exceeds plan limits

### Validation (`common/validation/`)
- `MonitorValidator` - validates monitor creation/update:
  - URL must have http/https scheme and a host
  - Interval must be >= 1
  - Timeout must be one of: 5, 10, 20, 30, 60
  - Alert after failures must be one of: 0, 1, 2, 3, 5
  - HTTP method must be valid
  - Keyword monitors must have `expectedKeyword` set

---

## Background Jobs Summary

| Job | Schedule | What It Does |
|-----|----------|-------------|
| `CheckScheduler` | Every 10 seconds | Finds due monitors, enqueues check jobs to Redis |
| `CronMissedScanner` | Every 60 seconds | Scans heartbeat monitors for missed pings |
| `SslExpiryScanner` | Daily at 2 AM | Checks SSL certs, sends expiry alerts at thresholds |
| `CheckRetentionJob` | Daily at 3 AM | Deletes check records older than 90 days |

---

## Data Flow Summary

```
User creates monitor
        |
        v
CheckScheduler (10s) -> finds due monitors -> pushes to Redis queue:checks
        |
        v
CheckWorker (5 threads) -> pops from queue -> dispatches to checker
        |
        v
Checker (Http/Keyword/Ssl/Cron) -> returns CheckResult
        |
        v
CheckResultWriter -> saves MonitorCheck -> calls IncidentService
        |
        v
IncidentService -> tracks failures -> creates/resolves incidents -> calls AlertDispatcher
        |
        v
AlertDispatcher -> checks preferences -> deduplicates -> saves Notification -> enqueues to Redis queue:alerts
        |
        v
AlertWorker (2 threads) -> pops from queue -> sends email via Resend -> saves AlertLog
```
