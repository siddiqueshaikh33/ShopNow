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

# Open items / backlog

# Concepts learned
- Spring AOP proxy self-invocation bypasses `@Transactional` when calling methods inside the same bean; separate worker services or external proxy calls are required for new transaction boundaries.
- Hibernate L1 (First-Level) cache retains in-memory entity versions across a single transaction; retry loops must execute across fresh transaction boundaries (`REQUIRES_NEW`) to clear the session and read fresh database rows.

# Archive

