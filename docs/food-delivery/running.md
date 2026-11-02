# Run It

## Option A — everything in Docker

```bash
cd food-delivery
```

```bash
docker compose --profile apps up -d --build
```

Open <http://localhost:8080>, click **Sign in**, pick a restaurant, add items, **Place order** and
watch the tracking panel update every 2 seconds until *DELIVERED*.

## Option B — infrastructure in Docker, services from Gradle/IDE

```bash
cd food-delivery && docker compose up -d --wait
```

Then, from the repository root, start each service in its own terminal:

```bash
./gradlew :food-delivery:restaurant-service:bootRun
```

```bash
./gradlew :food-delivery:payment-service:bootRun
```

```bash
./gradlew :food-delivery:order-service:bootRun
```

```bash
./gradlew :food-delivery:delivery-service:bootRun
```

```bash
./gradlew :food-delivery:notification-service:bootRun
```

```bash
./gradlew :food-delivery:api-gateway:bootRun
```

## Drive it with curl

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/token -H 'Content-Type: application/json' \
  -d '{"username":"alice"}' | jq -r .accessToken)
```

```bash
curl -s localhost:8080/api/restaurants | jq
```

```bash
curl -s -X POST localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-1' \
  -d '{"restaurantId":1,"deliveryAddress":"MG Road","items":[{"menuItemId":1,"quantity":2},{"menuItemId":2,"quantity":1}]}' | jq
```

```bash
curl -N "localhost:8080/api/orders/1/status-stream?access_token=$TOKEN"
```

Sample stream output (recorded from a real run):

```text
data:{"orderId":2,"status":"COURIER_ASSIGNED","latitude":null,"longitude":null,"at":"…"}
data:{"orderId":2,"status":"PICKED_UP","latitude":12.9716,"longitude":77.5946,"at":"…"}
data:{"orderId":2,"status":"IN_TRANSIT","latitude":12.9746,"longitude":77.5992,"at":"…"}
…
data:{"orderId":2,"status":"DELIVERED","latitude":13.0019,"longitude":77.6407,"at":"…"}
```

## Knobs to play with

| Variable / property | Effect |
|---|---|
| `PAYMENT_FAILURE_RATE=0.8` (payment-service) | watch retries, then the circuit breaker open, orders become `PAYMENT_FAILED` |
| `simulation.tick` / `simulation.steps` (delivery-service) | speed and length of courier trips |
| Re-send the same `Idempotency-Key` | same order returned, no second charge |
| Item from another restaurant in the cart | `422` problem detail from order-service |

## Ports

| Service | Port | Health |
|---|---|---|
| api-gateway | 8080 | `/actuator/health` |
| restaurant-service | 8081 | `/actuator/health` |
| order-service | 8082 | `/actuator/health` |
| delivery-service | 8083 | `/actuator/health` |
| notification-service | 8084 | `/actuator/health` |
| payment-service | 8085 | `/actuator/health` |
| PostgreSQL / MongoDB / Kafka | 5432 / 27017 / 9092 | |
