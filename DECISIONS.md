# How to use this file

This file is the single source of truth for all architectural and design decisions, pending backlog items, and learning notes for this project.

### Rules for the Agent
- Keep everything in this single file. Never create separate decision, notes, or learning files.
- Read DECISIONS.md before changing code in areas that already have an entry.
- Add an entry whenever we choose between approaches, find a risk and decide how to handle it, or reverse an earlier decision. Do not log routine work like renames or formatting.
- Never invent a decision. Use "Accepted" only if I said so. Otherwise use "Proposed".
- Never delete entries. Mark them Rejected or Superseded. When the file passes ~500 lines, move Implemented and Rejected entries to "Archive".
- After implementing something, update its Status and "Where in code".
- When we find a problem we are not fixing now, add it to "Open items / backlog". When a backlog item gets decided, turn it into a numbered decision entry and tick it off.
- When I learn a new concept during a task, add one short bullet to "Concepts learned".
- Number entries D-001, D-002, ... in order. Newest at the bottom of "Decisions".
- Keep entries short and in plain language.
- Use today's real date for new entries.

### Decision Entry Format
Every decision entry must use this exact format:

```markdown
### D-XXX: Short title
- Date: YYYY-MM-DD
- Status: Proposed | Accepted | Implemented | Rejected | Superseded by D-XXX
- Problem: what went wrong or could go wrong (1-3 lines)
- Options considered: for each option, what it gives and what it costs
- Decision: the option chosen
- Why chosen: reasoning tied to this project's size, traffic, and risk
- Why the others were NOT chosen: one line per rejected option
- Risks / known downsides of the chosen option
- How to verify it works: a test or manual check
- Where in code: files and classes affected (write "not implemented yet" if so)
- Revisit when: the condition that should make us reconsider
```

# Decisions

### D-001: Optimistic Locking with Transactional Worker and Stock Release Lifecycle
- Date: 2026-11-07
- Status: Implemented
- Problem: High-concurrency checkouts can cause overselling and lost updates. Decrementing stock during checkout creation leaves stock locked indefinitely if a user abandons payment. Outer @Transactional loops also risk stale reads and rollback-only crashes in Hibernate.
- Options considered:
  - Option A: Pessimistic locking (`SELECT FOR UPDATE`). Prevents overselling at the cost of database row locks, connection pool exhaustion, and slow browsing throughput.
  - Option B: Controller-level retry loop. Bypasses the proxy trap, but violates `AGENTS.md` layering rules by leaking transaction orchestration into HTTP controllers.
  - Option C: Worker pattern with optimistic locking (`@Version`) + dedicated `OrderTransactionService` (`REQUIRES_NEW`), combined with modal `ondismiss` stock release (Approach 3) and scheduled expiry fallback (Approach 1). Keeps controller clean, ensures fresh Hibernate L1 cache per attempt, and restores abandoned stock.
- Decision: Option C
- Why chosen: Matches project scale and layering rules in AGENTS.md, delivers high read throughput without database locks, ensures each retry gets a fresh transaction and clean Hibernate session, and guarantees inventory is restored when users abandon checkouts.
- Why the others were NOT chosen: Option A would bottleneck browsing and product discovery under load; Option B leaks business transaction loops into HTTP controllers.
- Risks / known downsides of the chosen option: Requires managing two service classes (`OrderService` orchestrator + `OrderTransactionService` worker); stock is temporarily held for up to the expiry window if the user abruptly terminates the browser before payment.
- How to verify it works: Unit tests verifying `Products.stock` decrements with `@Version`, `OutOfStockException` on insufficient stock, automatic retry on `OptimisticLockingFailureException`, and `/payment/cancel` restores stock.
- Where in code: `Products.java`, `OrderTransactionService.java`, `OrderService.java`, `OrderController.java`, `Cart.jsx`, `OrderServiceTest.java`, `OrderTransactionServiceTest.java`.
- Revisit when: Daily concurrent checkouts exceed 100,000 orders/minute (requiring Redis distributed locks / Lua scripts).

### D-002: Pure Stateless JWT Authentication Verification
- Date: 2026-11-08
- Status: Implemented
- Problem: AuthFilter queried MySQL jwt_tokens and users tables on every single HTTP request (2 queries per request), bottlenecking the 10-connection HikariCP pool and degrading throughput under high traffic.
- Options considered:
  - Option A: Redis session cache. Reduces MySQL load but introduces an external stateful infrastructure dependency, cluster failover management, and network hops.
  - Option B: Database-backed token checks with local caching. Stale cache invalidation complexity and still risks cache stampedes.
  - Option C: Pure stateless JWT verification. Embed userId and role claims directly in the signed JWT payload and verify signature/expiration purely in CPU memory using the HMAC secret key.
