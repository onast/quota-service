# Quota Service

## Overview

This project implements a small **API Usage & Quota Service** written in **Kotlin** using the **Ktor** framework.

The service tracks API usage per API key and enforces a configurable quota within a fixed time window.
It is intentionally scoped as an **MVP** suitable for discussion in a technical code review.

---

## Behavior

* Each API key has an independent quota (e.g., **100 units per hour**)
* Requests are:

    * **Accepted (200 OK)** if enough quota remains
    * **Rejected (429 Too Many Requests)** if the quota is exceeded
* Usage **automatically resets** after the configured time window
* The service behaves correctly under **concurrent requests**
* Invalid input (missing API key, invalid body, negative units) returns **400 Bad Request**

---

## API

### Consume quota

**POST** `/quota/consume`

Headers:

```
X-Api-Key: <api-key>
Content-Type: application/json
```

Body:

```json
{
  "units": 10
}
```

Successful response:

* **200 OK** if accepted
* **429 Too Many Requests** if quota exceeded

Response headers:

```
X-Quota-Limit
X-Quota-Remaining
X-Quota-Reset-Millis
```

Response body:

```json
{
  "accepted": true,
  "remaining": 90,
  "resetAtMs": 1710000000000
}
```

---

## Design Decisions

### Fixed time window

A **fixed window algorithm** was chosen for simplicity and clarity within MVP constraints.

Benefits:

* Easy to reason about
* Minimal state
* Straightforward concurrency handling

Trade-off:

* Boundary bursts may occur at window edges
  (would be solved with sliding window or token bucket in production)

---

### In-memory storage

State is stored in a **ConcurrentHashMap** per API key.

Reason:

* Single-instance constraint
* No external dependencies required
* Fast and simple for MVP

Limitation:

* Not suitable for multi-instance or long-term persistence.

---

### Concurrency correctness

Quota updates use:

```
ConcurrentHashMap.compute(...)
```

This provides **atomic per-key updates** without global locking, ensuring:

* No quota over-consumption
* Correct behavior under concurrent requests

---

### Separation of concerns

* **Routing layer** → HTTP handling & validation
* **QuotaService** → pure business logic
* **Clock abstraction** → deterministic testing

This keeps the core logic **testable and framework-independent**.

---

## Error Handling

Centralized via **Ktor StatusPages**:

* `IllegalArgumentException` → **400 Bad Request**
* `BadRequestException` (invalid JSON) → **400 Bad Request**
* Unexpected errors → **500 Internal Server Error**

---

## Testing Strategy

The project includes:

### Unit tests

* Accept vs reject logic
* Window reset behavior
* Edge conditions

### Concurrency test

Simulates many parallel requests to verify:

* **Accepted requests never exceed quota**
* **All requests accounted for**

This validates **thread-safety and correctness**, which is a key requirement of the task.

---

## Running the Service

Start the server:

```bash
./gradlew run
```

Health check:

```
GET http://localhost:8080/health
```

Example request:

```bash
curl -i -X POST http://localhost:8080/quota/consume \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: test-key" \
  -d '{"units":10}'
```

Run tests:

```bash
./gradlew test
```

---

## Trade-offs & Limitations

* Fixed window allows **burst at boundaries**
* **In-memory storage** limits scalability and durability
* No **per-key configuration** or authentication beyond API key
* No **metrics or observability** beyond logs

These are acceptable within the **time-boxed MVP scope**.

---

## Future Improvements

For a production-grade system, I would add:

* **Sliding window or token bucket** algorithm
* **Redis-backed distributed rate limiting**
* **Per-customer quota configuration**
* **Eviction / TTL** for inactive API keys
* **Metrics & monitoring** (Prometheus, dashboards)
* **Authentication & audit logging**
* **Configuration externalization**

---

## Summary

This implementation focuses on:

* **Correctness under concurrency**
* **Clear, maintainable Kotlin code**
* **Minimal but sound architecture**
* **Thoughtful trade-offs within MVP constraints**

It is intentionally simple while demonstrating how the service could evolve into a **production-ready distributed quota system**.
