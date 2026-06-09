# Project Details

KeepAlive is a lightweight uptime monitoring SaaS built to monitor websites, APIs, SSL certificates, cron jobs, and background services from a single dashboard.

The project follows a modern full-stack architecture with a Spring Boot backend, PostgreSQL for persistent storage, Redis for lightweight scheduling and caching, and a Next.js frontend. Instead of using heavyweight message brokers such as Kafka, Redis was chosen to keep infrastructure costs low and deployment simple for small-scale SaaS workloads.

The system periodically schedules monitoring jobs, executes health checks, records monitoring history, creates incidents during outages, and sends email notifications when services go down or recover. SSL certificates are inspected automatically and users receive alerts before expiration.

For cron jobs and background workers, the platform provides Heartbeat monitoring. Users receive a unique heartbeat URL that can be called from scheduled scripts. If the expected heartbeat is not received within the configured interval, an incident is automatically created and alerts are triggered.

The application uses Clerk for authentication and user management. Flyway is used for database versioning and schema migrations to ensure consistent deployments across environments.

Special attention was given to building the project as a production-oriented SaaS rather than a simple CRUD application. Features such as incident tracking, public status pages, response time monitoring, SSL monitoring, notification preferences, and heartbeat-based monitoring were designed to mirror capabilities commonly found in commercial uptime monitoring platforms.

The project is designed to be self-hostable on a single VPS while remaining scalable enough to support multiple users and thousands of monitor checks per day.

---

## Tech Stack & Why

| Technology | What | Why This Over Alternatives |
|------------|------|---------------------------|
| Spring Boot 4 + Java 25 | Backend framework | Virtual threads eliminate thread pool tuning for I/O-bound workloads. One virtual thread per request instead of managing pool sizes. |
| PostgreSQL | Database | UUID primary keys, array columns for status pages, mature and reliable for persistent storage. |
| Redis | Queues + Cache + Dedup | Lightweight compared to Kafka/RabbitMQ. Atomic list operations for job queues, TTL-based caching, SETNX for deduplication. No broker overhead for a small SaaS. |
| Flyway | Schema migrations | Schema-as-code. 6 versioned migrations ensure consistent database state across dev, staging, and production. |
| Clerk | Authentication | Handles JWT validation, user management, webhooks. No need to build and maintain custom auth. |
| Resend | Email delivery | Simple REST API for transactional emails. No SMTP setup, no email infrastructure to manage. |
| Next.js 16 + React 19 | Frontend | Server-side rendering, React Server Components, Tailwind CSS for fast UI development. |
| Docker | Deployment | Single `docker compose up` runs the entire stack. Reproducible environments. |

---

## Architecture

```
                        ┌─────────────────┐
                        │   Next.js UI    │
                        │  (port 3000)    │
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │  Spring Boot    │
                        │  (port 8080)    │
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼────────┐ ┌──────▼──────┐ ┌────────▼────────┐
     │   PostgreSQL    │ │    Redis     │ │   Resend API    │
     │  (persistent)   │ │ (queues +   │ │   (email)       │
     │                  │ │  cache)     │ │                 │
     └─────────────────┘ └──────┬──────┘ └─────────────────┘
                                │
                    ┌───────────┼───────────┐
                    │                       │
           ┌────────▼────────┐    ┌────────▼────────┐
           │  queue:checks   │    │  queue:alerts   │
           │                 │    │                 │
           │  ┌───────────┐  │    │  ┌───────────┐  │
           │  │ Worker x5 │  │    │  │ Worker x2 │  │
           │  │ (virtual) │  │    │  │ (virtual) │  │
           │  └───────────┘  │    │  └───────────┘  │
           └─────────────────┘    └─────────────────┘
```

---

## Two Queue Architecture

The system uses two separate Redis queues to decouple scheduling from execution and alerts from monitoring.

### Queue 1: `queue:checks`

**Purpose:** Decouple monitor scheduling from health check execution.

```
CheckScheduler (every 10s)
        │
        │  Finds monitors where nextCheckAt <= now
        │  Pushes CheckJob (monitorId + type) to Redis
        │
        ▼
   ┌──────────────┐
   │ queue:checks │  (Redis List)
   └──────┬───────┘
          │
          ▼
   5x CheckWorker (virtual threads)
          │
          │  Blocking pop with 5s timeout
          │  Dispatches to correct checker
          │
          ▼
   HttpChecker / SslChecker / KeywordChecker / CronChecker
          │
          │  Returns CheckResult
          │
          ▼
   CheckResultWriter → PostgreSQL
```

