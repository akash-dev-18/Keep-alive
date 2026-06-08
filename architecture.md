# Architecture

This file explains how I created KeepAlive in a simple way. The goal of this app is clear: users should be able to add their websites, APIs, SSL certificates, cron jobs, and keyword checks, and the app should keep watching them in the background.

I wanted the app to feel simple on the frontend, but the backend needed to handle many checks safely without blocking the whole system. That is why the app is split into frontend, backend, database, Redis, Nginx, and background workers.

I am hosting this like a practical VPS project. Everything runs on one normal Azure VM using Docker Compose. I am not using AWS services, managed PostgreSQL hosting, or managed Redis hosting. PostgreSQL and Redis run as containers in the same Compose stack.

## Basic Idea

KeepAlive has two main parts:

- A **Next.js frontend** where users sign in, create monitors, view dashboards, check incidents, and create status pages.
- A **Spring Boot backend** that stores monitors, runs checks, creates incidents, sends alerts, and exposes APIs.

The backend does not directly run every check inside user requests. User requests only create or update data. The real monitoring work happens in the background.

## High Level Flow

```text
User
  |
  v
Next.js frontend
  |
  v
Spring Boot API
  |
  |---- PostgreSQL stores users, monitors, checks, incidents, alerts
  |
  |---- Redis queues background check jobs and alert jobs
  |
  v
Background workers run HTTP, SSL, keyword, and cron checks
```

## Hosting Setup

The deployment is simple:

```text
Azure VM / VPS
  |
  |-- Docker Compose
      |
      |-- Nginx container
      |-- Frontend container
      |-- Backend container
      |-- PostgreSQL container
      |-- Redis container
```

Nginx works like the entry point for the app. Users do not directly hit the backend container or the frontend container ports in production. They hit the domain, Nginx receives the request, and then Nginx sends the request to the correct container.

The normal idea is:

- Website and dashboard traffic goes to the frontend container.
- API traffic like `/api/*` goes to the backend container.
- PostgreSQL stays inside the Docker network.
- Redis stays inside the Docker network.
- Docker volumes keep database data after container restarts.

This setup is enough for this project because it keeps hosting simple and cheap. I do not need separate managed database hosting or Redis hosting right now.

## Why I Used Next.js For Frontend

I used Next.js because the app needs both public pages and authenticated dashboard pages.

The landing page and public status pages are normal frontend pages. The dashboard pages are protected and call the backend APIs with Clerk authentication.

The frontend mainly does these things:

- Shows the operations dashboard.
- Lets users create HTTP, SSL, cron, and keyword monitors.
- Shows monitor details and check history.
- Shows incidents and alert logs.
- Lets users configure notification preferences.
- Lets users create public status pages.

The frontend does not decide if a monitor is up or down. It only displays the state that the backend calculates.

## Why I Used Spring Boot For Backend

I used Spring Boot because the backend needs strong API structure, background processing, database access, security, and validation.

The backend handles:

- Authentication validation with Clerk JWTs.
- Creating and managing monitors.
- Scheduling checks.
- Running actual HTTP, SSL, keyword, and cron checks.
- Writing check results.
- Opening and resolving incidents.
- Sending alert emails.
- Serving public status page data.
- Handling Clerk and Stripe webhooks.

Spring Boot also makes it easy to organize the app into modules like `monitor`, `check`, `incident`, `alert`, `statuspage`, `notification`, and `webhook`.

## Why I Used PostgreSQL

PostgreSQL is used as the main database because most data in this app is relational.

For example:

- A user owns many monitors.
- A monitor has many check results.
- A monitor can create many incidents.
- An incident can create alert logs.
- A user can create status pages.

PostgreSQL stores important permanent data:

- Users
- Monitors
- Monitor checks
- Incidents
- Notifications
- Alert logs
- Status pages
- Billing plan data

Flyway migrations are used so database schema changes are versioned and run automatically when the backend starts.

In production, PostgreSQL is not hosted separately. It runs as a container on the same Azure VM, and Docker volumes are used so data is not lost when the container restarts.

## Why I Used Redis