- Decision: Option C
- Why chosen: Zero database queries per authenticated request (100% auth DB offload), zero external dependencies, CPU verification in ~0.01ms, horizontally scalable across instances without shared session state.
- Why the others were NOT chosen: Option A introduces unnecessary infrastructure complexity for this project's scale; Option B still involves state synchronization issues.
- Risks / known downsides of the chosen option: Immediate token invalidation before expiry requires an explicit token revocation denylist if desired; mitigated by keeping token expiry reasonable (1 hour) and clearing the client-side cookie on logout.
- How to verify it works: Unit tests verifying claims extraction, signature verification in memory, expired/tampered token rejection, and zero database queries in AuthFilter.
- Where in code: `AuthService.java`, `AuthFilter.java`, `AuthServiceTest.java`.
- Revisit when: Strict immediate server-side revocation across devices is required prior to token expiration (requiring a distributed Redis blocklist or short-lived tokens with refresh token rotation).

### D-003: Scoped Refresh Token Cookie Path for Rotation and Logout Revocation
- Date: 2026-11-10
- Status: Implemented
- Problem: Scoping the refresh token cookie strictly to `Path=/api/auth/refresh` prevented the browser from attaching the cookie when calling `POST /api/auth/logout`. As a result, `rawRefreshToken` was always null on logout, making server-side token revocation dead code and leaving active tokens unrevoked in the database.
- Options considered:
  - Option A: Frontend explicitly sends the refresh token in the logout request body/header. Rejected because refresh tokens are stored in `HttpOnly` cookies to protect against XSS token theft; frontend JavaScript cannot and should not access them.
  - Option B: Broaden cookie path to root (`Path=/`). Exposes the refresh token on every single HTTP request across the entire application (`/api/products`, `/api/orders`, `/cart`), increasing request header overhead and attack surface.
  - Option C: Scope refresh token cookie to `Path=/api/auth`. Matches both `/api/auth/refresh` (for silent token rotation) and `/api/auth/logout` (for database revocation and cookie clearing), while completely isolating the refresh token from non-auth endpoints.
- Decision: Option C
- Why chosen: Adheres to Cookie RFC 6265 prefix path matching; allows the browser to automatically include the refresh token on both token rotation and logout without frontend JavaScript exposure, while shielding all product/cart/order APIs.
- Why the others were NOT chosen: Option A defeats the `HttpOnly` security boundary; Option B exposes the sensitive refresh token to unnecessary endpoints.
- Risks / known downsides of the chosen option: Endpoints added directly under `/api/auth/*` in the future will receive the refresh token cookie.
- How to verify it works: Unit tests verifying `createRefreshTokenCookie` and `clearRefreshTokenCookie` set `path=/api/auth`, and integration tests verifying `POST /api/auth/logout` receives and revokes the refresh token.
- Where in code: `AuthService.java`, `AuthController.java`, `AuthServiceTest.java`.
- Revisit when: Authentication endpoints are relocated to a different URI prefix or dedicated auth domain.

### D-004: Tiered Dual-Key Rate Limiting with In-Memory Caffeine Cache
- Date: 2026-11-10
- Status: Implemented
- Problem: Critical authentication and payment endpoints were vulnerable to brute-force credential stuffing and denial-of-service (DoS) attacks. Furthermore, botnets utilizing rotating proxy IP addresses could bypass standard IP-only rate limiting to crack passwords for specific user accounts. Unbounded in-memory rate-limit trackers also risk heap exhaustion and OutOfMemoryError (OOM) over time.
- Options considered:
  - Option A: Redis-backed distributed rate limiter (e.g., Bucket4j Redis extension). Provides cluster-wide synchronization across horizontal backend replicas, but introduces an external stateful infrastructure dependency, network round-trips for every HTTP request, and increased operational maintenance for this single-instance local deployment.
  - Option B: Single global IP-based rate limiter in memory (e.g. Guava RateLimiter). Uniform rate limits either throttle legitimate shoppers browsing catalog pages or are too loose to stop targeted password brute-forcing on `/api/auth/login`; also completely fails against distributed rotating-IP attacks.
  - Option C: Tiered Dual-Key in-memory rate limiting with Bucket4j and Caffeine Cache. Enforces strict IP limits (5 req/min) on auth/payment/user endpoints and general limits (100 req/min) on browsing, combined with a secondary username-keyed bucket (5 failed attempts/15 min) to thwart rotating IP attacks. Capped Caffeine caches with `expireAfterAccess` automatically evict idle entries to guarantee bounded memory usage.