**Why separate queue:** The scheduler runs every 10 seconds regardless of how many checks are pending. Workers consume at their own pace. If a check takes 30 seconds (SSL timeout), other checks keep flowing through the queue.

### Queue 2: `queue:alerts`

**Purpose:** Decouple incident management from email delivery.

```
IncidentService
        │
        │  Detects failure, creates incident
        │  Calls AlertDispatcher
        │
        ▼
  AlertDispatcher
        │
        │  Checks notification preferences
        │  Deduplicates via Redis (1h TTL)
        │  Saves in-app notification
        │  Enqueues AlertJob AFTER transaction commit
        │
        ▼
  ┌───────────────┐
  │ queue:alerts  │  (Redis List)
  └──────┬────────┘
         │
         ▼
  2x AlertWorker (virtual threads)
         │
         │  Sends styled HTML email via Resend API
         │  Saves AlertLog (sent/failed)
         │
         ▼
  User's Inbox
```

**Why transaction commit hook:** Alerts are enqueued only after the database transaction commits successfully. This prevents sending alerts for incidents that were rolled back due to errors.

---

## Virtual Threads

The backend runs on Java 25 virtual threads, not traditional platform threads.

### What Changed

| Traditional Threads | Virtual Threads |
|---------------------|-----------------|
| Fixed thread pool size (e.g., 200) | One virtual thread per task (millions possible) |
| Thread blocked during I/O = wasted OS thread | Virtual thread blocked = underlying OS thread is freed |
| Need to tune pool size for workload | No pool tuning needed |
| 200 concurrent HTTP checks = 200 OS threads | 200 concurrent HTTP checks = 200 virtual threads on few OS threads |

### Thread Allocation

```
Application Startup
        │
        ├── 5 CheckWorker virtual threads
        │      (pop from queue:checks, execute checks)
        │
        └── 2 AlertWorker virtual threads
               (pop from queue:alerts, send emails)
```

**Total: 7 long-running virtual threads for background processing.**

### Why Virtual Threads Fit This Workload

The monitoring workload is **I/O-bound**, not CPU-bound:
- HTTP requests: 5-60 seconds of waiting
- Redis blocking pop: 5 seconds of waiting
- Database queries: milliseconds of waiting

With platform threads, a thread making an HTTP request with a 30-second timeout is blocked for 30 seconds doing nothing. With virtual threads, the underlying OS thread is freed to handle other work while the virtual thread waits.

This means the server can handle thousands of concurrent checks without needing hundreds of OS threads.

---

## Monitoring Pipeline

The complete flow from scheduling to result storage:

### Step 1: Scheduling (Every 10 Seconds)

```
CheckScheduler (@Scheduled fixedDelay = 10000ms)
        │
        │  SQL: SELECT * FROM monitors
        │       WHERE is_active = true
        │       AND next_check_at <= now()
        │       LIMIT 200
        │
        │  For each monitor:
        │    - Update nextCheckAt = now + checkIntervalMin
        │    - Create CheckJob { monitorId, type }
        │    - Push to queue:checks
        │
        ▼
   Up to 200 monitors enqueued per cycle
```

### Step 2: Worker Dispatch (5 Virtual Threads)

```
CheckWorker.processQueue()
        │
        │  Blocking pop from queue:checks (5s timeout)
        │
        │  Switch on monitor type:
        │
        ├── type="http"    → HttpChecker.check(monitorId)
        ├── type="keyword" → KeywordChecker.check(monitorId)
        ├── type="ssl"     → SslChecker.check(monitorId)
        └── type="cron"    → CronChecker.evaluate(monitorId)
        │
        │  Returns CheckResult { status, responseTimeMs, httpStatusCode, errorMessage }
        │
        ▼
   CheckResultWriter.write(result)
```

### Step 3: The Four Checkers

```
┌─────────────────────────────────────────────────────────────────┐
│                        HttpChecker                             │
├─────────────────────────────────────────────────────────────────┤
│  Makes HTTP request (GET/POST/PUT/PATCH/DELETE/HEAD)           │
│  Supports: custom headers, request body, Basic Auth, Bearer    │
│  Compares response status to expectedStatus                     │
│  Response time measured in milliseconds                         │
│  Status: UP (match) / DOWN (mismatch or error)                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       KeywordChecker                            │
├─────────────────────────────────────────────────────────────────┤
│  HTTP GET to the URL                                            │
│  Reads response body as string                                  │
│  Checks if expectedKeyword exists in body                       │
│  Status: UP (found) / DOWN (not found)                          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         SslChecker                              │
├─────────────────────────────────────────────────────────────────┤
│  Opens SSL socket to port 443                                   │
│  Reads X.509 certificate                                       │
│  Calculates days until expiry                                   │
│  Thresholds:                                                    │
│    > 30 days → UP                                               │
│    1-30 days → DEGRADED                                         │
│    0 days    → DOWN (expired)                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        CronChecker                              │
├─────────────────────────────────────────────────────────────────┤
│  Reads lastPingedAt from monitor                                │
│  Threshold = checkIntervalMin × 2 (grace multiplier)            │
│  If minutesSinceLastPing >= threshold → DOWN                    │
│  Otherwise → UP                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Step 4: Result Writing

```
CheckResultWriter.write(result)
        │
        │  1. Save MonitorCheck record to PostgreSQL
        │  2. Update monitor.lastCheckedAt
        │  3. Call IncidentService.handleCheckResult()
        │  4. Invalidate Redis cache: cache:monitor:{id}:status
        │
        ▼
   IncidentService handles incident lifecycle
