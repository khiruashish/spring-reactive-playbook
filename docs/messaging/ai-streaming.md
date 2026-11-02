# Streaming AI Responses (Spring AI)

LLM responses arrive token by token — a natural `Flux<String>`. Spring AI's `ChatClient` exposes a
streaming API that plugs straight into WebFlux SSE.

```kotlin
implementation(platform("org.springframework.ai:spring-ai-bom:2.0.2"))
implementation("org.springframework.ai:spring-ai-starter-model-anthropic")   // or openai, ollama, …
```

```java
@RestController
class AssistantController {

    private final ChatClient chat;

    AssistantController(ChatClient.Builder builder) {
        this.chat = builder.defaultSystem("You are a concise shopping assistant.").build();
    }

    @GetMapping(path = "/assistant", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> ask(@RequestParam String question) {
        return chat.prompt()
                .user(question)
                .stream()
                .content()                                   // Flux<String> of tokens/chunks
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(e -> Flux.just("[assistant unavailable]"));
    }
}
```

## Why reactive matters here

- **Time to first token**: the user sees text after ~hundreds of ms instead of waiting for the full answer.
- **Cancellation**: if the browser closes the stream, the `Flux` is cancelled and the upstream model
  request is aborted — you stop paying for tokens nobody reads.
- **Concurrency**: thousands of long-lived streaming responses on a handful of event-loop threads.

## Patterns

| Need | Approach |
|---|---|
| tool/function calling | `.tools(...)` — tool methods can call reactive services (keep them non-blocking) |
| RAG | retrieve context (`Mono` from a vector store) → `flatMapMany` into the streaming prompt |
| rate limits | `limitRate`, a Resilience4j `RateLimiter`, and per-user quotas at the gateway |
| full answer as well | `.stream().chatResponse()` and `reduce`/`collectList` for post-processing |
