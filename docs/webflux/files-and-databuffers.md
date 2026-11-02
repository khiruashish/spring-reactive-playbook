# Files, DataBuffers & Large Payloads

At the lowest level, request and response bodies are `Flux<DataBuffer>` — chunks of bytes, often pooled
Netty memory. Working with them directly lets you stream gigabytes in constant memory.

## Streaming a file download

```java
@GetMapping(path = "/reports/{name}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
Flux<DataBuffer> download(@PathVariable String name) {
    Path file = reportsDir.resolve(name).normalize();
    return DataBufferUtils.read(file, DefaultDataBufferFactory.sharedInstance, 64 * 1024);  // 64 KB chunks
}
```

Or return a `Resource` (`new FileSystemResource(file)`) and WebFlux uses zero-copy file transfer where
the transport supports it.

## Streaming an upload to disk

```java
@PostMapping(path = "/uploads", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
Mono<Void> upload(@RequestBody Flux<DataBuffer> body) {
    return DataBufferUtils.write(body, uploadsDir.resolve(UUID.randomUUID().toString()),
            StandardOpenOption.CREATE_NEW);
}
```

## Proxying a body without loading it

```java
Mono<Void> proxy(ServerHttpRequest request, ServerHttpResponse response) {
    Flux<DataBuffer> upstream = webClient.get().uri("/big").retrieve().bodyToFlux(DataBuffer.class);
    return response.writeWith(upstream);       // backpressure end to end, constant memory
}
```

## Memory rules

- Pooled buffers must be **released**: if you consume `DataBuffer`s yourself, call
  `DataBufferUtils.release(buffer)` (or use `DataBufferUtils.join` and release the result).
- `bodyToMono(String.class)` aggregates the whole body — bounded by `spring.codec.max-in-memory-size`.
- Detect leaks in tests: `-Dio.netty.leakDetection.level=paranoid`.

## Reading files without blocking

`java.nio` file APIs are blocking. `DataBufferUtils.read(Path, …)` uses `AsynchronousFileChannel`
internally; for other file operations (listing, metadata, `Files.readString`) wrap them:
`Mono.fromCallable(() -> Files.size(p)).subscribeOn(Schedulers.boundedElastic())`.
