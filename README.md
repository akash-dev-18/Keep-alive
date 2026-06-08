# KeepAlive

KeepAlive is a full-stack uptime monitoring app for small teams and personal projects. It watches websites, APIs, SSL certificates, keyword presence, and cron jobs, then records checks, opens incidents, and sends alerts when something breaks.

The project has a Spring Boot backend, a Next.js frontend, PostgreSQL for persistent data, Redis for background check and alert queues, and it is intended to run on a normal Azure VM/VPS with everything inside one Docker Compose stack.

## What It Can Do

- **HTTP and API monitoring**: Check a URL on a fixed interval and mark it up or down based on the expected HTTP status code.
- **Custom HTTP checks**: Configure method, timeout, request headers, request body, expected status, Basic Auth, and Bearer token checks.
- **Keyword monitoring**: Fetch a page and fail the monitor if expected text is missing from the response body.
- **SSL certificate monitoring**: Track certificate issuer, expiry date, days remaining, expired certificates, and certificates close to expiry.
- **Cron heartbeat monitoring**: Create a heartbeat URL for scheduled jobs. If the job stops pinging on time, KeepAlive marks it down.
- **Incidents**: Automatically open incidents when monitors fail and resolve them when the monitor recovers.
- **Email alerts**: Send email notifications for downtime, recovery, missed cron heartbeats, and SSL expiry through Resend.
- **Notification center**: Store in-app notifications and allow users to mark them as read.
- **Alert logs**: Keep a history of alert delivery attempts, including sent/failed status.
- **Public status pages**: Publish selected monitors on a public page with a slug, description, logo URL, color, and optional custom domain field.
- **Dashboard summary**: Show total monitors, active monitors, up/down/unknown state, open incidents, SSL warnings, recent checks, and failures.

## Tech Stack

- **Frontend**: Next.js 16, React 19, TypeScript, Tailwind CSS 4, Clerk, Framer Motion, Recharts
- **Backend**: Spring Boot 4, Java 25, Spring Security, Spring Data JPA, Flyway, Redis
- **Database**: PostgreSQL
- **Queue/cache**: Redis
- **Auth**: Clerk JWTs and Clerk webhooks
- **Email**: Resend
- **Containers**: Docker and Docker Compose

## Project Structure

```text
.
├── backend/                 # Spring Boot API and background workers
├── frontend/                # Next.js app
├── docker-compose.yml       # Local full-stack Docker setup
├── docker-compose.prod.yml  # Production draft; adapt before using on the VM
├── details.md               # Low-level design notes
└── README.md
```

## Requirements

For Docker setup:

- Docker
- Docker Compose

For normal local setup:

- Java 25
- Node.js 22
- npm
- PostgreSQL 17 or compatible PostgreSQL
- Redis

## Environment Variables

The app needs database, Redis, Clerk, Resend, Stripe, and encryption settings.

For Docker Compose, create one root `.env` file because `docker-compose.yml` reads environment values from the project root:

```bash
cp backend/.env.example .env
cat frontend/.env.example >> .env
```

Then edit `.env` and fill in the real values:

```env
DATASOURCE_URL=jdbc:postgresql://localhost:5432/uptime_monitor
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379

CLERK_JWKS_URI=https://your-clerk-domain/.well-known/jwks.json
CLERK_ISSUER=https://your-clerk-domain
CLERK_WEBHOOK_SECRET=whsec_...

RESEND_API_KEY=re_...
RESEND_FROM_EMAIL=alerts@example.com

STRIPE_WEBHOOK_SECRET=whsec_...

KEEPALIVE_ENCRYPTION_SECRET=change-me-to-a-long-random-secret

NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY=pk_test_...
CLERK_SECRET_KEY=sk_test_...
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Important: do not commit real API keys or secrets. If real keys were ever pushed to a public repo, rotate them in Clerk, Resend, Stripe, and any other provider.

## Run With Docker Compose

This is the easiest way to run the complete app locally.

```bash
docker compose up --build
```

Open:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

Stop the stack:

```bash
docker compose down
```

Stop and delete the PostgreSQL volume:

```bash
docker compose down -v
```

The backend automatically runs Flyway migrations on startup.

## VPS Deployment Model

The production setup for this project is not AWS, not managed database hosting, and not managed Redis hosting.

The intended setup is one normal Azure VM/VPS running Docker Compose:

```text
Azure VM / VPS
  |
  |-- Nginx reverse proxy / API gateway
  |-- Next.js frontend container
  |-- Spring Boot backend container
  |-- PostgreSQL container
  |-- Redis container
  |-- Docker volumes for database persistence
