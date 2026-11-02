# Codecs, CORS & Configuration

## Codecs

Codecs turn `DataBuffer`s into objects and back. Boot configures Jackson 3 JSON, SSE, NDJSON, strings,
resources, form data and multipart.

```java
@Configuration
class CodecConfig implements WebFluxConfigurer {
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024);   // DataBufferLimitException above this
        configurer.defaultCodecs().enableLoggingRequestDetails(false);
    }
}
```

Or simply `spring.codec.max-in-memory-size=2MB`. Customise Jackson through `JsonMapperBuilderCustomizer`
beans (Jackson 3) rather than replacing the mapper.

## CORS

```java
@Configuration
class CorsConfig implements WebFluxConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://shop.example.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

With Spring Security, also enable `http.cors(Customizer.withDefaults())` so preflight requests are not
rejected before reaching the mapping. For functional endpoints use a `CorsWebFilter` bean.

## Multipart uploads (streaming)

```java
@PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
Mono<String> upload(@RequestPart("file") FilePart file) {
    Path target = Path.of("/tmp/uploads", file.filename());
    return file.transferTo(target).thenReturn("stored " + file.filename());  // non-blocking write
}
```

## Static resources

Files under `src/main/resources/static` are served at `/`. Configure caching with
`spring.web.resources.cache.cachecontrol.max-age`.

## Server tuning (Reactor Netty)

```yaml
server:
  netty:
    connection-timeout: 5s
    idle-timeout: 60s
    max-keep-alive-requests: 1000
  http2:
    enabled: true
```

For deeper control register a `NettyServerCustomizer`. Event-loop thread count defaults to the number
of CPUs (`-Dreactor.netty.ioWorkerCount=N` to override). See
[Performance Tuning](../production/performance-tuning.md).
