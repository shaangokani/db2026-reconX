# TICKET-ADV145 — Kafka consumer config review

AI-assisted review of `backend/src/main/resources/application.yml` (Kafka section)
and `backend/src/main/java/com/dbtraining/reconx/kafka/KafkaErrorHandlerConfig.java`,
covering backpressure/poll tuning, error handling/retry/DLQ, idempotence,
observability, and security. Decisions below optimize for **app functionality**
specifically — not production hardening in the abstract.

| # | Area | Finding | Recommendation | Decision | Rationale |
|---|------|---------|-----------------|----------|-----------|
| 1 | Backpressure | No `concurrency` set on any `@KafkaListener` — `trade-events` has 3 partitions but each consumer group runs a single thread, so the partitioning buys no parallelism | `@KafkaListener(..., concurrency = "3")` on the trade-events listeners | Defer | Throughput optimization, not a functional gap — the app processes every event correctly today at training-lab volumes. Revisit if real load testing shows lag building up. |
| 2 | Error handling | `ExponentialBackOff(1000, 2.0)` has no jitter — if many partitions fail at once (e.g. a DB outage), every retry synchronizes and hammers the dependency at the same instants | Wrap in a jittered `BackOff` | Defer | Matches the guide's own note that this is "a known production gap, not a bug you need to fix today." Retries work correctly as-is; this only matters under correlated-failure load this training app won't see. |
| 3 | Idempotence | Producer doesn't explicitly set `acks`/`enable.idempotence` — relies on Kafka 3.x's implicit default (which already is `acks=all` + idempotent) rather than declaring it | Add `acks: all` and `enable.idempotence: true` explicitly | **Accept** | Zero behavioral change (already the effective default) and zero risk — pins a durability guarantee this app already relies on against a silent regression on a future Kafka client upgrade. Implemented. |
| 4 | Observability | `correlationId`/`tradeRef` MDC context doesn't propagate across the Kafka boundary — a trade's HTTP-request log correlation is lost once execution crosses into consumer-side logs | Propagate correlationId as a Kafka header, read it back in each consumer | Defer | Debugging/tracing convenience, not a functional gap — every consumer already logs `tradeRef`/`eventId` directly, so cross-referencing is still possible, just manual. |
| 5 | Security | `bootstrap-servers` is PLAINTEXT, no SASL/ACLs | Use SASL_SSL in UAT/PROD | **Reject** (for this environment) | There is no secured broker to point at in this dev/training setup — turning this on now would break connectivity, not improve it. Tracked as a known, documented dev-only gap; revisit when Day 10 introduces real per-environment infra. |

**Review prompt used** (for the record — per ADV145's own template):

> Review the following Spring Kafka consumer configuration for production readiness. Flag any missing or risky settings in these areas: (1) backpressure & poll tuning, (2) error handling, retry & DLQ, (3) idempotence and exactly-once semantics, (4) observability — metrics, logging, traces, (5) security — TLS, SASL, ACLs. For each finding, give the concrete config key, the recommended value, and a one-line justification. Do NOT rewrite the whole file — just list findings. Application context: trade reconciliation service, ~500 events/sec, strict audit requirements.