```

---

## Alert Pipeline

### Step 1: Incident Lifecycle (IncidentService)

```
CheckResultWriter calls IncidentService.handleCheckResult()
        │
        │  Update monitor:
        │    - lastResponseTimeMs = result.responseTimeMs
        │    - consecutiveFailures++ (if DOWN) or = 0 (if UP)
        │    - status = new status
        │
        ├── If status is DOWN:
        │     │
        │     │  consecutiveFailures >= alertAfterFailures?
        │     │
        │     ├── YES: Create incident (pessimistic lock)
        │     │        Dispatch "down" alert
        │     │
        │     └── NO: Just update counters, no alert yet
        │
        └── If status recovers (DOWN → UP):
              │
              │  Find open incident
              │  Set resolvedAt = now
              │  Calculate durationSeconds
              │  Dispatch "recovery" alert
```

### Step 2: Alert Dispatching (AlertDispatcher)

```
AlertDispatcher.dispatch(monitor, incident, alertType)
        │
        │  1. Check notification preferences
        │     - emailOnDown (for down/cron_missed)
        │     - emailOnUp (for recovery)
        │     - emailOnSslExpiry (for ssl_expiry)
        │     - If disabled → skip
        │
        │  2. Deduplication via Redis
        │     Key: alert:sent:{monitorId}:{type}
        │     TTL: 1 hour (7 days for SSL)
        │     If key exists → skip (already sent recently)
        │
        │  3. Save Notification record (in-app notification)
        │
        │  4. Create AlertJob { monitorId, incidentId, type, email, name }
        │
        │  5. Enqueue AFTER transaction commit
        │     (prevents alerts for rolled-back transactions)
        │
        ▼
   Push to queue:alerts
