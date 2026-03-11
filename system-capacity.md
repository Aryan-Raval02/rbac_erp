# System Capacity & Performance Architecture Document

## 1. Current Architecture Overview
The current system is a monolithic Spring Boot application designed for a multi-tenant ERP environment. It utilizes a shared-database, isolated-schema multi-tenancy model backed by PostgreSQL. 

**Core Components:**
*   **Application Server:** Embedded Tomcat (Stateless, single JVM instance).
*   **Database:** PostgreSQL with schema-based isolation (`public` for global metadata/root users, tenant-specific schemas for client data).
*   **Connection Pool:** HikariCP.
*   **Security Context:** Spring Security with JWT token-based authentication.
*   **Authorization:** Hybrid RBAC. Permissions are loaded eagerly during the authentication phase and cached within the JWT or validated per request against a cached/in-memory principal.
*   **Session Management:** Completely stateless; no HTTP sessions are relied upon, minimizing memory footprint per user.
*   **Caching:** No distributed cache (Redis); reliant on local JVM memory (Caffeine or basic Maps) or direct database queries.

---

## 2. Request Lifecycle & Bottlenecks
A typical API request follows this lifecycle:
1.  **Ingress:** Tomcat Acceptor thread receives the TCP connection.
2.  **Worker Thread:** A Tomcat worker thread is assigned from the pool to process the HTTP request.
3.  **Authentication/Filter Chain:**
    *   `JwtAuthFilter` intercepts the request.
    *   Extracts and parses the JWT token (CPU intensive: cryptographic signature verification).
    *   Resolves the user identity and tenant context.
4.  **Tenant Resolution:** The TenantContext is set for the current thread.
5.  **Controller/Service:** Business logic executes.
6.  **Database Interaction:**
    *   Hibernate requests a connection from HikariCP.
    *   **Context Switch:** A JDBC connection `SET search_path TO tenant_schema` is executed (Overhead: 1 round-trip to DB or lightweight if pooled properly, though typically requires execution on borrowed connections).
    *   Query execution.
7.  **Response:** JSON serialized and returned; Tomcat thread released.

**Primary Bottlenecks:**
1.  **Database Connection Pool Exhaustion:** With a default of 10 connections, concurrent complex ERP queries will quickly queue requests.
2.  **Schema Switching Overhead:** Executing `SET search_path` on every borrowed connection adds latency.
3.  **CPU Bound JWT Verification:** RSA/HMAC validation on every request per thread.
4.  **Synchronous I/O:** Thread blocking during long-running database queries (reporting, complex joins).

---

## 3. Thread Handling in Spring Boot Internally
Spring Boot relies on standard Java threading models.
*   It does **not** use Project Loom (Virtual Threads) by default in Java 21 unless explicitly enabled (`spring.threads.virtual.enabled=true`).
*   Instead, it uses a **Thread-Per-Request** model provided by Tomcat.
*   Each incoming request consumes one full OS thread. A blocked thread (waiting on HikariCP or PostgreSQL) cannot serve other requests.
*   Thread creation is expensive, so Tomcat maintains a pool. When the pool is exhausted, requests are queued; when the queue is full, connections are rejected.

---

## 4. Tomcat Concurrent Request Handling Capacity
By default, Spring Boot configures Tomcat with:
*   `server.tomcat.threads.max`: 200
*   `server.tomcat.threads.min-spare`: 10
*   `server.tomcat.max-connections`: 8192
*   `server.tomcat.accept-count` (Queue queue): 100

**Capacity Analysis:**
The server can actively process **200 concurrent requests**. If request "A" takes 500ms (due to a heavy DB query), that thread is blocked for 500ms. 
If all 200 threads are blocked waiting for the database (which only has 10 connections by default), 190 threads are just idling, consuming ~1MB RAM each, waiting for HikariCP to yield a connection. The server handles bursts well but degrades sharply under sustained high-latency I/O.

---

## 5. HikariCP Database Connection Limits
Current Configuration (based on `application.yaml`):
*   `maximum-pool-size`: 10
*   `minimum-idle`: 2

**Capacity Analysis:**
This is the **hardest limit in the current architecture**. Only 10 queries can execute against PostgreSQL simultaneously.
*   If your average query latency is 20ms, 10 connections can handle `(1000ms / 20ms) * 10 = 500` transactions per second (TPS).
*   In an ERP, average query latency (with joins, analytical views, and schema switches) might be closer to 100ms-200ms.
*   At 100ms average DB time, the maximum theoretical DB throughput is **100 TPS**.
*   Any request beyond this blocks the Tomcat thread pool waiting for a connection.

