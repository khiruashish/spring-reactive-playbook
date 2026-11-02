# Security

Spring Security has a complete reactive implementation: a `WebFilter` chain, reactive authentication
managers, and a security context stored in the **Reactor Context** (no `ThreadLocal`).

| Page | Topics |
|---|---|
| [WebFlux Security](webflux-security.md) | `SecurityWebFilterChain`, users, password encoding, reading the principal |
| [OAuth2 & JWT](oauth2-jwt.md) | resource server, JWT validation, scopes → authorities, token relay with WebClient |
| [Method Security](method-security.md) | `@EnableReactiveMethodSecurity`, `@PreAuthorize` on `Mono`/`Flux` methods |