```

### Step 3: Email Delivery (AlertWorker)

```
AlertWorker (2 virtual threads)
        │
        │  Blocking pop from queue:alerts (5s timeout)
        │
        │  Validate email format
        │
        │  Send via Resend API:
        │    POST https://api.resend.com/emails
        │    Headers: Authorization: Bearer {key}
        │    Body: { from, to, subject, html }
        │
        │  Email styling:
        │    Down/cron_missed → Red (#ef4444)
        │    Recovery → Green (#10b981)
        │    SSL expiry → Amber (#f59e0b)
        │
        │  Save AlertLog (status: sent/failed)
        │
        ▼
   User receives email
```

---

## Key Design Decisions

### 1. Pessimistic Locking on Incidents

**Problem:** Two check workers might detect the same monitor failure simultaneously and try to create two incidents for the same monitor.

**Solution:** JPA pessimistic write lock with 3-second timeout on the `findOpenByMonitorId` query. Combined with a double-check pattern, this ensures only one incident is created per failure event.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
})
@Query("SELECT i FROM Incident i WHERE i.monitorId = :monitorId AND i.status = 'open'")
Optional<Incident> findOpenByMonitorId(@Param("monitorId") UUID monitorId);
```

### 2. Transaction Commit Hook for Alerts

**Problem:** If the incident creation transaction rolls back, the alert should not be sent.

**Solution:** Alert jobs are enqueued using `TransactionSynchronizationManager.registerSynchronization()`. The job is only pushed to Redis in the `afterCommit()` callback. If the transaction rolls back, the dedup key is deleted to allow future retries.

### 3. AES-256-GCM Encryption at Rest

**Problem:** Users provide Basic Auth passwords and Bearer tokens for monitored endpoints. These must be stored securely.

**Solution:** AES-256-GCM encryption with random IV per encryption. The encryption key is derived via SHA-256 from the `keepalive.encryption-secret` environment variable. Encrypted values are stored as Base64. Decryption happens only at check time. API responses never return actual secrets.

### 4. Redis Over Kafka

**Problem:** Need job queues for background processing.

**Why not Kafka:** Kafka requires a separate cluster, Zookeeper (or KRaft), and significant operational overhead. For a small SaaS with a few thousand checks per day, Kafka is overkill.

**Why Redis:** Atomic list operations (`rightPush`/`leftPop`), built-in TTL for caching and dedup, single dependency, trivial to deploy. Redis handles queues, caching, rate limiting, and deduplication in one service.

### 5. 90-Day Retention with Auto-Purge

**Problem:** Check results accumulate fast. A monitor checked every 2 minutes generates 720 records per day.

**Solution:** A daily job (`CheckRetentionJob` at 3 AM) deletes `monitor_checks` records older than 90 days. This keeps the database lean while retaining enough history for uptime calculations.

### 6. Heartbeat Rate Limiting

**Problem:** A misconfigured cron job might hit the ping endpoint thousands of times per second.

**Solution:** Redis-based rate limiting. `ratelimit:ping:{pingKey}` with 1-second TTL. Only 1 request per second per ping key is allowed. Additional requests return HTTP 429.

---

## What Makes It Production-Grade

| Feature | How It Works |
|---------|-------------|
| **Incident Lifecycle** | Incidents are created on failure, tracked with duration, resolved on recovery. Pessimistic locking prevents duplicates. |
| **Public Status Pages** | Share service health with customers. 30-day uptime calculation. Cached in Redis for 60 seconds. |
| **SSL Certificate Monitoring** | Automatic certificate expiry detection. Alerts at 30/15/7/3/1 day thresholds. Deduplicated per threshold. |
| **Heartbeat Monitoring** | Unique ping URL for cron jobs. 2x grace multiplier. CronMissedScanner detects missed heartbeats every 60 seconds. |
| **Notification Preferences** | Users control which alerts they receive (down, recovery, SSL expiry). Defaults to all enabled. |
| **Alert Audit Trail** | Every email sent or failed is logged with timestamps, recipient, and error messages. |
| **Encrypted Secrets** | Basic Auth passwords and Bearer tokens encrypted with AES-256-GCM. Never exposed in API responses. |
| **Automatic Cleanup** | Check records older than 90 days are purged daily. No manual database maintenance. |
| **Rate Limiting** | Heartbeat endpoints rate-limited to 1 req/sec per key via Redis. Prevents abuse. |

---

## Background Jobs

| Job | Schedule | Purpose |
|-----|----------|---------|
| `CheckScheduler` | Every 10 seconds | Finds due monitors, enqueues checks to Redis |
| `CronMissedScanner` | Every 60 seconds | Detects missed heartbeat pings, writes "down" results |
| `SslExpiryScanner` | Daily at 2:00 AM | Scans SSL certificates, sends expiry alerts at thresholds |
| `CheckRetentionJob` | Daily at 3:00 AM | Purges check records older than 90 days |
| `CheckWorker` (x5) | Continuous | Processes check job queue (blocking pop) |
| `AlertWorker` (x2) | Continuous | Processes alert job queue, sends emails |

---

## Database Schema (9 Tables)

| Table | Purpose |
|-------|---------|
| `users` | User accounts linked to Clerk. Plan, Stripe IDs. |
| `monitors` | All monitor configuration. URL, type, interval, auth, SSL details. |
| `monitor_checks` | Individual check results. Status, response time, HTTP code. 90-day retention. |
| `incidents` | Open/resolved incident records. Duration, cause. |
| `alert_logs` | Audit trail of sent/failed emails. |
| `notifications` | In-app notification records. |
| `status_pages` | Public status page configurations. Slug, monitors, branding. |
| `ssl_alert_sent` | Deduplication tracker for SSL expiry alerts. |
| `notification_preferences` | Per-user email notification toggles. |

---

## Security

- **Authentication:** Clerk JWT (OAuth2 Resource Server). Stateless. No HTTP sessions.
- **Public endpoints:** `/api/ping/**`, `/api/v1/heartbeat/**`, `/api/status/**`, `/api/webhooks/**`, `/actuator/**`
- **CORS:** Configured for frontend origins. Credentials allowed.
- **Encryption:** AES-256-GCM for secrets at rest. SHA-256 key derivation.
- **Webhook verification:** Svix for Clerk, Stripe SDK for Stripe.

---

## Deployment

- **Target:** Single VPS (Azure/DigitalOcean/AWS EC2)
- **Stack:** Docker Compose (frontend + backend + PostgreSQL + Redis)
- **CI/CD:** GitHub Actions builds Docker images, pushes to registry, deploys via SSH
- **Reverse proxy:** Nginx (ports 80/443)
- **Database:** Managed by Flyway migrations on startup