---

## 6. PostgreSQL Expected Throughput Under Current Design
PostgreSQL handles concurrent connections via a multi-process architecture (one OS process per connection).
*   A pool size of 10 is very light for Postgres; the DB itself is barely sweating.
*   However, the schema switching (`SET search_path`) pattern requires execution context changes on the connection. While fast, it prevents prepared statement caching across schemas efficiently in some JDBC driver configurations.
*   Assuming standard hardware (4 vCPU, 8GB RAM), PostgreSQL can comfortably handle 100-300 concurrent active connections. The current bottleneck is the Hikari pool, not the DB engine.

---

## 7. Multi-Tenant Schema Impact on Scalability
**Pros:** Excellent data isolation; simplifies backup/restore per tenant.
**Cons (Scalability):**
*   **Connection Pool Dilution:** Unlike a single-schema app, you cannot easily pool connections *per tenant*. You must use a single pool and execute `SET search_path` when borrowing. This adds a network round trip or internal DB context switch per request.
*   **Hibernate Metadata:** Hibernate builds a SessionFactory. In some configurations, it must validate metadata for all schemas. With 10 schemas, fine. With 10,000 schemas, startup time and JVM heap usage for ORM metadata become massive.
*   **Database Catalog Bloat:** PostgreSQL's internal catalog tables (`pg_class`, `pg_attribute`) swell. With thousands of tenants, schema migrations (Flyway) take hours, and DDL operations slow down globally.

---

## 8. JWT Authentication Overhead
**The Cost:**
*   Statelessness saves RAM (no server-side session state) but trades it for CPU cycles.
*   Validating a JWT requires cryptographic math (verifying the signature) on *every single API request*.
*   On a modern CPU, an HMAC (HS256) verification takes ~10-50 microseconds. RSA varies but is heavier.
*   For moderate loads, this is negligible. Under heavy load (>1000 RPS), JWT parsing becomes a noticeable CPU sink compared to simply looking up a session ID in a memory map or Redis.

---

## 9. Permission Loading Overhead During Login
*   **The Process:** Upon login, the system queries exactly what roles the user has, what actions belong to those roles, and any module-specific permissions.
*   **The Cost:** High database read overhead *during the login phase*. A login might require 3-5 joins (Users -> Roles -> Roles_Actions -> Actions -> Modules).
*   **Mitigation:** This is currently mitigated by packing this data into the JWT token or returning it once. If the token becomes too large (e.g., a user has 500 discrete permissions), it causes network overhead on every request (bloated HTTP headers).

---

## 10. Estimated Concurrent Users Supported
*Assumptions: Standard server (4 vCPU, 8GB RAM), Hikari pool=10, Tomcat threads=200.*

*   **Login Load:**
    *   Heaviest DB operation (bcrypt hashing + complex permission joins). Takes ~300ms.
    *   Max limit: ~30 logins per second before Hikari queuing begins.
*   **Normal API Load (Reads/Simple Writes):**
    *   Assuming 50ms total response time (40ms DB, 10ms App).
    *   Max limit: ~200 requests per second (RPS).
*   **Mixed ERP Usage (Heavy Reports + Simple APIs):**
    *   If 2/10 connections are tied up by 2-second reporting queries, only 8 connections are left for fast APIs.
    *   Estimated concurrent active users (making requests simultaneously): **~150-200**.

> *Note: "Concurrent users" means users clicking a button at the exact same millisecond. 200 concurrent users usually translates to ~2,000 "active session" users interacting normally (reading screens, typing, then clicking).*

---

## 11. Estimated Total Daily Active Users Supported
Assuming typical ERP usage patterns (office hours, varied activity spikes, 90/10 read/write ratio):
*   Current architecture can safely support **2,000 to 5,000 Daily Active Users (DAU)**.
*   Beyond 5,000 DAU, the HikariCP limit of 10 and the lack of a distributed cache for read-heavy operations will cause unacceptable queuing during peak business hours (e.g., Monday 9 AM logins).

---

