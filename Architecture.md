## Overview

## Database & Asynchronous Processing

### Payment Verification & Reminder Polling Pipeline (Redis)

To handle burst traffic during registration drops and enforce automated reminders without overwhelming the database, the system implements an isolated asynchronous worker pattern:

1. **Postgres Lock & Partial Indexes:** Scheduled pollers run `SELECT ... JOIN ... FOR UPDATE OF tps SKIP LOCKED`. This exclusively locks the payment records without locking joined tables (like `users`). Partial indexes (e.g., `WHERE email_sent IS NULL`) ensure instant lock acquisition without full table scans.
2. **Commit-Then-Publish:** To prevent race conditions, the locked records are updated to `queued` in Postgres, and the payload is published to the Redis Stream *only* via `TransactionSynchronizationManager.afterCommit()`.
3. **Consumer Groups & Thread Isolation:** Dedicated worker nodes use `XREADGROUP` to pull from the unified stream (`invente:payments:verified_stream`). Blocking operations are isolated into strict thread pools (`verificationPollerExecutor`, `reminderPollerExecutor`, and `EmailWorker-`) to prevent thread starvation cascades.
4. **Acknowledgment & Fault Tolerance:** Once the worker completes the task, it sends an `XACK`. Unacknowledged messages revert to the Pending Entries List (PEL). A background Sweeper uses `XCLAIM` to re-route idle messages from dead workers, and enforces Poison Pill protection (`getTotalDeliveryCount() > 5`) to permanently drop structurally invalid emails and flag them for manual review.

### Redis Connection Pooling

The Redis integration utilizes the `LettuceConnectionFactory` combined with `GenericObjectPoolConfig` to maintain a highly concurrent connection pool:

* `maxTotal`: 64 (Aligns with Tomcat thread limits to prevent bottlenecking)
* `maxIdle`: 32 (Buffer for scale-down)
* `minIdle`: 16 (Prevents cold-start latency spikes)

---

# SSN Invente 2026: Payment & Registration System Architecture - Documentation with Gemini, Designed by Saipranav

The Invente Payment System is a high-concurrency Spring Boot application built to handle unpredictable registration spikes without dropping transactions or deadlocking the database. The architecture strictly separates HTTP ingress from downstream processing, using an Nginx API Gateway at the edge, PostgreSQL as the absolute source of truth, and Redis Streams as the asynchronous message broker.

*Transactional Outbox Pattern Strictly Followed*

## 1. The Technology Stack

* **Edge / Gateway:** Nginx (SSL Termination, Dynamic CORS, Path Stripping, Rate Limiting)
* **Core:** Java 21 + Spring Boot 4.2.0 (Multi-platform Docker: AMD64/ARM64)
* **Database:** PostgreSQL
* **Data Access:** MyBatis
* **Messaging & Caching:** Redis (Lettuce Client with Apache Commons Pool 2 tuning)
* **Storage:** Cloudflare R2 (Receipt persistence)
* **Observability:** Micrometer + Prometheus + Grafana + Exporters

---

## 2. Data Modeling & Separation of Concerns

The application enforces a strict boundary between edge ingress, internal business logic, and persistent state.

* **The Edge (Nginx):** Handles all cross-origin security, drops malicious requests, and seamlessly proxies traffic to isolated internal Docker networks (e.g., stripping `/fileapi/` down to the Admin Backend).
* **Controllers & DTOs:** The API layer speaks exclusively in Data Transfer Objects (DTOs). Jackson is globally configured to translate incoming `snake_case` JSON into Java `camelCase`. Controllers handle zero business logic.
* **The Service Layer:** Acts as the traffic cop. It accepts DTOs, enforces business constraints (e.g., the 50-team hackathon cap), maps the data into rigid Database Entities, and manages `@Transactional` boundaries.
* **Entities & MyBatis:** Entities are mathematically pure, 1:1 representations of Postgres tables. MyBatis handles the I/O. For complex reads, MyBatis completely eliminates the N+1 problem by hydrating specialized Projection DTOs directly from SQL `JOIN` queries.
* **Database Triggers:** To ensure data integrity, `updated_at` columns are not managed by Java. PostgreSQL `BEFORE UPDATE` triggers handle timestamping autonomously, guaranteeing precision for our polling workers regardless of how the row was mutated.