I used Redis mainly as a lightweight queue and cache layer.

Monitoring apps need background work. If 100 monitors need to run, I do not want one API request to wait for all of them. Redis helps separate the work.

Redis is used for:

- Check queues
- Alert queues
- Deduplication for alerts
- Small cache invalidation
- Heartbeat ping rate limiting

The backend pushes jobs into Redis queues, and workers consume those jobs in the background.

This keeps the API fast and keeps monitoring work independent from user actions.

In production, Redis also runs as a container in the same Docker Compose stack. I am not using a separate Redis hosting provider.

## Why I Used Virtual Threads

I used Java virtual threads because monitor checks are mostly blocking I/O work.

For example, an HTTP check waits for another server to respond. An SSL check waits for a TLS handshake. Email sending waits for the Resend API. These tasks spend a lot of time waiting, not using CPU.

Without virtual threads, many blocking checks can waste normal platform threads. With virtual threads, the backend can run many checks at the same time without needing complex reactive code.

So instead of writing complicated reactive flows, I can write normal blocking Java code like:

```text
send HTTP request
wait for response
write result
open incident if needed
send alert if needed
```

Virtual threads make that simple style scale better.

## How Monitor Creation Works

When a user creates a monitor from the dashboard:

1. The frontend sends monitor details to the backend.
2. The backend validates the request.
3. The backend checks the user's plan limits.
4. The backend stores the monitor in PostgreSQL.
5. If the monitor is a cron heartbeat, the backend generates a unique ping key.
6. The monitor is ready for background checks.

The supported monitor types are:

- `http`
- `ssl`
- `cron`
- `keyword`

## HTTP Monitor Flow

An HTTP monitor checks if a website or API endpoint is working.

The user can configure:

- URL
- HTTP method
- Expected status code
- Timeout
- Request headers
- Request body
- Basic Auth
- Bearer token
- Alert after failures

The backend sends the request and compares the response status with the expected status. If the expected status matches, the monitor is marked up. If it does not match, or the request fails, the monitor is marked down.

## Keyword Monitor Flow

A keyword monitor is useful when just getting `200 OK` is not enough.

For example, a page may return 200 but the actual content is broken. In that case, the user can provide a keyword like `Welcome` or `Dashboard`.

The backend fetches the page and checks if the expected keyword exists in the response body.

If the keyword is found, the monitor is up. If it is missing, the monitor is down.

## SSL Monitor Flow

An SSL monitor checks the certificate of a domain.

The backend opens an SSL connection to the host, reads the certificate, and stores:

- Expiry date
- Days remaining
- Issuer

If the certificate is expired, the monitor is down. If the certificate is close to expiry, the monitor can be marked degraded and an SSL alert can be sent.

This helps users fix certificate problems before users or browsers start seeing errors.

## Cron Heartbeat Flow

Cron monitoring works differently from HTTP monitoring.

For cron jobs, the app does not call the user's server. Instead, the user's scheduled job calls KeepAlive.

When the user creates a cron monitor, KeepAlive generates a heartbeat URL:

```text
/api/v1/heartbeat/{pingKey}
```

The user's cron job should call this URL when it finishes successfully.

Example:

```bash
curl http://localhost:8080/api/v1/heartbeat/YOUR_PING_KEY
```

The backend records the last ping time. If the cron job does not ping within the expected time window, KeepAlive marks it down and creates an incident.

This is useful for jobs like:

- Database backups
- Email digest jobs
- Payment sync jobs
- Cleanup scripts
- Scheduled imports

## How Background Checks Work

The backend has scheduled logic that finds monitors ready to be checked.

The flow is:

1. Scheduler finds active monitors whose `nextCheckAt` time has arrived.
2. Scheduler pushes check jobs into Redis.
3. Check worker reads jobs from Redis.
4. Worker runs the correct checker based on monitor type.
5. Worker writes the check result into PostgreSQL.
6. Worker updates monitor status.
7. If needed, incident logic opens or resolves incidents.
8. If needed, alert logic creates notifications and queues emails.

