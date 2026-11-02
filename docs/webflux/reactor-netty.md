# Reactor Netty Directly

WebFlux and WebClient sit on [Reactor Netty](https://projectreactor.io/docs/netty/release/reference/),
which you can also use directly for HTTP, TCP and UDP with backpressure.

## HTTP client with full control

```java
HttpClient client = HttpClient.create(ConnectionProvider.builder("api")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(20))
                .metrics(true)                          // reactor.netty.connection.provider.* metrics
                .build())
        .baseUrl("https://api.example.com")
        .compress(true)
        .responseTimeout(Duration.ofSeconds(3))
        .wiretap("api-client", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);   // dump traffic when debugging

Mono<String> body = client.get().uri("/status").responseContent().aggregate().asString();
```

Plug it into WebClient with `new ReactorClientHttpConnector(client)`.

## A tiny TCP server and client

```java
DisposableServer server = TcpServer.create()
        .port(9000)
        .handle((in, out) -> out.sendString(in.receive().asString().map(String::toUpperCase)))
        .bindNow();

Connection connection = TcpClient.create().port(9000).connectNow();
String reply = connection.outbound().sendString(Mono.just("hello")).then()
        .then(connection.inbound().receive().asString().next())
        .block(Duration.ofSeconds(2));              // "HELLO" (block only in a main/test)
connection.disposeNow();
server.disposeNow();
```

## When to drop down to Reactor Netty

- custom binary protocols over TCP/UDP
- proxies and tunnels with fine-grained connection handling
- performance work: wiretap, connection-pool metrics, event-loop configuration (`LoopResources`)

## Server customisation in Boot

```java
@Bean
NettyServerCustomizer nettyCustomizer() {
    return server -> server
            .idleTimeout(Duration.ofSeconds(60))
            .metrics(true, uri -> uri.replaceAll("/\\d+", "/{id}"))   // bounded URI tag cardinality
            .accessLog(true);                                         // -Dreactor.netty.http.server.accessLogEnabled
}
```
