# Invente 2026 Payment Processing Pipeline

## Architecture & Data Flow

The lifecycle of a payment processing event flows through four distinct components:

### 1. The Pollers (`PaymentVerificationPoller` & `ReminderEmailPoller`)

* **Trigger:** Run every 2 seconds via `@Scheduled` on a separate dedicated 5 thread pools.
* **Batching:** Fetch up to 20 eligible payments from PostgreSQL per run, respecting a shared hard daily limit of 1000 emails tracked via Redis keys.
* **Payload Routing:** Inject an `email_type` flag (`tech`, `hack`, or `payment_reminder`) into the payload to dictate downstream worker behavior.
* **State Update:** Mark the fetched database rows as `queued` (updating either the `email_sent` or `reminder_email_sent` columns, depending on the poller).
* **Transaction Synchronization:** To prevent a race condition where workers pull messages before the database commit finishes, the Pollers use `TransactionSynchronizationManager.afterCommit()`. They only execute the Redis `XADD` command to publish the payload after PostgreSQL confirms the `queued` state is permanently saved.

### 2. The Message Broker (Redis Streams)

* **Stream Key:** `invente:payments:verified_stream`. (A unified stream is used for all email types to minimize connections and infrastructure overhead).
* **Consumer Group:** `email-workers-group`.
* **Mechanism:** Distributes messages to workers using the `XREADGROUP` command. This ensures mutual exclusion (no two workers receive the same message simultaneously) and tracks in-flight tasks in a Pending Entries List (PEL).

### 3. The Consumer (`PaymentEmailWorker`)

* **Listening Loop:** A `StreamMessageListenerContainer` continuously polls the Redis Stream using a 1-second block timeout. To prevent `RedisCommandTimeoutException` crashes, the underlying Lettuce client is configured with a broader 10-second command timeout.
* **Concurrency:** When messages arrive, they are dispatched to a dedicated 5-thread execution pool (`EmailWorker-1` to `EmailWorker-5`).
* **Dynamic Routing & Execution:** Extracts the `email_type` field to route the request dynamically (Tech Pass, Hackathon Pass, or Payment Proof Reminder). Executes the heavy SMTP/PDF generation network tasks, updates the specific PostgreSQL status column to `sent`, and only then issues an `XACK` command to Redis.
* **Completion:** The `XACK` command removes the message from the PEL, marking it permanently resolved.

### 4. Fault Tolerance (`PaymentStreamSweeper`)

* **Failure State:** If an `EmailWorker` thread crashes or hangs mid-process, the database remains marked as `queued` and the message sits unacknowledged in the Redis PEL indefinitely.
* **Recovery:** A background `@Scheduled` sweeper runs every 5 minutes. It queries the PEL (`XPENDING`) for any messages that have been stuck for 5 minutes or longer.
* **Re-routing:** Using the `XCLAIM` command, the sweeper forcefully strips ownership of the message from the dead worker thread and pipes the payload directly back into the `PaymentEmailWorker.onMessage()` method for reprocessing.

## Thread Pool Isolation

To prevent thread exhaustion cascading across the application, the system strictly isolates blocking operations:

* **Poller Pool:** 5 threads (`PollerWorker-`) dedicated exclusively to querying PostgreSQL and publishing to Redis. Backed by a queue capacity of 50 to absorb database slowdowns across multiple pollers.
* **Worker Pool:** 5 threads (`EmailWorker-`) dedicated exclusively to executing the slow SMTP/PDF generation tasks.
* **Database Pool (Hikari):** Worker methods are intentionally **not** `@Transactional` (except for the final micro-update). This ensures long-running email network calls do not hold database connections hostage, preserving the Hikari pool for external web traffic.

## Known Architectural Tradeoffs

**At-Least-Once Delivery:** This pipeline prioritizes architectural simplicity over strict exactly-once processing. It does not employ atomic locking on the worker side.

* **The Zombie Worker Scenario:** If an original worker thread stalls for >5 minutes, the Sweeper will claim and reprocess the message. If the original worker later wakes up and finishes its execution, a duplicate email will be sent. This minor redundancy is accepted as a standard tradeoff to avoid the complexity and overhead of distributed database locks.