---

## 3. The Asynchronous Processing Engine

The core technical challenge is safely moving verified payments out of the database and into downstream processes (email, QR generation, roster updates) without blocking API threads or processing the same payment twice. We solve this using a transactional polling pattern feeding into Redis Streams.

### Phase 1: Ingestion & Verification Lock

1. **User Registration:** A student registers and uploads a receipt. The system opens a `@Transactional` block, inserts the `Users` and `TicketPayments` records, sets the status to `NotVerified`, and commits.
2. **Volunteer Action:** A volunteer reviews the receipt and approves it. The Service layer updates the payment status to `Accepted` and writes an audit row to `PaymentVerificationLog` atomically. At this point, the `email_sent` flag remains `NULL`.

### Phase 2: The Scheduled Batch Pollers

To prevent database thrashing, we do not trigger downstream events on a per-request basis. Instead, two distinct scheduled workers (`PaymentVerificationPoller` and `ReminderEmailPoller`) sweep the database on isolated thread pools.

* A `@Scheduled` Spring Boot worker executes a batch query optimized with a partial index:

```sql
SELECT tps.ticket_id, tps.user_id, u.email 
FROM invente_payment_db.public.ticket_payments tps 
JOIN invente_payment_db.public.users u ON tps.user_id = u.user_id 
WHERE tps.status = 'Accepted' AND tps.email_sent IS NULL 
LIMIT 20 
FOR UPDATE OF tps SKIP LOCKED;

```

* **Why `OF tps SKIP LOCKED`?** If two poller nodes run simultaneously, Node B will instantly skip the rows Node A is currently locking. The `OF tps` directive ensures PostgreSQL only locks the payment records, leaving the joined `users` table entirely unlocked for concurrent registrations.

### Phase 3: The Handoff (Commit-Then-Publish)

The DB Poller cannot block while talking to Redis, nor can it risk publishing a message before the database commit completes.

1. The poller grabs the locked batch, evaluates daily quotas via an atomic Redis increment, and executes `UPDATE ticket_payments SET email_sent = 'queued' WHERE ticket_id IN (...)`.
2. Instead of publishing immediately, the worker registers a `TransactionSynchronizationManager.afterCommit()` callback.
3. Once PostgreSQL physically commits the `queued` state and drops the row locks, the callback fires and executes `XADD`, pushing the payment payloads into a Redis Stream.

### Phase 4: Redis Stream Consumers & Fault Tolerance

Redis now acts as our at-least-once delivery buffer, absorbing the shock of high-volume verifications.

1. **Consumer Groups:** A dedicated fleet of `EmailWorker-` threads continuously listens to the stream using `XREADGROUP`.
2. **Execution:** A worker pulls a message, routes it by `email_type`, hits the SMTP/QR libraries, and updates the database to `sent`.
3. **Acknowledgment:** Crucially, the worker then sends an `XACK` to Redis.
4. **The Sweeper (Self-Healing):** If a worker crashes mid-process, the message remains in the Redis Pending Entries List (PEL). A background sweeper running every 5 minutes identifies idle messages and uses `XCLAIM` to assign them to healthy workers.
5. **Poison Pill Protection:** If the Sweeper detects a message has a `getTotalDeliveryCount() > 5` (e.g., a structurally invalid RFC 5321 email address), it forces an `XACK` to permanently drop the message from Redis and updates the database to a manual review state, preventing infinite loops.

The Exception: If Redis goes down during the transaction (e.g., while executing the atomic daily quota check), your Lettuce client will fail and throw a RedisConnectionFailureException.

The Automatic Rollback: Spring's transaction manager intercepts this uncaught runtime exception and instantly sends a ROLLBACK command to PostgreSQL.

The Lock Release: The moment PostgreSQL receives the ROLLBACK, it aborts the transaction and natively drops all row locks associated with it.

The records remain safely in the database with their original state. Once Redis comes back online, the next scheduled polling cycle will simply pick them up again. In your RedisConfig, `commandTimeout(Duration.ofMillis(timeout))` is set to a strict ceiling. If the Redis server hangs, Lettuce times out immediately, throwing the exception and triggering the rollback to prevent long-running open transactions from hoarding locks and exhausting your PostgreSQL connection pool.