## 12. Main Bottlenecks in Current Architecture
1.  **HikariCP default sizing (10)** is too small for a medium ERP.
2.  **Synchronous Reporting/Exports:** Heavy queries via REST block Tomcat threads.
3.  **Schema Context Switching:** Unavoidable but adds constant baseline latency.
4.  **Absence of L2 Cache:** High frequency repetitive queries (e.g., getting configuration, menu structures) hit the DB every time.

---

## 13. Immediate Production Improvements Without Redis
You can drastically improve capacity with config changes only:
1.  **Enable Java 21 Virtual Threads:** In `application-prod.yaml`, add `spring.threads.virtual.enabled: true`. This changes Tomcat from 200 OS threads to millions of lightweight virtual threads. Thread exhaustion disappears as a bottleneck (though DB connection exhaustion remains).
2.  **Increase HikariCP Pool:** Raise `maximum-pool-size` to at least `cpu_cores * 2 + effective_spindle_count` (e.g., 30-50 for a mid-tier cloud DB).
3.  **Use Caffeine Local Cache:** Implement `@Cacheable` using Spring Boot's default ConcurrentHashMap or add the Caffeine dependency for local caching of static tenant configuration and permission definitions.
4.  **Prepared Statement Caching:** Ensure PostgreSQL JDBC string includes `preparedStatementCacheQueries=250&preparedStatementCacheSizeMiB=5`.

---

## 14. Scaling Path

### Target: 1,000 Users (Current Stage)
*   **Infrastructure:** Single App Unit (e.g., 2 vCPU, 4GB RAM), Single Managed PostgreSQL.
*   **Actionable:** Just optimize configuration (see section 15).

### Target: 5,000 Users
*   **Infrastructure:** 2 App Units behind a Load Balancer. Single Managed PostgreSQL (scaled up compute).
*   **Actionable:** Introduce Redis.
    *   Offload JWT blacklist validation to Redis.
    *   Cache global/tenant permissions and settings in Redis to cut DB round-trips by 40%.
*   **Storage:** Shift file uploads (if any) to S3, do not serve them via Tomcat or Postgres.

### Target: 10,000 Users
*   **Infrastructure:** 3-5 App Units. Primary PostgreSQL (Writes) + Read Replica PostgreSQL (Reads).
*   **Actionable:**
    *   Implement CQRS at the DB connection level (route `@Transactional(readOnly = true)` to the Read Replica).
    *   Move heavy operations (report generation, mass billing) to background workers using a message broker (RabbitMQ/Kafka) so they don't block API threads.

---

## 15. Recommended Production Configuration Values

Update your `application.yaml` or environment variables for production:

```yaml
server:
  tomcat:
    threads:
      max: 400            # Up from 200, or use virtual threads
      min-spare: 50
    accept-count: 200
    connection-timeout: 20000

spring:
  threads:
    virtual:
      enabled: true       # Highly recommended for Java 21

  datasource:
    hikari:
      maximum-pool-size: 50     # Drastic increase required for ERP DB Wait
      minimum-idle: 10
      connection-timeout: 10000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    properties:
      hibernate:
        generate_statistics: false # Ensure this is off in prod
```

**JVM Arguments (Heap):**
For an 8GB server, dedicate ~6GB to the heap:
`-Xms4g -Xmx6g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom`

**PostgreSQL Tuning (pg_config):**
*   `max_connections`: 500 (Ensure it's higher than all App server Hikari pools combined).
*   `shared_buffers`: 25% of DB Server RAM.
*   `work_mem`: 16MB (Crucial for complex ERP joins and sorts).

---

## 16. Risks in Current Project Design
1.  **"Noisy Neighbor" Tenancy:** Because all tenants share the same PostgreSQL compute resources, one tenant running a massive analytical report can degrade DB performance for all other tenants.
2.  **Flyway Lock Contention:** As schema count grows (100+ tenants), running Flyway on application startup will take minutes and may cause deployment timeouts.
3.  **Token Bloat:** If a user has extremely granular permissions, including them all in the JWT payload can push the HTTP header size beyond standard limits (8KB-16KB), causing 431 Request Header Fields Too Large errors.

---

## 17. Final Production Readiness Score
**Score: 7.5 / 10 (Solid Foundation)**

**Verdict:** The application has a strong, modern foundation (Java 21, stateless auth, schema isolation). It is fundamentally sound but requires tuning before hitting production. The default settings (Hikari=10, no caching, synchronous execution of everything) will cause it to fold under moderate ERP load. Applying the configuration recommendations in section 15 will immediately boost the score to 8.5, making it ready for the first 1,000 users.