- Decision: Option C
- Why chosen: Tailored protection that does not impact normal shopping browsing; stops both simple IP-based flooding and sophisticated distributed rotating-IP credential stuffing; operates in CPU RAM with sub-millisecond overhead; and bounds heap memory using automatic LRU/time-based eviction without external infrastructure dependencies.
- Why the others were NOT chosen: Option A adds unnecessary infrastructure complexity and operational overhead for single-instance scale; Option B cannot protect catalog browsing while mitigating brute-force attacks and cannot detect distributed rotating-IP botnets.
- Risks / known downsides of the chosen option: In-memory state is local to each JVM process; if the application scales to multiple instances behind a load balancer in the future without sticky sessions, rate limits will be per-instance rather than global.
- How to verify it works: Unit tests for `RateLimitingService` and `RateLimitingFilter`, and MockMvc integration tests verifying strict IP throttling (HTTP 429), general IP pass-through, and rotating-IP username lockout after 5 failed attempts with `Retry-After` headers and generic error JSON.
- Where in code: `RateLimitingService.java`, `RateLimitingFilter.java`, `AuthController.java`, `SecurityConfig.java`, `RateLimitingServiceTest.java`, `RateLimitingFilterTest.java`, `RateLimitingIntegrationTest.java`.
- Revisit when: The backend is scaled horizontally across multiple server instances behind a load balancer without sticky sessions, requiring centralized Redis/Valkey rate limiting.

### D-005: Sliced Infinite Pagination via Spring Data Slice and IntersectionObserver
- Date: 2026-11-12
- Status: Implemented
- Problem: The product catalog endpoint previously loaded all products in a single unbounded database query (`findAllWithPrimaryImage()`). Under realistic catalog sizes (thousands of products), this causes massive database memory spikes, heavy network payload serialization, and mobile browser jank. Using standard `Page<T>` pagination adds a redundant, table-scanning `SELECT COUNT(*)` query on every scroll step.
- Options considered:
  - Option A: Standard offset pagination with Spring Data `Page<T>`. Runs two SQL queries per request: one for the rows (`LIMIT`) and one for the total count (`SELECT COUNT(*)`). Scanning millions of rows for total count on every scroll event degrades throughput for infinite feeds.
  - Option B: Keyset (cursor-based) pagination (`WHERE id > :lastId LIMIT :size`). Highly efficient for strictly sequential reads, but complicated to maintain when adding multi-field sorting (e.g. price ascending/descending) and category filters.
  - Option C: Sliced pagination with Spring Data `Slice<T>` (`LIMIT size + 1`) coupled with client-side browser `IntersectionObserver`. Executes only one query per request with zero `COUNT(*)` overhead, pre-fetches products 200px before the viewport, and incorporates an explicit error circuit breaker with retry to prevent runaway request loops.
- Decision: Option C
- Why chosen: Tailored specifically for infinite scroll UI feeds; provides optimal database efficiency by querying `size + 1` rows to calculate `hasNext()` in a single query; avoids main-thread scroll jank via native `IntersectionObserver`; and prevents network spam loops on error through dedicated error state tracking and page rollback.
- Why the others were NOT chosen: Option A wastes significant database CPU scanning counts that infinite feeds never display; Option B adds unnecessary schema and query complexity for our catalog scale.
- Risks / known downsides of the chosen option: Deep scrolling (e.g., page 500) with SQL `OFFSET` can experience slower query performance on massive datasets; mitigated by catalog category partitioning and practical user browsing depths.
- How to verify it works: Unit tests in `ProductServiceTest` verifying `hasNext=true` on page 0 and `hasNext=false` on the final page; integration test in `ProductAndOrderIntegrationTest` verifying `GET /api/products?page=0&size=12` returns HTTP 200 and paginated slice schema.
- Where in code: `ProductRepository.java`, `ProductService.java`, `ProductController.java`, `PaginatedProductResponse.java`, `Dashboard.jsx`, `ProductServiceTest.java`, `ProductAndOrderIntegrationTest.java`.
- Revisit when: The product catalog scales past 500,000 items and deep pagination latency degrades, requiring Keyset/cursor-based indexing or Elasticsearch/OpenSearch.

