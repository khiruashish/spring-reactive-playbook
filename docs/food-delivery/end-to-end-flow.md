# End-to-End Flow

```mermaid
sequenceDiagram
    autonumber
    actor U as Browser
    participant G as api-gateway
    participant O as order-service
    participant R as restaurant-service
    participant P as payment-service
    participant K as Kafka
    participant D as delivery-service
    participant N as notification-service

    U->>G: POST /auth/token
    G-->>U: JWT
    U->>G: POST /api/orders (Bearer, Idempotency-Key)
    G->>O: + X-User-Id (from JWT)
    O->>R: GET /api/menu-items?ids=1&ids=2
    R-->>O: prices
    O->>O: Pricing.price() + insert (R2DBC tx)
    O->>P: POST /api/payments (Idempotency-Key, timeout, retry, breaker)
    P-->>O: CAPTURED
    O->>K: OrderPlaced (key=orderId)
    O-->>U: 201 Created {status: PAID}
    U->>G: GET /api/orders/{id}/status-stream (SSE)
    U->>G: WS /ws/notifications?orderId=…
    K->>N: OrderPlaced → "Order confirmed"
    N-->>U: WS message
    K->>D: OrderPlaced
    D->>D: assign courier (Mongo, unique orderId)
    loop every tick
        D->>K: DeliveryStatusChanged
        K->>O: update status (state machine)
        O-->>U: SSE status + coordinates
        K->>N: friendly message
        N-->>U: WS message
    end
    Note over U,D: stream completes at DELIVERED
```

## Failure paths worth trying

| Scenario | What happens |
|---|---|
| payment-service down | 2 retries with backoff → breaker records failures → order `PAYMENT_FAILED`, no event published |
| breaker open | calls fail fast without touching payment-service; half-open after 10 s |
| duplicate `POST` with same key | original order returned; payment not charged twice |
| Kafka redelivers `OrderPlaced` | unique index in Mongo → no second courier |
| late/out-of-order delivery event | `OrderStatus.canTransitionTo` ignores it |
| browser closes tab | SSE `Flux` cancelled → sink subscription and heartbeat stop |