```

In this model:

- Nginx receives public traffic on ports `80` and `443`.
- Nginx routes frontend traffic to the Next.js container.
- Nginx routes API traffic such as `/api/*` to the Spring Boot backend container.
- PostgreSQL runs in the same Compose stack, not on a hosted database service.
- Redis runs in the same Compose stack, not on a hosted Redis service.
- Docker volumes keep PostgreSQL data safe across container restarts.

For production, keep PostgreSQL and Redis ports private inside the Docker network unless you specifically need external access. Public users should normally reach only Nginx.

## Run With Docker Images Manually

Use this if you want to build and run images without Compose.

Create a Docker network:

```bash
docker network create keepalive-net
```

Run PostgreSQL:

```bash
docker run -d \
  --name keepalive-postgres \
  --network keepalive-net \
  -e POSTGRES_DB=uptime_monitor \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:17-alpine
```

Run Redis:

```bash
docker run -d \
  --name keepalive-redis \
  --network keepalive-net \
  -p 6379:6379 \
  redis:alpine
```

Build the backend image:

```bash
docker build -t keepalive-backend:local ./backend
```

Run the backend:

```bash
docker run -d \
  --name keepalive-backend \
  --network keepalive-net \
  --env-file .env \
  -e DATASOURCE_URL=jdbc:postgresql://keepalive-postgres:5432/uptime_monitor \
  -e REDIS_HOST=keepalive-redis \
  -p 8080:8080 \
  keepalive-backend:local
```

Build the frontend image:

```bash
set -a
source .env
set +a

docker build \
  --build-arg NEXT_PUBLIC_API_URL=http://localhost:8080 \
  --build-arg NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY="$NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY" \
  -t keepalive-frontend:local \
  ./frontend
```

Run the frontend:

```bash
docker run -d \
  --name keepalive-frontend \
  --network keepalive-net \
  --env-file .env \
  -p 3000:3000 \
  keepalive-frontend:local
```

If you publish images to Docker Hub, GHCR, or another registry, replace `keepalive-backend:local` and `keepalive-frontend:local` with your published image names.

## Run Normally Without Docker

Start PostgreSQL and Redis first. If you do not have them installed locally, you can still run only those two with Docker:

```bash
docker run -d --name keepalive-postgres \
  -e POSTGRES_DB=uptime_monitor \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:17-alpine

docker run -d --name keepalive-redis \
  -p 6379:6379 \
  redis:alpine
```

Create the database if you are using your own PostgreSQL install:

```sql
CREATE DATABASE uptime_monitor;
```

Start the backend:

```bash
cd backend
cp .env.example .env
set -a
source .env
set +a
./mvnw spring-boot:run
```

Start the frontend in a second terminal:

```bash
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

Open `http://localhost:3000`.

## How To Use The App

### 1. Sign In

KeepAlive uses Clerk for authentication. Configure Clerk keys in the environment, start the app, then sign in from the frontend. The backend expects Clerk JWTs on protected API routes.

For best results, configure the Clerk webhook endpoint:

```text
POST http://localhost:8080/api/webhooks/clerk
```

Use a public tunnel such as ngrok if Clerk needs to call your local machine.

### 2. Create A Monitor

Go to `Dashboard -> Monitors -> New Monitor`.

Choose one of these monitor types:

- **HTTP / API**: Provide a URL, method, expected status, timeout, interval, optional headers/body, and optional auth.
- **SSL certificate**: Provide an HTTPS URL. KeepAlive checks certificate validity and expiry.
- **Heartbeat / cron**: Create the monitor, copy the generated heartbeat URL, and call it from your scheduled job.
- **Keyword**: Provide a URL and expected keyword. The monitor fails if the keyword is missing.

Allowed check intervals in the UI are `1`, `2`, `3`, `5`, `10`, `15`, `30`, and `60` minutes. Free users cannot use intervals below 2 minutes.

Allowed timeouts are `5`, `10`, `20`, `30`, and `60` seconds.

### 3. Use Cron Heartbeats

After creating a cron monitor, KeepAlive gives you a URL like:

```text
http://localhost:8080/api/v1/heartbeat/{pingKey}
```

Call that URL when your job succeeds:

```bash
curl http://localhost:8080/api/v1/heartbeat/YOUR_PING_KEY
```

If the job misses its expected window, KeepAlive marks it down and creates an incident.

### 4. Watch Incidents

Open `Dashboard -> Incidents` to see downtime events. Incidents are opened when checks fail and resolved when checks recover.

### 5. Configure Alerts

Open `Dashboard -> Settings -> Notifications` to enable or disable email alerts for:

- Monitor down
- Monitor recovery
- SSL expiry

Open `Dashboard -> Alerts` to see alert delivery logs.

### 6. Create A Public Status Page

Open `Dashboard -> Status Pages`, create a page, choose monitors, and set the slug.

Public pages are available at:

```text
http://localhost:3000/status/{slug}
```

The backend public API is:

```text
GET http://localhost:8080/api/status/{slug}
```

## Backend API Overview

Most API routes require a Clerk Bearer token. Public routes are heartbeat, public status pages, webhooks, and actuator.

```text
GET    /api/dashboard/summary
GET    /api/dashboard/activity

GET    /api/monitors
POST   /api/monitors
GET    /api/monitors/{monitorId}
PUT    /api/monitors/{monitorId}
DELETE /api/monitors/{monitorId}
POST   /api/monitors/{monitorId}/pause
POST   /api/monitors/{monitorId}/resume
GET    /api/monitors/{monitorId}/checks
GET    /api/monitors/{monitorId}/incidents

GET    /api/incidents

GET    /api/notifications
POST   /api/notifications/read-all
POST   /api/notifications/{notificationId}/read

GET    /api/notification-preferences
PUT    /api/notification-preferences

GET    /api/alert-logs

GET    /api/status-pages
POST   /api/status-pages
GET    /api/status-pages/{statusPageId}
PUT    /api/status-pages/{statusPageId}
DELETE /api/status-pages/{statusPageId}

GET    /api/status/{slug}
GET    /api/v1/heartbeat/{pingKey}
GET    /api/ping/{pingKey}

POST   /api/webhooks/clerk
POST   /api/webhooks/stripe
```

## Testing

Run backend tests:

```bash
cd backend
./mvnw test
```

Run frontend lint:

```bash
cd frontend
npm run lint
```

Build frontend:

```bash
cd frontend
npm run build
```

## Notes For Production

- `docker-compose.yml` is the working local compose file.
- `docker-compose.prod.yml` currently looks like a draft and should be adapted before production use because it references services/build contexts that are not fully defined in this repo layout.
- The real production target is a single Azure VM/VPS running frontend, backend, PostgreSQL, Redis, and Nginx through Docker Compose.
- Update backend CORS in `SecurityConfig` before deploying to a real frontend domain. It currently allows local frontend origins.
- Keep PostgreSQL and Redis inside the Compose network. Do not expose them publicly unless there is a specific operational reason.
- Use a long random `KEEPALIVE_ENCRYPTION_SECRET`; changing it later can make stored encrypted monitor credentials unreadable.
- Put Nginx in front of the app with HTTPS before using real Clerk, Stripe, or Resend production secrets.
- Keep webhook secrets separate for Clerk and Stripe.