### D-006: Externalized 12-Factor Configuration and HTTPS-Enforced Cookie Security
- Date: 2026-11-12
- Status: Implemented
- Problem: Secrets, database credentials, and cookie security flags were previously hardcoded or tightly coupled to local profiles with inconsistent environment variable names (e.g. typos like `JWT_SECERT` and `RAZORPAY_SECERT`). Additionally, authentication cookies had hardcoded `secure(false)`, preventing HTTPS enforcement in production.
- Options considered:
  - Option A: Hardcode production configuration in a separate private repository branch. Rejected because branch drift creates deployment failures and leaks secrets into git history.
  - Option B: Use external secret management services (e.g. AWS Secrets Manager, HashiCorp Vault). Adds substantial infrastructure overhead and external dependencies unnecessary for current deployment scale.
  - Option C: 12-Factor externalized environment variable placeholders with local fallbacks and profile separation (`application.properties`, `application-prod.properties`, and `cookie.secure` injection). Backward-compatible with legacy typos while enforcing strict environment variable binding and `secure(true)` in production.
- Decision: Option C
- Why chosen: Zero overhead, 100% compliant with standard container and PaaS deployment environments (Docker, Render, Railway, Kubernetes), maintains local developer ergonomics without extra setup, and allows single-variable toggling of HTTPS cookie security (`COOKIE_SECURE=true`).
- Why the others were NOT chosen: Option A risks secret leaks and version drift; Option B adds unnecessary infrastructure complexity.
- Risks / known downsides of the chosen option: Deployments must supply required environment variables or the application will fail to connect. Documented fully in `.env.example`.
- How to verify it works: All 63 automated tests pass with default fallbacks; running with `COOKIE_SECURE=true` generates `Secure` response cookies.
- Where in code: `application.properties`, `application-prod.properties`, `AuthService.java`, `.env.example`.
- Revisit when: The application transitions to a Kubernetes cluster requiring HashiCorp Vault or AWS KMS dynamic secret injection.

### D-007: Multi-Tier Stock Limit Boundary Enforcement and Ceiling Protection
- Date: 2026-11-12
- Status: Accepted
- Problem: Shoppers could increment cart quantities beyond available product inventory, leading to checkout disappointment, overselling risks, or uninformative generic error messages.
- Options considered:
  - Option A: Frontend-only quantity clamp. Fails when malicious clients bypass the UI using direct API calls.
  - Option B: Reserve stock immediately on cart addition. Causes cart-abandonment denial-of-service where abandoned carts lock inventory away from genuine buyers.
  - Option C: Multi-tier stock ceiling enforcement: UI prevents carting > available stock (disables `+` button at ceiling, displays max stock badges, performs pre-checkout validation), while backend `CartService` and `CartController` enforce stock ceilings with descriptive HTTP 400 responses, and final deduction occurs atomically at payment checkout (`OrderTransactionService`).
- Decision: Option C
- Why chosen: Follows real-world e-commerce industry standards (Amazon, Shopify, Flipkart); provides zero-latency client-side feedback without locking inventory from other shoppers before checkout, while guaranteeing server-side integrity against API tampering.
- Why the others were NOT chosen: Option A leaves backend APIs unprotected against forged requests; Option B causes inventory hoarding DOS.
- Risks / known downsides of the chosen option: A competing buyer can purchase the last remaining item while another user has it in their cart, which is caught and gracefully handled by checkout validation.
- How to verify it works: Frontend disables the `+` button when quantity reaches available stock and displays "Maximum stock reached"; attempting to increase triggers a warning toast; pre-checkout verifies all item quantities against stock; direct API calls attempting to exceed stock receive HTTP 400 with a clear error message; all backend unit and integration tests pass.
- Where in code: `CartItems.jsx`, `Cart.jsx`, `Dashboard.jsx`, `Card.jsx`, `CartService.java`, `CartController.java`.
- Revisit when: Moving to a reserved-inventory timer pattern (e.g. concert ticketing with temporary 10-minute hold carts).

# Open items / backlog

