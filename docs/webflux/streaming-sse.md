# Streaming & Server-Sent Events

Returning a `Flux` with a streaming media type writes each element **as soon as it is emitted** — no
buffering of the whole result.

| Media type | Format | Consumer |
|---|---|---|
| `text/event-stream` (SSE) | `id:`/`event:`/`data:` frames | browsers (`EventSource`), WebClient |
| `application/x-ndjson` | one JSON per line | services, CLI tools, WebClient |
| `application/json` | a JSON array (still streamed on the wire) | any client, but parsed at the end |

```java
--8<-- "playbook-examples/src/main/java/com/example/reactive/playbook/web/PriceStreamController.java:sse"
```

## Consuming SSE in a browser

```javascript
const source = new EventSource('/products/p1/prices');
source.addEventListener('price', e => console.log(JSON.parse(e.data)));
// EventSource reconnects automatically and sends Last-Event-ID — use the SSE id to resume.
```

## Testing a stream

```java
--8<-- "playbook-examples/src/test/java/com/example/reactive/playbook/web/WebFluxExamplesTest.java:sseTest"
```

## Operating streams in production

- **Heartbeats**: proxies close idle connections (often after 60 s). Merge a comment event every 15 s:
  `Flux.merge(updates, Flux.interval(ofSeconds(15)).map(i -> ServerSentEvent.builder().comment("ping").build()))`.
- **Termination**: complete the stream when the client no longer needs it (`takeUntil(...)`), so it
  doesn't hold a connection forever.
- **Slow clients**: decide on a backpressure strategy for hot sources (`onBackpressureLatest()`).
- **Scale-out**: an in-memory `Sinks.Many` only knows events from *this* instance. Feed each instance
  from Kafka (one consumer group per instance) or Redis pub/sub.