This design is important because monitor checks can be slow. Redis queues prevent slow checks from blocking API requests.

## How Incidents Work

Incidents are created when a monitor goes down.

If a monitor fails, the backend checks if there is already an open incident for that monitor. If there is no open incident, it creates one.

When the monitor comes back up, the backend resolves the open incident and stores the duration.

This means users can see:

- What went down
- When it started
- When it recovered
- How long it was down
- What caused the failure

## How Alerts Work

Alerts are also handled through Redis.

When an incident or SSL warning needs an alert:

1. The backend creates an in-app notification.
2. The backend creates an alert job.
3. The alert job is pushed into Redis.
4. Alert worker reads the job.
5. Alert worker sends email through Resend.
6. Backend stores an alert log as sent or failed.

Redis is also used to deduplicate alerts, so the same alert is not sent again and again in a short time.

Users can control email preferences from the settings page.

## How Authentication Works

I used Clerk for authentication.

The frontend gets a Clerk session token. When it calls the backend, it sends the token as a Bearer token.

The backend validates the token using Clerk's JWKS URL and issuer.

Most API routes are protected. Some routes are public:

- Heartbeat ping routes
- Public status page routes
- Webhook routes
- Actuator routes

Clerk webhooks are used to sync users into the backend database. When a user signs up, updates their profile, or deletes their account, Clerk sends an event to the backend.

## How Status Pages Work

A status page lets users show selected monitors publicly.

The user creates a status page with:

- Name
- Slug
- Description
- Selected monitors
- Public/private setting
- Logo URL
- Primary color
- Optional custom domain field

The public frontend page uses the slug and calls the backend public status API.

This allows users to share service health without exposing the private dashboard.

## How Plan Limits Work

The backend enforces plan limits.

Free plan:

- 3 monitors
- 1 status page
- Minimum 2 minute check interval

Pro plan:

- 50 monitors
- 10 status pages
- Minimum 1 minute check interval

These limits are checked in the backend, not only in the frontend. This is important because users can bypass frontend limits by calling APIs directly.

Stripe webhooks can update a user's plan when subscription events happen.

## Why I Encrypt Monitor Secrets

Some monitors need private data like:

- Basic Auth password
- Bearer token

These values should not be stored as plain text. The backend encrypts them before saving them in PostgreSQL.

When a check runs, the backend decrypts the secret only when needed to make the request.

The encryption key comes from:

```text
KEEPALIVE_ENCRYPTION_SECRET
```

This secret should be long, random, and protected.

## Why The App Uses Separate Modules

I separated backend code by feature because it makes the app easier to understand and change.

The main modules are:

- `monitor`: create, update, pause, resume, and delete monitors.
- `check`: run HTTP, SSL, keyword, and cron checks.
- `incident`: create and resolve downtime incidents.
- `alert`: queue and send email alerts.
- `notification`: store in-app notifications and preferences.
- `statuspage`: create and serve public status pages.
- `dashboard`: provide summary and recent activity data.
- `webhook`: handle Clerk and Stripe events.
- `config`: security, Redis, threads, and startup configuration.

This keeps business logic from becoming one large file.

## Why This Architecture Works

This architecture works because each part has a clear job.

The frontend is only responsible for user experience. The backend owns all important decisions. PostgreSQL stores permanent data. Redis handles temporary background work. Nginx routes public traffic to the right container. Virtual threads allow many blocking checks to run without making the code complicated.

The result is an app that can stay simple but still handle real monitoring work on one Azure VM.

## Short Summary

I created KeepAlive as a monitoring system where users can add different types of checks and get notified when something fails.

The most important architecture decisions were:

- Next.js for the user dashboard and public pages.
- Spring Boot for APIs and business logic.
- PostgreSQL for permanent data.
- Redis queues for background checks and alerts.
- Nginx as the reverse proxy/API gateway on the VPS.
- One Docker Compose stack on a normal Azure VM.
- Java virtual threads for running many blocking checks simply.
- Clerk for authentication.
- Resend for email alerts.
- Flyway for database migrations.

This keeps the app practical, understandable, and easier to extend later.