# Concepts learned
- Spring AOP proxy self-invocation bypasses `@Transactional` when calling methods inside the same bean; separate worker services or external proxy calls are required for new transaction boundaries.
- Hibernate L1 (First-Level) cache retains in-memory entity versions across a single transaction; retry loops must execute across fresh transaction boundaries (`REQUIRES_NEW`) to clear the session and read fresh database rows.
- Pure stateless JWT verification uses cryptographic HMAC-SHA256 signature checks and in-token claims in CPU RAM, completely avoiding database queries on every authenticated request.
- In JPA `@Column(updatable = false)` tells Hibernate to omit the column from SQL `UPDATE` statements; it belongs on immutable creation timestamps (`created_at`), not modification timestamps (`updated_at`).
- Spring MVC controllers require `@RequestParam` (`org.springframework.web.bind.annotation`) for query parameter binding; Spring Data's `@Param` is strictly for repository `@Query` methods.
- Spring MVC request handler methods must be declared `public` for Spring's dispatching and proxying mechanisms to invoke them properly.
- React Router v6/v7 `<Routes>` components strictly enforce that all immediate children are `<Route>` or `<React.Fragment>`; any stray string or text node triggers an unhandled invariant violation crash at runtime.
- File path imports in frontend code must strictly match on-disk folder and file casing (e.g. `../assets/...`); Windows file systems mask casing discrepancies, but Linux, Docker, and CI/CD runners strictly enforce case sensitivity and fail with `ENOENT` on mismatched casing.
- Client-side route protection requires both authentication (logged-in session check) and role-based authorization (e.g. `requiredRole === "ADMIN"`) to prevent unauthorized customer navigation to sensitive administrative UIs.
- Frontend JSON request payload keys must strictly match backend DTO field names (e.g. `imgUrl` vs `imageUrl`); unmatched keys are silently dropped by Jackson deserialization unless configured with strict unknown property failure.
- Centralizing API calls into a single configured `axiosInstance` reading `VITE_API_BASE_URL` with global `withCredentials: true` eliminates hardcoded hosts/ports, guarantees environment independence, and enforces architectural boundaries.
- Centralized exception handling via `@RestControllerAdvice` transforms Java exceptions into RFC-7807 compliant JSON error structures (`timestamp`, `status`, `error`, `message`, `path`), preventing internal stack trace leaks to API clients.
- End-to-end integration tests combining `@SpringBootTest` and `@AutoConfigureMockMvc` test complete HTTP filter pipelines (including `AuthFilter` JWT decoding and security context) against controller endpoints with mocked downstream services.
- Containerizing full-stack web applications with multi-stage Docker builds decouples build-time SDKs (Maven/Node) from minimal production runtimes (Alpine JRE/Nginx), and Docker Compose healthchecks ensure MySQL is fully initialized before the Spring Boot container starts.
- Refresh Token Rotation with SHA-256 database hashing and scoped path cookies (`Path=/api/auth`, `SameSite=Strict`, `HttpOnly`) limits access token blast radius to 15 minutes while detecting replay attacks and revoking compromised token families on reuse.
- Cookie RFC 6265 Path matching: Browsers attach cookies only if the request path starts with the cookie's `Path`. Scoping a cookie to `/api/auth/refresh` prevents `/api/auth/logout` from receiving it; scoping to `/api/auth` delivers the cookie to both `/refresh` and `/logout` while safely hiding it from `/products`, `/cart`, and `/orders`.
- Token Bucket algorithm (Bucket4j) allows smooth bursts up to bucket capacity while continuously refilling tokens at a fixed rate, avoiding artificial request spikes at fixed time boundaries.
- Dual-Key rate limiting (IP + Username) mitigates distributed credential stuffing: tracking failed login attempts per username across rotating IP addresses prevents attackers from bypassing IP throttling.
- Caffeine in-memory cache eviction (`expireAfterAccess` + `maximumSize`) bounds heap usage and prevents memory leaks from one-off attacker IPs by automatically evicting idle tracking buckets.
- Preventing User Enumeration (CWE-204) in rate limiting: returning identical generic error responses (`"Too many requests. Please try again later."`) and status codes prevents attackers from probing whether an account exists or is currently locked out.
- Spring Data `Slice<T>` vs `Page<T>`: `Slice<T>` queries `LIMIT size + 1` to determine `hasNext()` in a single SQL query, completely eliminating the table-scanning `SELECT COUNT(*)` query required by `Page<T>`, optimizing performance for infinite scroll feeds.
- Infinite scroll circuit breaking: Unhandled API errors during infinite scroll cause runaway request loops if the observer sentinel remains in view; pausing the observer, setting an explicit error state, rolling back the page counter, and providing a manual retry button protects both the client and server.
- E-commerce inventory ceiling vs. immediate reservation: Carting items enforces available inventory limits (ceiling) without deducting warehouse stock until payment creation, preventing cart-abandonment hoarding denial-of-service while protecting shoppers from overselling.

# Archive

