
## Overview


## Database & Asynchronous Processing

### Payment Verification Polling Pipeline (Redis)

To handle burst traffic during registration drops without overwhelming the database, the system implements an asynchronous worker pattern:

1. **Postgres Lock:** A scheduled poller runs `SELECT ... WHERE status = 'Accepted' FOR UPDATE SKIP LOCKED`. This grabs unnotified, verified payments without blocking concurrent transactions.
2. **Redis Stream Publish:** The locked records are published to a Redis Stream.
3. **Consumer Groups:** Worker nodes use `XREADGROUP` to pull from the stream, ensuring at-least-once delivery for post-verification tasks (e.g., generating QR codes, sending emails).
4. **Acknowledgment:** Once the worker completes the task, it sends an `XACK`. Unacknowledged messages revert to the pending evaluation list for retry.

### Redis Connection Pooling

The Redis integration utilizes the `LettuceConnectionFactory` combined with `GenericObjectPoolConfig` to maintain a highly concurrent connection pool:

* `maxTotal`: 64 (Aligns with Tomcat thread limits to prevent bottlenecking)
* `maxIdle`: 32 (Buffer for scale-down)
* `minIdle`: 16 (Prevents cold-start latency spikes)

---


# SSN Invente 2026: Payment & Registration System Architecture - Generated with Gemini, Designed by Saipranav

The Invente Payment System is a high-concurrency Spring Boot application built to handle unpredictable registration spikes without dropping transactions or deadlocking the database. The architecture strictly separates HTTP ingress from downstream processing, using PostgreSQL as the absolute source of truth and Redis Streams as the asynchronous message broker.
*Transactional Outbox Pattern Strictly Followed*
## 1. The Technology Stack

* **Core:** Java 21 + Spring Boot 4.2.0
* **Database:** PostgreSQL 
* **Data Access:** MyBatis 
* **Messaging & Caching:** Redis (Lettuce Client with Apache Commons Pool 2 tuning)
* **Storage:** Cloudflare R2 (Receipt persistence)
* **Observability:** Micrometer + Prometheus + Spring Boot Actuator

---

## 2. Data Modeling & Separation of Concerns

The application enforces a strict boundary between what the API receives, how the business logic operates, and how data is persisted.

* **Controllers & DTOs:** The API layer speaks exclusively in Data Transfer Objects (DTOs). Jackson is globally configured to translate incoming `snake_case` JSON into Java `camelCase`. Controllers handle zero business logic.
* **The Service Layer:** Acts as the traffic cop. It accepts DTOs, enforces business constraints (e.g., the 50-team hackathon cap), maps the data into rigid Database Entities, and manages `@Transactional` boundaries.
* **Entities & MyBatis:** Entities are mathematically pure, 1:1 representations of Postgres tables. MyBatis handles the I/O. For complex reads, MyBatis bypasses Entities entirely and hydrates specialized Projection DTOs directly from SQL `JOIN` queries.
* **Database Triggers:** To ensure data integrity, `updated_at` columns are not managed by Java. PostgreSQL `BEFORE UPDATE` triggers handle timestamping autonomously, guaranteeing precision for our polling workers regardless of how the row was mutated.

---

## 3. The Asynchronous Processing Engine

The core technical challenge is safely moving verified payments out of the database and into downstream processes (email, QR generation, roster updates) without blocking API threads or processing the same payment twice. We solve this using a transactional polling pattern feeding into Redis Streams.

### Phase 1: Ingestion & Verification Lock

1. **User Registration:** A student registers and uploads a receipt. The system opens a `@Transactional` block, inserts the `Users` and `TicketPayments` records, sets the status to `NotVerified`, and commits.
2. **Volunteer Action:** A volunteer reviews the S3 receipt and approves it. The Service layer updates the payment status to `Accepted` and writes an audit row to `PaymentVerificationLog` atomically. At this point, the `email_sent` flag remains `NULL`.

### Phase 2: The Scheduled Batch Poller

To prevent database thrashing, we do not trigger downstream events on a per-request basis. Instead, a scheduled worker sweeps the database.

* A `@Scheduled` Spring Boot worker running on a dedicated `ThreadPoolTaskScheduler` executes a batch query:
```sql
SELECT ticket_id, user_id FROM ticket_payments 
WHERE status = 'Accepted' AND email_sent IS NULL 
LIMIT 100 
FOR UPDATE SKIP LOCKED;

```


* **Why `SKIP LOCKED`?** This is for concurrent workers. If two poller nodes run simultaneously, Node B will instantly skip the rows Node A is currently locking, preventing deadlocks and duplicate reads.

### Phase 3: The Handoff (Multi-Threaded Publish)

The DB Poller cannot block while talking to Redis.

1. The poller grabs the locked batch and hands the dataset off to a separate `ExecutorService` (a dedicated thread pool for publishing).
2. These publisher threads iterate over the batch and execute `XADD`, pushing the payment payloads into a Redis Stream.
3. Once the payload is safely committed to Redis, the publisher thread executes `UPDATE ticket_payments SET email_sent = 'queued' WHERE ticket_id IN (...)` and the Postgres row locks are released.

### Phase 4: Redis Stream Consumers

Redis now acts as our at-least-once delivery buffer, absorbing the shock of high-volume verifications.

1. **Consumer Groups:** A fleet of worker nodes continuously listens to the stream using `XREADGROUP`.
2. **Execution:** A worker pulls a message, hits the AWS S3/Email APIs, and generates the final ticket assets.
3. **Finalization:** The worker executes a final `UPDATE ticket_payments SET email_sent = 'sent' WHERE ticket_id = ?`.
4. **Acknowledgment:** Crucially, the worker then sends an `XACK` to Redis. If the worker crashes mid-process before the `XACK`, the message remains in the Redis Pending Evaluation List (PEL), where another worker will automatically claim and retry it after a timeout.
   
###
The Exception: If Redis goes down, your Lettuce client will fail to execute the stream publish and throw a RedisConnectionFailureException (or a timeout exception).

The Automatic Rollback: Spring's transaction manager intercepts this uncaught runtime exception and instantly sends a ROLLBACK command to PostgreSQL.

The Lock Release: The moment PostgreSQL receives the ROLLBACK, it aborts the transaction and natively drops all row locks associated with it.

The records remain safely in the database with their original state (status = 'Accepted' and email_sent IS NULL). Once Redis comes back online, the next scheduled polling cycle will simply pick them up again as if nothing happened.

In your earlier RedisConfig, you properly set commandTimeout(Duration.ofMillis(timeout)) to 2000ms. This is vital. If the Redis server hangs or the network drops, Lettuce will time out in exactly 2 seconds, throw the exception, and trigger the rollback quickly. This prevents long-running open transactions from hoarding locks and exhausting your PostgreSQL connection pool.

---
